# HX TV 0.9.14 — Carregamento sem tela vazia e troca de fileiras estável

## Fonte observada
Gravação de 60,93 s: `YouCut_20261120_193505453.mp4`, enviada pelo usuário após instalar HX TV 0.9.13 na TCL P7K.

### Evidências
- 00:00–00:04: Home inicia com aviso de carregamento e começa a exibir conteúdos reais.
- 00:05–00:18: fileiras AO VIVO, esportes, games, assuntos e recomendações com capas e navegação visíveis.
- 00:25–00:28: outra seção fica quase vazia, com painel grande de texto durante a consulta; depois a Home populariza.
- 00:38–00:44: abertura de vídeo apresenta tela escura e spinner com PREPARANDO VÍDEO durante cerca de 5–6s. Logo depois o vídeo inicia e o aviso desaparece.
- 00:46–00:55: controles e painel de Sugestões no rodapé deixam a imagem principal visível acima. As fileiras de sugestões podem ser percorridas, sem um travamento visível na gravação.
- 00:58–01:00: ao sair do vídeo com PiP ativo, a Home inicialmente exibe apenas um card; outros aparecem gradualmente em 1–2s. A área PiP não parece sobrepor o controle, mas fica uma sensação de refresh parcial.
- Essa gravação não determina a causa exata do tempo de startup do vídeo (rede/servidor/parser/decoder), nem comprova estabilidade de longa duração.

## Atualizações
1. **Skeleton 16:9 sem mídia fictícia** na troca de seção: quatro superfícies de imagem 174×96dp e duas barras de texto, gradiente grafite e dimensões próximas aos cards reais, não clicáveis e sem shimmer custoso.
2. Se o serviço não retornar resultados após 4,7s, remover os skeletons e apresentar texto explicativo e comando focável TENTAR NOVAMENTE.
3. **Micro-lote 300ms de ACTION_REPLACE** quando a seção ainda tem conteúdo visível: preserva fileira antiga temporariamente e aplica a nova sequência de grupos em uma passagem, em vez de oferecer um único card e reconstruir a tela a cada resposta.
4. Os lotes são cancelados ao trocar seção e se a geração da página mudou. As ações de foco e restauração de foco do controle continuam ativas.
5. **Sem alterar ExoPlayer**; mantidos os controles, PiP, área de Sugestões, histórico, filtros AO VIVO, 28 assuntos, pesquisa e atualizador. A 0.9.13 permanece publicada como backup.
6. v0.9.14/versionCode 2492; certificado persistente e OTA só liberado após APK ARM compilar, assinar, release ser publicado e cópia baixada ser idêntica.

## Testes obrigatórios na TV
- Abrir uma categoria: placeholder de cards deve aparecer instantaneamente, sem conseguir selecionar cards inexistentes.
- Após dados reais, skeleton desaparece; capas e títulos verdadeiros aparecem sem duplicações.
- Se não chegar resultado, texto explicativo e TENTAR NOVAMENTE aparecem; navegação com D-pad e clique funcionam.
- Voltar de uma live com PiP: Home não deve saltar de 4 cards para 1 e depois 4; verificar foco.
- Inspecionar troca repetida de seções e resultados que chegam fora de ordem, para não deixar conteúdo antigo da seção errada.
- Conferir player, sugestões, qualidade, áudio, legendas e carregamento como na 0.9.13.
- Testar 4K quando a fonte permitir, não prometer disponibilidade ou tempo fixo.

**Status:** código sujeito a build e teste real. Não confundir compilação verde com aprovação visual.
