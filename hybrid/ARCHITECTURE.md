# Projeto híbrido — SmartTube + Cobalt
**Status:** arquitetura inicial e matriz de aceitação; **sem APK híbrido compilado**. Não usar o código `native-home.js` como base de navegação.

## Decisão de arquitetura

- **Base do aplicativo e interface:** fonte do [SmartTube](https://github.com/yuliskov/SmartTube), licença MIT; interface Android TV nativa (não uma sobreposição HTML ou camada de CSS sobre o YouTube original).
- **Base inicial verificada para auditoria:** `yuliskov/SmartTube@41fbc7c60ee28e6f13cee74ff630dac6df2724bf` em 19/11/2026. Fazer checkout de submódulos e auditar dependências antes de compilar; não publicar simplesmente um APK de terceiros renomeado.
- **Reprodução SmartTube:** manter player/ExoPlayer existente como primeiro motor experimental, com controles nativos, SponsorBlock e demais funcionalidades permitidas pela licença.
- **Reprodução Cobalt:** segunda estratégia, principalmente para CazéTV AO VIVO e 4K. Fonte de referência [TizenTubeCobalt](https://github.com/reisxd/TizenTubeCobalt), versão v2.0.3, código BSD-3-Clause e componentes adicionais.
- **Integração entre motores:** **não existe ainda**. O Cobalt não pode ser simplesmente copiado como biblioteca do SmartTube. Estudar duas rotas:
  1. **Ponte entre apps por Intent/deep link** (primeiro teste de viabilidade). É preciso que uma build Cobalt aceite um vídeo ou playlist por Intent e devolva resultado/retorno sem cair na tela do YouTube. Limitação: não é um único processo nem garante miniplayer atravessando apps.
  2. **Player Cobalt incorporado** (objetivo definitivo, somente se a API/runtime e dependências de licenças permitirem). Exige engenharia e build própria do Cobalt, ciclo de vida de vídeo, foco do controle, áudio, sessão, codec, buffers e retorno de tela. Não assumir que embutir dois apps funciona.
- **Decisão de produto:** sem sucesso nas transições, retornar à experiência SmartTube nativa com seletor manual para Cobalt externo como opção experimental e **sem chamar isso de híbrido integrado**.

## Requisitos mandatórios do usuário

1. Interface **nativa SmartTube** (ou personalização direta sobre o código nativo) com nome e logo **provisórios**. Não mexer somente no CSS da tela original do YouTube e não criar máscara/overlay.
2. Sem prévias animadas nem autoplay ao navegar. Ao voltar do player, voltar ao menu nativo do projeto, não à interface original do Cobalt.
3. **VOLTAR curto:** comportamento normal do contexto (player → interface principal do projeto; menus → seção anterior).
4. **VOLTAR pressionado ~900 ms:** minimizar vídeo atual para miniplayer do próprio aplicativo quando possível, sem pausar áudio ou perder sessão.
5. **OK pressionado ~900 ms:** menu de contexto com **Retornar ao vídeo em reprodução** quando houver miniplayer; OK curto continua selecionando.
6. **CazéTV AO VIVO:** verificar se não ocorre salto para trás, repetição ou congelamento. Não aceitar "corrigir" baixando permanentemente de 2160p para 1080p.
7. **4K/2160p**, VP9/AV1/HDR quando conteúdo, aparelho e fontes permitirem. O critério é comparar a mesma transmissão no Cobalt HX e na build híbrida; não prometer que todo live é 4K.
8. SponsorBlock, bloqueios de anúncios, velocidade, conta, inscrições, histórico, pesquisa, legendas e funcionalidades da base; auditar diferenças dos dois clientes.
9. **Nome e ícone Android próprios, diferentes de TizenTube**, identificador do pacote novo e assinatura estável. Evitar dois ícones/nomes TizenTube iguais na TV.
10. **Configurações → Atualizar aplicativo**, com atualização assinada *in-place* (mesma chave segura em releases) e autorização de instalação do Android quando exigida. Atualização de conteúdo remoto não pode fingir ser atualização do APK.
11. Não apagar, substituir, redefinir dados nem desinstalar o **Cobalt HX atual**: é nosso aplicativo de comparação e recuperação.

## Etapas e critérios de aprovação

### P0 — Estudar o código e provar o player
- Identificar classes da UI, player, URLs e navegação do SmartTube; documentar versões e dependências.
- Teste de Intents/deep links Cobalt (o upstream registra limitação em https://github.com/reisxd/TizenTubeCobalt/issues/129).
- Testar um vídeo normal e uma transmissão da CazéTV, comparando resolução, buffer e salto de tempo.
- Registrar o método real de retorno ao app sem sair no YouTube original.

### P1 — Primeiro APK nativo SmartTube personalizado
- Compilar do código-fonte auditado, não usar repack de APK externo.
- App com nome/ícone temporário claramente distintos; grade/menus nativos visíveis.
- Desabilitar qualquer preview automático de vídeo.
- Testar navegação, busca, login e reprodução, sem modificar o Cobalt HX instalado.
- Se não houver integração real de reprodução Cobalt, chamar este APK **prévia SmartTube nativa**, nunca "híbrido" concluído.

### P2 — Integração de reprodução, miniplayer e controle
- Selecionar SmartTube ou Cobalt por vídeo/fluxo com retorno consistente e seguro.
- Preservar miniplayer ao navegar no aplicativo; documentar se for impossível ao trocar processos.
- Implementar VOLTAR e OK longos sem bloquear toques curtos.
- Adicionar diagnóstico com tipo de player, codec, resolução, posição LIVE e log de erros.

### P3 — Atualização e release
- Assinatura **persistente e privada**, versionCode incremental e instalação in-place validada em duas versões.
- Configurações → Atualizar aplicativo; permitir mudança de tema sem download quando somente preferências.
- Build em fonte revisável e release com link direto para o Downloader, somente após validação das etapas necessárias.

## Critérios de bloqueio

**Não divulgar APK como pronto** antes de compilar, verificar assinatura/pacote, comprovar interface nativa e registrar testes de reprodução. Não reutilizar os APKs HX Native 0.1.x como se fossem o novo projeto. Cobalt e SmartTube possuem arquiteturas distintas; um vídeo aberto em outro processo não conserva automaticamente o miniplayer do processo original.

## Licenças e segurança

- SmartTube: MIT — conservar avisos e licenças. Confirmado no repositório oficial.
- TizenTube Cobalt: BSD-3-Clause e bibliotecas de terceiros — preservar atribuições e cumprir condições específicas.
- Usar dependências de fonte confiável e releases próprios assinados com chave persistente; não reutilizar assinatura temporária nem publicar chaves privadas.
- Não coletar credenciais nem tokens do YouTube no projeto de ponte.

## Resultado real do primeiro build — 19/11/2026

- **Build concluído com sucesso**: [GitHub Actions #44462366312](https://github.com/helbermoura2/cobalt-script/actions/runs/44462366312).
- APK publicado: [HX TV Native 0.1.0 ARM](https://github.com/helbermoura2/cobalt-script/releases/download/hx-smarttube-native-v0.1.0/HX-TV-Native-0.1.0-arm.apk), 29.318.505 bytes; SHA-256 `b9558d4536aeb954b70de52382a28b292d45ed77beca1ec68663f38c65431699`.
- Compilação do fonte SmartTube fixado em `41fbc7c60ee28e6f13cee74ff630dac6df2724bf`, incluindo submódulos originais, por `hybrid/patch_smarttube.py`.
- **Identidade própria validada pelo aapt**: `br.com.hxnative.tvpreview`, `versionCode=2470`, `versionName=0.1.0`.
- Assinatura de teste APK v1 e v2 verificada pelo `apksigner`.
- Configuração beta oficial de atualização substituída por `hybrid/updates/native.json`, que inclui somente nosso APK, no menu nativo Sobre/Verificar atualizações.
- Recurso de prévia automática removido diretamente de `ComplexImageCardView`; sem sobreposição visual.
- Imagens provisórias de app/banner substituídas por desenhos originais HX; `google-services.json` upstream removido.
- **Limitações ainda não resolvidas**: este APK usa chave de assinatura *debug efêmera* do runner. O verificador de atualizações existe, mas futuras atualizações de APK **não poderão instalar por cima sem uma chave privada estável**, sob pena de conflito de assinatura. Precisamos configurar armazenamento secreto de chave de assinatura antes de considerar atualização in-place pronta.
- **Não é um player híbrido Cobalt/SmartTube ainda.** Esta primeira prévia usa o player SmartTube; a ponte com Cobalt, VOLTAR/OK longos e possíveis melhorias na CazéTV ainda requerem engenharia e teste em dispositivo real.
- **Não testado na TCL P7K**: o sucesso da compilação não comprova qualidade de vídeo, 4K, foco, navegação, contas ou miniplayer.
