# HX TV — redesign premium experimental (20/11/2026)

Este pacote altera a **interface Android nativa**, sem WebView e sem máscara sobre o SmartTube. A versão está em uma ramificação de teste; não substitui o APK 0.9.2 assinado.

## Problemas identificados nas fotos da TCL P7K

1. O miniplayer flutuante cobre os botões no topo direito e atrapalha visualizar o foco.
2. A navegação superior nem sempre passa corretamente para os vídeos na página inicial.
3. Quando a lista ainda está carregando, a navegação por setas pode não encontrar nenhum destino.
4. O destaque e os controles ainda não apresentam uma hierarquia visual premium consistente.

## Solução implementada no código-fonte

- **Trilho lateral fixo** de 172 dp: logo HX TV, categorias verticais roláveis, pesquisar, atualizar e modo clássico.
- **Nenhum botão ativo no canto superior direito**, reservado visualmente para o miniplayer do Android TV. O miniplayer ainda pode cobrir parte da imagem decorativa de destaque (limitação do Picture-in-Picture), mas não a navegação.
- **Setas consistentes**: direita sobre uma categoria entra nos cards; esquerda do primeiro card de uma faixa retorna ao menu; para cima na primeira fileira retorna à categoria atual; OK seleciona normalmente.
- **Carregamento assíncrono:** o pedido de entrar na grade fica pendente até a chegada de um VideoGroup com primeiro card, sem obrigar o usuário a apertar a seta repetidamente.
- **Destaque de cinema**: imagem larga real com vinheta escura do lado do título, selo de live, título e metadados bem separados.
- **Cards:** leve ampliação de 2,5% e elevação no foco. Letreiro rolante original é preservado.
- **Player:** controles originais e ícones intactos, com StateListAnimator nativo que aumenta discretamente o botão focado por 140 ms. Fundo OLED/bronze mantido.
- **Atualizador:** continua no menu lateral e em Configurações; URL de atualização beta e release 0.9.2 NÃO foram modificadas.

Referências: Taste Skill (hierarquia), Transitions.dev (microtransições reversíveis), React Bits (resposta ao foco). Não carregar dependências React dentro de Android nativo.

## Critérios para aprovar o próximo APK

- Testar em 720p e 1080p na TCL P7K, tanto com reprodução normal quanto após acionar o miniplayer.
- Início → direita → primeira miniatura, mover entre linhas e retornar pelo controle com esquerda/para cima.
- Histórico → vídeo → voltar → foco no mesmo card e miniplayer sem ocultar ações do menu.
- Pesquisar, Inscrições, AO VIVO, Histórico, Configurações, Modo clássico e Atualizar.
- Vídeo longo, ao vivo (especialmente CazéTV), pausa, 4K quando disponível, legibilidade e tempo de resposta dos controles.
- Confirmar que a assinatura/código de versão do futuro APK é incremental e compatível com 0.9.2.
- Não publicar a nova versão no canal beta automático antes do build completo e da validação do retorno de vídeo.

**Estado:** patch nativo preparado em branch de revisão. Compilação, assinatura e teste no aparelho ainda não realizados.
