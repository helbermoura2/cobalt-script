# HX TV 0.9.8 — Horizon & Live Hub (20/11/2026)

## Por que este redesign
O vídeo de 11 segundos e as três fotografias da TCL P7K mostraram que a 0.9.7 tem um card em expansão grande demais, cabeçalho alto com miniplayer flutuante, corte de conteúdo e poucos resultados ao vivo evidentes. O efeito de story foi **reprovado** pelo usuário.

## Mudanças desta versão
- **Story removido totalmente.** Não há animações com atraso de 1.100ms, expansão variável de largura nem alteração brusca das fileiras. Card HOME fixo em 205×164dp, imagem 16:9 (205×115dp), título e canal/visualizações, miniaturas reais, foco com contorno sutil e microescala de 1,8%.
- **Destaque compacto** de 158dp em vez de 240dp. Título em até 2 linhas, informação curta, botão Assistir de 130×30dp. Área direita permanece não interativa para não colocar o foco atrás do miniplayer.
- **Navegação reorganizada:** menu lateral retrátil, cabeçalho simples, seção Ao vivo mais visível, 28 temas acessíveis perto do topo.
- **Hub AO VIVO AGORA** na Home e também na seção AO VIVO. Oito fontes de busca: notícias, esportes, games, música, podcasts, natureza, tecnologia e mundo.
- Cada busca usa `SearchOptions.FEATURE_LIVE | SearchOptions.TYPE_VIDEO` na API de conteúdo da base SmartTube, depois confere `Video.isLive && !isLiveEnd && !isUpcoming` antes de exibir. Não conta gravações como lives e não promete quantidade fixa.
- Resultados distintos por `video.videoId` mesclados em uma linha única, carregamento gradual das buscas, até 56 vídeos reais, sem API paga. Se não houver live de uma categoria, outros temas continuam.
- Chips filtrados por tema permitem abrir a busca original com o controle. O catálogo existente e a descoberta por 28 temas seguem ativos.
- Linhas dinâmicas ordenadas por posição visual real para navegação UP/DOWN, independentemente da ordem das respostas de rede.
- Player com botões e trilhos de vidro mais discretos, pouca sombra, e sugestões menores 154×87dp. Funções originais preservadas.
- Visual grafite/prata e menos azul intenso. Atualizador interno continua usando APK com mesma assinatura.

## Verificação obrigatória
1. Atualizar da 0.9.7 pelo botão Atualizar, sem desinstalar.
2. Na Home, testar o tamanho fixo das miniaturas e rolagem horizontal. Não deve existir expansão tardia.
3. Encontrar o hub AO VIVO AGORA e seus oito temas, com live somente quando a fonte sinalizar transmissão ativa.
4. Abrir seção AO VIVO, aguardar resultados e verificar se o novo hub amplia as possibilidades sem resultados gravados rotulados como live.
5. Testar uma live, sair e recuperar foco na Home e no Histórico.
6. Mover entre fileiras enquanto resultados de lives e de categorias chegam; verificar que o foco não desaparece.
7. Player: 4K quando disponível, áudio, legendas, velocidade, sugestões e miniplayer.
8. Confirmar botão Atualizar, configurações, pesquisa, modo clássico.
9. Avaliar carregamento e memória da TCL P7K, particularmente após carregamento de 8 consultas de live e 4 de tópicos.
10. APK publicado só após Gradle, validação ARM, assinatura permanente e comparação do arquivo baixado da release. 0.9.7 fica disponível como backup.

**Estado:** modificações no código, a experiência visual ainda requer teste real na TV. Não tratar build como aprovação do design.
