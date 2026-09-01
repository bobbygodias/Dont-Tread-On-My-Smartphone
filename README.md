# Dont-Tread-On-My-Smartphone

> **O aparelho é seu. O controle também deve ser.**

**Dont-Tread-On-My-Smartphone** é um aplicativo Android open source voltado à autonomia do proprietário do aparelho.

Ele usa **Shizuku** para disponibilizar, sem root obrigatório, controles que normalmente exigiriam ADB via computador. Quando iniciado por Wireless Debugging/ADB, o backend do Shizuku opera como **UID 2000 (shell)**: isso oferece poderes relevantes de administração local, mas **não é root** e o projeto não finge que seja.

## Estado atual — v0.2.0-dev

A versão atual já compila em CI e inclui:

- autorização e reconexão com Shizuku;
- identificação do UID real do backend;
- lista ampla de pacotes instalados, incluindo desativados e sem launcher;
- `FORCE_RESIZE_APP` e `FORCE_NON_RESIZE_APP` por aplicativo;
- tentativa de split screen entre dois aplicativos;
- abertura explícita em tela cheia;
- overrides globais de `force_resizable_activities` e `enable_freeform_support`;
- AppOps de segundo plano e standby bucket;
- inclusão/remoção da whitelist de otimização de bateria;
- diagnóstico de AppOps, permissões e pacote;
- ativação e reativação de pacotes para o usuário 0;
- desinstalação pelo fluxo oficial do Android com confirmação;
- instalação de APK escolhido pelo proprietário através do Package Installer;
- acesso direto à configuração “Instalar apps desconhecidos”;
- suporte opcional a **Device Administrator**;
- diagnóstico de DPM / Device Owner;
- shell avançado limitado aos privilégios efetivos do Shizuku;
- log local acumulado da sessão;
- relatório TXT exportável com dispositivo, Android, Shizuku, ações, comandos e resultados;
- aviso inicial obrigatório e aviso permanente na interface;
- identidade visual Gadsden em amarelo/preto e ícone próprio;
- frase **Fuck-You-Big-G!** no aviso e no rodapé.

## Identidade técnica

```text
applicationId: org.donttreadonmysmartphone.app
namespace:     org.donttreadonmysmartphone.app
module:        :app
version:       0.2.0-dev
```

O projeto é independente do **Soberania**. São aplicativos e repositórios distintos.

## Aviso importante

Este software pode alterar configurações relevantes do Android. Algumas mudanças podem interferir no funcionamento de aplicativos ou do sistema.

O usuário deve saber o que está alterando e assumir responsabilidade pelas ações executadas. O aplicativo procura mostrar o comando e o resultado real do Android, em vez de esconder falhas ou prometer poderes inexistentes.

APKs de fontes desconhecidas podem conter código malicioso. O projeto não verifica nem endossa arquivos escolhidos pelo usuário.

## Device Administrator e Device Owner

A v0.2 já pode solicitar registro opcional como **Device Administrator** através do fluxo oficial do Android.

Isso **não transforma** o aplicativo automaticamente em Device Owner e não concede root.

O Android oferece provisionamento de Device Owner através de DPM, por exemplo:

```sh
adb shell dpm set-device-owner pacote/.DeviceAdminReceiver
```

Esse provisionamento possui pré-condições impostas pelo Android. A versão atual diagnostica DPM / Device Owner, mas não força provisionamento silencioso.

## Instalação de APK

A v0.2 usa o seletor de documentos e o Package Installer do Android. Quando necessário, abre diretamente a permissão por aplicativo para **Instalar apps desconhecidos**.

O proprietário escolhe explicitamente o APK e recebe aviso antes da ação.

## Relatório TXT

O aplicativo mantém um log apenas em memória durante a sessão e permite exportá-lo manualmente como TXT.

O relatório inclui:

- timestamp;
- versão do app;
- versão / SDK do Android;
- fabricante e modelo;
- build fingerprint;
- estado de Device Admin;
- estado / UID / versão / contexto SELinux do Shizuku quando disponível;
- comandos executados;
- resultados e erros observados.

Nada é enviado automaticamente para servidores.

## Build

Na raiz do repositório:

```sh
gradle --no-daemon :app:assembleDebug
```

APK:

```text
app/build/outputs/apk/debug/app-debug.apk
```

## Princípios

- zero telemetria;
- nenhuma conta obrigatória;
- nenhuma coleta invisível;
- código aberto;
- privilégios exibidos com honestidade;
- autonomia do proprietário;
- avisos claros antes de ações de alto impacto;
- nenhuma ação privilegiada automática;
- compatibilidade ampla sempre que tecnicamente possível.

## Continuidade do projeto

Veja [docs/PROJECT-CHECKPOINT.md](docs/PROJECT-CHECKPOINT.md).

## Licença

Consulte [LICENSE](LICENSE).
