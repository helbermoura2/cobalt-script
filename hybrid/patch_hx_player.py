#!/usr/bin/env python3
"""HX TV: acabamento discreto dos controles nativos do player.
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
import android.graphics.drawable.StateListDrawable;""")
    substituir(classe,
        """            View v = LayoutInflater.from(parent.getContext())
                    .inflate(mLayoutResourceId, parent, false);
            return new ActionViewHolder(v);""",
        """            View v = LayoutInflater.from(parent.getContext())
                    .inflate(mLayoutResourceId, parent, false);
            ActionViewHolder holder = new ActionViewHolder(v);
            // HX: fundo OLED quando o botão está solto e bronze discreto
            // quando ele recebe foco pelo controle remoto. Mantém os ícones,
            // acessibilidade, cliques e todos os atalhos do player original.
            float density = parent.getResources().getDisplayMetrics().density;
            int radius = Math.round(16 * density);
            int stroke = Math.max(1, Math.round(1.5f * density));
            GradientDrawable focus = new GradientDrawable();
            focus.setColor(0xF0372A1E);
            focus.setCornerRadius(radius);
            focus.setStroke(stroke, 0xFFD9AF78);
            GradientDrawable idle = new GradientDrawable();
            idle.setColor(0xCC11151B);
            idle.setCornerRadius(radius);
            StateListDrawable states = new StateListDrawable();
            states.addState(new int[] {android.R.attr.state_focused}, focus);
            states.addState(new int[] {android.R.attr.state_pressed}, focus);
            states.addState(new int[] {}, idle);
            if (holder.mFocusableView != null) {
                holder.mFocusableView.setBackground(states);
            }
            return holder;""")
    gradient=base/"res/drawable/player_background_controls.xml"
    gradient.write_text("""<?xml version="1.0" encoding="utf-8"?>
<!-- HX TV: degradê OLED na base. Só aparece junto dos controles nativos. -->
<shape xmlns:android="http://schemas.android.com/apk/res/android">
    <gradient android:angle="90"
        android:startColor="#F2070A0F"
        android:centerColor="#DC111820"
        android:centerY="0.12"
        android:endColor="#00111820" />
</shape>
""",encoding="utf-8")
    ET.parse(gradient)
    print("HX PLAYER: botões bronze no foco, fundo OLED suave; comandos preservados")

if __name__=="__main__":
    main()
