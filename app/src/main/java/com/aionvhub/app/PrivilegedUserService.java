package com.aionvhub.app;

import android.content.Context;
import android.system.Os;
import androidx.annotation.Keep;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.concurrent.TimeUnit;

/**
 * Processo executado pelo Shizuku com identidade ADB/shell.
 * Mantém a superfície privilegiada mínima: executar comandos locais explícitos
 * solicitados pelo próprio AION V Hub e devolver o resultado ao processo do app.
 */
public class PrivilegedUserService extends IPrivilegedBridge.Stub {

    public PrivilegedUserService() {}

    @Keep
    public PrivilegedUserService(Context context) {}

    @Override
    public int getUid() {
        return Os.getuid();
    }

    @Override
    public String execute(String command) {
        if (command == null || command.trim().isEmpty()) return "ERRO: comando vazio";
        Process process = null;
        try {
            process = new ProcessBuilder("sh", "-c", command)
                    .redirectErrorStream(true)
                    .start();

            StringBuilder out = new StringBuilder();
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    if (out.length() > 0) out.append('\n');
                    out.append(line);
                    if (out.length() > 12000) {
                        out.append("\n[saída truncada]");
                        break;
                    }
                }
            }

            boolean finished = process.waitFor(45, TimeUnit.SECONDS);
            if (!finished) {
                process.destroyForcibly();
                return "ERRO: timeout de 45 s";
            }
            int code = process.exitValue();
            return "exit=" + code + (out.length() == 0 ? "" : "\n" + out);
        } catch (Throwable t) {
            return "ERRO: " + t.getClass().getSimpleName() + ": " + String.valueOf(t.getMessage());
        } finally {
            if (process != null) process.destroy();
        }
    }

    public void destroy() {
        System.exit(0);
    }
}
