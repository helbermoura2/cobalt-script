#!/usr/bin/env python3
"""Static regression assertions for HX TV 0.9.14 Stable Shelves (no extra dependencies)."""
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
  "PiP state lifecycle": "isHxMiniPlayerActive()" in player and "hxMiniPlayerActive = false" in player,
  "PiP push observer": "setHxPipObserver(HxPipObserver observer)" in player and "notifyHxPiP()" in player and "setHxPipObserver(hxPipObserver)" in home,
  "home safe dock": "private void atualizarReservaPiP()" in home and "reservaPiP.setVisibility" in home,
  "buffering message": "CARREGANDO VÍDEO..." in player and "STATE_BUFFERING" in player,
  "startup early loading": "hxShowLoadingLater()" in player and "1700" in player,
  "cancel loading when ready": "private void hxHideLoading()" in player and "state == Player.STATE_READY" in player,
  "no stale timer after playback ready": "mPlayer.getPlaybackState() == Player.STATE_READY" in player and "ticket != mHxLoadingTicket" in player,
  "PiP dock more compact": "new LinearLayout.LayoutParams(-1,dp(76))" in home,
  "suggestions viewport": "hxResizeSuggestionsViewport(boolean compact)" in player and "screenHeight * 0.46f" in player,
  "first frame telemetry": "onRenderedFirstFrame()" in player and "SystemClock.elapsedRealtime()" in player,
  "discovery pacing": "long intervalo=estaNaHome()?900L:580L" in home and "carregarProximosAssuntos(2)" in home,
  "compact player suggestions": "Math.round(146f * d), Math.round(82f * d)" in player,
  "visible blank section state": "private void mostrarCarregamentoSecao()" in home and "Nenhum vídeo recebido nesta seção." in home,
  "retry no-results section": "TENTAR NOVAMENTE" in home and "abrirSecao(secaoAtual)" in home,
  "clear on arriving content": "limparEstadoSecao();" in home and "if(principal&&videoDestaque==null)" in home,
  "replace state with empty group": "VideoGroup.ACTION_REPLACE){" in home and "mostrarCarregamentoSecao();" in home,
  "dark branded cover fallback": 'texto("▶  HX TV",13' in home and ".error(android.R.color.transparent)" in home,
  "layout-consistent skeletons": "esqueletosSecao=new HorizontalScrollView" in home and "for(int i=0;i<4;i++)" in home,
  "noninteractive skeleton cards": "esqueletosSecao.setFocusable(false)" in home and "sem capas ou vídeos fictícios" in home,
  "hide skeleton when source empty": "esqueletosSecao.setVisibility(View.GONE)" in home and "TENTAR NOVAMENTE" in home,
  "batched refresh": "private final List<VideoGroup> loteAtualizacao" in home and "postDelayed(finalizarLoteAtualizacao,300)" in home,
  "stale refresh cancelled": "loteAtualizacao.clear();" in home and "if(geracao!=geracaoPagina||categoria!=secaoAtual)" in home,
  "consistent app version": 'VERSION_NAME = "0.9.14"' in native and "VERSION_CODE = 2492" in native,
  "manifest next version": manifest.get("0.9.14",{}).get("versionCode")==2492,
  "signature hash gate": "expected='1e929c67bf65dd36a8875595f882089f036b4d8c68700a6f4fd70fb23efd86d7'" in workflow,
  "OTA only after release check": "cmp out/HX-TV-Native-0.9.14-assinado-arm.apk test-release-final/HX-TV-Native-0.9.14-assinado-arm.apk" in workflow,
  "Java balanced braces":home.count("{")==home.count("}"),
}
for desc,success in checks.items():
    print(("PASS" if success else "FAIL"),desc)
if not all(checks.values()):
    sys.exit(1)
print(f"HX TV 0.9.14: {len(checks)} regression gates passed")
