package com.aionvhub.app;

import android.app.*;
import android.os.*;
import android.content.*;
import android.graphics.Color;
import android.net.*;
import android.provider.Settings;
import android.view.*;
import android.widget.*;
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

    @Override public void onCreate(Bundle b) {
        super.onCreate(b);
        buildUi();
        refreshDiagnostics();
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
        root.addView(label("Protótipo pessoal • diagnóstico e mídia",16,muted));
        status = label("Analisando dispositivo…",16,text); root.addView(status);

        root.addView(label("Diagnóstico",22,text));
        diagBox = new LinearLayout(this); diagBox.setOrientation(LinearLayout.VERTICAL); diagBox.setPadding(22,18,22,18); diagBox.setBackgroundColor(panel); root.addView(diagBox);
        root.addView(button("Atualizar diagnóstico", v -> refreshDiagnostics()));

        root.addView(label("Mídia no celular",22,text));
        root.addView(button("Abrir YouTube", v -> openUri("https://www.youtube.com")));
        root.addView(button("Abrir YouTube Music", v -> openUri("https://music.youtube.com")));
        root.addView(button("Abrir Spotify", v -> openPackageOrWeb("com.spotify.music", "https://open.spotify.com")));

        root.addView(label("Testes",22,text));
        root.addView(button("Abrir configurações do Android Auto", v -> openAndroidAutoSettings()));
        root.addView(button("Testar navegador", v -> openUri("https://www.google.com")));

        TextView warning = label("Segurança: este protótipo não remove bloqueios de segurança do veículo e não força reprodução de vídeo durante a condução.",14,muted);
        warning.setPadding(0,24,0,0); root.addView(warning);
    }

    private void refreshDiagnostics() {
        diagBox.removeAllViews();
        boolean online = false;
        ConnectivityManager cm = (ConnectivityManager)getSystemService(CONNECTIVITY_SERVICE);
        if (cm != null) {
            Network n = cm.getActiveNetwork();
            NetworkCapabilities c = n == null ? null : cm.getNetworkCapabilities(n);
            online = c != null && c.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET);
        }
        boolean aa = isInstalled("com.google.android.projection.gearhead");
        addDiag("Android", Build.VERSION.RELEASE + " (API " + Build.VERSION.SDK_INT + ")");
        addDiag("Fabricante", Build.MANUFACTURER + " " + Build.MODEL);
        addDiag("Android Auto", aa ? "instalado" : "não localizado");
        addDiag("Internet", online ? "conectada" : "sem conexão detectada");
        addDiag("Última leitura", new SimpleDateFormat("dd/MM/yyyy HH:mm:ss", Locale.getDefault()).format(new Date()));
        status.setText(aa ? "✓ Telefone preparado para testes com Android Auto" : "⚠ Android Auto não foi localizado neste telefone");
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
