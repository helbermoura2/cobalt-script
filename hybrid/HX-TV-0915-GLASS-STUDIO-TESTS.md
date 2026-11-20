# HX TV 0.9.15 — Glass Studio

## Origem: feedback específico do usuário na TCL P7K

A 0.9.14 foi considerada visualmente insuficiente, com seis problemas:
1. Títulos esmagados nos cards.
2. Sugestões durante reprodução cortadas: apareciam textos mas não imagens.
3. Inscrições exibidas em carrosséis horizontais; necessidade de feed vertical.
4. Botões do player com aparência antiga e pouco sofisticada.
5. Menu desalinhado e três opções finais (Pesquisa, Atualizar, Modo Clássico) presas fora do scroll.
6. Ausência de transparências, vidro e acabamento Premium VIP.

## Implementação — mudanças estruturais

- **Cards**: altura de 218dp na Home; 226dp nas demais fileiras. Miniaturas continuam 16:9 (205×115/225×123). O título usa até três linhas, espaçamento vertical real e área com peso para não ficar comprimido pelo rodapé. Ao focar um vídeo, o cabeçalho usa marquee para oferecer leitura do título completo sem expandir/reformatar os cards.
- **Sugestões**: a antiga limitação `Math.max(260dp, 46% da tela)` do contêiner de sugestões estava recortando a parte superior das imagens. Restaura a altura integral do contêiner e `clipChildren=false`, define keyline da grade Leanback em 70% quando selecionada e aumenta miniaturas para 190×107dp. Isso prioriza mostrar as capas inteiras; não promete que toda a área de reprodução permanecerá livre de sobreposição.
- **Inscrições**: renderizador próprio `desenharInscricoesEmLista` ativado pelo título da seção (Inscrições/Subscriptions). Cada vídeo é uma linha vertical, com capa à esquerda, três linhas de título à direita e segunda linha de metadados. Direcional CIMA/BAIXO percorre os vídeos; ESQUERDA volta ao menu; OK e clique longo preservam o comportamento original. Carregamento incremental.
- **Menu unificado**: todos os itens de navegação (inclusive Pesquisa, Atualizar e Modo Clássico) estão no mesmo `ScrollView`. Larguras do trilho 186dp aberto/68dp recolhido, com ícones nativos, centralização e bordas de foco coerentes. O botão de recolher permanece junto ao cabeçalho, mas as três opções antes fixas rolam com o catálogo.
- **Vidro premium**: superfícies translúcidas em grafite/prata com gradiente e contorno suave, sem depender de blur custoso para TCL. Botões do player receberam novas dimensões de toque na biblioteca Leanback (54dp/50dp), ícones em 26dp, e removem a arte legada por trás do novo background. Foco e animação menores, mas perceptíveis.
- **Compatibilidade**: mantém o ExoPlayer original, busca de vídeos, AO VIVO com filtros, PiP, histórico, legendas, qualidade, conta, configurações e atualizador. V0.9.15/code2493; assinatura permanente da 0.9.14, canal OTA beta ativado apenas se APK ARM e assinatura passarem e o arquivo da release for conferido.

## Referências
Taste Skill (princípios de acabamento), Transitions.dev (transições pequenas que não mudam a geometria), React Bits (referência de microinteração). Não foram inseridos componentes web ou React num aplicativo Android nativo.

## Testes manuais prioritários TCL P7K
1. Home, AO VIVO, Games: verificar título com 2–3 linhas que não esmaga mais. Focar vídeo de título longo e verificar título completo rolante no topo.
2. Durante reprodução abrir Sugestões e navegar para 1ª/2ª/3ª fileira: **as capas precisam aparecer por inteiro**, não apenas os títulos. Voltar aos controles.
3. Inscrições: deve ser uma lista que cresce VERTICALMENTE, com capa de vídeo 16:9 à esquerda e detalhes à direita. Verificar CIMA/BAIXO/ESQUERDA/OK.
4. Expandir/recolher menu: ícones em posições uniformes, caixas translúcidas, sem perder foco. Rolar até baixo: Pesquisa, Atualizar, Modo Clássico seguem na mesma rolagem.
5. Testar Atualizar na última opção e verificar que informa esta versão, sem desinstalar.
6. PiP ativo: verificar foco e itens clicáveis sem sobreposição problemática; voltar ao player.
7. Testar legendas, qualidade/4K quando fonte disponibiliza, áudio, velocidade, avançar, fila, histórico e Live.
8. Verificar regressões de scroll e alinhamento após mais vídeos carregarem e após 10 trocas de seção.

## Critério de liberação
Build e verificadores estáticos não bastam para declarar que o design está aprovado. O pacote só deve ser oferecido pela atualização interna após compilação, assinatura, release e comparação binária verificadas; aprovação visual depende da TCL real.
