# Dont-Tread-On-My-Smartphone

> **O aparelho é seu. O controle também deve ser.**

**Dont-Tread-On-My-Smartphone** é um aplicativo Android open source voltado à autonomia do proprietário do aparelho.

Ele usa **Shizuku** para disponibilizar, sem root, controles que normalmente exigiriam ADB via computador. Quando iniciado por Wireless Debugging/ADB, o backend do Shizuku opera como **UID 2000 (shell)**: isso oferece poderes relevantes de administração local, mas **não é root** e o projeto não finge que seja.

## Estado atual — v0.1 dev

A base funcional já inclui:

- autorização e reconexão com Shizuku;
- identificação do UID real do backend;
- listagem de aplicativos instalados com activity de lançamento;
- `FORCE_RESIZE_APP` por aplicativo;
- tentativa de abrir dois aplicativos em split screen;
- abertura explícita em tela cheia;
- override global `force_resizable_activities`;
- controles básicos de execução em segundo plano via AppOps / standby bucket;
- diagnóstico de split screen;
- diagnóstico de Device Policy / DPM;
- shell avançado limitado aos privilégios efetivos do Shizuku;
- saída bruta dos comandos para facilitar diagnóstico.

## Identidade técnica

```text
applicationId: org.donttreadonmysmartphone.app
namespace:     org.donttreadonmysmartphone.app
```

O projeto é independente do **Soberania**. São aplicativos e repositórios distintos.

## Aviso importante

Este software pode alterar configurações relevantes do Android. Algumas mudanças podem interferir no funcionamento de aplicativos ou do sistema.

O usuário deve saber o que está alterando, manter acesso aos meios de recuperação do próprio aparelho e assumir responsabilidade pelas ações executadas. O aplicativo deve sempre apresentar o comando e o resultado real do Android, em vez de esconder falhas ou prometer poderes inexistentes.

## Sobre Device Owner

O Android oferece provisionamento de Device Owner através de DPM, por exemplo:

```sh
adb shell dpm set-device-owner pacote/.DeviceAdminReceiver
```

Isso possui pré-condições impostas pelo próprio Android. Shizuku não elimina magicamente essas condições. O projeto trata Device Owner como uma capacidade separada e explícita, nunca como uma promessa falsa de "root sem root".

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
- compatibilidade ampla sempre que tecnicamente possível.

## Licença

Consulte [LICENSE](LICENSE).
