# HX TV — Referências visuais e critérios para a nova interface

**Data:** 20 de novembro de 2026  
**Situação:** planejamento de uma interface nova. **Este documento não é um APK e não significa que o aplicativo já foi modificado.**

## 1. Decisão principal

Vamos parar de fazer pequenos ajustes na aparência original do SmartTube. A proposta é **criar uma tela inicial e um sistema de navegação próprios do HX TV**, usando componentes nativos do Android TV.

O SmartTube continuará fornecendo, quando possível, as funções que já funcionam: reprodução de vídeos, contas, histórico, inscrições e acesso aos conteúdos.

**Não vamos colocar uma máscara sobre a tela antiga.** Também não vamos usar uma página da internet dentro do aplicativo para fingir que o visual foi substituído.

## 2. Como os três sites podem ajudar

- **Taste Skill** — https://www.tasteskill.dev  
  Referência para uma interface bem organizada, com espaço adequado entre os elementos, títulos legíveis, bom uso das cores e revisão visual em diferentes tamanhos de tela. Suas ferramentas voltadas a sites não serão instaladas diretamente no Android TV.

- **Transitions.dev** — https://transitions.dev  
  Inspiração para movimentos suaves ao abrir o menu, mudar o vídeo em destaque e passar de uma tela para outra. Esses movimentos deverão ser recriados com os recursos do próprio Android.

- **React Bits** — https://reactbits.dev  
  Inspiração para efeitos discretos, detalhes de tipografia e movimentos de seleção. Os componentes de páginas React não funcionam diretamente em um aplicativo Android TV nativo; serão usados apenas como referência visual.

## 3. Como deverá ser a tela inicial

1. **Cabeçalho exclusivo HX TV:** identificação do aplicativo, pesquisa, conta do usuário e acesso fácil às categorias.
2. **Destaque principal bem diagramado:** imagem maior à direita, título e informações em uma área reservada à esquerda. Nada de textos cobrindo rostos ou saindo para fora da tela.
3. **Faixa de recomendações:** miniaturas menores logo abaixo do destaque; ao selecionar outra, o painel principal muda, mas o vídeo não começa a tocar sozinho.
4. **Menu lateral compacto:** aparece quando necessário e recolhe quando o usuário navega pelos vídeos, sem desperdiçar um grande espaço da tela.
5. **Reprodução preservada:** o vídeo continuará abrindo no sistema de reprodução já utilizado pelo aplicativo.
6. **Dados preservados:** manter, sempre que tecnicamente possível, contas, histórico, inscrições, recomendações e preferências do usuário.

**O objetivo é que a diferença seja evidente assim que o HX TV for aberto, e não apenas ao examinar as bordas dos cards.**

## 4. Movimentos e efeitos

- **Ao selecionar um vídeo:** ampliação pequena, contorno bronze discreto e movimento rápido e suave.
- **Ao mudar o destaque:** transição suave entre imagens e informações; não iniciar a reprodução automaticamente.
- **Ao abrir o menu:** deslocamento lateral sem saltos.
- **Ao carregar imagens:** aparecimento progressivo, sem piscar.
- **Para títulos longos:** manter o letreiro rolante que já funciona no SmartTube.
- **Acessibilidade:** respeitar preferências de redução de movimento quando disponíveis e garantir que o item selecionado continue visível à distância.

Todos esses efeitos devem funcionar com o controle remoto da TCL P7K.

## 5. O que precisa ser testado antes de entregar um APK

- Verificar as telas em **720p e 1080p**, tanto com o menu aberto quanto fechado.
- **Nenhum título, botão ou informação pode ficar cortado.** As fotos da versão 0.8 mostraram o destaque superior invadindo a área do cabeçalho; isso precisa ser corrigido.
- A tela inicial precisa ter uma identidade diferente do SmartTube — não apenas miniaturas maiores.
- Testar todas as direções do controle remoto, botão OK, OK pressionado por mais tempo e botão Voltar.
- Testar pesquisa, contas, inscrições, histórico, vídeos ao vivo e reprodução.
- Não deixar aparecer o antigo logotipo do SmartTube ou uma segunda interface por trás.
- Verificar desempenho e consumo de memória na TCL P7K.
- Compilar o APK e verificar se sua assinatura é **a mesma das versões anteriores**, para atualizar sem desinstalar.
- **Não mudar o canal de atualizações até testar a interface e a reprodução na TV.**
- **Compilação bem-sucedida não significa que o visual está aprovado.**

## 6. Limites e cuidados

Os três sites servem para melhorar nosso projeto visual, não para substituir ferramentas nativas do Android. Não devemos adicionar bibliotecas pagas, páginas web sobrepostas ou componentes React que não funcionem no Android TV.

A compilação deve continuar **gratuita e reproduzível pelo GitHub Actions**.

**Versão atual:** a HX TV 0.8 continua disponível. A interface nova descrita neste documento ainda precisa ser implementada e testada; não há um novo APK correspondente a este planejamento.
