package org.donttreadonmysmartphone.app;

import static android.content.pm.PackageManager.PERMISSION_GRANTED;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.admin.DevicePolicyManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.os.RemoteException;
import android.provider.Settings;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import rikka.shizuku.Shizuku;

public class MainActivity extends Activity {

    private static final int SHIZUKU_REQUEST = 4701;
    private static final int REQUEST_PICK_APK = 5001;
    private static final int REQUEST_CREATE_LOG = 5002;
    private static final int REQUEST_DEVICE_ADMIN = 5003;

    private static final int GADSDEN_YELLOW = 0xFFFFD21C;
    private static final int DANGER_ORANGE = 0xFFFF9800;

    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private final List<AppEntry> apps = new ArrayList<>();
    private final StringBuilder sessionLog = new StringBuilder();

    private TextView status;
    private TextView output;
    private Spinner appA;
    private Spinner appB;
    private EditText manualCommand;

    private IControlService controlService;
    private Uri selectedApkUri;

    private DevicePolicyManager devicePolicyManager;
    private ComponentName adminComponent;

    private final Shizuku.OnBinderReceivedListener binderReceived = () -> {
        renderStatus("Shizuku detectado. Verificando autorização...");
        ensureShizukuPermission();
    };

    private final Shizuku.OnBinderDeadListener binderDead = () -> {
        controlService = null;
        renderStatus("Shizuku desconectado.");
    };

    private final Shizuku.OnRequestPermissionResultListener permissionResult = (requestCode, grantResult) -> {
        if (requestCode != SHIZUKU_REQUEST) return;
        if (grantResult == PERMISSION_GRANTED) {
            bindShellService();
        } else {
            renderStatus("Permissão do Shizuku negada.");
        }
    };

    private final ServiceConnection serviceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder binder) {
            controlService = IControlService.Stub.asInterface(binder);
            try {
                int remoteUid = controlService.uid();
                renderStatus("Pronto. Serviço remoto UID " + remoteUid +
                        (remoteUid == 2000 ? " (shell/ADB)." : remoteUid == 0 ? " (root)." : "."));
            } catch (RemoteException e) {
                renderStatus("Serviço conectado, mas UID indisponível: " + e.getMessage());
            }
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            controlService = null;
            renderStatus("Serviço shell desconectado.");
        }
    };

    private final Shizuku.UserServiceArgs userServiceArgs =
            new Shizuku.UserServiceArgs(
                    new ComponentName(BuildConfig.APPLICATION_ID, ShellUserService.class.getName()))
                    .daemon(false)
                    .processNameSuffix("shell")
                    .debuggable(BuildConfig.DEBUG)
                    .version(BuildConfig.VERSION_CODE);

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        devicePolicyManager = (DevicePolicyManager) getSystemService(Context.DEVICE_POLICY_SERVICE);
        adminComponent = new ComponentName(this, OwnerDeviceAdminReceiver.class);

        appendLog("=== sessão iniciada ===");
        appendLog("app=" + BuildConfig.APPLICATION_ID + " version=" + BuildConfig.VERSION_NAME);

        setContentView(buildUi());
        loadInstalledApps();
        showFirstRunWarningIfNeeded();

        Shizuku.addBinderReceivedListenerSticky(binderReceived);
        Shizuku.addBinderDeadListener(binderDead);
        Shizuku.addRequestPermissionResultListener(permissionResult);

        if (!Shizuku.pingBinder()) {
            renderStatus("Shizuku ainda não está disponível. Inicie-o e volte para cá.");
        }
    }

    @Override
    protected void onDestroy() {
        Shizuku.removeBinderReceivedListener(binderReceived);
        Shizuku.removeBinderDeadListener(binderDead);
        Shizuku.removeRequestPermissionResultListener(permissionResult);
        executor.shutdownNow();
        super.onDestroy();
    }

    @Override
    @SuppressWarnings("deprecation")
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == REQUEST_PICK_APK && resultCode == RESULT_OK && data != null) {
            selectedApkUri = data.getData();
            if (selectedApkUri != null) {
                try {
                    getContentResolver().takePersistableUriPermission(
                            selectedApkUri,
                            Intent.FLAG_GRANT_READ_URI_PERMISSION);
                } catch (Throwable ignored) {
                }
                appendLog("APK selecionado: " + selectedApkUri);
                installSelectedApk();
            }
            return;
        }

        if (requestCode == REQUEST_CREATE_LOG && resultCode == RESULT_OK && data != null) {
            Uri uri = data.getData();
            if (uri != null) {
                writeReport(uri);
            }
            return;
        }

        if (requestCode == REQUEST_DEVICE_ADMIN) {
            appendLog("Retorno da tela de Device Admin. ativo=" + isDeviceAdminActive());
            refreshAdminStatus();
        }
    }

    private View buildUi() {
        ScrollView scroll = new ScrollView(this);
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(Color.rgb(250, 250, 245));
        int p = dp(14);
        root.setPadding(p, p, p, p);
        scroll.addView(root);

        TextView title = new TextView(this);
        title.setText("DONT-TREAD-ON-MY-SMARTPHONE");
        title.setTextSize(22);
        title.setTypeface(Typeface.DEFAULT_BOLD);
        title.setTextColor(Color.BLACK);
        root.addView(title);

        TextView subtitle = new TextView(this);
        subtitle.setText("Sem root obrigatório. Shizuku. Zero telemetria. O aparelho é seu.");
        subtitle.setPadding(0, dp(4), 0, dp(10));
        root.addView(subtitle);

        TextView warning = new TextView(this);
        warning.setBackgroundColor(GADSDEN_YELLOW);
        warning.setTextColor(Color.BLACK);
        warning.setTypeface(Typeface.DEFAULT_BOLD);
        warning.setPadding(dp(12), dp(12), dp(12), dp(12));
        warning.setText(
                "ATENÇÃO: este aplicativo altera configurações reais do Android. " +
                "Algumas ações podem impedir apps de abrir, remover aplicativos ou mudar políticas do sistema. " +
                "Leia a ação antes de executar. Você é responsável pelas escolhas feitas no seu próprio aparelho.\n\n" +
                "Fuck-You-Big-G!");
        root.addView(warning);

        status = new TextView(this);
        status.setText("Inicializando...");
        status.setTextIsSelectable(true);
        status.setPadding(0, dp(10), 0, dp(4));
        root.addView(status);

        root.addView(button("Autorizar / reconectar Shizuku", v -> ensureShizukuPermission()));
        root.addView(button("Abrir Opções do desenvolvedor", v -> openDeveloperSettings()));
        root.addView(button("Diagnóstico do aparelho", v -> runShell(diagnosticCommand())));
        root.addView(button("Recarregar lista de aplicativos", v -> loadInstalledApps()));

        addHeader(root, "Aplicativo A");
        appA = new Spinner(this);
        root.addView(appA);

        addHeader(root, "Aplicativo B");
        appB = new Spinner(this);
        root.addView(appB);

        addHeader(root, "Janelas / multitarefa");
        root.addView(button("A: FORÇAR REDIMENSIONAMENTO", v ->
                withA(a -> runShell("am compat enable FORCE_RESIZE_APP " + sq(a.packageName)))));

        root.addView(dangerButton("A: IMPEDIR REDIMENSIONAMENTO", v ->
                withA(a -> confirm(
                        "Impedir redimensionamento?",
                        "Isso força o Android a tratar o aplicativo como não redimensionável. Pode impedir split screen.",
                        () -> runShell(
                                "am compat enable FORCE_NON_RESIZE_APP " + sq(a.packageName) +
                                "; am compat disable FORCE_RESIZE_APP " + sq(a.packageName))))));

        root.addView(button("A: RESTAURAR POLÍTICA DE JANELA", v ->
                withA(a -> runShell(
                        "am compat disable FORCE_RESIZE_APP " + sq(a.packageName) +
                        "; am compat disable FORCE_NON_RESIZE_APP " + sq(a.packageName)))));

        root.addView(button("DIVIDIR A | B", v -> splitSelectedApps()));

        root.addView(button("A: ABRIR EM TELA CHEIA", v ->
                withLaunchableA(a -> runShell(
                        "am start -W --windowingMode 1 -n " + sq(a.component.flattenToString())))));

        addHeader(root, "Compatibilidade global");
        root.addView(button("Forçar apps redimensionáveis: ON", v ->
                runShell("settings put global force_resizable_activities 1; " +
                        "settings get global force_resizable_activities")));

        root.addView(button("Forçar apps redimensionáveis: PADRÃO", v ->
                runShell("settings delete global force_resizable_activities; " +
                        "settings get global force_resizable_activities")));

        root.addView(button("Janelas livres / freeform: ON", v ->
                runShell("settings put global enable_freeform_support 1; " +
                        "settings get global enable_freeform_support")));

        root.addView(button("Janelas livres / freeform: PADRÃO", v ->
                runShell("settings delete global enable_freeform_support; " +
                        "settings get global enable_freeform_support")));

        addHeader(root, "Segundo plano / bateria — Aplicativo A");
        root.addView(button("A: LIBERAR SEGUNDO PLANO", v ->
                withA(a -> runShell(
                        "cmd appops set " + sq(a.packageName) + " RUN_IN_BACKGROUND allow; " +
                        "cmd appops set " + sq(a.packageName) + " RUN_ANY_IN_BACKGROUND allow; " +
                        "am set-standby-bucket " + sq(a.packageName) + " active; " +
                        "am get-standby-bucket " + sq(a.packageName)))));

        root.addView(button("A: RESTRINGIR SEGUNDO PLANO", v ->
                withA(a -> runShell(
                        "cmd appops set " + sq(a.packageName) + " RUN_IN_BACKGROUND ignore; " +
                        "cmd appops set " + sq(a.packageName) + " RUN_ANY_IN_BACKGROUND ignore; " +
                        "am set-standby-bucket " + sq(a.packageName) + " restricted; " +
                        "am get-standby-bucket " + sq(a.packageName)))));

        root.addView(button("A: IGNORAR OTIMIZAÇÃO DE BATERIA", v ->
                withA(a -> runShell(
                        "dumpsys deviceidle whitelist +" + a.packageName + "; " +
                        "dumpsys deviceidle whitelist | grep -F " + sq(a.packageName)))));

        root.addView(button("A: RESTAURAR OTIMIZAÇÃO DE BATERIA", v ->
                withA(a -> runShell(
                        "dumpsys deviceidle whitelist -" + a.packageName + "; " +
                        "dumpsys deviceidle whitelist | grep -F " + sq(a.packageName)))));

        addHeader(root, "AppOps / permissões — Aplicativo A");
        root.addView(button("A: MOSTRAR APPOPS", v ->
                withA(a -> runShell("cmd appops get " + sq(a.packageName)))));

        root.addView(button("A: MOSTRAR PACOTE / PERMISSÕES", v ->
                withA(a -> runShell(
                        "dumpsys package " + sq(a.packageName) +
                        " | head -n 260"))));

        addHeader(root, "Estado do aplicativo A");
        root.addView(dangerButton("A: DESATIVAR PARA O USUÁRIO 0", v ->
                withA(a -> confirm(
                        "Desativar " + a.label + "?",
                        "O aplicativo pode desaparecer do launcher e deixar de funcionar até ser reativado.",
                        () -> runShell(
                                "pm disable-user --user 0 " + sq(a.packageName) +
                                "; pm list packages -d | grep -F " + sq(a.packageName))))));

        root.addView(button("A: REATIVAR", v ->
                withA(a -> runShell(
                        "pm enable --user 0 " + sq(a.packageName) +
                        "; pm list packages -e | grep -F " + sq(a.packageName)))));

        root.addView(dangerButton("A: DESINSTALAR PELO ANDROID", v ->
                withA(this::requestAndroidUninstall)));

        addHeader(root, "Instalar APK escolhido pelo proprietário");
        root.addView(button("Selecionar APK e instalar", v -> chooseApk()));
        root.addView(button("Abrir permissão 'Instalar apps desconhecidos'", v -> openUnknownSourcesSettings()));

        addHeader(root, "Administrador do dispositivo / DPM");
        root.addView(button("Ativar como administrador do dispositivo", v -> requestDeviceAdmin()));
        root.addView(dangerButton("Remover administrador do dispositivo", v -> removeDeviceAdmin()));
        root.addView(button("Mostrar proprietários / administradores", v ->
                runShell("dpm list-owners 2>&1 || dpm list active-admins 2>&1 || dumpsys device_policy | head -n 180")));
        root.addView(button("Diagnosticar Device Owner", v ->
                runShell(deviceOwnerDiagnosticCommand())));

        addHeader(root, "Relatório / log local");
        root.addView(button("GERAR DIAGNÓSTICO COMPLETO", v -> runShell(fullDiagnosticCommand())));
        root.addView(button("EXPORTAR RELATÓRIO TXT", v -> exportReport()));
        root.addView(button("LIMPAR LOG DESTA SESSÃO", v -> clearSessionLog()));

        addHeader(root, "Shell avançado (UID do Shizuku)");
        manualCommand = new EditText(this);
        manualCommand.setSingleLine(false);
        manualCommand.setMinLines(2);
        manualCommand.setHint("Ex.: settings get global force_resizable_activities");
        root.addView(manualCommand);
        root.addView(button("EXECUTAR COMANDO", v -> runManualCommand()));

        addHeader(root, "Saída");
        output = new TextView(this);
        output.setTypeface(Typeface.MONOSPACE);
        output.setTextIsSelectable(true);
        output.setMinLines(8);
        output.setPadding(0, dp(6), 0, dp(20));
        root.addView(output);

        TextView footer = new TextView(this);
        footer.setText("Fuck-You-Big-G!");
        footer.setTextColor(Color.BLACK);
        footer.setBackgroundColor(GADSDEN_YELLOW);
        footer.setGravity(android.view.Gravity.CENTER);
        footer.setTypeface(Typeface.DEFAULT_BOLD);
        footer.setTextSize(20);
        footer.setPadding(dp(8), dp(14), dp(8), dp(14));
        root.addView(footer);

        return scroll;
    }

    private void showFirstRunWarningIfNeeded() {
        boolean accepted = getSharedPreferences("dtoms", MODE_PRIVATE)
                .getBoolean("warningAccepted", false);
        if (accepted) return;

        new AlertDialog.Builder(this)
                .setTitle("ATENÇÃO — CONTROLE REAL DO ANDROID")
                .setMessage(
                        "Dont-Tread-On-My-Smartphone executa ações reais no seu aparelho. " +
                        "Algumas funções podem alterar políticas de janela, segundo plano, bateria, " +
                        "desativar ou desinstalar aplicativos.\n\n" +
                        "O projeto não coleta seus dados e não age sozinho. Você escolhe cada ação.\n\n" +
                        "Fuck-You-Big-G!")
                .setCancelable(false)
                .setPositiveButton("LI E ENTENDI", (dialog, which) -> {
                    getSharedPreferences("dtoms", MODE_PRIVATE)
                            .edit()
                            .putBoolean("warningAccepted", true)
                            .apply();
                    appendLog("Aviso inicial aceito.");
                })
                .show();
    }

    private void addHeader(LinearLayout root, String text) {
        TextView v = new TextView(this);
        v.setText(text);
        v.setTypeface(Typeface.DEFAULT_BOLD);
        v.setTextSize(17);
        v.setTextColor(Color.BLACK);
        v.setPadding(0, dp(18), 0, dp(6));
        root.addView(v);
    }

    private Button button(String text, View.OnClickListener listener) {
        Button b = new Button(this);
        b.setText(text);
        b.setAllCaps(false);
        b.setTextColor(Color.BLACK);
        b.setBackgroundColor(GADSDEN_YELLOW);
        b.setOnClickListener(listener);
        return b;
    }

    private Button dangerButton(String text, View.OnClickListener listener) {
        Button b = button(text, listener);
        b.setBackgroundColor(DANGER_ORANGE);
        return b;
    }

    @SuppressWarnings("deprecation")
    private void loadInstalledApps() {
        PackageManager pm = getPackageManager();
        List<ApplicationInfo> installed = pm.getInstalledApplications(PackageManager.MATCH_DISABLED_COMPONENTS);
        apps.clear();

        for (ApplicationInfo info : installed) {
            Intent launch = pm.getLaunchIntentForPackage(info.packageName);
            ComponentName component = launch == null ? null : launch.getComponent();
            CharSequence label = pm.getApplicationLabel(info);
            apps.add(new AppEntry(
                    label == null ? info.packageName : label.toString(),
                    info.packageName,
                    component,
                    info.enabled));
        }

        apps.sort(Comparator
                .comparing((AppEntry e) -> e.label, String.CASE_INSENSITIVE_ORDER)
                .thenComparing(e -> e.packageName));

        ArrayAdapter<AppEntry> adapterA = new ArrayAdapter<>(
                this, android.R.layout.simple_spinner_item, new ArrayList<>(apps));
        adapterA.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        appA.setAdapter(adapterA);

        ArrayAdapter<AppEntry> adapterB = new ArrayAdapter<>(
                this, android.R.layout.simple_spinner_item, new ArrayList<>(apps));
        adapterB.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        appB.setAdapter(adapterB);

        appendLog("Lista de apps carregada: " + apps.size());
        toast("Aplicativos carregados: " + apps.size());
    }

    private void ensureShizukuPermission() {
        if (!Shizuku.pingBinder()) {
            renderStatus("Shizuku não está rodando.");
            return;
        }

        try {
            if (Shizuku.checkSelfPermission() == PERMISSION_GRANTED) {
                bindShellService();
            } else if (Shizuku.shouldShowRequestPermissionRationale()) {
                renderStatus("Shizuku está rodando, mas a autorização foi negada. Libere no gerenciador Shizuku.");
            } else {
                renderStatus("Solicitando autorização ao Shizuku...");
                Shizuku.requestPermission(SHIZUKU_REQUEST);
            }
        } catch (Throwable t) {
            renderStatus("Falha ao falar com Shizuku: " + t.getMessage());
        }
    }

    private void bindShellService() {
        try {
            renderStatus("Autorizado. Iniciando serviço shell...");
            Shizuku.bindUserService(userServiceArgs, serviceConnection);
        } catch (Throwable t) {
            renderStatus("Não foi possível iniciar o serviço shell: " + t);
        }
    }

    private void splitSelectedApps() {
        AppEntry a = selected(appA);
        AppEntry b = selected(appB);
        if (a == null || b == null) return;
        if (!a.isLaunchable() || !b.isLaunchable()) {
            toast("Os dois aplicativos precisam ter activity de lançamento.");
            return;
        }
        if (a.packageName.equals(b.packageName)) {
            toast("Escolha dois aplicativos diferentes.");
            return;
        }

        String command =
                "echo '[1/6] compat A'; " +
                "am compat enable FORCE_RESIZE_APP " + sq(a.packageName) + "; " +
                "echo '[2/6] compat B'; " +
                "am compat enable FORCE_RESIZE_APP " + sq(b.packageName) + "; " +
                "echo '[3/6] suporte do aparelho'; " +
                "am supports-split-screen-multi-window 2>&1; " +
                "echo '[4/6] abrindo A como primário'; " +
                "am start -W --windowingMode 3 -n " + sq(a.component.flattenToString()) + " 2>&1; " +
                "sleep 1; " +
                "echo '[5/6] abrindo B como secundário'; " +
                "am start -W --windowingMode 4 -n " + sq(b.component.flattenToString()) + " 2>&1; " +
                "echo '[6/6] estado resumido'; " +
                "dumpsys activity activities 2>/dev/null | grep -E 'mResumedActivity|topResumedActivity|windowingMode' | head -n 60";

        runShell(command);
    }

    private String diagnosticCommand() {
        return "echo '=== DONT-TREAD-ON-MY-SMARTPHONE ==='; " +
                "echo SDK=$(getprop ro.build.version.sdk); " +
                "echo RELEASE=$(getprop ro.build.version.release); " +
                "echo FABRICANTE=$(getprop ro.product.manufacturer); " +
                "echo MODELO=$(getprop ro.product.model); " +
                "echo '--- split-screen ---'; " +
                "am supports-split-screen-multi-window 2>&1; " +
                "echo '--- force_resizable_activities ---'; " +
                "settings get global force_resizable_activities; " +
                "echo '--- enable_freeform_support ---'; " +
                "settings get global enable_freeform_support; " +
                "echo '--- DPM owners ---'; " +
                "(dpm list-owners 2>&1 || true)";
    }

    private String deviceOwnerDiagnosticCommand() {
        return "echo '=== DEVICE OWNER / DPM ==='; " +
                "echo COMPONENT=" + BuildConfig.APPLICATION_ID + "/.OwnerDeviceAdminReceiver; " +
                "(dpm list-owners 2>&1 || true); " +
                "echo '--- active admins ---'; " +
                "(dpm list active-admins 2>&1 || true); " +
                "echo '--- users ---'; pm list users; " +
                "echo '--- accounts hint ---'; " +
                "dumpsys account 2>/dev/null | head -n 80";
    }

    private String fullDiagnosticCommand() {
        AppEntry a = selectedSilently(appA);
        String selected = a == null ? "" :
                "; echo '--- SELECTED APP ---'; " +
                "echo PACKAGE=" + a.packageName + "; " +
                "cmd appops get " + sq(a.packageName) + " 2>&1 | head -n 160; " +
                "dumpsys package " + sq(a.packageName) + " 2>&1 | head -n 220";

        return diagnosticCommand() +
                "; echo '--- users ---'; pm list users" +
                "; echo '--- device idle whitelist ---'; dumpsys deviceidle whitelist 2>&1 | head -n 160" +
                "; echo '--- package installer ---'; dumpsys package installer 2>&1 | head -n 120" +
                "; echo '--- device policy ---'; dumpsys device_policy 2>&1 | head -n 220" +
                selected;
    }

    private void runManualCommand() {
        String cmd = manualCommand.getText().toString().trim();
        if (cmd.isEmpty()) {
            toast("Digite um comando.");
            return;
        }
        runShell(cmd);
    }

    private void runShell(String command) {
        IControlService service = controlService;
        if (service == null) {
            renderStatus("Serviço shell não está pronto. Tentando reconectar...");
            ensureShizukuPermission();
            return;
        }

        appendLog("$ " + command);
        output.setText("$ " + command + "\n\nexecutando...");
        executor.execute(() -> {
            String result;
            try {
                result = service.exec(command);
            } catch (Throwable t) {
                result = t.getClass().getSimpleName() + ": " + t.getMessage();
                controlService = null;
            }

            appendLog(result);
            String finalResult = result;
            runOnUiThread(() -> output.setText("$ " + command + "\n\n" + finalResult));
        });
    }

    private void chooseApk() {
        new AlertDialog.Builder(this)
                .setTitle("Instalar APK escolhido por você")
                .setMessage(
                        "APK de fonte desconhecida pode conter código malicioso, roubar dados ou danificar sua experiência. " +
                        "O Dont-Tread-On-My-Smartphone não verifica nem endossa o arquivo escolhido. " +
                        "A escolha e a responsabilidade são do proprietário do aparelho.")
                .setNegativeButton("Cancelar", null)
                .setPositiveButton("Escolher APK", (dialog, which) -> {
                    Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
                    intent.addCategory(Intent.CATEGORY_OPENABLE);
                    intent.setType("application/vnd.android.package-archive");
                    intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                    startActivityForResult(intent, REQUEST_PICK_APK);
                })
                .show();
    }

    private void installSelectedApk() {
        if (selectedApkUri == null) {
            toast("Nenhum APK selecionado.");
            return;
        }

        if (!getPackageManager().canRequestPackageInstalls()) {
            appendLog("Instalação de fontes desconhecidas ainda não autorizada para este app.");
            new AlertDialog.Builder(this)
                    .setTitle("Permissão necessária")
                    .setMessage(
                            "O Android exige autorização explícita para que este aplicativo envie APKs ao instalador. " +
                            "Abra a configuração, habilite a permissão e depois use 'Selecionar APK e instalar' novamente.")
                    .setNegativeButton("Agora não", null)
                    .setPositiveButton("Abrir configuração", (dialog, which) -> openUnknownSourcesSettings())
                    .show();
            return;
        }

        try {
            Intent install = new Intent(Intent.ACTION_VIEW);
            install.setDataAndType(selectedApkUri, "application/vnd.android.package-archive");
            install.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            appendLog("Enviando APK ao Package Installer: " + selectedApkUri);
            startActivity(install);
        } catch (Throwable t) {
            appendLog("Falha ao abrir instalador: " + t);
            toast("Falha ao abrir o instalador: " + t.getMessage());
        }
    }

    private void openUnknownSourcesSettings() {
        try {
            Intent intent = new Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES);
            intent.setData(Uri.parse("package:" + getPackageName()));
            appendLog("Abrindo configuração de fontes desconhecidas.");
            startActivity(intent);
        } catch (Throwable t) {
            appendLog("Falha ao abrir fontes desconhecidas: " + t);
            toast("Não consegui abrir essa configuração.");
        }
    }

    private void requestAndroidUninstall(AppEntry app) {
        confirm(
                "Desinstalar " + app.label + "?",
                "O pedido será entregue ao desinstalador oficial do Android. O sistema exibirá a confirmação final.",
                () -> {
                    try {
                        Intent intent = new Intent(Intent.ACTION_DELETE);
                        intent.setData(Uri.parse("package:" + app.packageName));
                        appendLog("Solicitando desinstalação Android: " + app.packageName);
                        startActivity(intent);
                    } catch (Throwable t) {
                        appendLog("Falha ao solicitar desinstalação: " + t);
                        toast("Não foi possível abrir o desinstalador.");
                    }
                });
    }

    private void requestDeviceAdmin() {
        if (isDeviceAdminActive()) {
            toast("Este app já está ativo como administrador do dispositivo.");
            appendLog("Device Admin já estava ativo.");
            return;
        }

        try {
            Intent intent = new Intent(DevicePolicyManager.ACTION_ADD_DEVICE_ADMIN);
            intent.putExtra(DevicePolicyManager.EXTRA_DEVICE_ADMIN, adminComponent);
            intent.putExtra(
                    DevicePolicyManager.EXTRA_ADD_EXPLANATION,
                    "Administração opcional do próprio aparelho. Não concede root nem Device Owner automaticamente.");
            appendLog("Solicitando ativação como Device Admin.");
            startActivityForResult(intent, REQUEST_DEVICE_ADMIN);
        } catch (Throwable t) {
            appendLog("Falha ao solicitar Device Admin: " + t);
            toast("Não foi possível abrir a ativação de administrador.");
        }
    }

    private void removeDeviceAdmin() {
        if (!isDeviceAdminActive()) {
            toast("Este app não está ativo como administrador.");
            return;
        }

        confirm(
                "Remover administrador?",
                "Isso remove apenas o papel de Device Admin deste aplicativo. Não altera Shizuku nem root.",
                () -> {
                    try {
                        devicePolicyManager.removeActiveAdmin(adminComponent);
                        appendLog("Device Admin removido.");
                        refreshAdminStatus();
                    } catch (Throwable t) {
                        appendLog("Falha ao remover Device Admin: " + t);
                        toast("Falha ao remover administrador: " + t.getMessage());
                    }
                });
    }

    private boolean isDeviceAdminActive() {
        return devicePolicyManager != null && devicePolicyManager.isAdminActive(adminComponent);
    }

    private void refreshAdminStatus() {
        renderStatus("Device Admin ativo: " + isDeviceAdminActive());
    }

    private void exportReport() {
        Intent intent = new Intent(Intent.ACTION_CREATE_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("text/plain");
        intent.putExtra(Intent.EXTRA_TITLE,
                "Dont-Tread-On-My-Smartphone-" + timestampForFile() + ".txt");
        startActivityForResult(intent, REQUEST_CREATE_LOG);
    }

    private void writeReport(Uri uri) {
        executor.execute(() -> {
            try (OutputStream out = getContentResolver().openOutputStream(uri, "wt")) {
                if (out == null) throw new IllegalStateException("OutputStream indisponível.");
                String report = buildReportText();
                out.write(report.getBytes(StandardCharsets.UTF_8));
                out.flush();
                appendLog("Relatório exportado: " + uri);
                runOnUiThread(() -> toast("Relatório TXT salvo."));
            } catch (Throwable t) {
                appendLog("Falha ao exportar relatório: " + t);
                runOnUiThread(() -> toast("Falha ao salvar relatório: " + t.getMessage()));
            }
        });
    }

    private String buildReportText() {
        StringBuilder report = new StringBuilder();
        report.append("Dont-Tread-On-My-Smartphone — relatório local\n");
        report.append("Gerado em: ").append(now()).append("\n");
        report.append("App: ").append(BuildConfig.APPLICATION_ID)
                .append(" / ").append(BuildConfig.VERSION_NAME).append("\n");
        report.append("Android: ").append(Build.VERSION.RELEASE)
                .append(" / SDK ").append(Build.VERSION.SDK_INT).append("\n");
        report.append("Fabricante: ").append(Build.MANUFACTURER).append("\n");
        report.append("Modelo: ").append(Build.MODEL).append("\n");
        report.append("Fingerprint: ").append(Build.FINGERPRINT).append("\n");
        report.append("Device Admin ativo: ").append(isDeviceAdminActive()).append("\n");

        try {
            report.append("Shizuku binder: ").append(Shizuku.pingBinder()).append("\n");
            if (Shizuku.pingBinder()) {
                report.append("Shizuku UID: ").append(Shizuku.getUid()).append("\n");
                report.append("Shizuku API: ").append(Shizuku.getVersion()).append("\n");
                report.append("SELinux context: ").append(Shizuku.getSELinuxContext()).append("\n");
            }
        } catch (Throwable t) {
            report.append("Shizuku diagnóstico: ").append(t).append("\n");
        }

        report.append("\n=== LOG DA SESSÃO ===\n");
        synchronized (sessionLog) {
            report.append(sessionLog);
        }
        return report.toString();
    }

    private void clearSessionLog() {
        synchronized (sessionLog) {
            sessionLog.setLength(0);
        }
        appendLog("Log limpo pelo usuário.");
        if (output != null) output.setText("Log da sessão limpo.");
    }

    private void appendLog(String text) {
        synchronized (sessionLog) {
            sessionLog.append("[").append(now()).append("] ")
                    .append(text == null ? "null" : text)
                    .append("\n");
        }
    }

    private void openDeveloperSettings() {
        try {
            startActivity(new Intent(Settings.ACTION_APPLICATION_DEVELOPMENT_SETTINGS));
        } catch (Throwable t) {
            toast("Não consegui abrir as opções do desenvolvedor.");
        }
    }

    private void confirm(String title, String message, Runnable action) {
        new AlertDialog.Builder(this)
                .setTitle(title)
                .setMessage(message)
                .setNegativeButton("Cancelar", null)
                .setPositiveButton("Executar", (dialog, which) -> action.run())
                .show();
    }

    private void withA(AppConsumer consumer) {
        AppEntry a = selected(appA);
        if (a != null) consumer.accept(a);
    }

    private void withLaunchableA(AppConsumer consumer) {
        AppEntry a = selected(appA);
        if (a == null) return;
        if (!a.isLaunchable()) {
            toast("Este pacote não possui activity de lançamento disponível.");
            return;
        }
        consumer.accept(a);
    }

    private AppEntry selected(Spinner spinner) {
        AppEntry entry = selectedSilently(spinner);
        if (entry == null) {
            toast("Nenhum aplicativo selecionado.");
        }
        return entry;
    }

    private AppEntry selectedSilently(Spinner spinner) {
        if (spinner == null) return null;
        Object item = spinner.getSelectedItem();
        return item instanceof AppEntry ? (AppEntry) item : null;
    }

    private void renderStatus(String text) {
        appendLog("STATUS: " + text);
        runOnUiThread(() -> {
            if (status != null) status.setText(text);
            if (output != null && output.getText().length() == 0) {
                output.setText(text);
            }
        });
    }

    private void toast(String text) {
        Toast.makeText(this, text, Toast.LENGTH_SHORT).show();
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }

    private static String sq(String value) {
        return "'" + value.replace("'", "'\\''") + "'";
    }

    private static String now() {
        return new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US).format(new Date());
    }

    private static String timestampForFile() {
        return new SimpleDateFormat("yyyyMMdd-HHmmss", Locale.US).format(new Date());
    }

    private interface AppConsumer {
        void accept(AppEntry app);
    }

    private static final class AppEntry {
        final String label;
        final String packageName;
        final ComponentName component;
        final boolean enabled;

        AppEntry(String label, String packageName, ComponentName component, boolean enabled) {
            this.label = label;
            this.packageName = packageName;
            this.component = component;
            this.enabled = enabled;
        }

        boolean isLaunchable() {
            return component != null;
        }

        @Override
        public String toString() {
            String state = enabled ? "" : " [DESATIVADO]";
            String launch = component == null ? " [SEM LAUNCHER]" : "";
            return label + " — " + packageName + state + launch;
        }
    }
}
