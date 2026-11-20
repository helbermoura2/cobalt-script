# HX TV 0.9.7 — Premium Motion

## Objetivo (feedback nas 4 fotos da TCL P7K, 20/11/2026)
A imagem da 0.9.6 tinha cards retrato estáticos, thumbnails duplicadas dentro dos cards, opções do player visualmente volumosas e sugestões excessivamente grandes. A referência enviada anteriormente mostra cartões estilo story que **expandem depois de 1 segundo de foco sustentado**, não por simples foco instantâneo.

## Implementação
1. Home: cards reais do YouTube nascem com 158dp de largura e 248dp de altura; fundo de imagem ocupa a área inteira, sem a miniatura menor repetida ao centro.
2. Uma única tarefa por foco espera 1.100ms; se o controle se mover antes, ela é cancelada.
3. Após o tempo, o card abre de 158dp para 338dp em 300ms com desaceleração; a capa horizontal passa a ser exibida amplamente, o título aumenta e os metadados secundários aparecem.
4. Ao perder foco, o card fecha em 200ms, preservando o item selecionado; não deixa animações pendentes ao trocar de seção ou sair da tela.
5. A fileira pode se deslocar para mostrar a borda direita do card aberto, evitando que ele seja cortado na TV.
6. O menu começa recolhido e expande quando o foco retorna às categorias. Pesquisa, Atualizar e Modo clássico permanecem.
7. PlaybackFragment usa instância própria de VideoCardPresenter somente para sugestões com thumbnails 164×92dp (16:9) e títulos de duas linhas. A home não é reduzida.
8. Fundo do overlay de sugestões suavizado, mantendo o vídeo mais visível; controles com vidro fosco mais fino, cantos de 13dp, contornos e movimentação mais leves.
9. Player original intacto quanto a comandos, codecs, 4K, live, legendas, velocidade, qualidade, conta e histórico; biblioteca de 28 assuntos, buscas de conteúdo e atualização interna preservadas.
10. Mesmo nome de pacote e certificado privado da 0.9.6, versionCode 2485. OTA beta ativado somente após build, assinatura, inspeção, download e comparação do arquivo publicado.

## Roteiro de teste
- No Início, mover o foco rapidamente por várias capas: **nenhuma deve expandir** antes de 1.100ms.
- Parar 1–2 segundos em uma capa: abre a prévia horizontal, mostrando apenas uma imagem. Navegar para a seguinte: a anterior fecha, a nova abre após o tempo. Sem perda do seletor.
- Abrir video de um story expandido, voltar para a home: a posição de foco deve ser recuperada.
- Conferir card no fim da fileira (horizontal scroll) e dentro das linhas adicionais dos assuntos.
- Testar Home, Esportes, Histórico, AO VIVO, Inscrições, Notícias e Música.
- Ao reproduzir, abrir as sugestões: cards devem ser **menores que os da home**, 16:9, sem ocupar a maior parte do vídeo.
- Verificar os botões de legenda, velocidade, qualidade, rewind, avançar, repetir, inscrever, curtir e opções.
- Atualizar pela própria TV sem desinstalar; 0.9.6 permanece como backup manual.
- Validar fluidez e uso de memória da TCL P7K em vídeo normal e live.

**Estado:** requer build de validação e teste visual real na TV. Não confundir compilação bem-sucedida com aprovação da experiência em dispositivo.
