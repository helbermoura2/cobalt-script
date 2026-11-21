# HX TV 0.9.18 — Black Glass / OLED

## Contexto
O review visual da 0.9.17 apontou cinco problemas:
1. Layout azul em vez de preto OLED.
2. Transparência imperceptível, parecendo painéis opacos.
3. Botões do player com fundos azuis.
4. Botões do player parecem contidos dentro de outro botão comprido.
5. Cards com miniaturas quadradas, mesmo tendo fundos arredondados.

## Implementação
- **Tema preto neutro**: cor base OLED `#000000`, fundo do conteúdo em gradiente de pretos (`#101010`, `#050505`, preto), menu lateral em preto/cinza com alfa. Texto branco/prata, sem acentos azuis na Home.
- **Vidro com transparência real**: gradiente nativo com alfa `0x26FFFFFF`, centro configurável semitransparente, base `0x20000000`, contornos cinza. É transparência real de superfícies nativas, **não** desfoque gaussiano do conteúdo por trás. Sem blur GPU em tempo real; preservar fluidez da TCL P7K.
- **Tema SmartTube prata/preto**: migrar os recursos fastlane, leanback, search, card default, ícones e focos de ciano/azul para neutros. Telas que ainda usam o tema Android não devem ganhar cor azul da versão anterior.
- **Player sem dupla cápsula**: não adicionar `android:background="@drawable/hx_player_primary_rail"` nem equivalente na dock secundária; não criar arquivos `hx_player_*_rail.xml`. Os botões individuais continuam com seus ícones, hit targets, ouvintes, foco e dimensão original, e recebem apenas estados visuais de vidro preto/prata via StateListDrawable.
- **Miniaturas realmente arredondadas**: `setClipToOutline(true)` no card externo e no FrameLayout das capas, com `GradientDrawable` arredondado de raio 14dp para a capa da Home. Contornos de foco de borda desenhados com `setForeground`, **acima** das imagens. Também recortar as capas no feed vertical das Inscrições. O hero mantém clipping que já funcionava.
- **Preservado**: duração real nos vídeos gravados, formatos e disposição atuais, PiP, busca, filtros AO VIVO, player e sugestões, atualização interna e conta.
- Versão **0.9.18 / versionCode 2496**, pacote `br.com.hxnative.tvpreview`, assinatura privada persistente. O manifesto beta não muda até build, release, assinatura e comparação binária.

## Testes importantes na TCL P7K
1. Home, menu expandido e recolhido: verificar cores pretas, brilho das bordas prata, contraste e foco sem dominante azul.
2. Nas capas reais, verificar cantos **visivelmente** arredondados; selecionar e desfocar o card, garantindo que a linha fina de foco apareça acima da imagem.
3. Inscrições verticais: mesma verificação do recorte e clicabilidade de linhas.
4. Player: verificar que só existem botões individuais, sem uma cápsula longa envolvendo cada fileira, e que eles não são azuis.
5. **Obrigatório**: pausa/play, avançar/retroceder, escolher qualidade, selecionar vídeo em Sugestões, sair por BACK com PiP e retomar reprodução.
6. Conferir legendas, áudio, qualidade/4K (se disponível), live e histórico.
7. Verificar sobre vídeo com fundos claros e escuros: contraste dos controles, leitura de rótulos e duração.
8. Conferir atualização 0.9.17 → 0.9.18 pelo próprio aplicativo sem desinstalação.

## Limites
O efeito produzido é *glass* nativo por alfa e borda, sem blur de fundo em tempo real. Em fundo preto liso a transparência por si só não evidencia elementos atrás. Não confundir compilação/assinatura concluídas com validação visual e do D-pad na TCL. Manter 0.9.17 como backup até aprovação.
