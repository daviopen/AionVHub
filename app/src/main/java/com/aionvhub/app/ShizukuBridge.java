package com.aionvhub.app;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.IBinder;
import android.provider.Settings;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import rikka.shizuku.Shizuku;

/**
 * Integração nativa do AION V Hub com Shizuku.
 * O Hub funciona normalmente sem Shizuku; esta classe apenas libera diagnóstico
 * privilegiado quando o usuário já iniciou e autorizou o serviço Shizuku.
 */
public final class ShizukuBridge {
    public static final int REQUEST_CODE = 6013;

    public interface Listener {
        void onStateChanged();
        void onOutput(String title, String output);
    }

    private final Activity activity;
    private final Listener listener;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private IPrivilegedBridge remote;
    private boolean binding;
    private Runnable pendingReadyAction;

    private final Shizuku.OnBinderReceivedListener binderReceivedListener = () -> {
        notifyState();
        if (permissionGranted()) bind();
    };

    private final Shizuku.OnBinderDeadListener binderDeadListener = () -> {
        remote = null;
        binding = false;
        notifyState();
    };

    private final Shizuku.OnRequestPermissionResultListener permissionListener = (requestCode, grantResult) -> {
        if (requestCode != REQUEST_CODE) return;
        notifyState();
        if (grantResult == PackageManager.PERMISSION_GRANTED) bind();
        else emit("Shizuku", "Permissão negada pelo usuário.");
    };

    private final ServiceConnection connection = new ServiceConnection() {
        @Override public void onServiceConnected(ComponentName name, IBinder service) {
            remote = IPrivilegedBridge.Stub.asInterface(service);
            binding = false;
            notifyState();
            Runnable r = pendingReadyAction;
            pendingReadyAction = null;
            if (r != null) r.run();
        }

        @Override public void onServiceDisconnected(ComponentName name) {
            remote = null;
            binding = false;
            notifyState();
        }
    };

    public ShizukuBridge(Activity activity, Listener listener) {
        this.activity = activity;
        this.listener = listener;

        // Sticky é importante quando o Shizuku já estava em execução antes de
        // abrir o AION V Hub. Nesse cenário o evento original do Binder pode ter
        // ocorrido antes de esta Activity registrar o listener.
        Shizuku.addBinderReceivedListenerSticky(binderReceivedListener);
        Shizuku.addBinderDeadListener(binderDeadListener);
        Shizuku.addRequestPermissionResultListener(permissionListener);

        notifyState();
        if (binderAlive() && permissionGranted()) bind();
    }

    public void destroy() {
        Shizuku.removeBinderReceivedListener(binderReceivedListener);
        Shizuku.removeBinderDeadListener(binderDeadListener);
        Shizuku.removeRequestPermissionResultListener(permissionListener);
        executor.shutdownNow();
    }

    public boolean installed() {
        try {
            activity.getPackageManager().getPackageInfo("moe.shizuku.privileged.api", 0);
            return true;
        } catch (Throwable t) {
            return false;
        }
    }

    public boolean binderAlive() {
        try { return Shizuku.pingBinder(); }
        catch (Throwable t) { return false; }
    }

    public boolean permissionGranted() {
        if (!binderAlive()) return false;
        try {
            return !Shizuku.isPreV11() && Shizuku.checkSelfPermission() == PackageManager.PERMISSION_GRANTED;
        } catch (Throwable t) {
            return false;
        }
    }

    public boolean privilegedReady() {
        return remote != null;
    }

    public String status() {
        if (!installed()) return "NÃO INSTALADO";
        if (!binderAlive()) return "INSTALADO, mas Binder Shizuku ainda não foi recebido";
        if (!permissionGranted()) return "EM EXECUÇÃO, aguardando autorização do AION V Hub";
        if (remote == null) return binding ? "AUTORIZADO, conectando Bridge privilegiado…" : "AUTORIZADO, Bridge ainda não conectado";
        try { return "PRONTO • UID privilegiado=" + remote.getUid(); }
        catch (Throwable t) { return "PRONTO • UID indisponível"; }
    }

    public void requestPermission() {
        if (!installed()) {
            emit("Shizuku", "Shizuku não está instalado.");
            openShizuku();
            return;
        }
        if (!binderAlive()) {
            emit("Shizuku", "Shizuku está em execução, mas o Binder ainda não chegou ao AION V Hub. Feche e reabra o Hub; a v0.7.1 usa detecção sticky para recuperar esse estado automaticamente.");
            return;
        }
        try {
            if (permissionGranted()) {
                bind();
                emit("Shizuku", "Permissão já concedida. Conectando o Bridge privilegiado.");
            } else if (Shizuku.shouldShowRequestPermissionRationale()) {
                emit("Shizuku", "A autorização foi negada anteriormente. Abra o Shizuku e autorize o AION V Hub.");
                openShizuku();
            } else {
                Shizuku.requestPermission(REQUEST_CODE);
            }
        } catch (Throwable t) {
            emit("Shizuku", "Falha ao solicitar permissão: " + t.getClass().getSimpleName() + ": " + String.valueOf(t.getMessage()));
        }
    }

    public void bind() {
        if (binding || remote != null || !binderAlive() || !permissionGranted()) return;
        try {
            binding = true;
            Shizuku.UserServiceArgs args = new Shizuku.UserServiceArgs(
                    new ComponentName(activity, PrivilegedUserService.class))
                    .daemon(false)
                    .processNameSuffix("aion_priv")
                    .version(BuildConfig.VERSION_CODE)
                    .debuggable(false);
            Shizuku.bindUserService(args, connection);
            notifyState();
        } catch (Throwable t) {
            binding = false;
            emit("Shizuku", "Falha ao conectar UserService: " + t.getClass().getSimpleName() + ": " + String.valueOf(t.getMessage()));
            notifyState();
        }
    }

    public void runDeepAndroidAutoDiagnostic() {
        withReady(() -> executeAsync("Diagnóstico privilegiado Android Auto", deepDiagnosticCommand()));
    }

    public void runIdentityTest() {
        withReady(() -> executeAsync("Teste do Bridge privilegiado", "id; echo; pm list packages -i | grep -F 'com.aionvhub.app' || true"));
    }

    private void withReady(Runnable action) {
        if (remote != null) {
            action.run();
            return;
        }
        pendingReadyAction = action;
        if (!binderAlive() || !permissionGranted()) {
            requestPermission();
        } else {
            bind();
        }
    }

    private void executeAsync(String title, String command) {
        IPrivilegedBridge service = remote;
        if (service == null) {
            emit(title, "Bridge privilegiado não conectado.");
            return;
        }
        emit(title, "Executando…");
        executor.execute(() -> {
            String out;
            try { out = service.execute(command); }
            catch (Throwable t) { out = "ERRO: " + t.getClass().getSimpleName() + ": " + String.valueOf(t.getMessage()); }
            final String result = out;
            activity.runOnUiThread(() -> emit(title, result));
        });
    }

    private String deepDiagnosticCommand() {
        return "echo '=== IDENTIDADE SHIZUKU ==='; " +
                "id; " +
                "echo; echo '=== AION V HUB / INSTALLER ==='; " +
                "pm list packages -i | grep -F 'com.aionvhub.app' || true; " +
                "echo; echo '=== PACKAGE DUMP (FILTRO) ==='; " +
                "dumpsys package com.aionvhub.app 2>&1 | grep -iE 'versionName|versionCode|installer|installSource|initiating|originating|packageSource|updateOwner|enabled=|flags=|privateFlags=|firstInstallTime|lastUpdateTime' | head -n 180 || true; " +
                "echo; echo '=== ANDROID AUTO / GEARHEAD LOGS ==='; " +
                "logcat -d -v brief 2>&1 | grep -iE 'com\\.aionvhub\\.app|CAR\\.AUTH|isPackageAllowed|PlayGearhead|gearhead' | tail -n 180 || true";
    }

    public void openShizuku() {
        try {
            Intent i = activity.getPackageManager().getLaunchIntentForPackage("moe.shizuku.privileged.api");
            if (i != null) {
                activity.startActivity(i);
                return;
            }
        } catch (Throwable ignored) {}
        try {
            activity.startActivity(new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                    Uri.parse("package:moe.shizuku.privileged.api")));
        } catch (Throwable ignored) {}
    }

    private void notifyState() {
        if (listener != null) activity.runOnUiThread(listener::onStateChanged);
    }

    private void emit(String title, String output) {
        if (listener != null) activity.runOnUiThread(() -> listener.onOutput(title, output));
    }
}
