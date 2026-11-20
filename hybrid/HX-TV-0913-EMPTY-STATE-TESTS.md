# HX TV 0.9.13 — Estados vazios, capas e faixa PiP

## Evidências: gravação de 107 segundos em 20/11/2026
Arquivo: YouCut_20261120_190919781.mp4. O review da versão 0.9.12 mostra:
- 00:00–00:12: Home e fileiras horizontais com navegação e muitos assuntos.
- ~00:15: ao entrar em uma seção tipo Playlists, grande painel vazio sem orientação.
- ~00:45–00:51: na área AO VIVO, diversas thumbnails ainda aparecem em cinza, mas os títulos e selos estão presentes.
- ~01:00 e ~01:15: ao mudar para Canais/Histórico, a interface fica sem conteúdo nem explicação temporariamente.
- ~01:21–01:27: indicador PREPARANDO VÍDEO durante carregamento, desaparecendo quando a transmissão começa. O defeito de 0.9.11 não é visível neste trecho.
- ~01:30–01:33: reprodução e controles aparecem sem erro aparente no trecho.
- ~01:36–01:45: vídeo em PiP ativo ao navegar na Home; faixa de reserva mantém conteúdo utilizável mas consome espaço vertical.
- A gravação não valida exaustivamente as Sugestões ou durações de reprodução prolongadas; não afirmar o contrário.

## Implementação
1. Mensagem carregando dentro da seção até chegarem vídeos, sem tela completamente vazia.
2. Após ~4,7s sem grupo real, mostrar estado informativo: `Nenhum vídeo recebido nesta seção. Ela pode estar vazia ou ainda não sincronizada`, e botão focável `TENTAR NOVAMENTE`.
3. A nova tentativa aciona novamente o foco do BrowsePresenter na seção; não promete vídeos se conta, rede ou fonte estiver indisponível.
4. Lógica protege resultados posteriores: cancela temporizador ao trocar seções, quando grupo real chega e ao sair da Home; não cria vídeos artificiais.
5. Capas: background grafite em gradiente com logo HX TV atrás da imagem real. Se a thumbnail falhar, o fundo da marca aparece, não retângulo cinza vazio. A imagem verdadeira é exibida quando carregar; sem shimmer que consuma recursos da TV.
6. Faixa reservada ao PiP reduzida de 108dp para 76dp, texto mais discreto; o posicionamento do PiP continua controlado pelo Android TV.
7. Todos os controles de reprodução, aviso de carregamento corrigido, área AO VIVO com filtros, 28 temas, conta e histórico são mantidos.
8. APK 0.9.13 (versionCode 2491), assinado com mesmo certificado persistente, para atualizar sobre 0.9.12. O manifesto OTA somente muda depois de compilar, verificar a assinatura e comparar o arquivo publicado.

## Testes de aceitação TCL P7K
- Abrir Playlists, Canais e Histórico: verificar mensagem temporária e, se sem vídeos, texto explicativo + botão TENTAR NOVAMENTE.
- Quando dados chegarem depois, a mensagem deve desaparecer e os cards focáveis assumir o conteúdo.
- Fazer a nova tentativa: verificar navegação pelo controle, sem travar ou criar cards fictícios.
- Ao Vivo: conferir retângulos grafite personalizados enquanto as thumbnails carregam e que conteúdos reais substituem o estado visual.
- Abrir vídeo com carregamento: PREPARANDO VÍDEO deve desaparecer ao iniciar a imagem.
- Voltar à Home com mini-janela: faixa menor, sem cobrir botões; testar modo aberto/recolhido do menu.
- Confirmar controles, Sugestões, 4K quando disponível, legendas, pesquisa, central AO VIVO, configurações e atualizador interno.
- Testar 10 mudanças de seção consecutivas e trocar de vídeo, observando vazamento de foco e mensagens antigas.

**Atenção:** build verde e assinatura válida não comprovam fluidez na TCL nem disponibilidade de dados externos; dependem de teste real.
