#!/usr/bin/env python3
"""HX TV Studio: controles nativos refinados, sugestões discretas e PiP 16:9.
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
            int radius = Math.round(11 * density);
            int stroke = Math.max(1, Math.round(1f * density));
            GradientDrawable focus = new GradientDrawable(
                    GradientDrawable.Orientation.TL_BR,
                    new int[]{0xE94A5B6B, 0xF02B3644});
            focus.setCornerRadius(radius);
            focus.setStroke(stroke, 0xFFF0F5FD);
            GradientDrawable idle = new GradientDrawable(
                    GradientDrawable.Orientation.TL_BR,
                    new int[]{0xB625303C, 0xC6161E29});
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
                    selected.setDuration(160);
                    AnimatorSet normal = new AnimatorSet();
                    normal.playTogether(
                        ObjectAnimator.ofFloat(holder.mFocusableView, "scaleX", 1.035f, 1.0f),
                        ObjectAnimator.ofFloat(holder.mFocusableView, "scaleY", 1.035f, 1.0f),
                        ObjectAnimator.ofFloat(holder.mFocusableView, "alpha", 1.0f, 0.86f),
                        ObjectAnimator.ofFloat(holder.mFocusableView, "translationZ", 8f * density, 0f));
                    normal.setDuration(150);
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
                        Math.round(168f * d), Math.round(95f * d));
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
    print("HX PLAYER HORIZON: controles flutuantes e sugestões 154x87dp, funções preservadas")

if __name__=="__main__":
    main()
