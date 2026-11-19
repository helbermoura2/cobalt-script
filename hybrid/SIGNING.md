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
