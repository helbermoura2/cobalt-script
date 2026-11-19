# HX TV Native — preparação 0.3.0

## Implementado no código-fonte, ainda não distribuído

- Nova opção nativa **HX TV — Bronze Imperial (OLED)** no lugar da opção cinza padrão (índice 1), mantendo a escolha dos usuários que optaram por outros temas.
- Telas de navegação, player e configurações com estilos e paleta nativos; sem WebView/HTML de sobreposição.
- Recursos de marca provisória HX substituem também `app_logo_semi_red.png` e `app_logo_semi_grey.png`, responsáveis por permanecer visível o símbolo vermelho do SmartTube Beta em temas alternativos.
- Títulos com **letreiro rolante** e foco original preservados, como confirmado pelo usuário. Nada de forçar o texto completo dentro do card.
- A prévia automática continua desativada no componente Android de cards.
- Segurança do atualizador: somente links de APK do canal assinado do projeto e somente quando a instalação atual usar o certificado permanente HX identificado pelo SHA-256 `1e929c67bf65dd36a8875595f882089f036b4d8c68700a6f4fd70fb23efd86d7`. O Android ainda valida a assinatura final do APK instalado.
- Próxima versão reservada no fonte: `versionName=0.3.0`, `versionCode=2472`; mantém pacote `br.com.hxnative.tvpreview`.
- Compilação automática pelo GitHub Actions **desativada**: o workflow de prévia só executa se iniciado manualmente; nenhuma execução foi solicitada nesta etapa.

## Não concluído

- O APK **0.3.0 ainda não foi compilado nem instalado**. O ambiente de execução não dispõe do Android SDK/Gradle nem tem rede direta para baixar as dependências do SmartTube, e a preferência do usuário é não executar GitHub Actions.
- O HX TV 0.2.0 corrigido, com a assinatura local, abriu na TCL P7K conforme foto do usuário. Isso não valida por si só futuras atualizações no próprio aplicativo.
- O manifest `hybrid/updates/native.json` **ainda aponta para um APK debug antigo** e não foi liberado para atualizar a instalação assinada. O código 0.3.0 contém uma trava explícita contra esse download.
- Necessário compilar o APK 0.3.0 com a mesma chave privada da instalação 0.2.0, publicar o binário assinado em release pública e só então alterar o manifest para oferecer a atualização in-app. A chave privada não deve ir para o repositório.
- A integração de reprodução SmartTube + Cobalt para CazéTV AO VIVO/4K e os comandos de miniplayer personalizados seguem pendentes; não confundir com as mudanças visuais.

## Arquivos alterados

- `hybrid/patch_hx_design.py`: tema nativo, identidade provisória e proteção do atualizador.
- `hybrid/patch_smarttube.py`: versão 0.3.0 reservada, base SmartTube, atualizador e prévias.
- `.github/workflows/hx-smarttube-native.yml`: manual-only e inclusão do patch nativo.

**Orientação de instalação:** manter o APK 0.2.0 assinado já instalado e não clicar em atualização até existir APK 0.3.0 assinado com certificado igual e realmente publicado.
