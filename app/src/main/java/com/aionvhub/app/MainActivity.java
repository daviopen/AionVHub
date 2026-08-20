package com.aionvhub.app;

import android.app.*;
import android.os.*;
import android.content.*;
import android.graphics.Color;
import android.net.*;
import android.provider.Settings;
import android.view.*;
import android.widget.*;
import androidx.car.app.connection.CarConnection;
import androidx.lifecycle.Observer;
import java.text.SimpleDateFormat;
import java.util.*;

public class MainActivity extends Activity {
    private LinearLayout root, diagBox;
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
        TextView v = new TextView(this); v.setText(s); v.setTextSize(sp); v.setTextColor(color);
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
        root.addView(label("v0.2 • Android 8.0+ • diagnóstico Android Auto",16,muted));
        status = label("Analisando conexão…",16,text); root.addView(status);

        root.addView(label("Diagnóstico",22,text));
        diagBox = new LinearLayout(this); diagBox.setOrientation(LinearLayout.VERTICAL); diagBox.setPadding(22,18,22,18); diagBox.setBackgroundColor(panel); root.addView(diagBox);
        root.addView(button("Atualizar diagnóstico", v -> refreshDiagnostics()));

        root.addView(label("Aplicativos",22,text));
        addAppButton("Waze", "com.waze", "https://www.waze.com");
        addAppButton("YouTube", "com.google.android.youtube", "https://www.youtube.com");
        addAppButton("YouTube Music", "com.google.android.apps.youtube.music", "https://music.youtube.com");
        addAppButton("Spotify", "com.spotify.music", "https://open.spotify.com");

        root.addView(label("Android Auto",22,text));
        root.addView(button("Abrir configurações do Android Auto", v -> openAndroidAutoSettings()));
        root.addView(button("Testar navegador", v -> openUri("https://www.google.com")));

        TextView info = label("A v0.2 também registra um serviço de mídia de teste para o Android Auto. Em APKs instalados fora da Play Store, pode ser necessário ativar o modo de desenvolvedor do Android Auto e a opção de fontes desconhecidas para apps de mídia.",14,muted);
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
        boolean online = false;
        ConnectivityManager cm = (ConnectivityManager)getSystemService(CONNECTIVITY_SERVICE);
        if (cm != null) {
            Network n = cm.getActiveNetwork();
            NetworkCapabilities c = n == null ? null : cm.getNetworkCapabilities(n);
            online = c != null && c.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET);
        }

        boolean aaPackage = isInstalled("com.google.android.projection.gearhead");
        String connection;
        if (carConnectionType == CarConnection.CONNECTION_TYPE_PROJECTION) connection = "conectado por Android Auto (projeção)";
        else if (carConnectionType == CarConnection.CONNECTION_TYPE_NATIVE) connection = "Android Automotive nativo";
        else connection = "não conectado";

        addDiag("Versão do Hub", "0.2.0");
        addDiag("Android", Build.VERSION.RELEASE + " (API " + Build.VERSION.SDK_INT + ")");
        addDiag("Compatibilidade", Build.VERSION.SDK_INT >= 26 ? "compatível" : "Android 8.0+ necessário");
        addDiag("Dispositivo", Build.MANUFACTURER + " " + Build.MODEL);
        addDiag("Android Auto no sistema", aaPackage ? "localizado" : "não visível / integrado ao sistema");
        addDiag("Conexão com carro", connection);
        addDiag("Internet", online ? "conectada" : "sem conexão detectada");
        addDiag("Waze", isInstalled("com.waze") ? "instalado" : "não instalado");
        addDiag("YouTube", isInstalled("com.google.android.youtube") ? "instalado" : "não instalado");
        addDiag("Spotify", isInstalled("com.spotify.music") ? "instalado" : "não instalado");
        addDiag("Última leitura", new SimpleDateFormat("dd/MM/yyyy HH:mm:ss", Locale.getDefault()).format(new Date()));

        if (carConnectionType == CarConnection.CONNECTION_TYPE_PROJECTION) {
            status.setText("✓ Android Auto conectado ao veículo");
        } else if (carConnectionType == CarConnection.CONNECTION_TYPE_NATIVE) {
            status.setText("✓ Executando em sistema automotivo Android");
        } else {
            status.setText("○ Aguardando conexão com Android Auto");
        }
    }

    private void addDiag(String k, String v) {
        TextView x = label(k + ":  " + v,15,text); diagBox.addView(x);
    }

    private boolean isInstalled(String pkg) {
        try { getPackageManager().getPackageInfo(pkg,0); return true; } catch (Exception e) { return false; }
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
