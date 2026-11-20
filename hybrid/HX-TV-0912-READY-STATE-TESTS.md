# HX TV 0.9.12 — hotfix do aviso de carregamento

## Evidências no vídeo real da 0.9.11
Vídeo: YouCut_20261120_184510433.mp4, duração 66,43s, TCL P7K.
- 0–14s: Home e menu funcionando.
- 14–34s: catálogo amplo, embora com algumas fileiras pouco preenchidas.
- 34,5–41s: carregamento antes do vídeo (cerca de 6s nesta gravação), com indicação PREPARANDO VÍDEO visível.
- 41–51s: gameplay reproduz normalmente, mas PREPARANDO VÍDEO **continua na tela**.
- 52–66s: o PiP da reprodução continua ativo na Home, porém a mensagem é vista até dentro da mini-janela. A faixa reservada ao PiP funciona; pode ser mais compacta.
- As Sugestões são mostradas como ação no player, mas a gravação não demonstra o painel expandido; não afirmar validação desse comportamento.

## Causa de código identificada
A lógica da 0.9.11 ocultava o aviso no estado ENDED ou READY **somente se** mHxFirstFrame tivesse sido atualizado pela callback onRenderedFirstFrame. Em alguns aparelhos/fluxos o vídeo já estava rodando e o flag não era atualizado, fazendo o aviso permanecer visível.

## Correção
1. Ocultar no estado `Player.STATE_READY` **independentemente** de `mHxFirstFrame`; continuar ocultando no primeiro quadro quando callback existir.
2. Cancelar timers anteriores com `mHxLoadingTicket` e validar o estado atual antes de mostrar, impedindo overlays atrasados na reprodução já pronta.
3. Ao abrir outro vídeo, limpar o aviso antigo e iniciar nova medição. Animar apenas a entrada com fade de 160ms, mantendo a saída imediata e segura.
4. Reduzir altura da faixa superior reservada ao PiP de 126dp para 108dp; ajustar textos e padding.
5. Preservar o SmartTube ExoPlayer original, painel de sugestões, menu, live e atualizador. Certificado e packageId idênticos aos da 0.9.11.

## Testar na TCL P7K
- Abrir transmissão que demore mais de 1,7s. PREPARANDO VÍDEO deve aparecer, e DESAPARECER assim que a reprodução estiver pronta.
- Manter o vídeo por 15–30 segundos: não pode restar nenhuma etiqueta sobre o jogo ou notícia.
- Voltar à Home com PiP: mini-janela deve exibir somente o vídeo, sem aviso PREPARANDO; faixa reservada não deve cobrir cards.
- Trocar vídeos e tentar live e gravado: cada aviso é independente, nunca fica preso ao anterior.
- Abrir Sugestões e percorrer várias fileiras: ainda precisa de validação visual real separada.
- Testar controles, 4K, áudio, legendas, qualidade, Histórico, AO VIVO, atualizar sobre a versão anterior.

**Importante:** build assinado não comprova que o atraso de conexão foi reduzido; a mudança é de feedback e limpeza do estado visual.
