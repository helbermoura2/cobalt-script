# HX TV Native 0.3.0 — compilação release e assinatura

**Estado em 20/11/2026 (UTC):** build release concluído no Actions e assinatura privada aplicada localmente. O APK assinado está entregue como arquivo da conversa ChatGPT, não como release binária pública do GitHub.

## Resultados conferidos

- [Build release no GitHub Actions](https://github.com/helbermoura2/cobalt-script/actions/runs/44647981222): sucesso (Gradle `:smarttubetv:assembleStbetaRelease`).
- Aplicativo nativo SmartTube personalizado (sem overlay): theme HX OLED/bronze, marca HX provisória, prévias automáticas de cards desativadas e atualizador protegido.
- Pacote: `br.com.hxnative.tvpreview`.
- `versionCode=2472`, `versionName=0.3.0` (conferido com Android `aapt dump badging`).
- APK release compilado sem sinalizador `application-debuggable`; `zipalign -c 4` aprovado no Actions.
- APK unsigned SHA-256: `614347d3233dc96e65e393677ef8481391ca13c28290176ecefa791222c66b4f`.
- APK assinado local: `HX-TV-Native-0.3.0-ATUALIZACAO-ASSINADA.apk`, 25.574.721 bytes.
- SHA-256 do APK assinado: `6b906ecb43d988f0f02e0d49cf1d19ab4be1f60463f12bb31c56da1dd3d5dfd6`.
- Certificado SHA-256 (o mesmo usado na instalação HX 0.2.0 corrigida): `1e929c67bf65dd36a8875595f882089f036b4d8c68700a6f4fd70fb23efd86d7`.
- `apksigner verify --verbose --print-certs`: **v1, v2 e v3 VERIFICADOS**; teste completo da estrutura ZIP aprovado.
- A chave privada PKCS12 foi usada somente no ambiente local, não enviada a este repositório nem ao GitHub Actions.

## Instalação e limites

- A versão 0.2.0 corrigida, instalada anteriormente na TCL P7K, usa o mesmo certificado e versionCode 2471. A 0.3.0 assinada deve ser instalável **por cima sem desinstalar**, preservando dados. **Isso ainda requer validação na TV.**
- O arquivo assinado não foi publicado em uma release do GitHub: o conector GitHub disponível permite alterar arquivos de texto e consultar artefatos, não publicar um APK binário local como release. A versão 0.3.0 que existe na release pública `hx-smarttube-native-v0.3.0` é um APK **debug com certificado temporário**, incompatível com a instalação da TCL. Não usar como atualização.
- O botão `Configurações → Atualizar aplicativo` ainda não pode instalar 0.3.0 sem URL pública de APK assinado. O manifest `hybrid/updates/native.json` não foi alterado para apontar a um arquivo incompatível.
- Após hospedar o APK exatamente assinado com a chave permanente, atualizar o manifest para o URL correto e versionCode 2472, então testar OTA real. Não usar a chave privada na fonte pública.
- SmartTube continua sendo o motor de reprodução desta versão; integração Cobalt/SmartTube para CazéTV 4K e comandos longos de miniplayer ainda pendentes.

## Correções do build release

O SmartTube original pinado depende de tarefas Gradle antigas que tentam usar `org.codehaus.groovy:groovy:3.0.10` para a extração de anotações e esperam `typedefs.txt` nas bibliotecas. O workflow ignora apenas a extração opcional e prepara receitas de metadados vazias; não modifica as bibliotecas de decodificação. Isto permitiu compilar o APK release.
