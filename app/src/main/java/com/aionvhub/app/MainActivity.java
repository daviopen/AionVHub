package com.aionvhub.app;

import android.app.*;
import android.os.*;
import android.content.*;
import android.content.pm.*;
import android.graphics.Color;
import android.net.*;
import android.provider.Settings;
import android.view.*;
import android.widget.*;
import androidx.car.app.connection.CarConnection;
import androidx.lifecycle.Observer;
import java.security.MessageDigest;
import java.text.SimpleDateFormat;
import java.util.*;

public class MainActivity extends Activity {
    private LinearLayout root, diagBox, logBox;
    private TextView status;
    private final int bg = Color.rgb(14,21,26);
    private final int panel = Color.rgb(25,35,42);
    private final int text = Color.rgb(239,245,246);
    private final int muted = Color.rgb(164,180,187);
    private final int accent = Color.rgb(100,216,203);
    private int carConnectionType = CarConnection.CONNECTION_TYPE_NOT_CONNECTED;
    private CarConnection carConnection;
    private Observer<Integer> carObserver;

    @Override public void onCreate(Bundle b) {
        super.onCreate(b);
        buildUi();
        startCarConnectionObserver();
        refreshDiagnostics();
    }

    @Override protected void onResume() {
        super.onResume();
        if (diagBox != null) refreshDiagnostics();
    }

    @Override protected void onDestroy() {
        if (carConnection != null && carObserver != null) {
            carConnection.getType().removeObserver(carObserver);
        }
        super.onDestroy();
    }

    private void startCarConnectionObserver() {
        carConnection = new CarConnection(this);
        carObserver = type -> {
            if (type != null) carConnectionType = type;
            refreshDiagnostics();
        };
        carConnection.getType().observeForever(carObserver);
    }

    private TextView label(String s, int sp, int color) {
        TextView v = new TextView(this);
        v.setText(s); v.setTextSize(sp); v.setTextColor(color); v.setTextIsSelectable(true);
        v.setPadding(0,8,0,8); return v;
    }

    private Button button(String title, View.OnClickListener l) {
        Button b = new Button(this); b.setText(title); b.setAllCaps(false); b.setTextSize(16); b.setOnClickListener(l);
        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(-1,-2); p.setMargins(0,8,0,8); b.setLayoutParams(p); return b;
    }

    private void buildUi() {
        ScrollView scroll = new ScrollView(this); scroll.setBackgroundColor(bg);
        root = new LinearLayout(this); root.setOrientation(LinearLayout.VERTICAL); root.setPadding(32,32,32,48);
        scroll.addView(root); setContentView(scroll);

        root.addView(label("AION V HUB",28,accent));
        root.addView(label("Diagnóstico avançado • Android 8.0+ • Android Auto",16,muted));
        status = label("Analisando conexão…",16,text); root.addView(status);

        root.addView(label("Diagnóstico do sistema",22,text));
        diagBox = new LinearLayout(this); diagBox.setOrientation(LinearLayout.VERTICAL); diagBox.setPadding(22,18,22,18); diagBox.setBackgroundColor(panel); root.addView(diagBox);
        root.addView(button("Atualizar diagnóstico", v -> refreshDiagnostics()));

        root.addView(label("Eventos do Android Auto",22,text));
        logBox = new LinearLayout(this); logBox.setOrientation(LinearLayout.VERTICAL); logBox.setPadding(22,18,22,18); logBox.setBackgroundColor(panel); root.addView(logBox);
        root.addView(button("Limpar histórico de eventos", v -> { HubDiagnostics.clear(this); refreshDiagnostics(); }));

        root.addView(label("Aplicativos",22,text));
        addAppButton("Waze", "com.waze", "https://www.waze.com");
        addAppButton("YouTube", "com.google.android.youtube", "https://www.youtube.com");
        addAppButton("YouTube Music", "com.google.android.apps.youtube.music", "https://music.youtube.com");
        addAppButton("Spotify", "com.spotify.music", "https://open.spotify.com");

        root.addView(label("Ferramentas",22,text));
        root.addView(button("Abrir configurações do Android Auto", v -> openAndroidAutoSettings()));
        root.addView(button("Testar navegador", v -> openUri("https://www.google.com")));

        TextView info = label("Como interpretar: se o histórico chegar até 'onGetTemplate', o Android Auto abriu nosso serviço e pediu a tela. Se parar antes disso, saberemos exatamente em qual etapa o host interrompeu o processo.",14,muted);
        info.setPadding(0,24,0,8); root.addView(info);

        TextView warning = label("Segurança: o app não remove bloqueios de movimento e não força vídeo durante a condução.",14,muted);
        root.addView(warning);
    }

    private void addAppButton(String name, String pkg, String web) {
        boolean installed = isInstalled(pkg);
        root.addView(button((installed ? "Abrir " : "Abrir site do ") + name, v -> openPackageOrWeb(pkg, web)));
    }

    private void refreshDiagnostics() {
        if (diagBox == null) return;
        diagBox.removeAllViews();

        ConnectivityManager cm = (ConnectivityManager)getSystemService(CONNECTIVITY_SERVICE);
        Network active = cm == null ? null : cm.getActiveNetwork();
        NetworkCapabilities caps = (cm == null || active == null) ? null : cm.getNetworkCapabilities(active);
        boolean internetCapability = caps != null && caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET);
        boolean validated = caps != null && caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED);

        String connection;
        if (carConnectionType == CarConnection.CONNECTION_TYPE_PROJECTION) connection = "CONECTADO — Android Auto por projeção";
        else if (carConnectionType == CarConnection.CONNECTION_TYPE_NATIVE) connection = "CONECTADO — Android Automotive nativo";
        else connection = "NÃO CONECTADO";

        addSection("Aplicativo");
        addDiag("Versão", BuildConfig.VERSION_NAME + " (code " + BuildConfig.VERSION_CODE + ")");
        addDiag("Package", getPackageName());
        addDiag("Assinatura SHA-256", ownSignatureFingerprint());
        addDiag("Instalador", installerPackage());

        addSection("Telefone");
        addDiag("Android", Build.VERSION.RELEASE + " (API " + Build.VERSION.SDK_INT + ")");
        addDiag("Dispositivo", Build.MANUFACTURER + " " + Build.MODEL);
        addDiag("Build", Build.DISPLAY);
        addDiag("Compatibilidade mínima", Build.VERSION.SDK_INT >= 26 ? "OK — Android 8.0+" : "INCOMPATÍVEL");

        addSection("Android Auto / Google");
        addDiag("Android Auto", packageVersion("com.google.android.projection.gearhead"));
        addDiag("Google Play Services", packageVersion("com.google.android.gms"));
        addDiag("Conexão com carro", connection);
        addDiag("Modo desenvolvedor", "não verificável por API");
        addDiag("Fontes desconhecidas", "não verificável por API");

        addSection("Registro automotivo");
        addDiag("CarAppService declarado", serviceDeclared(HubCarAppService.class) ? "SIM" : "NÃO");
        addDiag("MediaBrowserService declarado", serviceDeclared(HubMediaService.class) ? "SIM" : "NÃO");
        addDiag("Categoria Car App", "POI");
        addDiag("Descritor automotivo", "media + template");
        addDiag("Car API mínima", "1");
        addDiag("Último evento do host", HubDiagnostics.getLastEvent(this));
        addDiag("Tempo desde último evento", eventAge());

        addSection("Rede");
        addDiag("Transporte ativo", networkTransport(caps));
        addDiag("Capability INTERNET", internetCapability ? "SIM" : "NÃO");
        addDiag("Internet validada", validated ? "SIM" : "NÃO");
        addDiag("Observação", carConnectionType == CarConnection.CONNECTION_TYPE_PROJECTION && !validated ? "Android Auto pode usar Wi‑Fi dedicado sem internet validada" : "—");

        addSection("Apps detectados");
        addDiag("Waze", packageVersion("com.waze"));
        addDiag("YouTube", packageVersion("com.google.android.youtube"));
        addDiag("YouTube Music", packageVersion("com.google.android.apps.youtube.music"));
        addDiag("Spotify", packageVersion("com.spotify.music"));
        addDiag("Última leitura", new SimpleDateFormat("dd/MM/yyyy HH:mm:ss", Locale.getDefault()).format(new Date()));

        if (logBox != null) {
            logBox.removeAllViews();
            String log = HubDiagnostics.getLog(this);
            TextView logText = label(log,14,text);
            logText.setTypeface(android.graphics.Typeface.MONOSPACE);
            logBox.addView(logText);
        }

        if (carConnectionType == CarConnection.CONNECTION_TYPE_PROJECTION) {
            status.setText("✓ Android Auto conectado • último host: " + HubDiagnostics.getLastEvent(this));
        } else if (carConnectionType == CarConnection.CONNECTION_TYPE_NATIVE) {
            status.setText("✓ Android Automotive detectado");
        } else {
            status.setText("○ Aguardando conexão com Android Auto");
        }
    }

    private void addSection(String title) {
        TextView x = label("\n" + title.toUpperCase(Locale.getDefault()),13,accent);
        diagBox.addView(x);
    }

    private void addDiag(String k, String v) {
        TextView x = label(k + ":  " + v,14,text); diagBox.addView(x);
    }

    private boolean isInstalled(String pkg) {
        try { getPackageManager().getPackageInfo(pkg,0); return true; } catch (Exception e) { return false; }
    }

    private String packageVersion(String pkg) {
        try {
            PackageInfo pi = getPackageManager().getPackageInfo(pkg,0);
            long code = Build.VERSION.SDK_INT >= 28 ? pi.getLongVersionCode() : pi.versionCode;
            return "instalado — " + pi.versionName + " (" + code + ")";
        } catch (Exception e) {
            return "não localizado / não visível";
        }
    }

    private boolean serviceDeclared(Class<?> cls) {
        try {
            getPackageManager().getServiceInfo(new ComponentName(this, cls), 0);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    private String networkTransport(NetworkCapabilities c) {
        if (c == null) return "nenhuma rede ativa";
        List<String> out = new ArrayList<>();
        if (c.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)) out.add("Wi‑Fi");
        if (c.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR)) out.add("Celular");
        if (c.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET)) out.add("Ethernet");
        if (c.hasTransport(NetworkCapabilities.TRANSPORT_VPN)) out.add("VPN");
        if (Build.VERSION.SDK_INT >= 26 && c.hasTransport(NetworkCapabilities.TRANSPORT_BLUETOOTH)) out.add("Bluetooth");
        return out.isEmpty() ? "outro" : android.text.TextUtils.join(" + ", out);
    }

    private String installerPackage() {
        try {
            if (Build.VERSION.SDK_INT >= 30) {
                InstallSourceInfo i = getPackageManager().getInstallSourceInfo(getPackageName());
                String p = i.getInstallingPackageName();
                return p == null ? "sideload / desconhecido" : p;
            }
            String p = getPackageManager().getInstallerPackageName(getPackageName());
            return p == null ? "sideload / desconhecido" : p;
        } catch (Exception e) {
            return "desconhecido";
        }
    }

    private String ownSignatureFingerprint() {
        try {
            PackageInfo pi;
            byte[] cert;
            if (Build.VERSION.SDK_INT >= 28) {
                pi = getPackageManager().getPackageInfo(getPackageName(), PackageManager.GET_SIGNING_CERTIFICATES);
                cert = pi.signingInfo.getApkContentsSigners()[0].toByteArray();
            } else {
                pi = getPackageManager().getPackageInfo(getPackageName(), PackageManager.GET_SIGNATURES);
                cert = pi.signatures[0].toByteArray();
            }
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(cert);
            StringBuilder sb = new StringBuilder();
            for (int i=0; i<digest.length; i++) {
                if (i > 0) sb.append(':');
                sb.append(String.format(Locale.US, "%02X", digest[i]));
            }
            return sb.toString();
        } catch (Exception e) {
            return "indisponível: " + e.getClass().getSimpleName();
        }
    }

    private String eventAge() {
        long ts = HubDiagnostics.getLastEventTimestamp(this);
        if (ts <= 0) return "nenhum evento";
        long sec = Math.max(0, (System.currentTimeMillis() - ts) / 1000);
        if (sec < 60) return sec + " s";
        if (sec < 3600) return (sec / 60) + " min";
        return (sec / 3600) + " h";
    }

    private void openPackageOrWeb(String pkg, String web) {
        Intent i = getPackageManager().getLaunchIntentForPackage(pkg);
        if (i != null) startActivity(i); else openUri(web);
    }

    private void openUri(String u) {
        try { startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(u))); }
        catch(Exception e) { Toast.makeText(this,"Não foi possível abrir.",Toast.LENGTH_SHORT).show(); }
    }

    private void openAndroidAutoSettings() {
        try {
            Intent i = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:com.google.android.projection.gearhead"));
            startActivity(i);
        } catch(Exception e) {
            startActivity(new Intent(Settings.ACTION_SETTINGS));
        }
    }
}
