# HX TV 0.9.16 — Recuperação urgente do player

## Evidência fornecida em 20/11/2026
- Foto do HX TV 0.9.15 na TCL P7K: Home Glass Studio, inscrições presentes no menu, títulos mais legíveis, menu e superfície grafite aceitáveis como base. Home com vídeo PiP no canto superior direito.
- Foto da reprodução: vídeo em tela inteira, comandos não utilizáveis e balão de ajuda "Configurações de qualidade de reprodução" fixo no canto inferior esquerdo.
- Relato explícito do usuário: não consegue voltar/retroceder vídeos, selecionar qualidade, usar botões do player ou selecionar vídeos das sugestões. Priorizar FUNCIONALIDADE antes de mais design.
- Usuário diz que o visual da Home "está quase lá", mas ainda deseja vidro realmente transparente/translúcido em iteração posterior.

## Diagnóstico de código (não é diagnóstico definitivo de execução)
A v0.9.15 modificou simultaneamente três partes da biblioteca Leanback que podem afetar foco e ações:
1. Atribuiu manualmente `setWindowAlignmentOffsetPercent`, `setWindowAlignment(WINDOW_ALIGN_NO_EDGE)` e `setItemAlignmentOffsetPercent` à grade das sugestões.
2. Alterou a altura e largura dos layouts nativos `lb_control_button_{primary,secondary}.xml`.
3. Removeu o desenho-base `setImageDrawable(null)` do ImageView interativo do controle.

O relatório e a foto tornam essas mudanças candidatas fortes; ainda não há logs de DPAD ou stacktrace para atribuir 100% a causa a uma delas.

## 0.9.16: recuperação conservadora
- **Restauração do player da 0.9.14**, última versão em que os comandos funcionavam segundo os reviews do usuário. Recoloca exatamente o código de controles Leanback e as dimensões nativas, com os handlers, drawables e StateListAnimator antigos.
- Retira o realinhamento manual e a modificação da keyline/seleção de sugestões. É a grade Leanback nativa que decide o foco.
- Limita a intervenção nas sugestões a aumentar a altura disponível de 46% para 68% do display (mínimo 340dp) e liberar clipChildren / clipToPadding; conserva as miniaturas estáveis 146×82dp da 0.9.14. Isso dá mais espaço para mostrar imagem sem mexer no controle remoto.
- **Preserva a Home Glass Studio e o menu da 0.9.15**: títulos altos, Inscrições verticais, paleta translúcida grafite, botões Pesquisa/Atualizar/Modo Clássico rolando no mesmo ScrollView.
- Evita novos ajustes do layout do player até haver um teste manual positivo na TCL.
- Versão 0.9.16, versionCode 2494, mesmo packageId e certificado persistente. OTA beta só deve mudar após APK ARM compilado, assinado, publicação, e arquivo baixado idêntico à build.
- Release 0.9.14 fica disponível como backup; 0.9.15 também permanece publicada.

## Teste manual obrigatório na TCL P7K (ordem)
1. Abrir vídeo e usar pausa/play, voltar/retroceder, avançar e barra de progresso pelo controle remoto.
2. Selecionar QUALIDADE, percorrer resoluções e confirmar mudança, inclusive 1080p e 4K se a fonte permitir.
3. Ir a SUGESTÕES, percorrer miniaturas com CIMA/BAIXO e ESQUERDA/DIREITA, pressionar OK e confirmar que abre o vídeo escolhido.
4. Voltar com BACK à Home e PiP e entrar novamente em outro vídeo.
5. Avaliar se as thumbnails realmente aparecem; se não, corrigir a composição posteriormente SEM voltar a mudar a geometria de foco.
6. Conferir áudio, legendas, histórico, Inscrições verticais, títulos e menu.
7. Não declarar o player recuperado apenas por build verde: sem registro do controle remoto, funcionalidade depende da TCL.

## Próximo passo de design, separado
Depois de confirmar estabilidade, trabalhar na transparência real dos elementos visuais, sem colocar um overlay web por cima do player e sem mexer na lógica das ações. Usar Taste Skill, Transitions.dev e React Bits como inspiração de princípios visuais, não componentes React embutidos na TV.
