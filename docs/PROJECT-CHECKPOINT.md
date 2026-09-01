# PROJECT CHECKPOINT — Dont-Tread-On-My-Smartphone

**Atualizado:** 2026-09-01  
**Marco:** v0.2.0-dev — Owner Controls  
**Repositório correto:** https://github.com/bobbygodias/Dont-Tread-On-My-Smartphone

---

## REGRA DE IDENTIDADE

Este projeto NÃO é o Soberania.

```text
Dont-Tread-On-My-Smartphone
repository:    bobbygodias/Dont-Tread-On-My-Smartphone
applicationId: org.donttreadonmysmartphone.app
namespace:     org.donttreadonmysmartphone.app
module:        :app
```

O **Soberania** é outro aplicativo e outro repositório, dedicado a VPN, WireGuard, Arti/Tor, DNS protegido, kill switch e privacidade de rede.

---

## HISTÓRICO DE CORREÇÃO

O primeiro protótipo de controle foi colocado por engano dentro do Soberania como `:control` / `org.soberania.control`.

A mistura foi corrigida em 2026-09-01:

- Soberania voltou ao estado anterior ao módulo de controle;
- Dont-Tread-On-My-Smartphone recebeu o projeto Android standalone;
- package oficial passou a ser `org.donttreadonmysmartphone.app`.

Bootstrap correto integrado anteriormente:

```text
PR #1
merge: 518159711b15e080057b9eceda92f684d7ebb7a9
```

---

## v0.2.0-dev — IMPLEMENTADO

### Avisos e identidade
- aviso inicial obrigatório na primeira execução;
- banner de advertência permanente;
- cores Gadsden amarelo/preto;
- ícone Gadsden fornecido pelo projeto;
- `Fuck-You-Big-G!` no aviso e rodapé;
- zero telemetria.

### Shizuku
- detecção do binder;
- solicitação de autorização;
- UserService remoto;
- exibição do UID real;
- shell avançado;
- timeout e limite de captura de saída;
- comandos e respostas registrados no log local.

### Apps / pacotes
- lista ampliada de pacotes, inclusive desativados e sem launcher;
- indicação visual de pacote desativado / sem launcher;
- recarregar lista manualmente;
- abrir activity de lançamento quando disponível.

### Resize / multitarefa
- `FORCE_RESIZE_APP`;
- `FORCE_NON_RESIZE_APP`;
- restaurar overrides de janela;
- split A | B;
- tela cheia explícita;
- `force_resizable_activities` global ON / padrão;
- `enable_freeform_support` ON / padrão.

### Segundo plano / bateria
- `RUN_IN_BACKGROUND`;
- `RUN_ANY_IN_BACKGROUND`;
- standby bucket active / restricted;
- device-idle whitelist add / remove.

### AppOps / diagnóstico de pacote
- `cmd appops get`;
- `dumpsys package`;
- diagnóstico geral do aparelho;
- diagnóstico completo acumulável no relatório.

### Ativar / desativar aplicativo
- `pm disable-user --user 0` com confirmação;
- `pm enable --user 0`.

### Desinstalação
- fluxo oficial `ACTION_DELETE` do Android;
- aviso e confirmação antes de abrir o desinstalador.

### Instalação de APK
- `REQUEST_INSTALL_PACKAGES`;
- seletor `ACTION_OPEN_DOCUMENT`;
- aviso explícito sobre risco do APK;
- acesso a `ACTION_MANAGE_UNKNOWN_APP_SOURCES`;
- envio do APK escolhido ao Package Installer do Android.

### Device Administrator
- `DeviceAdminReceiver` próprio;
- metadata `android.app.device_admin`;
- ativação via `ACTION_ADD_DEVICE_ADMIN`;
- remoção voluntária do Device Admin;
- nenhuma tentativa silenciosa de virar Device Owner.

### DPM / Device Owner
- listar owners / admins quando suportado;
- `dumpsys device_policy`;
- usuários do aparelho;
- diagnóstico de contas / pré-condições;
- component preparado:
  `org.donttreadonmysmartphone.app/.OwnerDeviceAdminReceiver`.

### Log / TXT
- log local de sessão;
- timestamps;
- comandos;
- respostas;
- erros;
- informações do aparelho;
- Android / SDK;
- fabricante / modelo / fingerprint;
- Device Admin ativo ou não;
- Shizuku binder / UID / API / contexto SELinux;
- exportação manual via `ACTION_CREATE_DOCUMENT`;
- nenhum upload automático.

---

## CI DO v0.2

Branch de desenvolvimento:

```text
feature/v0.2-owner-controls
```

PR:

```text
#2 — app: add v0.2 owner controls
```

O GitHub Actions executou:

```text
gradle --no-daemon :app:assembleDebug
```

e o passo **Compile Dont-Tread-On-My-Smartphone** concluiu com sucesso, seguido de upload do APK.

---

## LIMITES ATUAIS

- Shizuku iniciado por ADB/Wireless Debugging continua sendo UID 2000 (shell), não root.
- Device Administrator não equivale a Device Owner.
- Device Owner exige pré-condições de provisionamento impostas pelo Android.
- Instalação de APK v0.2 passa pelo Package Installer oficial; não é instalação silenciosa.
- OEMs podem alterar ou bloquear comportamentos de janela, AppOps e DPM.
- alguns comandos podem existir em uma versão/OEM e não em outra; a saída real deve ser preservada no log.
- o app ainda depende de uma instalação funcional do Shizuku; integração do motor/starter do Shizuku dentro do próprio APK é trabalho futuro.

---

## PRÓXIMOS BLOCOS PRIORITÁRIOS

1. **Shizuku integrado / bootstrap sem PC**, pesquisando a arquitetura correta para reduzir dependência do app Shizuku externo.
2. **Instalador privilegiado via Shizuku**, mantendo aviso e confirmação explícitos.
3. **Gerenciamento de pacotes por usuário**, incluindo restaurar pacotes removidos para user 0.
4. **Permissões por aplicativo**, com UI clara para grant/revoke onde o shell realmente puder operar.
5. **AppOps editor**, não apenas diagnóstico.
6. **Device Owner guiado**, somente quando os pré-requisitos puderem ser verificados com segurança.
7. **Logs mais completos**, incluindo trechos de logcat filtrados do próprio app e comandos executados.
8. **Matriz de compatibilidade por Android/OEM**.
9. **Importar todos os tamanhos/adaptive icon** do pacote de ícones.
10. **Automação de release** quando a fase de testes no aparelho estiver estável.

---

## REGRA DE CONTINUIDADE PARA O ANDREW

Antes de qualquer mudança grande:

1. confirmar que o alvo é `bobbygodias/Dont-Tread-On-My-Smartphone`;
2. confirmar package `org.donttreadonmysmartphone.app`;
3. ler este checkpoint;
4. verificar a `main` real do GitHub;
5. criar branch;
6. implementar;
7. rodar CI;
8. só fazer merge com build verde;
9. atualizar este Markdown;
10. gerar um checkpoint baixável para ser anexado às fontes do projeto.

---

**Soberania ≠ Dont-Tread-On-My-Smartphone.**
