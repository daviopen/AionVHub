package com.aionvhub.app;

import android.app.*;
import android.os.*;
import android.content.*;
import android.content.pm.*;
import android.content.res.XmlResourceParser;
import android.graphics.Color;
import android.net.*;
import android.provider.Settings;
import android.view.*;
import android.widget.*;
import android.support.v4.media.MediaBrowserCompat;
import androidx.car.app.connection.CarConnection;
import androidx.lifecycle.Observer;
import org.xmlpull.v1.XmlPullParser;
import java.security.MessageDigest;
import java.text.SimpleDateFormat;
import java.util.*;

public class MainActivity extends Activity {
    private LinearLayout root, diagBox, logBox;
    private TextView status, privilegedOutput;
    private final int bg = Color.rgb(14,21,26);
    private final int panel = Color.rgb(25,35,42);
    private final int text = Color.rgb(239,245,246);
    private final int muted = Color.rgb(164,180,187);
    private final int accent = Color.rgb(100,216,203);
    private int carConnectionType = CarConnection.CONNECTION_TYPE_NOT_CONNECTED;
    private CarConnection carConnection;
    private Observer<Integer> carObserver;
    private MediaBrowserCompat selfTestBrowser;
    private ShizukuBridge shizukuBridge;

    @Override public void onCreate(Bundle b) {
        super.onCreate(b);
        buildUi();
        shizukuBridge = new ShizukuBridge(this, new ShizukuBridge.Listener() {
            @Override public void onStateChanged() {
                if (diagBox != null) refreshDiagnostics();
            }

            @Override public void onOutput(String title, String output) {
                if (privilegedOutput != null) {
                    privilegedOutput.setText(title + "\n\n" + output);
                }
                HubDiagnostics.event(MainActivity.this, "PRIV " + title + ": " + firstLine(output));
                refreshDiagnostics();
            }
        });
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
        if (selfTestBrowser != null && selfTestBrowser.isConnected()) {
            selfTestBrowser.disconnect();
        }
        if (shizukuBridge != null) shizukuBridge.destroy();
        super.onDestroy();
    }

    private String firstLine(String s) {
        if (s == null || s.trim().isEmpty()) return "sem saída";
        int i = s.indexOf('\n');
        String x = i >= 0 ? s.substring(0, i) : s;
        return x.length() > 100 ? x.substring(0,100) : x;
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
        root.addView(label("v0.7 • Media Browser v2 isolado • Shizuku integrado • Android Auto",16,muted));
        status = label("Analisando conexão…",16,text); root.addView(status);

        root.addView(label("Diagnóstico do sistema",22,text));
        diagBox = new LinearLayout(this); diagBox.setOrientation(LinearLayout.VERTICAL); diagBox.setPadding(22,18,22,18); diagBox.setBackgroundColor(panel); root.addView(diagBox);
        root.addView(button("Atualizar diagnóstico", v -> refreshDiagnostics()));

        root.addView(label("Bridge privilegiado / Shizuku",22,text));
        root.addView(button("Autorizar / conectar Shizuku", v -> {
            if (shizukuBridge != null) shizukuBridge.requestPermission();
        }));
        root.addView(button("Testar Bridge privilegiado", v -> {
            if (shizukuBridge != null) shizukuBridge.runIdentityTest();
        }));
        root.addView(button("Diagnóstico profundo Android Auto", v -> {
            if (shizukuBridge != null) shizukuBridge.runDeepAndroidAutoDiagnostic();
        }));
        root.addView(button("Abrir Shizuku", v -> {
            if (shizukuBridge != null) shizukuBridge.openShizuku();
        }));

        privilegedOutput = label("Nenhum diagnóstico privilegiado executado.",13,text);
        privilegedOutput.setTypeface(android.graphics.Typeface.MONOSPACE);
        privilegedOutput.setPadding(22,18,22,18);
        privilegedOutput.setBackgroundColor(panel);
        root.addView(privilegedOutput);
        root.addView(button("Copiar saída privilegiada", v -> copyPrivilegedOutput()));

        root.addView(label("Testes do Bridge Android Auto",22,text));
        root.addView(button("Autoteste completo do Media Browser v2", v -> runMediaSelfTest()));
        root.addView(button("Abrir Bridge diretamente no telefone", v -> openBridgeLocally()));

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

        TextView info = label(
                "Como interpretar: a v0.7 usa somente o caminho Media Browser para descoberta no Android Auto 17.3, evitando competição com uma segunda interface automotiva. O autoteste valida árvore, item e busca. O diagnóstico privilegiado usa o Shizuku para consultar Package Manager e logs locais.",
                14,
                muted
        );
        info.setPadding(0,24,0,8); root.addView(info);

        TextView warning = label("Segurança: o Bridge não remove restrições de movimento nem força conteúdo durante a condução.",14,muted);
        root.addView(warning);
    }

    private void copyPrivilegedOutput() {
        if (privilegedOutput == null) return;
        ClipboardManager cm = (ClipboardManager)getSystemService(CLIPBOARD_SERVICE);
        if (cm != null) {
            cm.setPrimaryClip(ClipData.newPlainText("AION V Hub diagnóstico", privilegedOutput.getText()));
            Toast.makeText(this,"Diagnóstico copiado.",Toast.LENGTH_SHORT).show();
        }
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

        addSection("Proveniência da instalação");
        addDiag("Instalador registrado", installerPackage());
        addDiag("Iniciador real", initiatingPackage());
        addDiag("Origem declarada", originatingPackage());
        addDiag("Fonte do pacote", packageSource());
        addDiag("Dono das atualizações", updateOwnerPackage());

        addSection("Shizuku integrado");
        if (shizukuBridge == null) {
            addDiag("Estado", "inicializando…");
        } else {
            addDiag("Shizuku instalado", shizukuBridge.installed() ? "SIM" : "NÃO");
            addDiag("Serviço Shizuku", shizukuBridge.binderAlive() ? "EM EXECUÇÃO" : "PARADO / não detectado");
            addDiag("Permissão AION V Hub", shizukuBridge.permissionGranted() ? "AUTORIZADA" : "NÃO AUTORIZADA");
            addDiag("Bridge privilegiado", shizukuBridge.status());
        }

        addSection("Telefone");
        addDiag("Android", Build.VERSION.RELEASE + " (API " + Build.VERSION.SDK_INT + ")");
        addDiag("Dispositivo", Build.MANUFACTURER + " " + Build.MODEL);
        addDiag("Build", Build.DISPLAY);
        addDiag("Compatibilidade mínima", Build.VERSION.SDK_INT >= 26 ? "OK — Android 8.0+" : "INCOMPATÍVEL");

        addSection("Android Auto / Google");
        addDiag("Android Auto", packageVersion("com.google.android.projection.gearhead"));
        addDiag("Google Play Services", packageVersion("com.google.android.gms"));
        addDiag("Conexão com carro", connection);
        addDiag("Modo desenvolvedor", "não verificável por API comum");
        addDiag("Application Mode = Developer", "confirme manualmente nas opções do Android Auto");
        addDiag("Fontes desconhecidas", "não verificável por API comum");

        addSection("AION V Hub Bridge");
        addDiag("Estratégia", "MediaBrowserServiceCompat v2 isolado • Android Auto 17.3");
        addDiag("Media Browser v2", "árvore + onLoadItem + onSearch + MediaSession");
        addDiag("Pesquisa Android Auto", "SEARCH_SUPPORTED + PLAY_FROM_SEARCH");
        addDiag("Content style hints", "browsable + playable");
        addDiag("Descoberta concorrente", "DESATIVADA — somente descritor media");
        addDiag("BridgeActivity local", activityDeclared(BridgeActivity.class) ? "SIM — sem CAR_LAUNCHER" : "NÃO");
        addDiag("MediaBrowserServiceCompat", serviceDeclared(HubMediaService.class) ? "SIM" : "NÃO");
        addDiag("CarAppService legado", serviceDeclared(HubCarAppService.class) ? "SIM" : "NÃO — removido do manifest v0.4");
        addDiag("Descritor automotivo real", automotiveDescriptor());
        addDiag("Último evento", HubDiagnostics.getLastEvent(this));
        addDiag("Tempo desde último evento", eventAge());
        addDiag("Leitura do estado", interpretBridgeState());

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
            status.setText("✓ Android Auto conectado • Bridge: " + shortBridgeState());
        } else if (carConnectionType == CarConnection.CONNECTION_TYPE_NATIVE) {
            status.setText("✓ Android Automotive detectado • Bridge: " + shortBridgeState());
        } else {
            status.setText("○ Aguardando conexão com Android Auto");
        }
    }

    private void runMediaSelfTest() {
        try {
            if (selfTestBrowser != null && selfTestBrowser.isConnected()) selfTestBrowser.disconnect();
        } catch (Exception ignored) {}

        HubDiagnostics.event(this, "SELFTEST v2 iniciando MediaBrowserCompat");
        Toast.makeText(this, "Executando autoteste completo…", Toast.LENGTH_SHORT).show();

        selfTestBrowser = new MediaBrowserCompat(
                this,
                new ComponentName(this, HubMediaService.class),
                new MediaBrowserCompat.ConnectionCallback() {
                    @Override public void onConnected() {
                        try {
                            String rootId = selfTestBrowser.getRoot();
                            HubDiagnostics.event(MainActivity.this, "SELFTEST v2 conectado root=" + rootId);
                            selfTestBrowser.subscribe(rootId, new MediaBrowserCompat.SubscriptionCallback() {
                                @Override public void onChildrenLoaded(String parentId, List<MediaBrowserCompat.MediaItem> children) {
                                    HubDiagnostics.event(MainActivity.this, "SELFTEST v2 children=" + children.size());
                                    runMediaItemSelfTest(children.size());
                                }

                                @Override public void onError(String parentId) {
                                    failMediaSelfTest("listar raiz " + parentId);
                                }
                            });
                        } catch (Throwable t) {
                            failMediaSelfTest("exceção " + t.getClass().getSimpleName());
                        }
                    }

                    @Override public void onConnectionSuspended() {
                        failMediaSelfTest("conexão suspensa");
                    }

                    @Override public void onConnectionFailed() {
                        failMediaSelfTest("conexão recusada");
                    }
                },
                null
        );
        selfTestBrowser.connect();
    }

    private void runMediaItemSelfTest(final int childrenCount) {
        if (selfTestBrowser == null || !selfTestBrowser.isConnected()) {
            failMediaSelfTest("browser desconectado antes de onLoadItem");
            return;
        }

        selfTestBrowser.getItem(HubMediaCatalog.CONNECTION_ID, new MediaBrowserCompat.ItemCallback() {
            @Override public void onItemLoaded(MediaBrowserCompat.MediaItem item) {
                if (item == null || item.getMediaId() == null) {
                    failMediaSelfTest("onLoadItem retornou vazio");
                    return;
                }
                HubDiagnostics.event(MainActivity.this, "SELFTEST v2 item=" + item.getMediaId());
                runMediaSearchSelfTest(childrenCount, item.getMediaId());
            }

            @Override public void onError(String itemId) {
                failMediaSelfTest("onLoadItem " + itemId);
            }
        });
    }

    private void runMediaSearchSelfTest(final int childrenCount, final String itemId) {
        if (selfTestBrowser == null || !selfTestBrowser.isConnected()) {
            failMediaSelfTest("browser desconectado antes de onSearch");
            return;
        }

        selfTestBrowser.search("android auto", null, new MediaBrowserCompat.SearchCallback() {
            @Override public void onSearchResult(String query, Bundle extras, List<MediaBrowserCompat.MediaItem> items) {
                int count = items == null ? 0 : items.size();
                if (count <= 0) {
                    failMediaSelfTest("onSearch sem resultados");
                    return;
                }

                HubDiagnostics.event(
                        MainActivity.this,
                        "SELFTEST OK v2 children=" + childrenCount + " item=" + itemId + " search=" + count
                );
                Toast.makeText(
                        MainActivity.this,
                        "Autoteste v2 OK: catálogo, item e busca responderam.",
                        Toast.LENGTH_LONG
                ).show();
                disconnectSelfTestBrowser();
                refreshDiagnostics();
            }

            @Override public void onError(String query, Bundle extras) {
                failMediaSelfTest("onSearch " + query);
            }
        });
    }

    private void failMediaSelfTest(String reason) {
        HubDiagnostics.event(this, "SELFTEST ERRO v2 " + reason);
        Toast.makeText(this, "Autoteste falhou: " + reason, Toast.LENGTH_LONG).show();
        disconnectSelfTestBrowser();
        refreshDiagnostics();
    }

    private void disconnectSelfTestBrowser() {
        try {
            if (selfTestBrowser != null && selfTestBrowser.isConnected()) {
                selfTestBrowser.unsubscribe(HubMediaCatalog.ROOT_ID);
                selfTestBrowser.disconnect();
            }
        } catch (Exception ignored) {}
    }

    private void openBridgeLocally() {
        HubDiagnostics.event(this, "SELFTEST abrindo BridgeActivity localmente");
        startActivity(new Intent(this, BridgeActivity.class));
    }

    private String interpretBridgeState() {
        String log = HubDiagnostics.getLog(this);
        if (log.contains("onGetRoot cliente=com.google.android.projection.gearhead")) {
            return "ANDROID AUTO CHAMOU O SERVIÇO DE MÍDIA";
        }
        if (log.contains("MEDIA-COMPAT onGetRoot cliente=com.aionvhub.app") && log.contains("SELFTEST OK")) {
            return "SERVIÇO LOCAL V2 OK; aguardando o Android Auto chamar";
        }
        if (log.contains("BRIDGE onCreate")) {
            return "BridgeActivity foi iniciada apenas no teste local";
        }
        if (carConnectionType == CarConnection.CONNECTION_TYPE_PROJECTION) {
            return "Android Auto conectado; Media Browser ainda não chamado pelo host";
        }
        return "aguardando conexão / teste";
    }

    private String shortBridgeState() {
        String full = interpretBridgeState();
        if (full.startsWith("ANDROID AUTO")) return "HOST OK";
        if (full.startsWith("SERVIÇO LOCAL")) return "LOCAL V2 OK / HOST PENDENTE";
        if (full.startsWith("BridgeActivity")) return "TESTE LOCAL";
        return "AGUARDANDO HOST";
    }

    private String automotiveDescriptor() {
        List<String> uses = new ArrayList<>();
        XmlResourceParser parser = null;
        try {
            parser = getResources().getXml(R.xml.automotive_app_desc);
            int event = parser.getEventType();
            while (event != XmlPullParser.END_DOCUMENT) {
                if (event == XmlPullParser.START_TAG && "uses".equals(parser.getName())) {
                    String name = parser.getAttributeValue(null, "name");
                    if (name != null && !name.trim().isEmpty()) uses.add(name.trim());
                }
                event = parser.next();
            }
        } catch (Exception e) {
            return "erro ao ler XML: " + e.getClass().getSimpleName();
        } finally {
            if (parser != null) parser.close();
        }
        return uses.isEmpty() ? "nenhum <uses>" : android.text.TextUtils.join(" + ", uses);
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

    private boolean activityDeclared(Class<?> cls) {
        try {
            getPackageManager().getActivityInfo(new ComponentName(this, cls), 0);
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

    private InstallSourceInfo installSourceInfo() {
        if (Build.VERSION.SDK_INT < 30) return null;
        try {
            return getPackageManager().getInstallSourceInfo(getPackageName());
        } catch (Exception e) {
            return null;
        }
    }

    private String installerPackage() {
        try {
            if (Build.VERSION.SDK_INT >= 30) {
                InstallSourceInfo i = installSourceInfo();
                String p = i == null ? null : i.getInstallingPackageName();
                return p == null ? "sideload / desconhecido" : p;
            }
            String p = getPackageManager().getInstallerPackageName(getPackageName());
            return p == null ? "sideload / desconhecido" : p;
        } catch (Exception e) {
            return "desconhecido";
        }
    }

    private String initiatingPackage() {
        if (Build.VERSION.SDK_INT < 30) return "API < 30";
        try {
            InstallSourceInfo i = installSourceInfo();
            String p = i == null ? null : i.getInitiatingPackageName();
            return p == null ? "não informado" : p;
        } catch (Exception e) {
            return "indisponível: " + e.getClass().getSimpleName();
        }
    }

    private String originatingPackage() {
        if (Build.VERSION.SDK_INT < 30) return "API < 30";
        try {
            InstallSourceInfo i = installSourceInfo();
            String p = i == null ? null : i.getOriginatingPackageName();
            return p == null ? "não informado / acesso restrito" : p;
        } catch (Exception e) {
            return "indisponível: " + e.getClass().getSimpleName();
        }
    }

    private String packageSource() {
        if (Build.VERSION.SDK_INT < 33) return "API < 33";
        try {
            InstallSourceInfo i = installSourceInfo();
            if (i == null) return "indisponível";
            int source = i.getPackageSource();
            if (source == PackageInstaller.PACKAGE_SOURCE_STORE) return "STORE (loja)";
            if (source == PackageInstaller.PACKAGE_SOURCE_LOCAL_FILE) return "LOCAL_FILE (arquivo local)";
            if (source == PackageInstaller.PACKAGE_SOURCE_DOWNLOADED_FILE) return "DOWNLOADED_FILE (arquivo baixado)";
            if (source == PackageInstaller.PACKAGE_SOURCE_OTHER) return "OTHER (outra origem)";
            return "UNSPECIFIED (não especificada)";
        } catch (Exception e) {
            return "indisponível: " + e.getClass().getSimpleName();
        }
    }

    private String updateOwnerPackage() {
        if (Build.VERSION.SDK_INT < 34) return "API < 34";
        try {
            InstallSourceInfo i = installSourceInfo();
            String p = i == null ? null : i.getUpdateOwnerPackageName();
            return p == null ? "nenhum" : p;
        } catch (Exception e) {
            return "indisponível: " + e.getClass().getSimpleName();
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
