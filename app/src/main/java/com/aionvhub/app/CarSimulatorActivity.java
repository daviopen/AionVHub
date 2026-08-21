package com.aionvhub.app;

import android.app.Activity;
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

import java.util.List;

/**
 * Demo interativa da experiência planejada para a central do carro.
 * A simulação lê sempre as configurações atuais salvas em AndroidAutoConfigStore.
 */
public class CarSimulatorActivity extends Activity {
    private final int bg = Color.rgb(7, 11, 14);
    private final int surface = Color.rgb(25, 31, 36);
    private final int surface2 = Color.rgb(36, 44, 50);
    private final int text = Color.rgb(244, 247, 248);
    private final int muted = Color.rgb(157, 175, 183);
    private final int accent = Color.rgb(75, 211, 205);

    private LinearLayout content;
    private TextView screenTitle;
    private TextView backButton;

    @Override protected void onCreate(Bundle state) {
        super.onCreate(state);
        buildFrame();
        showHome();
    }

    @Override protected void onResume() {
        super.onResume();
        if (content != null) showHome();
    }

    private void buildFrame() {
        LinearLayout page = new LinearLayout(this);
        page.setOrientation(LinearLayout.VERTICAL);
        page.setPadding(dp(14), dp(12), dp(14), dp(12));
        page.setBackgroundColor(bg);
        setContentView(page);

        LinearLayout header = new LinearLayout(this);
        header.setOrientation(LinearLayout.HORIZONTAL);
        header.setGravity(Gravity.CENTER_VERTICAL);

        ImageView brand = new ImageView(this);
        brand.setImageBitmap(HubArtwork.brand(dp(44)));
        header.addView(brand, new LinearLayout.LayoutParams(dp(44), dp(44)));

        LinearLayout titles = new LinearLayout(this);
        titles.setOrientation(LinearLayout.VERTICAL);
        titles.setPadding(dp(10), 0, 0, 0);
        TextView eyebrow = label("DEMO DA CENTRAL", 10, accent);
        eyebrow.setLetterSpacing(.12f);
        titles.addView(eyebrow);
        screenTitle = label("AION V Hub", 21, text);
        titles.addView(screenTitle);
        header.addView(titles, new LinearLayout.LayoutParams(0, -2, 1f));

        TextView live = label("CONFIG ATUAL", 10, accent);
        live.setPadding(dp(10), dp(6), dp(10), dp(6));
        live.setBackground(pill(Color.rgb(21, 61, 64)));
        header.addView(live);
        page.addView(header);

        LinearLayout car = new LinearLayout(this);
        car.setOrientation(LinearLayout.HORIZONTAL);
        car.setBackground(frame());
        LinearLayout.LayoutParams cp = new LinearLayout.LayoutParams(-1, 0, 1f);
        cp.setMargins(0, dp(10), 0, dp(8));
        page.addView(car, cp);

        LinearLayout rail = new LinearLayout(this);
        rail.setOrientation(LinearLayout.VERTICAL);
        rail.setGravity(Gravity.CENTER_HORIZONTAL);
        rail.setPadding(dp(7), dp(12), dp(7), dp(12));
        rail.setBackgroundColor(Color.rgb(13, 18, 21));
        car.addView(rail, new LinearLayout.LayoutParams(dp(64), -1));

        TextView home = railButton("⌂");
        home.setOnClickListener(v -> showHome());
        rail.addView(home);

        backButton = railButton("‹");
        backButton.setOnClickListener(v -> showHome());
        LinearLayout.LayoutParams bp = new LinearLayout.LayoutParams(-1, -2);
        bp.setMargins(0, dp(10), 0, 0);
        rail.addView(backButton, bp);

        LinearLayout mediaArea = new LinearLayout(this);
        mediaArea.setOrientation(LinearLayout.VERTICAL);
        mediaArea.setPadding(dp(16), dp(14), dp(16), dp(14));
        car.addView(mediaArea, new LinearLayout.LayoutParams(0, -1, 1f));

        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(true);
        mediaArea.addView(scroll, new LinearLayout.LayoutParams(-1, -1));
        content = new LinearLayout(this);
        content.setOrientation(LinearLayout.VERTICAL);
        scroll.addView(content);

        LinearLayout map = new LinearLayout(this);
        map.setOrientation(LinearLayout.VERTICAL);
        map.setGravity(Gravity.CENTER);
        map.setPadding(dp(12), dp(12), dp(12), dp(12));
        map.setBackground(mapBackground());
        LinearLayout.LayoutParams mp = new LinearLayout.LayoutParams(0, -1, .42f);
        mp.setMargins(dp(4), dp(4), dp(4), dp(4));
        car.addView(map, mp);

        TextView nav = label("NAVEGAÇÃO", 10, Color.rgb(88, 103, 111));
        nav.setLetterSpacing(.12f);
        map.addView(nav);
        TextView roads = label("╱     ╲\n   ╲  ╱\n     ●\n  ╱    ╲\n╲     ╱", 27, Color.rgb(116, 128, 135));
        roads.setGravity(Gravity.CENTER);
        map.addView(roads, new LinearLayout.LayoutParams(-1, 0, 1f));
        map.addView(label("Mapa ativo", 12, Color.rgb(88, 101, 108)));

        TextView disclaimer = label("Simulação baseada nas configurações atuais. O Android Auto real pode adaptar grade, abas e espaçamento.", 10, muted);
        disclaimer.setGravity(Gravity.CENTER);
        page.addView(disclaimer);
    }

    private void showHome() {
        screenTitle.setText("AION V Hub");
        backButton.setVisibility(View.INVISIBLE);
        content.removeAllViews();

        content.addView(label("Minha central", 23, text));
        content.addView(label("A mesma seleção feita no tablet, em uma demo navegável.", 12, muted));

        GridLayout menus = new GridLayout(this);
        menus.setColumnCount(2);
        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(-1, -2);
        p.setMargins(0, dp(12), 0, 0);
        content.addView(menus, p);

        addMenu(menus, HubMediaCatalog.APPS_ID, "Apps", "Aplicativos escolhidos", v -> showApps());
        addMenu(menus, HubMediaCatalog.FAVORITES_ID, "Favoritos", "Seus acessos rápidos", v -> showFavorites());
        addMenu(menus, HubMediaCatalog.IPTV_ID, "IPTV / Streams", "Fontes configuradas", v -> showIptv());
        addMenu(menus, HubMediaCatalog.RADIOS_ID, "Rádios", "Áudio e estações", v -> showRadios());
        addMenu(menus, HubMediaCatalog.DIAGNOSTICS_ID, "Diagnóstico", "Estado do Hub", v -> showDiagnostics());

        if (menus.getChildCount() == 0) {
            content.addView(infoCard("Nenhum menu habilitado", "Volte em Minha central e escolha o que quer mostrar no carro."));
        }
    }

    private void addMenu(GridLayout grid, String mediaId, String title, String subtitle, View.OnClickListener click) {
        if (!AndroidAutoConfigStore.isFeatureEnabled(this, mediaId)) return;
        LinearLayout card = tile(mediaId, title, subtitle);
        card.setOnClickListener(click);
        grid.addView(card, gridParams(2));
    }

    private void showApps() {
        showSectionHeader("Apps");
        List<HubAppCatalog.LaunchableApp> apps = AndroidAutoConfigStore.visibleApps(this);
        content.addView(label(apps.size() + " apps disponíveis nesta configuração", 12, muted));

        if (apps.isEmpty()) {
            content.addView(infoCard("Nenhum app selecionado", "Escolha os apps em Minha central no tablet."));
            return;
        }

        GridLayout grid = new GridLayout(this);
        grid.setColumnCount(3);
        LinearLayout.LayoutParams gp = new LinearLayout.LayoutParams(-1, -2);
        gp.setMargins(0, dp(10), 0, 0);
        content.addView(grid, gp);

        for (HubAppCatalog.LaunchableApp app : apps) {
            LinearLayout card = new LinearLayout(this);
            card.setOrientation(LinearLayout.VERTICAL);
            card.setGravity(Gravity.CENTER);
            card.setPadding(dp(8), dp(10), dp(8), dp(9));
            card.setBackground(tileBackground(surface2));
            card.setClickable(true);
            card.setOnClickListener(v -> Toast.makeText(this,
                    "Demo: no carro, “" + app.label + "” tentaria abrir no tablet.", Toast.LENGTH_SHORT).show());

            ImageView icon = new ImageView(this);
            icon.setImageDrawable(HubAppCatalog.icon(this, app));
            card.addView(icon, new LinearLayout.LayoutParams(dp(45), dp(45)));
            TextView name = label(app.label, 11, text);
            name.setGravity(Gravity.CENTER);
            name.setMaxLines(2);
            card.addView(name);
            grid.addView(card, gridParams(3));
        }
    }

    private void showFavorites() {
        showSectionHeader("Favoritos");
        HubStreamStore.Config c = HubStreamStore.get(this);
        if (c.configured() && c.favorite) {
            LinearLayout card = tile(HubMediaCatalog.CUSTOM_STREAM_ID, c.title, "Fonte favorita");
            card.setOnClickListener(v -> showNowPlaying(c.title, "Favorito • stream configurado"));
            content.addView(card);
        } else {
            content.addView(infoCard("Nenhum favorito", "Marque uma fonte como favorita no tablet."));
        }
    }

    private void showIptv() {
        showSectionHeader("IPTV / Streams");
        HubStreamStore.Config c = HubStreamStore.get(this);
        if (!c.configured()) {
            content.addView(infoCard("Nenhuma fonte configurada", "Configure uma URL de mídia no tablet."));
            return;
        }
        LinearLayout card = tile(HubMediaCatalog.CUSTOM_STREAM_ID, c.title, "Fonte configurada no AION V Hub");
        card.setOnClickListener(v -> showNowPlaying(c.title, "IPTV / Stream"));
        content.addView(card);
    }

    private void showRadios() {
        showSectionHeader("Rádios");
        content.addView(infoCard("Rádios", "Esta área mostrará as rádios e streams de áudio configurados no Hub."));
    }

    private void showDiagnostics() {
        showSectionHeader("Diagnóstico");
        content.addView(infoCard("Android Auto", "MediaBrowserService • configuração atual carregada"));
        content.addView(infoCard("Apps", AndroidAutoConfigStore.visibleApps(this).size() + " apps visíveis na central"));
        content.addView(infoCard("Shizuku", "Opcional para diagnóstico avançado"));
    }

    private void showNowPlaying(String title, String subtitle) {
        screenTitle.setText("Reproduzindo");
        backButton.setVisibility(View.VISIBLE);
        content.removeAllViews();

        ImageView art = new ImageView(this);
        art.setImageBitmap(HubArtwork.forEntry(this, HubMediaCatalog.CUSTOM_STREAM_ID, dp(170)));
        art.setScaleType(ImageView.ScaleType.CENTER_INSIDE);
        content.addView(art, new LinearLayout.LayoutParams(-1, dp(190)));

        TextView t = label(title, 24, text);
        t.setGravity(Gravity.CENTER);
        content.addView(t);
        TextView s = label(subtitle, 13, muted);
        s.setGravity(Gravity.CENTER);
        content.addView(s);

        LinearLayout controls = new LinearLayout(this);
        controls.setOrientation(LinearLayout.HORIZONTAL);
        controls.setGravity(Gravity.CENTER);
        controls.setPadding(0, dp(16), 0, 0);
        TextView pause = label("Ⅱ", 26, text);
        pause.setGravity(Gravity.CENTER);
        pause.setPadding(dp(22), dp(12), dp(22), dp(12));
        pause.setBackground(pill(Color.rgb(232, 238, 240)));
        pause.setTextColor(Color.rgb(20, 27, 31));
        controls.addView(pause);
        content.addView(controls);
    }

    private void showSectionHeader(String title) {
        screenTitle.setText(title);
        backButton.setVisibility(View.VISIBLE);
        content.removeAllViews();
        content.addView(label(title, 23, text));
    }

    private LinearLayout tile(String mediaId, String title, String subtitle) {
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.HORIZONTAL);
        card.setGravity(Gravity.CENTER_VERTICAL);
        card.setPadding(dp(12), dp(12), dp(12), dp(12));
        card.setBackground(tileBackground(surface));
        card.setClickable(true);

        ImageView icon = new ImageView(this);
        icon.setImageBitmap(HubArtwork.forEntry(this, mediaId, dp(43)));
        card.addView(icon, new LinearLayout.LayoutParams(dp(43), dp(43)));

        LinearLayout copy = new LinearLayout(this);
        copy.setOrientation(LinearLayout.VERTICAL);
        copy.setPadding(dp(10), 0, 0, 0);
        copy.addView(label(title, 14, text));
        TextView sub = label(subtitle, 10, muted);
        sub.setMaxLines(2);
        copy.addView(sub);
        card.addView(copy, new LinearLayout.LayoutParams(0, -2, 1f));
        return card;
    }

    private View infoCard(String title, String subtitle) {
        LinearLayout card = tile(HubMediaCatalog.ROOT_ID, title, subtitle);
        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(-1, -2);
        p.setMargins(0, dp(10), 0, 0);
        card.setLayoutParams(p);
        return card;
    }

    private TextView railButton(String value) {
        TextView v = label(value, 26, text);
        v.setGravity(Gravity.CENTER);
        v.setPadding(dp(8), dp(7), dp(8), dp(7));
        v.setBackground(pill(Color.rgb(25, 31, 36)));
        return v;
    }

    private GridLayout.LayoutParams gridParams(int columns) {
        GridLayout.LayoutParams p = new GridLayout.LayoutParams();
        p.width = 0;
        p.height = -2;
        p.columnSpec = GridLayout.spec(GridLayout.UNDEFINED, 1f);
        p.setMargins(dp(4), dp(4), dp(4), dp(4));
        return p;
    }

    private TextView label(String value, int sp, int color) {
        TextView v = new TextView(this);
        v.setText(value);
        v.setTextSize(sp);
        v.setTextColor(color);
        v.setPadding(0, dp(3), 0, dp(3));
        return v;
    }

    private GradientDrawable frame() {
        GradientDrawable d = new GradientDrawable(
                GradientDrawable.Orientation.TL_BR,
                new int[]{Color.rgb(31, 37, 41), Color.rgb(19, 25, 29), Color.rgb(11, 16, 19)});
        d.setCornerRadius(dp(23));
        d.setStroke(dp(2), Color.rgb(67, 82, 90));
        return d;
    }

    private GradientDrawable tileBackground(int color) {
        GradientDrawable d = new GradientDrawable();
        d.setColor(color);
        d.setCornerRadius(dp(16));
        d.setStroke(dp(1), Color.rgb(51, 62, 68));
        return d;
    }

    private GradientDrawable pill(int color) {
        GradientDrawable d = new GradientDrawable();
        d.setColor(color);
        d.setCornerRadius(dp(30));
        return d;
    }

    private GradientDrawable mapBackground() {
        GradientDrawable d = new GradientDrawable(
                GradientDrawable.Orientation.TL_BR,
                new int[]{Color.rgb(236, 238, 232), Color.rgb(214, 221, 216)});
        d.setCornerRadius(dp(18));
        return d;
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }
}
