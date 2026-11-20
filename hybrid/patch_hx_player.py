#!/usr/bin/env python3
"""HX TV 0.9.15 Glass Studio: botões translúcidos e sugestões completas, sem recortes.
Conserva IDs, ações, atalhos, barras de progresso e comportamento existente.
"""
from pathlib import Path
import argparse
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
    # O tamanho anterior dos botões herdava os círculos antigos de Leanback.
    # Substituir os próprios layouts mantém uma só fonte de geometria/touch target.
    recursos=root/"leanback-1.0.0/src/main/res/layout"
    for arquivo,botao,dimen in (
        ("lb_control_button_primary.xml","54dp","lb_control_button_diameter"),
        ("lb_control_button_secondary.xml","50dp","lb_control_button_secondary_diameter"),
    ):
        controle=recursos/arquivo
        substituir(controle,
            f'android:layout_width="@dimen/{dimen}"',
            f'android:layout_width="{botao}"')
        altura="lb_control_button_height" if "primary" in arquivo else "lb_control_button_secondary_height"
        substituir(controle,
            f'android:layout_height="@dimen/{altura}"',
            f'android:layout_height="{botao}"')
        substituir(controle,
            'android:layout_width="@dimen/lb_control_icon_width"',
            'android:layout_width="26dp"')
        substituir(controle,
            'android:layout_height="@dimen/lb_control_icon_height"',
            'android:layout_height="26dp"')
        ET.parse(controle)

    classe=base/"java/com/liskovsoft/smartyoutubetv2/tv/ui/mod/leanback/playerglue/tooltips/ControlButtonPresenterSelector.java"
    substituir(classe,
        "import android.text.TextUtils;",
        """import android.text.TextUtils;
import android.graphics.drawable.GradientDrawable;
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
            // HX Signature: superfície vidro grafite e borda gelo
            // no foco, sem modificar a ação de cada botão. Mantém os ícones,
            // acessibilidade, cliques e todos os atalhos do player original.
            float density = parent.getResources().getDisplayMetrics().density;
            int radius = Math.round(16 * density);
            int stroke = Math.max(1, Math.round(1f * density));
            GradientDrawable focus = new GradientDrawable(
                    GradientDrawable.Orientation.TL_BR,
                    new int[]{0xC47F93A8, 0xA43B526C});
            focus.setCornerRadius(radius);
            focus.setStroke(stroke, 0xFFF6FAFF);
            GradientDrawable idle = new GradientDrawable(
                    GradientDrawable.Orientation.TL_BR,
                    new int[]{0x763B4A5F, 0x6A192536});
            idle.setCornerRadius(radius);
            idle.setStroke(Math.max(1, Math.round(density)), 0x547889A0);
            StateListDrawable states = new StateListDrawable();
            states.addState(new int[] {android.R.attr.state_focused}, focus);
            states.addState(new int[] {android.R.attr.state_pressed}, focus);
            states.addState(new int[] {}, idle);
            if (holder.mFocusableView != null) {
                holder.mFocusableView.setBackground(states);
                // Remove o círculo legado por trás do novo material de vidro.
                if (holder.mFocusableView instanceof ImageView)
                    ((ImageView) holder.mFocusableView).setImageDrawable(null);
                // Microtransição nativa curta, sem dependências React e
                // sem atrasar o foco, o clique nem o controle remoto.
                if (android.os.Build.VERSION.SDK_INT >= 21) {
                    StateListAnimator motion = new StateListAnimator();
                    AnimatorSet selected = new AnimatorSet();
                    selected.playTogether(
                        ObjectAnimator.ofFloat(holder.mFocusableView, "scaleX", 1.0f, 1.045f),
                        ObjectAnimator.ofFloat(holder.mFocusableView, "scaleY", 1.0f, 1.045f),
                        ObjectAnimator.ofFloat(holder.mFocusableView, "alpha", 0.92f, 1.0f),
                        ObjectAnimator.ofFloat(holder.mFocusableView, "translationZ", 0f, 2f * density));
                    selected.setDuration(150);
                    AnimatorSet normal = new AnimatorSet();
                    normal.playTogether(
                        ObjectAnimator.ofFloat(holder.mFocusableView, "scaleX", 1.045f, 1.0f),
                        ObjectAnimator.ofFloat(holder.mFocusableView, "scaleY", 1.045f, 1.0f),
                        ObjectAnimator.ofFloat(holder.mFocusableView, "alpha", 1.0f, 0.92f),
                        ObjectAnimator.ofFloat(holder.mFocusableView, "translationZ", 2f * density, 0f));
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
                        Math.round(190f * d), Math.round(107f * d));
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
            aviso.setTextColor(0xFFF1F6FD);
            aviso.setTextSize(12);
            aviso.setGravity(Gravity.CENTER);
            aviso.setPadding(22, 10, 22, 10);
            GradientDrawable fundo = new GradientDrawable();
            fundo.setColor(0xDB101A26);
            fundo.setCornerRadius(16f * getResources().getDisplayMetrics().density);
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
        // O recorte de 46% escondia as capas acima da borda, deixando SÓ TÍTULOS.
        // A grade volta a ter altura integral; Leanback posiciona a seleção
        // na faixa inferior, mantendo o vídeo visível atrás da camada translúcida.
        FrameLayout.LayoutParams params = (FrameLayout.LayoutParams) dock.getLayoutParams();
        if (params.height != ViewGroup.LayoutParams.MATCH_PARENT) {
            params.height = ViewGroup.LayoutParams.MATCH_PARENT;
            params.gravity = Gravity.BOTTOM;
            dock.setLayoutParams(params);
        }
        if (dock instanceof ViewGroup) {
            ((ViewGroup) dock).setClipChildren(false);
            ((ViewGroup) dock).setClipToPadding(false);
        }
        if (mRowsSupportFragment != null &&
                mRowsSupportFragment.getVerticalGridView() != null) {
            androidx.leanback.widget.VerticalGridView grid =
                    mRowsSupportFragment.getVerticalGridView();
            grid.setWindowAlignment(androidx.leanback.widget.BaseGridView.WINDOW_ALIGN_NO_EDGE);
            grid.setWindowAlignmentOffsetPercent(compact ? 70f : 86f);
            grid.setItemAlignmentOffsetPercent(50f);
        }
    }
    private void updatePlayerBackground() {
        if (isOverlayShown()) {
            boolean compact = isSuggestionsShown();
            setBackgroundResource(compact ? R.drawable.player_background_suggestions :
                    R.drawable.player_background_controls);
            hxResizeSuggestionsViewport(compact);
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

    # Camadas de vidro nativas ao redor das duas fileiras do SmartTube.
    # Preserva os IDs e listeners originais, incluindo ações de qualidade/live.
    controls=base/"res/layout/lb_playback_transport_controls_row.xml"
    substituir(controls,
        'android:id="@+id/controls_dock"\n                android:layout_width="wrap_content"',
        'android:id="@+id/controls_dock"\n                android:background="@drawable/hx_player_primary_rail"\n                android:padding="3dp"\n                android:layout_width="wrap_content"')
    substituir(controls,
        'android:id="@+id/secondary_controls_dock"\n                android:layout_width="wrap_content"',
        'android:id="@+id/secondary_controls_dock"\n                android:background="@drawable/hx_player_secondary_rail"\n                android:padding="2dp"\n                android:layout_marginTop="4dp"\n                android:layout_width="wrap_content"')
    ET.parse(controls)
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
    for arquivo,basecolor,borda in (
        ("hx_player_primary_rail.xml","#663E5268","#687A94AD"),
        ("hx_player_secondary_rail.xml","#4A344657","#506C8198"),
    ):
        d=base/"res/drawable"/arquivo
        d.write_text(f'''<?xml version="1.0" encoding="utf-8"?>
<shape xmlns:android="http://schemas.android.com/apk/res/android" android:shape="rectangle">
    <solid android:color="{basecolor}"/>
    <corners android:radius="19dp"/>
    <stroke android:color="{borda}" android:width="1dp"/>
</shape>
''',encoding="utf-8")
        ET.parse(d)

    gradient=base/"res/drawable/player_background_controls.xml"
    gradient.write_text("""<?xml version="1.0" encoding="utf-8"?>
<!-- HX TV: degradê OLED na base. Só aparece junto dos controles nativos. -->
<shape xmlns:android="http://schemas.android.com/apk/res/android">
    <gradient android:angle="90"
        android:startColor="#B80B101B"
        android:centerColor="#90121C2C"
        android:centerY="0.35"
        android:endColor="#00121C2A" />
</shape>
""",encoding="utf-8")
    ET.parse(gradient)
    # Backdrop mais leve para que o vídeo permaneça visível atrás das sugestões.
    suggestions=base/"res/drawable/player_background_suggestions.xml"
    suggestions.write_text("""<?xml version="1.0" encoding="utf-8"?>
<shape xmlns:android="http://schemas.android.com/apk/res/android">
    <gradient android:angle="90"
        android:startColor="#C0121B2B"
        android:centerColor="#76152030"
        android:centerY="0.50"
        android:endColor="#00152030" />
</shape>
""",encoding="utf-8")
    ET.parse(suggestions)
    print("HX PLAYER GLASS STUDIO: thumbnails 190x107dp, full-height Leanback grid, translucent buttons")

if __name__=="__main__":
    main()
