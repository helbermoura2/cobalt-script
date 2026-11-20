# HX TV 0.9.10 — Playback & PiP Polish

## Motivo
Na gravação da 0.9.9 o usuário viu tempo prolongado de carregamento antes do vídeo, muitas sugestões ocupando a imagem e o miniplayer flutuante cobrindo a Home. A Home e navegação já melhoraram, então esta versão é deliberadamente menor e conservadora.

## Implementado
1. **Carregamento:** mostra "CARREGANDO VÍDEO" somente se o ExoPlayer continuar em estado BUFFERING por mais de 1.800ms. Esconde em READY/ENDED e quando o primeiro quadro é renderizado. Nenhuma mudança nas URLs dos fluxos ou nos codecs.
2. **Métricas:** loga tempo até o primeiro quadro via VideoListener.onRenderedFirstFrame e SystemClock.elapsedRealtime, reiniciando contagem ao definir outro vídeo. Isso **não comprova ganho de velocidade**, mas torna um atraso reproduzível e diagnosticável.
3. **Sugestões:** apresentador específico do player com miniaturas 146×82dp (16:9), distinto dos cards da Home; títulos de 2 linhas e metadados reduzidos. Mantém acesso às fileiras originais.
4. **Controles:** microtransição de foco de 140/130ms, fundo OLED com alto contraste no selecionado; ações/atalhos nativos do SmartTube preservados.
5. **PiP:** PlaybackActivity disponibiliza um booleano de estado de mini-janela, atualizado na entrada/saída e limpo ao destruir a Activity. A Home passa a reservar uma faixa superior não interativa quando detecta PiP, para preservar cards abaixo do vídeo flutuante. Aspect ratio 16:9 permanece configurado quando suportado. **A posição final continua sendo escolha do Android TV.**
6. **Concorrência:** Home carrega apenas duas fileiras de descoberta temática inicialmente, aumentando sob demanda; distribui pesquisas adicionais com intervalos maiores. Ao Vivo continua pesquisando oito assuntos, com espaçamento contextual: Home mais conservadora; página AO VIVO mais responsiva.
7. **Compatibilidade:** versãoName 0.9.10, code 2488, mesma chave privada de 0.9.9. Manifesto beta só é publicado após compilação ARM, verificação de pacote, assinatura, release e comparação do APK baixado.

## Testes manuais obrigatórios na TCL P7K
- Atualizar diretamente pelo botão ATUALIZAR sem desinstalar.
- Abrir vídeo gravado e ao vivo; quando conexão demorar, legenda de buffering deve aparecer após ~1,8s e desaparecer quando vídeo começar.
- Navegar durante a reprodução, acessar controles e sugestões; confirmar qualidade, legendas, velocidade, likes e atalhos sem regressões.
- Verificar sugestões menores e títulos legíveis.
- Sair para a Home com PiP: testar reserva visual no topo, menu aberto/retraído, navegação entre cards, retorno ao vídeo; checar que a reserva some quando a mini-janela deixa de existir.
- Ao Vivo: filtro Todos, notícias, jogos, música etc. Resultados podem variar conforme disponibilidade real.
- Fazer buscas rápidas e abrir vídeo enquanto a Home carrega; verificar se não há perda de foco.
- Validar assinatura e appId idênticos à versão instalada.

## Limitações
- A gravação não permite provar que os ~8s de espera são causados pelo aplicativo. Podem resultar de CDN, extração do fluxo, vídeo, conexão ou decodificador. Este release melhora o feedback e reduz pesquisas paralelas, mas **não promete tempo fixo de inicialização**.
- O miniplayer do Android TV não pode ser movido à vontade pelo aplicativo; só se evita conteúdo interativo atrás dele.
- Build bem-sucedida não substitui teste de desempenho e visual na TV real.
