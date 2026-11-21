#!/usr/bin/env python3
"""Static regression assertions for HX TV 0.9.17 Glass Polish (no extra dependencies)."""
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
  "suggestions viewport": "hxResizeSuggestionsViewport(boolean compact)" in player and "screenHeight * 0.68f" in player and "setClipChildren(false)" in player and "setWindowAlignmentOffsetPercent" not in player,
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
  "consistent app version": 'VERSION_NAME = "0.9.17"' in native and "VERSION_CODE = 2495" in native,
  "manifest next version": manifest.get("0.9.17",{}).get("versionCode")==2495,
  "signature hash gate": "expected='1e929c67bf65dd36a8875595f882089f036b4d8c68700a6f4fd70fb23efd86d7'" in workflow,
  "OTA only after release check": "cmp out/HX-TV-Native-0.9.17-assinado-arm.apk test-release-final/HX-TV-Native-0.9.17-assinado-arm.apk" in workflow,
  "glass panels and buttons": "private GradientDrawable vidro(int cor, int borda, int raio)" in home and "StateListDrawable states" in player,
  "duration from source only": "video.getDurationMs()" in home and "duracao<=0" in home,
  "live and upcoming have no minutes": "video.isLive||video.isUpcoming" in home,
  "hour and minute duration supported": '"%d:%02d:%02d"' in home and '"%d:%02d"' in home,
  "duration in both layouts": home.count("adicionarDuracao(capa,video);")==2,
  "subtitle cards size preserved": "ALTURA_CAPA_HOME=218" in home and "dp(cartaz?LARGURA_CAPA_HOME:225),dp(cartaz?ALTURA_CAPA_HOME:226)" in home,
  "translucent navigation": "new int[]{0xAF465E77,0x7B23374E,0x97101729}" in home,
  "live glass chips": "chip.setBackground(vidro(" in home,
  "glass player coloring only": "new int[]{0xAA7390AB, 0x72415A74}" in player and "new int[]{0x71506C86, 0x3D23384E}" in player,
  "same DPAD and button dimensions": "screenHeight * 0.68f" in player and "Math.round(146f * d)" in player and "setImageDrawable(null)" not in player and "setWindowAlignmentOffsetPercent" not in player,

  "unified scrolling actions": "menu.addView(acao,lp)" in home and "lateral.addView(pesquisa,new" not in home and "lateral.addView(atualizar,new" not in home,
  "nav icon alignment": "private void posicionarNav(TextView alvo,String rotulo)" in home and "alvo.setCompoundDrawables(null,icone,null,null)" in home,
  "titles not crushed": "ALTURA_CAPA_HOME=218" in home and "nomeCartao.setMaxLines(3)" in home and "tituloSelecionado.setText(video.getTitle())" in home,
  "subscriptions vertical": "private void desenharInscricoesEmLista" in home and "coluna.setOrientation(LinearLayout.VERTICAL)" in home,
  "subscriptions remote": "if(key==KeyEvent.KEYCODE_DPAD_DOWN && pos+1<lista.getChildCount())" in home,
  "suggestions images unclipped": "screenHeight * 0.68f" in player and "setClipChildren(false)" in player,
  "native click targets restored": 'recursos=root/"leanback-1.0.0/src/main/res/layout"' not in player and "setImageDrawable(null)" not in player,
  "critical player rollback": "setWindowAlignmentOffsetPercent" not in player and "setImageDrawable(null)" not in player,
  "keep selection row logic": "setPlayerRowIndex(1);" in player and "setPlayerRowIndex(0);" in player,
  "focus events intact": "holder.mFocusableView.setBackground(states)" in player and "mFocusableView.setStateListAnimator(motion)" in player,
  "Java balanced braces":home.count("{")==home.count("}"),
}
for desc,success in checks.items():
    print(("PASS" if success else "FAIL"),desc)
if not all(checks.values()):
    sys.exit(1)
print(f"HX TV 0.9.17: {len(checks)} regression gates passed")
