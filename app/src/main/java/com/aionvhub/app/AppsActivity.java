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

/** Grade dinâmica dos aplicativos instalados, sem lista fixa de pacotes. */
public class AppsActivity extends Activity {
    private final int bg = Color.rgb(10, 15, 19);
    private final int surface = Color.rgb(22, 29, 34);
    private final int surface2 = Color.rgb(28, 38, 44);
    private final int text = Color.rgb(244, 247, 248);
    private final int muted = Color.rgb(161, 176, 184);
    private final int accent = Color.rgb(77, 210, 205);

    @Override protected void onCreate(Bundle state) {
        super.onCreate(state);
        buildUi();
    }

    private void buildUi() {
        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(true);
        scroll.setBackgroundColor(bg);

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(22), dp(24), dp(22), dp(36));
        scroll.addView(root);
        setContentView(scroll);

        TextView eyebrow = text("AION V HUB", 13, accent);
        eyebrow.setLetterSpacing(.12f);
        root.addView(eyebrow);

        root.addView(text("Apps do tablet", 30, text));
        root.addView(text("Detectados automaticamente. Instale ou remova apps e esta tela se atualiza sozinha.", 15, muted));

        List<HubAppCatalog.LaunchableApp> apps = HubAppCatalog.listLaunchable(this);

        TextView count = text(apps.size() + " aplicativos disponíveis", 14, muted);
        count.setPadding(0, dp(10), 0, dp(14));
        root.addView(count);

        GridLayout grid = new GridLayout(this);
        grid.setColumnCount(3);
        grid.setUseDefaultMargins(false);
        root.addView(grid, new LinearLayout.LayoutParams(-1, -2));

        if (apps.isEmpty()) {
            TextView empty = text("Nenhum aplicativo iniciável foi encontrado.", 16, text);
            empty.setPadding(dp(18), dp(24), dp(18), dp(24));
            empty.setBackground(cardBackground(surface));
            root.addView(empty);
            return;
        }

        for (HubAppCatalog.LaunchableApp app : apps) {
            grid.addView(appCard(app), gridParams());
        }
    }

    private View appCard(HubAppCatalog.LaunchableApp app) {
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setGravity(Gravity.CENTER_HORIZONTAL);
        card.setPadding(dp(12), dp(16), dp(12), dp(14));
        card.setBackground(cardBackground(surface2));
        card.setClickable(true);
        card.setFocusable(true);

        ImageView icon = new ImageView(this);
        icon.setImageDrawable(HubAppCatalog.icon(this, app));
        icon.setScaleType(ImageView.ScaleType.FIT_CENTER);
        card.addView(icon, new LinearLayout.LayoutParams(dp(54), dp(54)));

        TextView title = text(app.label, 14, text);
        title.setGravity(Gravity.CENTER);
        title.setMaxLines(2);
        title.setPadding(0, dp(9), 0, 0);
        card.addView(title, new LinearLayout.LayoutParams(-1, -2));

        card.setOnClickListener(v -> {
            if (!HubAppCatalog.launch(this, app.packageName)) {
                Toast.makeText(this, "Não foi possível abrir " + app.label, Toast.LENGTH_SHORT).show();
            }
        });
        return card;
    }

    private GridLayout.LayoutParams gridParams() {
        GridLayout.LayoutParams p = new GridLayout.LayoutParams();
        p.width = 0;
        p.height = -2;
        p.columnSpec = GridLayout.spec(GridLayout.UNDEFINED, 1f);
        p.setMargins(dp(5), dp(5), dp(5), dp(5));
        return p;
    }

    private GradientDrawable cardBackground(int color) {
        GradientDrawable d = new GradientDrawable();
        d.setColor(color);
        d.setCornerRadius(dp(18));
        d.setStroke(dp(1), Color.rgb(38, 52, 59));
        return d;
    }

    private TextView text(String value, int sp, int color) {
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
