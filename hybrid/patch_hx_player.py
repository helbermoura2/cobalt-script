#!/usr/bin/env python3
"""HX TV: controles individuais Black Glass com foco neon.
Conserva IDs, ações e atalhos do player.
"""
from pathlib import Path
import argparse
import re
import xml.etree.ElementTree as ET

def substituir(arquivo,antes,depois):
    s=arquivo.read_text(encoding="utf-8")
    n=s.count(antes)
    if n!=1:
        raise RuntimeError(f"Marcador mudou: {arquivo} ({n})")
    arquivo.write_text(s.replace(antes,depois,1),encoding="utf-8")

def main():
    args=argparse.ArgumentParser()
    args.add_argument("upstream",type=Path)
    root=args.parse_args().upstream.resolve()

    base=root/"smarttubetv/src/main"
    widgets=base/"java/com/liskovsoft/smartyoutubetv2/tv/ui/widgets"
    fonte=Path(__file__).resolve().parent/"native-src/ContornoNeon.java"
    (widgets/fonte.name).write_bytes(fonte.read_bytes())
    classe=base/"java/com/liskovsoft/smartyoutubetv2/tv/ui/mod/leanback/playerglue/tooltips/ControlButtonPresenterSelector.java"
    substituir(classe,
        "import android.text.TextUtils;",
        """import android.text.TextUtils;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.LayerDrawable;
import com.liskovsoft.smartyoutubetv2.tv.ui.widgets.ContornoNeon;
import android.graphics.drawable.StateListDrawable;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.animation.StateListAnimator;""")
    substituir(classe,
        """            View v = LayoutInflater.from(parent.getContext())
                    .inflate(mLayoutResourceId, parent, false);
            return new ActionViewHolder(v);""",
        """            View v = LayoutInflater.from(parent.getContext())
                    .inflate(mLayoutResourceId, parent, false);
            ActionViewHolder holder = new ActionViewHolder(v);
            // O ícone continua na camada original acima da superfície de vidro.
            float density = parent.getResources().getDisplayMetrics().density;
            int radius = Math.round(10 * density);
            int stroke = Math.max(1, Math.round(1f * density));
            GradientDrawable focus = new GradientDrawable(
                    GradientDrawable.Orientation.TL_BR,
                    new int[]{0xFA162632, 0xFD03080D});
            focus.setCornerRadius(radius);
            GradientDrawable idle = new GradientDrawable(
                    GradientDrawable.Orientation.TL_BR,
                    new int[]{0xF42A2D32, 0xFA07090C});
            idle.setCornerRadius(radius);
            idle.setStroke(stroke, 0xAABAC3CB);
            if (holder.mFocusableView != null) {
                StateListDrawable states = new StateListDrawable();
                states.addState(new int[] {android.R.attr.state_focused},
                        new LayerDrawable(new Drawable[]{focus,
                                new ContornoNeon(holder.mFocusableView, radius, true, true)}));
                states.addState(new int[] {}, idle);
                holder.mFocusableView.setBackground(states);
                // Microtransição nativa curta, sem dependências React e
                // sem atrasar o foco, o clique nem o controle remoto.
                if (android.os.Build.VERSION.SDK_INT >= 21) {
                    StateListAnimator motion = new StateListAnimator();
                    AnimatorSet selected = new AnimatorSet();
                    selected.playTogether(
                        ObjectAnimator.ofFloat(holder.mFocusableView, "scaleX", 1.0f, 1.035f),
                        ObjectAnimator.ofFloat(holder.mFocusableView, "scaleY", 1.0f, 1.035f),
                        ObjectAnimator.ofFloat(holder.mFocusableView, "translationZ", 0f, 8f * density));
                    selected.setDuration(140);
                    AnimatorSet normal = new AnimatorSet();
                    normal.playTogether(
                        ObjectAnimator.ofFloat(holder.mFocusableView, "scaleX", 1.035f, 1.0f),
                        ObjectAnimator.ofFloat(holder.mFocusableView, "scaleY", 1.035f, 1.0f),
                        ObjectAnimator.ofFloat(holder.mFocusableView, "translationZ", 8f * density, 0f));
                    normal.setDuration(130);
                    motion.addState(new int[] {android.R.attr.state_focused}, selected);
                    motion.addState(new int[] {}, normal);
                    holder.mFocusableView.setStateListAnimator(motion);
                }
            }
            return holder;""")
    # Recomendacoes do player: dimensão própria, sem mudar cards da home.
    # Mantém foco/ações do player e ainda usa imagens reais do Youtube.
    playback=base/"java/com/liskovsoft/smartyoutubetv2/tv/ui/playback/PlaybackFragment.java"
    substituir(playback,
        "        mCardPresenter = new VideoCardPresenter();",
        """        mCardPresenter = new VideoCardPresenter() {
            @Override
            protected android.util.Pair<Integer, Integer> getCardDimensPx(
                    android.content.Context context) {
                float d = context.getResources().getDisplayMetrics().density;
                return new android.util.Pair<>(
                        Math.round(146f * d), Math.round(82f * d));
            }
            @Override
            protected boolean isCardMultilineTitleEnabled(
                    android.content.Context context) {
                return true;
            }
            @Override
            protected boolean isCardMultilineSubtitleEnabled(
                    android.content.Context context) {
                return false;
            }
        };""")

    # Telemetria de tempo até o primeiro frame e estado legível em buffering.
    # Não altera URLs, codecs, requisições nem o mecanismo original de reprodução.
    substituir(playback,
        "import android.view.ViewGroup;",
        """import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.TextView;
import android.view.Gravity;
import android.graphics.drawable.GradientDrawable;
import android.os.SystemClock;""")
    substituir(playback,
        "    private SimpleExoPlayer mPlayer;",
        """    private SimpleExoPlayer mPlayer;
    private TextView mHxBufferStatus;
    private long mHxVideoOpenedAt;
    private boolean mHxFirstFrame;
    private int mHxLoadingTicket;
    private void hxHideLoading() {
        mHxFirstFrame = true;
        ++mHxLoadingTicket;
        if (mHxBufferStatus != null) {
            mHxBufferStatus.animate().cancel();
            mHxBufferStatus.setVisibility(View.GONE);
            mHxBufferStatus.setAlpha(1f);
        }
    }
    private void hxShowLoadingLater() {
        final TextView aviso = mHxBufferStatus;
        final int ticket = ++mHxLoadingTicket;
        if (aviso == null) return;
        aviso.postDelayed(() -> {
            if (ticket != mHxLoadingTicket || mHxBufferStatus != aviso
                    || mHxFirstFrame || !isAdded()) return;
            if (mPlayer != null && mPlayer.getPlaybackState() == Player.STATE_READY)
                return;
            aviso.animate().cancel();
            aviso.setText("PREPARANDO VÍDEO...");
            if (aviso.getVisibility() != View.VISIBLE) {
                aviso.setAlpha(0f);
                aviso.setVisibility(View.VISIBLE);
                aviso.animate().alpha(1f).setDuration(160).start();
            }
        }, 1700);
    }""")
    substituir(playback,
        "        ProgressBarManager.setup(getProgressBarManager(), (ViewGroup) root);",
        """        ProgressBarManager.setup(getProgressBarManager(), (ViewGroup) root);
        // Sem tela preta muda: informar ao usuário sobre buffering prolongado.
        if (root instanceof FrameLayout) {
            TextView aviso = new TextView(getContext());
            aviso.setText("PREPARANDO REPRODUÇÃO...");
            aviso.setTextColor(0xFFF5F5F5);
            aviso.setTextSize(12);
            aviso.setGravity(Gravity.CENTER);
            aviso.setPadding(22, 10, 22, 10);
            GradientDrawable fundo = new GradientDrawable();
            fundo.setColor(0xAB121212);
            fundo.setCornerRadius(16f * getResources().getDisplayMetrics().density);
            fundo.setStroke(Math.max(1, Math.round(getResources().getDisplayMetrics().density)),0xAFC7C7C7);
            aviso.setBackground(fundo);
            aviso.setFocusable(false);
            aviso.setVisibility(View.GONE);
            FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(
                    FrameLayout.LayoutParams.WRAP_CONTENT,
                    FrameLayout.LayoutParams.WRAP_CONTENT,
                    Gravity.BOTTOM | Gravity.CENTER_HORIZONTAL);
            params.bottomMargin = Math.round(75f * getResources().getDisplayMetrics().density);
            ((FrameLayout) root).addView(aviso, params);
            mHxBufferStatus = aviso;
            mHxFirstFrame = false;
            hxShowLoadingLater();
        }""")
    substituir(playback,
        "    public void setVideo(Video video) {\n        mExoPlayerController.setVideo(video);",
        """    public void setVideo(Video video) {
        mHxVideoOpenedAt = SystemClock.elapsedRealtime();
        mHxFirstFrame = false;
        if (mHxBufferStatus != null) {
            mHxBufferStatus.animate().cancel();
            mHxBufferStatus.setVisibility(View.GONE);
        }
        hxShowLoadingLater();
        mExoPlayerController.setVideo(video);""")
    substituir(playback,
        "        mExoPlayerController.setPlayer(mPlayer);",
        """        mExoPlayerController.setPlayer(mPlayer);
        mHxVideoOpenedAt = SystemClock.elapsedRealtime();
        mPlayer.addListener(new Player.EventListener() {
            @Override
            public void onPlayerStateChanged(boolean playWhenReady, int state) {
                final TextView aviso = mHxBufferStatus;
                if (aviso == null) return;
                if (state == Player.STATE_READY || state == Player.STATE_ENDED) {
                    // READY chega mesmo em aparelhos que não notificam primeiro frame.
                    hxHideLoading();
                } else if (state == Player.STATE_BUFFERING && !mHxFirstFrame) {
                    final int ticket = mHxLoadingTicket;
                    aviso.postDelayed(() -> {
                        if (ticket != mHxLoadingTicket || mHxFirstFrame
                                || mHxBufferStatus != aviso || mPlayer == null
                                || mPlayer.getPlaybackState() != Player.STATE_BUFFERING)
                            return;
                        aviso.animate().cancel();
                        aviso.setText("CARREGANDO VÍDEO...");
                        aviso.setAlpha(0f);
                        aviso.setVisibility(View.VISIBLE);
                        aviso.animate().alpha(1f).setDuration(160).start();
                    }, 1800);
                }
            }
        });
        mPlayer.addVideoListener(new com.google.android.exoplayer2.video.VideoListener() {
            @Override
            public void onRenderedFirstFrame() {
                long tempo = SystemClock.elapsedRealtime() - mHxVideoOpenedAt;
                Log.d(TAG, "HX first frame after " + tempo + " ms");
                if (mHxBufferStatus != null)
                    mHxBufferStatus.post(() -> hxHideLoading());
                else hxHideLoading();
            }
        });""")

    # O painel de sugestões tem altura própria, sem ocupar a tela inteira.
    # Playback permanece full-screen; apenas a área de recomendações é limitada.
    substituir(playback,
        """    private void updatePlayerBackground() {
        if (isOverlayShown()) {
            setBackgroundResource(isSuggestionsShown() ? R.drawable.player_background_suggestions : R.drawable.player_background_controls);
        }
    }""",
        """    private void hxResizeSuggestionsViewport(boolean compact) {
        View root = getView();
        if (root == null) return;
        View dock = root.findViewById(R.id.playback_controls_dock);
        if (dock == null || !(dock.getLayoutParams() instanceof FrameLayout.LayoutParams)) return;
        FrameLayout.LayoutParams params = (FrameLayout.LayoutParams) dock.getLayoutParams();
        float density = getResources().getDisplayMetrics().density;
        int screenHeight = getResources().getDisplayMetrics().heightPixels;
        // Usar mais altura SEM modificar o posicionamento da grade Leanback:
        // a 0.9.15 mudou a keyline e os controles perderam a navegação.
        // Esta correção conserva a geometria de foco funcional da 0.9.14.
        int target = compact ? Math.max(Math.round(340f * density),
                    Math.round(screenHeight * 0.68f))
                : ViewGroup.LayoutParams.MATCH_PARENT;
        if (params.height != target) {
            params.height = target;
            params.gravity = Gravity.BOTTOM;
            dock.setLayoutParams(params);
        }
        if (dock instanceof ViewGroup) {
            ((ViewGroup) dock).setClipChildren(false);
            ((ViewGroup) dock).setClipToPadding(false);
        }
    }
    private void updatePlayerBackground() {
        if (isOverlayShown()) {
            boolean compact = isSuggestionsShown();
            setBackgroundResource(compact ? R.drawable.player_background_suggestions :
                    R.drawable.player_background_controls);
            hxResizeSuggestionsViewport(compact);
        } else {
            setBackgroundResource(android.R.color.transparent);
        }
    }""")
    substituir(playback,
        """        if (show && !isSuggestionsShown() && !isSuggestionsEmpty()) {
            setPlayerRowIndex(1);
        }""",
        """        if (show && !isSuggestionsShown() && !isSuggestionsEmpty()) {
            setPlayerRowIndex(1);
            hxResizeSuggestionsViewport(true);
        }""")
    substituir(playback,
        """        showOverlay(show);

        setPlayerRowIndex(0);
    }""",
        """        showOverlay(show);

        setPlayerRowIndex(0);
        hxResizeSuggestionsViewport(false);
    }""")

    # Nunca criar uma capsula externa ao redor de cada fileira: ela criava
    # o efeito de botoes dentro de outro botao maior. Preservar o layout
    # nativo de controles, foco, clique, geometria e espaco de toque.
    controls=base/"res/layout/lb_playback_transport_controls_row.xml"
    xmlOriginal=controls.read_text(encoding="utf-8")
    # O SmartTube usa dois controles_dock em variantes diferentes do layout,
    # enquanto secondary_controls_dock tem apenas uma ocorrencia.
    # Validar presenca e nao unicidade para respeitar o XML original.
    for idControle in ("controls_dock","secondary_controls_dock"):
        if xmlOriginal.count(f'android:id="@+id/{idControle}"')<1:
            raise RuntimeError(f"Dock original ausente: {idControle}")
    if "hx_player_primary_rail" in xmlOriginal or "hx_player_secondary_rail" in xmlOriginal:
        raise RuntimeError("Capsula externa nao pode voltar")
    ET.parse(controls)
    for variante in ("primary", "secondary"):
        layout=root/f"leanback-1.0.0/src/main/res/layout/lb_control_button_{variante}.xml"
        substituir(layout, f'\n        android:src="@drawable/lb_control_button_{variante}"', '')
        # A largura vem da fileira; o botão preenche a célula e o ícone
        # mantém seu tamanho original no centro.
        sufixo="" if variante=="primary" else "_secondary"
        substituir(layout, f'android:layout_width="@dimen/lb_control_button{sufixo}_diameter"',
                   'android:layout_width="match_parent"')
        ET.parse(layout)
    presenter=base/"java/com/liskovsoft/smartyoutubetv2/tv/ui/mod/leanback/playerglue/tweaks/PlaybackTransportRowPresenter.java"
    substituir(presenter, "        TypedValue outValue = new TypedValue();\n        if (context.getTheme()\n                .resolveAttribute(R.attr.playbackProgressPrimaryColor, outValue, true)) {\n            return context.getResources().getColor(outValue.resourceId);\n        }\n        return context.getResources().getColor(R.color.lb_playback_progress_color_no_theme);",
               "        return 0xFF42DEFF;")
    # Posicionar a linha do tempo acima das duas fileiras com o mesmo SeekBar nativo.
    busca=re.search(r'        <[^\n]*SeekBar\s+android:id="@\+id/playback_progress"[\s\S]*? />\n\n', xmlOriginal)
    if busca is None:
        raise RuntimeError("Barra de progresso nativa ausente")
    barra=busca.group()
    xml=xmlOriginal.replace(barra, "", 1)
    ancora="        <!-- Original top player row -->"
    if xml.count(ancora)!=1:
        raise RuntimeError("Posição da primeira fileira alterada")
    controls.write_text(xml.replace(ancora, barra+ancora, 1), encoding="utf-8")
    # Tirar os metadados da largura reservada aos botões. Os mesmos IDs são
    # usados pelo presenter para atualização e para o modo de busca no vídeo.
    xml=controls.read_text(encoding="utf-8")
    metadados=[]
    for id_info, id_dock in (("additional_info","controls_dock"),("time_info","secondary_controls_dock")):
        busca=re.search(
            rf'        <RelativeLayout android:layout_width="match_parent"\n(?:(?!^        </RelativeLayout>).)*?'
            rf'            <RelativeLayout android:id="@\+id/{id_info}"(?:(?!^        </RelativeLayout>).)*?'
            r'^        </RelativeLayout>\n', xml, re.S|re.M)
        if not busca:
            raise RuntimeError(f"Fileira nativa ausente: {id_dock}")
        trecho=busca.group()
        inicio=trecho.index(f'            <RelativeLayout android:id="@+id/{id_info}"')
        fim=trecho.rindex('        </RelativeLayout>')
        info=trecho[inicio:fim].replace('android:layout_centerVertical="true"', 'android:layout_gravity="center_vertical"')
        info=info.replace('                            android:layout_gravity="bottom"\n','')
        if id_info=="additional_info":
            info=info.replace('android:layout_alignParentEnd="true"', 'android:layout_alignParentStart="true"')
        metadados.append(info)
        margem=' android:layout_marginTop="5dp"' if id_dock=="secondary_controls_dock" else ' android:layout_marginTop="18dp"'
        dock=f"""        <FrameLayout android:id="@+id/{id_dock}"
            android:layout_width="match_parent"
            android:layout_height="wrap_content"
            android:layoutDirection="ltr"{margem} />
"""
        xml=xml.replace(trecho,dock,1)
    faixa='        <RelativeLayout android:layout_width="match_parent" android:layout_height="wrap_content">\n'
    faixa+=''.join(metadados)+'        </RelativeLayout>\n\n'
    xml=xml.replace(barra,faixa+barra,1)
    controls.write_text(xml,encoding="utf-8")

    diretório=presenter.parent
    barra_controles=diretório/"ControlBar.java"
    substituir(barra_controles,"    private int mChildMarginFromCenter;",
        """    private int mChildMarginFromCenter;
    private boolean mHxDistributed;

    void setHxDistributed() {
        mHxDistributed = true;
        setGravity(android.view.Gravity.CENTER_HORIZONTAL);
    }""")
    substituir(barra_controles,
        "        super.onMeasure(widthMeasureSpec, heightMeasureSpec);",
        """        int count = getChildCount();
        if (mHxDistributed && count > 0 && MeasureSpec.getMode(widthMeasureSpec) != MeasureSpec.UNSPECIFIED) {
            int width = MeasureSpec.getSize(widthMeasureSpec);
            int available = Math.max(0, width - getPaddingLeft() - getPaddingRight());
            float density = getResources().getDisplayMetrics().density;
            int gap = count > 1 ? Math.min(Math.round(6 * density), available / (count * 5)) : 0;
            int usable = Math.max(0, available - gap * (count - 1));
            int maxTile = Math.round(82 * density);
            for (int i = 0; i < count; i++) {
                LayoutParams params = (LayoutParams) getChildAt(i).getLayoutParams();
                params.width = Math.min(maxTile, usable * (i + 1) / count - usable * i / count);
                params.setMarginStart(i == 0 ? 0 : gap);
            }
            super.onMeasure(MeasureSpec.makeMeasureSpec(width, MeasureSpec.EXACTLY), heightMeasureSpec);
            return;
        }
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);""")
    substituir(diretório/"ControlBarPresenter.java","static final int MAX_CONTROLS = 12;",
               "static final int MAX_CONTROLS = 16;")
    for holder,dock in (("mControlsVh","mControlsDock"),("mSecondaryControlsVh","mSecondaryControlsDock")):
        substituir(presenter, f"        vh.{dock}.addView(vh.{holder}.view);",
                   f"        vh.{holder}.mControlBar.setHxDistributed();\n"
                   f"        vh.{dock}.addView(vh.{holder}.view);")
    tooltip=classe.with_name("TooltipPopup.java")
    substituir(tooltip,
        """        if (fromTouch) {
            if (yAbove >= 0) {""",
        """        View controls = appView.findViewById(R.id.controls_dock);
        if (controls != null && controls.isShown()) {
            controls.getLocationOnScreen(mTmpAnchorPos);
            int baseline = mTmpAnchorPos[1] - mTmpAppPos[1];
            outParams.y = Math.max(0, baseline - tooltipHeight - tooltipOffset);
            return;
        }
        if (fromTouch) {
            if (yAbove >= 0) {""")
    # Horário e codec: informações auxiliares e secundárias, não chamadas principais.
    xml=controls.read_text(encoding="utf-8")
    for identificador in ("quality_info","date_time","ending_time",
                          "current_time","separate_time","total_time"):
        marcador=f'android:id="@+id/{identificador}"'
        if xml.count(marcador)!=1:
            raise RuntimeError(f"Controle de tempo ausente: {identificador}")
        xml=xml.replace(marcador,marcador+
            '\n                    android:textSize="12sp"'+
            '\n                    android:alpha="0.82"',1)
    controls.write_text(xml,encoding="utf-8")
    ET.parse(controls)
    # Proporção de PiP é definida pela activity; o posicionamento final é do Android.
    atividade=base/"java/com/liskovsoft/smartyoutubetv2/tv/ui/playback/PlaybackActivity.java"
    substituir(atividade,"import android.os.Build;",
        "import android.os.Build;\nimport android.util.Rational;")
    substituir(atividade,
        "PictureInPictureParams.Builder params = new PictureInPictureParams.Builder();",
        """PictureInPictureParams.Builder params = new PictureInPictureParams.Builder();
                        params.setAspectRatio(new Rational(16,9));""")
    # Compartilhar apenas o estado booleano do PiP com a Home nativa.
    # A posição da janela continua sob controle do sistema Android TV.
    substituir(atividade,
        "    private static final String TAG = PlaybackActivity.class.getSimpleName();",
        """    private static final String TAG = PlaybackActivity.class.getSimpleName();
    private static volatile boolean hxMiniPlayerActive = false;
    public interface HxPipObserver { void onPipStateChanged(boolean active); }
    private static java.lang.ref.WeakReference<HxPipObserver> hxPipObserver;
    public static void setHxPipObserver(HxPipObserver observer) {
        hxPipObserver = observer == null ? null :
                new java.lang.ref.WeakReference<>(observer);
        if (observer != null) observer.onPipStateChanged(hxMiniPlayerActive);
    }
    private static void notifyHxPiP() {
        HxPipObserver observer = hxPipObserver == null ? null : hxPipObserver.get();
        if (observer != null) observer.onPipStateChanged(hxMiniPlayerActive);
    }
    public static boolean isHxMiniPlayerActive() {
        return hxMiniPlayerActive;
    }""")
    substituir(atividade,
        "        super.onPictureInPictureModeChanged(isInPictureInPictureMode);",
        """        super.onPictureInPictureModeChanged(isInPictureInPictureMode);
        hxMiniPlayerActive = isInPictureInPictureMode;
        notifyHxPiP();""")
    substituir(atividade,
        "    @Override\n    public void onPictureInPictureModeChanged(boolean isInPictureInPictureMode) {",
        """    @Override
    protected void onDestroy() {
        hxMiniPlayerActive = false;
        notifyHxPiP();
        super.onDestroy();
    }

    @Override
    public void onPictureInPictureModeChanged(boolean isInPictureInPictureMode) {""")
    # Nenhuma barra externa de controles e gerada. Apenas os botoes
    # individuais recebem vidro neutro por StateListDrawable.
    gradient=base/"res/drawable/player_background_controls.xml"
    gradient.write_text("""<?xml version="1.0" encoding="utf-8"?>
<!-- HX TV: degradê OLED na base. Só aparece junto dos controles nativos. -->
<shape xmlns:android="http://schemas.android.com/apk/res/android">
    <gradient android:angle="90"
        android:startColor="#A6000000"
        android:centerColor="#4D000000"
        android:centerY="0.22"
        android:endColor="#00000000" />
</shape>
""",encoding="utf-8")
    ET.parse(gradient)
    # Backdrop mais leve para que o vídeo permaneça visível atrás das sugestões.
    suggestions=base/"res/drawable/player_background_suggestions.xml"
    suggestions.write_text("""<?xml version="1.0" encoding="utf-8"?>
<shape xmlns:android="http://schemas.android.com/apk/res/android">
    <gradient android:angle="90"
        android:startColor="#99000000"
        android:centerColor="#38000000"
        android:centerY="0.44"
        android:endColor="#08000000" />
</shape>
""",encoding="utf-8")
    ET.parse(suggestions)
    print("HX PLAYER: individual glass controls with animated cyan focus")

if __name__=="__main__":
    main()
