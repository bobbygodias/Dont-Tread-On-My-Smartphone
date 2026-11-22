# Incorporação do MEGA3 Split Rescue no Don't Tread On My Smartphone

## Decisão e procedência
Solicitação de Bobby em 22/11/2026: recursos do `MEGA3-Split-Rescue` devem ser incorporados ao `Dont-Tread-On-My-Smartphone`; o `Soberania` é independente e fica intocado. Nunca tomar a existência de dois repositórios como intenção de manter dois aplicativos definitivos.

Fontes verificadas:
- `MEGA3-Split-Rescue/app/src/main/java/com/andrewvox/mega3splitrescue/MainActivity.java` — comandos de diagnóstico, flags globais e fluxo de reparo.
- `MEGA3-Split-Rescue/app/src/main/java/com/andrewvox/mega3splitrescue/SplitAccessibilityService.java` — ação global de alternar split-screen.
- `MEGA3-Split-Rescue/app/src/main/res/xml/split_accessibility_service.xml` — configuração de acessibilidade.
- `Dont-Tread-On-My-Smartphone/app/src/main/java/org/donttreadonmysmartphone/app/MainActivity.java` — UI atual, shell e tratamento de permissões.
- `Dont-Tread-On-My-Smartphone/app/src/main/java/org/donttreadonmysmartphone/app/ShellUserService.java` — execução remota com limite de tempo e saída.

## Escopo do porte (branch, não main)
- Ações de diagnóstico só de leitura: suporte multi-janela, saída real dos comandos, flags globais e estado do aparelho.
- Reparação de flags do MEGA3 com confirmação explícita e verificação por leitura de volta.
- Serviço de Acessibilidade opcional para executar apenas `GLOBAL_ACTION_TOGGLE_SPLIT_SCREEN` quando acionado; nenhum acesso a texto ou conteúdo de janela. Atributo `BIND_ACCESSIBILITY_SERVICE` limita chamadas externas de bind.
- Integração ao shell já presente no app principal, sem duplicar interfaces, serviços de execução ou criar um segundo aplicativo.
- Preservação da marca, package e UI do projeto de destino.

## Lacunas ainda reais
- **O dispositivo continua sem tela dividida**; não houve teste físico do porte.
- O APK 0.3 do app separado era de depuração e não validou o objetivo final.
- Segundo Bobby, o DokeOS não recebeu atualizações ou patches conhecidos no intervalo de falha; não inferir que o fabricante removeu o recurso.
- O suporte `Shizuku` ainda é necessário para comandos privilegiados. Ele não equivale a root; a ideia de diminuir dependências existe, mas não foi implementada neste porte.
- O projeto de destino usa `compileSdk 36`; ajustar dependências e plugins somente após evidência de CI.
- `wm set-multi-window-config` e comandos `am supports-...` podem não existir ou falhar em determinadas versões/OEMs. Falha deve aparecer no log, sem ser mascarada como sucesso.
- A ação de Acessibilidade retornar `true` indica aceitação da solicitação, não comprovação de que o layout foi dividido.

## Checklist de verificação
1. Compilar branch em CI e examinar logs, sem publicar release.
2. Conferir lint do manifesto, XML, ciclo de vida do serviço, permissões e mudança de telas.
3. Rodar diagnóstico *sem alterar configurações*, exportar log e identificar a razão da falha.
4. Com autorização e condição física adequada, testar alternador de Acessibilidade.
5. Comparar resultado com caminhos que já funcionaram (por exemplo Gmail + Firefox lado a lado relatado em 29/08/2026).
6. Só após compreender diagnóstico, testar comandos de reparo com confirmação e registro; avaliar se exigem reversão.
7. Confirmar split-screen manual e o lançamento de dois apps no tablet real. Só então considerar conclusão.
8. Atualizar issue #1 no repositório antigo e documentação do projeto principal, mantendo histórico da origem.

Sem comandos destrutivos, sem formatação, sem ações remotas no dispositivo. Branch/PR não significa merge realizado.
