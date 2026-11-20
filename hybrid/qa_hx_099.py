#!/usr/bin/env python3
"""Static regression assertions for HX TV 0.9.9 Studio (no extra dependencies)."""
from pathlib import Path
import json,re,sys

root=Path(__file__).resolve().parent
home=(root/"native-src/HxHomeScreen.java").read_text(encoding="utf-8")
player=(root/"patch_hx_player.py").read_text(encoding="utf-8")
native=(root/"patch_smarttube.py").read_text(encoding="utf-8")
workflow=(root.parent/".github/workflows/hx-smarttube-native.yml").read_text(encoding="utf-8")
manifest=json.loads((root/"updates/native-beta-next.json").read_text(encoding="utf-8"))

checks={
  "no story expansion": "focoStory(" not in home and "DEMORA_EXPANSAO_MS" not in home,
  "native home and controls": "class HxHomeScreen extends FrameLayout" in home and "dispatchKeyEvent(KeyEvent evento)" in home,
  "per-card remote navigation": "cartao.setOnKeyListener" in home and "KEYCODE_DPAD_LEFT" in home,
  "deduplicated recommendations": "semDuplicarNaHome" in home and "idsNaTela.put(chave,atuais)" in home,
  "strict live validation": "video.isLive&&!video.isLiveEnd" in home and "SearchOptions.FEATURE_LIVE" in home,
  "actual topic filter": "filtroLiveSelecionado=indice" in home and "atualizarFileiraLive()" in home,
  "manual live refresh": "private void iniciarBuscaLives()" in home and "rodada==rodadaLives" in home,
  "stable live row focus": "faixaAnterior.hasFocus()" in home and "livesPendentes" in home,
  "hero artwork debounce": "agendarDestaque(video)" in home and "postDelayed(destaquePendente,170)" in home,
  "safe player actions": "ControlButtonPresenterSelector.java" in player and "secondary_controls_dock" in player,
  "native PiP aspect ratio": "params.setAspectRatio(new Rational(16,9))" in player,
  "compact player suggestions": "Math.round(168f * d), Math.round(95f * d)" in player,
  "consistent app version": 'VERSION_NAME = "0.9.9"' in native and "VERSION_CODE = 2487" in native,
  "manifest next version": manifest.get("0.9.9",{}).get("versionCode")==2487,
  "signature hash gate": "expected='1e929c67bf65dd36a8875595f882089f036b4d8c68700a6f4fd70fb23efd86d7'" in workflow,
  "OTA only after release check": "cmp out/HX-TV-Native-0.9.9-assinado-arm.apk test-release-final/HX-TV-Native-0.9.9-assinado-arm.apk" in workflow,
  "Java balanced braces":home.count("{")==home.count("}"),
}
for desc,success in checks.items():
    print(("PASS" if success else "FAIL"),desc)
if not all(checks.values()):
    sys.exit(1)
print(f"HX TV Studio: {len(checks)} regression gates passed")
