package com.aionvhub.app;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.widget.GridLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.car.app.connection.CarConnection;
import androidx.lifecycle.Observer;

/** Tela inicial do Hub com identidade visual inspirada na central do AION V. */
public class HomeActivity extends Activity {
    private final int bg = Color.rgb(9, 14, 18);
    private final int surface = Color.rgb(21, 29, 34);
    private final int surface2 = Color.rgb(27, 37, 43);
    private final int text = Color.rgb(244, 247, 248);
    private final int muted = Color.rgb(157, 175, 183);
    private final int accent = Color.rgb(75, 211, 205);
    private final int accentDeep = Color.rgb(30, 111, 113);

    private LinearLayout root;
    private TextView connectionStatus;
    private TextView streamStatus;
    private TextView appsStatus;
    private CarConnection carConnection;
    private Observer<Integer> carObserver;

    @Override protected void onCreate(Bundle state) {
        super.onCreate(state);
        buildUi();
        startCarObserver();
    }

    @Override protected void onResume() {
        super.onResume();
        refreshSummary();
    }

    @Override protected void onDestroy() {
        if (carConnection != null && carObserver != null) {
            carConnection.getType().removeObserver(carObserver);
        }
        super.onDestroy();
    }

    private void buildUi() {
        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(true);
        scroll.setBackgroundColor(bg);

        root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(22), dp(24), dp(22), dp(40));
        scroll.addView(root);
        setContentView(scroll);

        LinearLayout brandRow = new LinearLayout(this);
        brandRow.setOrientation(LinearLayout.HORIZONTAL);
        brandRow.setGravity(Gravity.CENTER_VERTICAL);
        ImageView brandArt = new ImageView(this);
        brandArt.setImageBitmap(HubArtwork.brand(dp(58)));
        brandRow.addView(brandArt, new LinearLayout.LayoutParams(dp(58), dp(58)));
        LinearLayout brandText = new LinearLayout(this);
        brandText.setOrientation(LinearLayout.VERTICAL);
        brandText.setPadding(dp(12), 0, 0, 0);
        TextView brand = label("AION V HUB", 13, accent);
        brand.setLetterSpacing(.14f);
        brandText.addView(brand);
        brandText.addView(label("Drive & Media", 28, text));
        brandRow.addView(brandText, new LinearLayout.LayoutParams(0, -2, 1f));
        root.addView(brandRow);
        root.addView(label("Seu tablet como central complementar do AION V", 15, muted));

        LinearLayout hero = new LinearLayout(this);
        hero.setOrientation(LinearLayout.VERTICAL);
        hero.setPadding(dp(20), dp(18), dp(20), dp(18));
        hero.setBackground(heroBackground());
        LinearLayout.LayoutParams heroParams = new LinearLayout.LayoutParams(-1, -2);
        heroParams.setMargins(0, dp(18), 0, dp(18));
        root.addView(hero, heroParams);

        connectionStatus = label("Verificando Android Auto…", 18, text);
        hero.addView(connectionStatus);
        appsStatus = label("Apps: carregando…", 14, muted);
        hero.addView(appsStatus);
        streamStatus = label("Mídia: carregando…", 14, muted);
        hero.addView(streamStatus);
        hero.addView(label("v" + BuildConfig.VERSION_NAME + " • Bridge MediaBrowser validado no AION V", 12, accent));

        root.addView(section("PAINEL"));

        GridLayout grid = new GridLayout(this);
        grid.setColumnCount(2);
        root.addView(grid, new LinearLayout.LayoutParams(-1, -2));

        grid.addView(featureCard(HubMediaCatalog.APPS_ID, "Apps", "Detecta automaticamente os apps instalados", v ->
                startActivity(new Intent(this, AppsActivity.class))), featureParams());
        grid.addView(featureCard(HubMediaCatalog.IPTV_ID, "IPTV / Streams", "Configure sua fonte de mídia", v ->
                startActivity(new Intent(this, StreamSettingsActivity.class))), featureParams());
        grid.addView(featureCard(HubMediaCatalog.CUSTOM_STREAM_ID, "Player", "Vídeo no tablet com o veículo estacionado", v -> openVideoPlayer()), featureParams());
        grid.addView(featureCard(HubMediaCatalog.DIAGNOSTICS_ID, "Diagnóstico", "Bridge, rede, Android Auto e Shizuku", v ->
                startActivity(new Intent(this, MainActivity.class))), featureParams());

        root.addView(section("COMO FUNCIONA"));
        LinearLayout note = new LinearLayout(this);
        note.setOrientation(LinearLayout.VERTICAL);
        note.setPadding(dp(18), dp(16), dp(18), dp(16));
        note.setBackground(cardBackground(surface));
        note.addView(label("Apps agnósticos", 17, text));
        note.addView(label("O Hub consulta o Android e monta a lista dos apps iniciáveis. Não há uma lista fixa de UniTV, YouTube, Spotify ou outros.", 14, muted));
        note.addView(label("No Android Auto, os apps aparecem como catálogo informativo com seus próprios ícones. A interface de um APK comum não é projetada dentro da central.", 13, muted));
        root.addView(note);

        TextView footer = label("AION V Hub • experiência inspirada na interface do veículo", 12, muted);
        footer.setGravity(Gravity.CENTER);
        footer.setPadding(0, dp(24), 0, 0);
        root.addView(footer);
    }

    private void startCarObserver() {
        carConnection = new CarConnection(this);
        carObserver = type -> {
            if (connectionStatus == null) return;
            if (type != null && type == CarConnection.CONNECTION_TYPE_PROJECTION) {
                connectionStatus.setText("● Android Auto conectado");
                connectionStatus.setTextColor(accent);
            } else if (type != null && type == CarConnection.CONNECTION_TYPE_NATIVE) {
                connectionStatus.setText("● Android Automotive conectado");
                connectionStatus.setTextColor(accent);
            } else {
                connectionStatus.setText("○ Aguardando Android Auto");
                connectionStatus.setTextColor(text);
            }
        };
        carConnection.getType().observeForever(carObserver);
    }

    private void refreshSummary() {
        if (appsStatus != null) {
            int count = HubAppCatalog.listLaunchable(this).size();
            appsStatus.setText(count + " apps do tablet detectados automaticamente");
        }
        if (streamStatus != null) {
            HubStreamStore.Config c = HubStreamStore.get(this);
            streamStatus.setText(c.configured()
                    ? "Fonte de mídia: " + c.title
                    : "Nenhuma fonte IPTV/stream configurada");
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
                .setTitle("Player visual")
                .setMessage("Use a reprodução de vídeo somente com o veículo estacionado.")
                .setNegativeButton("Cancelar", null)
                .setPositiveButton("Estou estacionado", (dialog, which) ->
                        startActivity(new Intent(this, VideoPlayerActivity.class)))
                .show();
    }

    private View featureCard(String mediaId, String title, String subtitle, View.OnClickListener listener) {
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setPadding(dp(17), dp(17), dp(17), dp(17));
        card.setBackground(cardBackground(surface2));
        card.setClickable(true);
        card.setFocusable(true);
        card.setOnClickListener(listener);

        ImageView art = new ImageView(this);
        art.setScaleType(ImageView.ScaleType.FIT_CENTER);
        art.setImageBitmap(HubArtwork.forEntry(this, mediaId, dp(48)));
        LinearLayout.LayoutParams artParams = new LinearLayout.LayoutParams(dp(48), dp(48));
        artParams.setMargins(0, 0, 0, dp(8));
        card.addView(art, artParams);

        card.addView(label(title, 19, text));
        TextView sub = label(subtitle, 13, muted);
        sub.setMaxLines(3);
        card.addView(sub);
        return card;
    }

    private GridLayout.LayoutParams featureParams() {
        GridLayout.LayoutParams p = new GridLayout.LayoutParams();
        p.width = 0;
        p.height = -1;
        p.columnSpec = GridLayout.spec(GridLayout.UNDEFINED, 1f);
        p.setMargins(dp(5), dp(5), dp(5), dp(5));
        return p;
    }

    private TextView section(String value) {
        TextView v = label(value, 12, accent);
        v.setLetterSpacing(.12f);
        v.setPadding(0, dp(16), 0, dp(8));
        return v;
    }

    private GradientDrawable heroBackground() {
        GradientDrawable d = new GradientDrawable(
                GradientDrawable.Orientation.TL_BR,
                new int[]{Color.rgb(19, 38, 43), Color.rgb(18, 28, 34), Color.rgb(12, 18, 22)}
        );
        d.setCornerRadius(dp(24));
        d.setStroke(dp(1), accentDeep);
        return d;
    }

    private GradientDrawable cardBackground(int color) {
        GradientDrawable d = new GradientDrawable();
        d.setColor(color);
        d.setCornerRadius(dp(18));
        d.setStroke(dp(1), Color.rgb(38, 52, 59));
        return d;
    }

    private TextView label(String value, int sp, int color) {
        TextView v = new TextView(this);
        v.setText(value);
        v.setTextSize(sp);
        v.setTextColor(color);
        v.setPadding(0, dp(4), 0, dp(4));
        return v;
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }
}
