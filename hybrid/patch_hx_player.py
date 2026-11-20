#!/usr/bin/env python3
"""HX TV 0.9.11: PiP responsivo, carregamento desde o clique e painel de sugestões.
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
            int radius = Math.round(12 * density);
            int stroke = Math.max(1, Math.round(1f * density));
            GradientDrawable focus = new GradientDrawable(
                    GradientDrawable.Orientation.TL_BR,
                    new int[]{0xEB475D72, 0xF0203142});
            focus.setCornerRadius(radius);
            focus.setStroke(stroke, 0xFFF0F5FD);
            GradientDrawable idle = new GradientDrawable(
                    GradientDrawable.Orientation.TL_BR,
                    new int[]{0xAC202B38, 0xC1121D2B});
            idle.setCornerRadius(radius);
            idle.setStroke(Math.max(1, Math.round(density)), 0x246A7686);
            StateListDrawable states = new StateListDrawable();
            states.addState(new int[] {android.R.attr.state_focused}, focus);
            states.addState(new int[] {android.R.attr.state_pressed}, focus);
            states.addState(new int[] {}, idle);
            if (holder.mFocusableView != null) {
                holder.mFocusableView.setBackground(states);
                // Microtransição nativa curta, sem dependências React e
                // sem atrasar o foco, o clique nem o controle remoto.
                if (android.os.Build.VERSION.SDK_INT >= 21) {
                    StateListAnimator motion = new StateListAnimator();
                    AnimatorSet selected = new AnimatorSet();
                    selected.playTogether(
                        ObjectAnimator.ofFloat(holder.mFocusableView, "scaleX", 1.0f, 1.035f),
                        ObjectAnimator.ofFloat(holder.mFocusableView, "scaleY", 1.0f, 1.035f),
                        ObjectAnimator.ofFloat(holder.mFocusableView, "alpha", 0.86f, 1.0f),
                        ObjectAnimator.ofFloat(holder.mFocusableView, "translationZ", 0f, 8f * density));
                    selected.setDuration(140);
                    AnimatorSet normal = new AnimatorSet();
                    normal.playTogether(
                        ObjectAnimator.ofFloat(holder.mFocusableView, "scaleX", 1.035f, 1.0f),
                        ObjectAnimator.ofFloat(holder.mFocusableView, "scaleY", 1.035f, 1.0f),
                        ObjectAnimator.ofFloat(holder.mFocusableView, "alpha", 1.0f, 0.86f),
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
    private long mHxVideoOpenedAt;""")
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
        }""")
    substituir(playback,
        "    public void setVideo(Video video) {\n        mExoPlayerController.setVideo(video);",
        """    public void setVideo(Video video) {
        mHxVideoOpenedAt = SystemClock.elapsedRealtime();
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
                if (state == Player.STATE_BUFFERING) {
                    aviso.postDelayed(() -> {
                        if (mHxBufferStatus == aviso && mPlayer != null
                                && mPlayer.getPlaybackState() == Player.STATE_BUFFERING) {
                            aviso.setText("CARREGANDO VÍDEO...  •  CONEXÃO OU FONTE");
                            aviso.setVisibility(View.VISIBLE);
                        }
                    }, 1800);
                } else {
                    aviso.setVisibility(View.GONE);
                }
            }
        });
        mPlayer.addVideoListener(new com.google.android.exoplayer2.video.VideoListener() {
            @Override
            public void onRenderedFirstFrame() {
                long tempo = SystemClock.elapsedRealtime() - mHxVideoOpenedAt;
                Log.d(TAG, "HX first frame after " + tempo + " ms");
                if (mHxBufferStatus != null)
                    mHxBufferStatus.post(() -> mHxBufferStatus.setVisibility(View.GONE));
            }
        });""")

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
        ("hx_player_primary_rail.xml","#69161E27","#33677383"),
        ("hx_player_secondary_rail.xml","#41151D26","#23677383"),
    ):
        d=base/"res/drawable"/arquivo
        d.write_text(f'''<?xml version="1.0" encoding="utf-8"?>
<shape xmlns:android="http://schemas.android.com/apk/res/android" android:shape="rectangle">
    <solid android:color="{basecolor}"/>
    <corners android:radius="15dp"/>
    <stroke android:color="{borda}" android:width="1dp"/>
</shape>
''',encoding="utf-8")
        ET.parse(d)

    gradient=base/"res/drawable/player_background_controls.xml"
    gradient.write_text("""<?xml version="1.0" encoding="utf-8"?>
<!-- HX TV: degradê OLED na base. Só aparece junto dos controles nativos. -->
<shape xmlns:android="http://schemas.android.com/apk/res/android">
    <gradient android:angle="90"
        android:startColor="#CD090E16"
        android:centerColor="#A50A121F"
        android:centerY="0.30"
        android:endColor="#00121C2A" />
</shape>
""",encoding="utf-8")
    ET.parse(gradient)
    # Backdrop mais leve para que o vídeo permaneça visível atrás das sugestões.
    suggestions=base/"res/drawable/player_background_suggestions.xml"
    suggestions.write_text("""<?xml version="1.0" encoding="utf-8"?>
<shape xmlns:android="http://schemas.android.com/apk/res/android">
    <gradient android:angle="90"
        android:startColor="#D80B121D"
        android:centerColor="#AB0C1420"
        android:centerY="0.42"
        android:endColor="#190C1420" />
</shape>
""",encoding="utf-8")
    ET.parse(suggestions)
    print("HX PLAYER 0.9.10: sugestões 146x82dp, buffering legível, telemetria e estado PiP")

if __name__=="__main__":
    main()
