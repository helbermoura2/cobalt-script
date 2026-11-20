# HX TV 0.9.6 — Descoberta cinematográfica (beta)

## Referência enviada em 20/11/2026
Vídeo curto de interface de catálogo com: menu de ícones à esquerda, grande destaque com imagem de fundo e descrição, fileiras horizontais de capas verticais, realce ao focar uma capa e troca de destaque. A referência usa pôsteres de obras; **HX TV usa miniaturas reais do YouTube** e não possui metadados de pôster de filme.

## Recursos
- Mantidos menu lateral retrátil e o foco testado na TCL P7K em 0.9.4 e 0.9.5.
- Na home: cards mais verticais, imagem real do vídeo no fundo, miniatura original horizontal ao centro e informações da publicação abaixo.
- O destaque principal se atualiza a cada vídeo que recebe foco, com transição de imagem; OK reproduz.
- Antes havia corte em 24 cards por grupo, independentemente de quantos vídeos a fonte retornava. Agora o catálogo começa com 32 e cresce em blocos de 18 **somente quando o usuário navega** para o fim da fileira; pede mais à fonte ao alcançar o fim real.
- Painel **Explorar / 28 assuntos**, navegável pelo controle, com pesquisa nativa usando os termos de cada tema.
- Na home: primeiras quatro fileiras temáticas reais carregam sem interação adicional; cada aproximação do fim de uma fileira temática libera mais três temas.
- Fonte das fileiras: `YouTubeServiceManager.getContentService().getSearchObserve(...)`, sem API paga nem conteúdo inventado. Consultas podem retornar poucos resultados ou falhar, caso em que outras linhas permanecem utilizáveis.
- Cancela solicitações de descoberta ao trocar de tela e ignora respostas atrasadas.
- Assinatura, nome do pacote e instalador interno são os mesmos da versão anterior. Canal beta só aponta para 0.9.6 depois do APK validado e publicado.

## Testar na TCL P7K
1. Atualizar da 0.9.5 pela opção ATUALIZAR; não desinstalar.
2. Início: seta direita abre destaque, seta baixo entra nas capas, OK reproduz.
3. Mover por capa: imagem e título do destaque mudam. As fileiras deslizam na horizontal, inclusive após o 24º vídeo.
4. Scroll vertical: visualizar o painel Explorar e fileiras de Cinema, Séries, Tecnologia, IA etc. A quantidade exata de resultados depende das buscas reais.
5. Selecionar um dos 28 assuntos: abrir resultados da busca nativa.
6. Navegar até o final de uma fileira temática: outras fileiras são carregadas progressivamente.
7. Histórico, Inscrições, Vídeos ao vivo, Pesquisa, Configurações, Modo clássico e atualização.
8. Durante reprodução, confirmar qualidade, velocidade, legendas e miniplayer sem perder foco ao voltar.
9. Observar fluidez e memória nas fileiras verticais em 720p e 1080p.

## Preservação
APK 0.9.5 fica publicado como rollback manual. Não há conversão de mídias do YouTube em um serviço de catálogo de filmes nem garantia de disponibilidade de vídeos específicos.
