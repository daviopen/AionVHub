package com.aionvhub.app;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

/** Tela principal do AION V Hub. O diagnóstico completo permanece separado. */
public class HomeActivity extends Activity {
    private final int bg = Color.rgb(14,21,26);
    private final int panel = Color.rgb(25,35,42);
    private final int text = Color.rgb(239,245,246);
    private final int muted = Color.rgb(164,180,187);
    private final int accent = Color.rgb(100,216,203);
    private LinearLayout root;
    private TextView streamStatus;

    @Override protected void onCreate(Bundle state) {
        super.onCreate(state);
        buildUi();
    }

    @Override protected void onResume() {
        super.onResume();
        refreshStreamStatus();
    }

    private void buildUi() {
        ScrollView scroll = new ScrollView(this);
        scroll.setBackgroundColor(bg);
        root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(32,32,32,48);
        scroll.addView(root);
        setContentView(scroll);

        root.addView(label("AION V HUB", 30, accent));
        root.addView(label("v" + BuildConfig.VERSION_NAME + " • Hub de mídia para Android Auto", 16, muted));
        root.addView(label("Início", 23, text));

        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setPadding(22,18,22,18);
        card.setBackgroundColor(panel);
        streamStatus = label("Carregando fonte de mídia…", 16, text);
        card.addView(streamStatus);
        root.addView(card);

        root.addView(button("Configurar IPTV / stream", v ->
                startActivity(new Intent(this, StreamSettingsActivity.class))));
        root.addView(button("Player de vídeo no tablet (somente estacionado)", v -> openVideoPlayer()));

        root.addView(label("Acessos rápidos", 23, text));
        root.addView(button("Abrir Waze", v -> openPackage("com.waze")));
        root.addView(button("Abrir Spotify", v -> openPackage("com.spotify.music")));
        root.addView(button("Abrir YouTube no tablet", v -> openPackage("com.google.android.youtube")));

        root.addView(label("Manutenção", 23, text));
        root.addView(button("Diagnóstico completo", v ->
                startActivity(new Intent(this, MainActivity.class))));

        TextView info = label(
                "No Android Auto, o Hub usa o MediaBrowser/MediaSession já validado no Aion V. " +
                "O Shizuku é opcional e só amplia o diagnóstico. Vídeo não é forçado na tela do carro durante a condução.",
                14,
                muted
        );
        info.setPadding(0,22,0,0);
        root.addView(info);
    }

    private void refreshStreamStatus() {
        if (streamStatus == null) return;
        HubStreamStore.Config c = HubStreamStore.get(this);
        if (c.configured()) {
            streamStatus.setText("Fonte configurada: " + c.title + "\nDisponível no catálogo do AION V Hub.");
        } else {
            streamStatus.setText("Nenhuma fonte IPTV/stream configurada.\nConfigure uma URL direta de mídia para começar.");
        }
    }

    private void openVideoPlayer() {
        HubStreamStore.Config c = HubStreamStore.get(this);
        if (!c.configured()) {
            Toast.makeText(this, "Configure primeiro uma URL de mídia.", Toast.LENGTH_SHORT).show();
            startActivity(new Intent(this, StreamSettingsActivity.class));
            return;
        }

        new AlertDialog.Builder(this)
                .setTitle("Vídeo somente estacionado")
                .setMessage("O player visual é destinado a uso com o veículo estacionado. O AION V Hub não remove nem contorna bloqueios de segurança do Android Auto.")
                .setNegativeButton("Cancelar", null)
                .setPositiveButton("Estou estacionado", (dialog, which) ->
                        startActivity(new Intent(this, VideoPlayerActivity.class)))
                .show();
    }

    private void openPackage(String packageName) {
        Intent i = getPackageManager().getLaunchIntentForPackage(packageName);
        if (i != null) {
            startActivity(i);
            return;
        }
        Toast.makeText(this, "Aplicativo não instalado.", Toast.LENGTH_SHORT).show();
    }

    private TextView label(String s, int sp, int color) {
        TextView v = new TextView(this);
        v.setText(s);
        v.setTextSize(sp);
        v.setTextColor(color);
        v.setPadding(0,8,0,8);
        return v;
    }

    private Button button(String title, View.OnClickListener listener) {
        Button b = new Button(this);
        b.setText(title);
        b.setAllCaps(false);
        b.setTextSize(16);
        b.setOnClickListener(listener);
        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(-1,-2);
        p.setMargins(0,8,0,8);
        b.setLayoutParams(p);
        return b;
    }
}
