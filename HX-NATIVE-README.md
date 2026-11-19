# HX Native — prévia experimental 0.1.0

Este projeto é uma **prévia de interface própria para Android TV**, com nome e marca provisórios. Usa **TizenTube Cobalt 2.0.3** como motor existente, seu userscript de bloqueios/SponsorBlock e o controlador pessoal de miniplayer. A parte visual é independente em HTML/CSS/JS, mas **não constitui um novo motor de reprodução nem um player Android reescrito do zero**.

## Build e arquivos

- **Release da prévia:** [HX Native 0.1.0](https://github.com/helbermoura2/cobalt-script/releases/tag/hx-native-preview-v0.1.0)
- **APK:** `HX-Native-0.1.0-arm.apk`
- **Pacote isolado:** `io.gh.reisxd.tizentube.cobhn1` (não substitui o Cobalt HX anterior)
- **Interface:** `native-home.js` em `main`; fonte inicial embutida no `t.js` da referência `hn1`.
- **Compilação:** `build-hx-native.py` e `.github/workflows/hx-native-preview.yml`.

## Funcionalidades implementadas para o teste

- Tela inicial própria OLED/bronze, cards construídos a partir do catálogo da interface subjacente do YouTube TV, com miniaturas quando as URLs são acessíveis.
- Sem reproduzir prévia de vídeos automaticamente.
- Menu lateral Início, Pesquisar, Inscrições, Ao Vivo, Histórico, Biblioteca e Configurações.
- Navegação por controle remoto; toque curto em OK para selecionar. Algumas seções delegam à interface original temporariamente nesta prévia.
- Cobalt continua responsável por player, transmissão ao vivo, resolução, bloqueios, SponsorBlock e ações do controle.
- Atalho existente: segurar VOLTAR (~900 ms) durante vídeo para pedir miniplayer; segurar OK para abrir o menu original, incluindo retorno ao vídeo, quando disponível.
- Configurações com tema e efeitos leves, mais **Atualizar interface agora**. A atualização consulta `native-home.js` remoto, sem baixar APK. Pode haver atraso por cache do CDN.

## Limitações e transparência

- Este APK é **experimental** e não teve ainda validação em dispositivo TCL P7K.
- O catálogo depende da estrutura de HTML/DOM do YouTube TV. Se mudar, pode faltar miniatura, navegação, categoria ou abertura de vídeo.
- A opção Ao Vivo inicialmente filtra títulos do catálogo atual. Não é um catálogo global de todas as transmissões.
- As seções Pesquisar/Inscrições/Histórico/Biblioteca nesta prévia abrem temporariamente a navegação do YouTube TV de origem; não são telas nativas completas.
- **Atualizar interface** não é a mesma coisa que atualizar o APK. O Android exige instalador e a mesma assinatura para atualizar o aplicativo sem desinstalar. Este build usa assinatura temporária de testes; configurar uma assinatura permanente de forma privada é uma tarefa pendente.
- A reprodução de 4K, áudio, miniplayer e canais AO VIVO não foi modificada intencionalmente, mas deve ser testada no televisor. A CazéTV é a principal referência de teste.
- Evitar instalar sobre a versão anterior: o pacote desta prévia é isolado para coexistência.
- Não há marcas/logos definitivos. A criação da identidade será feita posteriormente.

## Checklist de validação na TCL P7K

1. Instalar o APK da prévia **sem remover o Cobalt HX anterior**.
2. Verificar se a página inicial própria aparece e se carrega imagens de quatro ou mais cards.
3. Testar setas, OK, VOLTAR, navegação lateral, Pesquisa e Configurações.
4. Abrir vídeo comum e verificar áudio, imagem, pausa, legendas e velocidade.
5. Testar transmissões da **CazéTV AO VIVO** nas mesmas condições do aplicativo anterior, principalmente atrasos/retrocessos e 2160p quando oferecido.
6. Durante vídeo, segurar VOLTAR por aproximadamente 900 ms e observar miniplayer.
7. Quando miniplayer ativo, segurar OK e testar Voltar ao vídeo em reprodução.
8. Ir a Configurações → Atualizar interface agora e confirmar resposta do atualizador.
9. Só depois dos testes acima prosseguir para nova arquitetura completa e assinatura permanente.

## Referências visuais

- [Taste Skill](https://www.tasteskill.dev/) — hierarquia, composição, diagramação.
- [Transitions.dev](https://transitions.dev/) — movimentos leves e reversíveis sem atrasar a navegação.
- [React Bits](https://www.reactbits.dev/) — padrões de feedback de foco e interações. Como a prévia usa a runtime Cobalt, o visual foi implementado com CSS próprio, sem carregar componentes React.
- [TizenTube Cobalt (original)](https://github.com/reisxd/TizenTubeCobalt) — motor e app de referência; cumprir licenças da base e bibliotecas incorporadas.
