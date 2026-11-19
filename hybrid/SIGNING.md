## 19/11/2026 — Assinatura local sem GitHub Actions

Uma copia do HX TV Native 0.2.0 foi assinada localmente, usando o APK ja compilado anteriormente no run 44609431204. O codigo-fonte NAO foi recompilado nesta etapa local.

- Arquivo gerado: HX-TV-Native-0.2.0-assinatura-permanente.apk
- Pacote Android: br.com.hxnative.tvpreview; versionCode=2471; manifesto binario preservado.
- APK SHA-256: 7f2dd04f07edb7180ca3a9232fe386ca826d12e67bbce831326da2bdc5743952
- Certificado X.509 SHA-256: 1e929c67bf65dd36a8875595f882089f036b4d8c68700a6f4fd70fb23efd86d7
- Assinaturas: APK v1 usando jarsigner e v2 RSA SHA-256 usando assinatura e verificacao locais. Sem teste na TV real.
- Chave privada: PKCS12 protegido por senha, entregue separadamente ao usuario. Nunca publicar a chave no GitHub publico.
- Este ambiente nao possui Android SDK nem Gradle ou rede direta para clonar o codigo-fonte, portanto nao e possivel compilar o projeto nativo integralmente nele.
- A release publica hx-smarttube-native-v0.2.0 ainda contem o APK debug original, nao o APK assinado localmente.
- ATUALIZADOR AINDA NAO FUNCIONAL: hybrid/updates/native.json continua apontando para 0.1.0 debug. Nao alterar para um APK sem URL publica e certificado compativel.
- Para liberar OTA e preciso hospedar um APK binario assinado com o mesmo certificado em URL publica direta, depois mudar manifesto para versionCode maior e testar instalacao na TCL P7K.
- Nao desinstalar Cobalt HX. A migracao inicial da instalacao HX TV Preview 0.1.0 debug para a chave permanente exige remover apenas o HX TV Preview.

---
# HX TV — atualização real sem baixar APK manualmente

## Situação atual

- A versão `HX TV Preview 0.1.0` foi compilada com uma chave de depuração **temporária** no runner do GitHub.
- A versão `0.2.0` (teste) tem as novas cores e o menu **Atualizar aplicativo**, mas também foi compilada com uma chave de depuração temporária.
- O Android **não** permite atualizar um pacote existente se a nova versão for assinada com outra chave, mesmo que o identificador Android seja idêntico.
- **Nunca publicar as chaves privadas no código-fonte, commits, releases, logs ou documentação.**

## Código já preparado

1. Em `hybrid/patch_smarttube.py`, o menu nativo de Configurações ganha uma ação direta `Atualizar aplicativo` chamando `AppUpdatePresenter`.
2. O menu `Sobre` abre o `AboutSettingsPresenter` completo, mesmo com o identificador personalizado `br.com.hxnative.tvpreview`.
3. O manifesto de atualização `hybrid/updates/native.json` está vinculado exclusivamente aos nossos releases, **não** aos APKs oficiais do SmartTube.
4. O workflow de assinatura segura está disponível em `.github/workflows/hx-smarttube-signed.yml` no branch padrão. Ele **não publica nada** se faltarem segredos.
5. O workflow valida pacote Android, versionCode e certificado antes de publicar uma release assinada.

## Configuração segura necessária, uma única vez

É preciso criar uma keystore Android própria e guardar uma cópia privada fora do GitHub público. O administrador do repositório deve cadastrar em **GitHub → Settings → Secrets and variables → Actions → New repository secret**:

- `HX_KEYSTORE_B64`: conteúdo da keystore privada codificado em Base64, **sem quebras de linha**.
- `HX_KEYSTORE_PASSWORD`: senha forte da keystore, também usada para proteger a chave (para esta primeira configuração).
- `HX_KEY_ALIAS`: alias da chave.

Não compartilhar esses valores em conversas nem em commits. Não é possível obter a chave privada das versões debug anteriores.

## Primeiro APK assinado

Depois de configurar os segredos:

1. Executar manualmente o workflow `HX TV signed in-app update channel`, selecionar o branch padrão e verificar que concluiu com sucesso.
2. Fazer uma última **migração limpa**, desinstalando **apenas o HX TV Preview debug**, não o Cobalt HX. Instalar o novo APK assinado.
3. Em versões futuras, manter o mesmo `applicationId`, a mesma keystore e usar `versionCode` maior a cada release.
4. Publicar a nova versão assinada no GitHub e apontar o `hybrid/updates/native.json` para o novo APK apenas **depois** da verificação da assinatura.
5. Testar na TCL P7K: Configurações → Atualizar aplicativo → baixar APK → confirmação de instalação Android → atualização preservando os dados.

**Não marcar o fluxo de atualização como validado até testar a sequência completa com duas versões assinadas pela mesma keystore.**

## O que não é necessário

- Não há necessidade de desinstalar nem alterar o Cobalt HX original.
- Depois da migração para a chave estável, o Downloader não precisa ser usado em cada nova versão do HX TV.
- A atualização nativa do Android continuará solicitando confirmação de instalação quando exigido pelo sistema.
