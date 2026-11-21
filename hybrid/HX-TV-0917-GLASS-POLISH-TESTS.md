# HX TV 0.9.17 — Glass Polish e minutagem real

## Escopo aprovado e divisão em duas fases
**Primeiro:** efeitos de vidro/transparência nos elementos do HX TV, inclusive botões do player, e minutagem nos vídeos.
**Depois, em outra versão:** disposição das fileiras, proporções, quantidade de cards e formatos. Esta fase NÃO altera tamanhos, conteúdo nem orientação dos cards.

## O que muda na 0.9.17
1. **Superfícies nativas translúcidas:** reaproveita `vidro(cor,borda,raio)` com alfa real, gradiente grafite prateado e borda fria. Refinou menu lateral, cards, hero, faixas e botões de ações, área de PiP, filtros AO VIVO, estados vazios e skeletons. O fundo ambiente permanece OLED, com um leve gradiente que dá profundidade às superfícies sobrepostas. Não usa WebView nem efeito de desfoque em tempo real que sobrecarregue a TCL P7K.
2. **Minutagem verdadeira:** `video.getDurationMs()` da biblioteca SmartTube, convertida para `mm:ss` ou `h:mm:ss` e exibida no canto inferior direito da capa em pequenos indicadores de vidro. Aparece nas fileiras da Home e nas capas das Inscrições verticais. Não inventar duração se ausente; nunca colocar duração fixa em `video.isLive` ou `video.isUpcoming`.
3. **Player visual apenas:** os botões mantêm o exato foco, imagens nativas, dimensões e ações que funcionaram novamente na 0.9.16. Alteradas somente as cores de fundo com transparência e bordas, os fundos das duas fileiras de controle e gradientes de overlay/tooltip. Não modificar `setWindowAlignmentOffsetPercent`, `setImageDrawable(null)`, `lb_control_button_primary.xml`, ou outras alterações que danificaram o player na 0.9.15.
4. **Selos e filtros:** AO VIVO e EM BREVE com vidro colorido, contrastando sobre a miniatura sem mascará-la. Elementos focados têm borda visível, importante para o controle remoto.
5. **Preserva** busca, 28 temas, filtros AO VIVO, menu único rolável, Histórico, Inscrições verticais, PiP e mecanismos de atualização. Mesma assinatura do APK instalado, versionName 0.9.17 / versionCode 2495.
6. **OTA só depois de release validada:** build ARM, análise do appId e de assinatura, publicação e download do APK idêntico ao original antes de atualizar `native-beta.json`.

## Testes na TCL P7K
- Navegar menu expandido/recolhido: observar transparência e foco claro em todos os botões; verificar rolagem de Pesquisa/Atualizar/Modo Clássico.
- Ver Home e feeds de Inscrições: duração real no rodapé direito da imagem nos vídeos gravados; duração acima de 1h usa formato `h:mm:ss`.
- Ver AO VIVO e EM BREVE: sem duração falsa, mas com etiquetas legíveis. Vídeos sem metadado de duração também não recebem etiqueta falsa.
- Player: qualidade, pausa, avanço/retrocesso, legendas, sugestões e seleção de outro vídeo funcionam como na 0.9.16. Isso tem prioridade sobre o acabamento.
- PiP e Home: superfície de reserva translúcida e navegação por DPAD preservada.
- Vídeos com imagem clara e escura: verificar se os controles continuam legíveis sobre ambos; sem brilho excessivo.
- Testar recursos 4K quando o vídeo disponibilizar, áudio e histórico.
- Validar atualização pelo botão no próprio aplicativo sem desinstalar.

## Importante
Esta implementação usa **transparência alfa e gradientes de vidro**, não desfoque gaussiano real. No Android TV, blur de fundo vivo pode consumir GPU e diminuir a fluidez. Se o resultado visual parecer insuficiente, a próxima avaliação pode estudar efeitos de blur com fallback, desde que o player permaneça intacto.

## Fase futura — layout dos cards (não implementar agora)
Explorar proporções 16:9, retrato/poster, listas densas por gênero, fileiras agrupadas e modo de foco, baseados no review real. Analisar antes de alterar para evitar mudanças destrutivas. Referências: Taste Skill, Transitions.dev e React Bits (somente princípios visuais, sem inserir React na TV).
