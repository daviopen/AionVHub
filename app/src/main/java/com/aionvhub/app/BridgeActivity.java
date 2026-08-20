package com.aionvhub.app;

import android.app.Activity;
import android.graphics.Color;
import android.os.Bundle;
import android.view.Gravity;
import android.widget.LinearLayout;
import android.widget.TextView;

public class BridgeActivity extends Activity {
    private final int bg = Color.rgb(14, 21, 26);
    private final int text = Color.rgb(239, 245, 246);
    private final int muted = Color.rgb(164, 180, 187);
    private final int accent = Color.rgb(100, 216, 203);

    @Override
    public void onCreate(Bundle state) {
        super.onCreate(state);
        HubDiagnostics.event(this, "BRIDGE onCreate");
        buildUi();
    }

    @Override
    protected void onResume() {
        super.onResume();
        HubDiagnostics.event(this, "BRIDGE onResume");
    }

    @Override
    protected void onPause() {
        HubDiagnostics.event(this, "BRIDGE onPause");
        super.onPause();
    }

    @Override
    protected void onDestroy() {
        HubDiagnostics.event(this, "BRIDGE onDestroy");
        super.onDestroy();
    }

    private void buildUi() {
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setGravity(Gravity.CENTER);
        root.setPadding(48, 48, 48, 48);
        root.setBackgroundColor(bg);

        TextView title = text("AION V HUB", 30, accent);
        title.setGravity(Gravity.CENTER);
        root.addView(title);

        TextView ok = text("Bridge iniciado com sucesso", 22, text);
        ok.setGravity(Gravity.CENTER);
        ok.setPadding(0, 24, 0, 16);
        root.addView(ok);

        TextView details = text(
                "Esta tela confirma que o host automotivo conseguiu abrir diretamente a atividade do AION V Hub.\n\n" +
                "Versão: " + BuildConfig.VERSION_NAME + "\n" +
                "Android: " + android.os.Build.VERSION.RELEASE + " (API " + android.os.Build.VERSION.SDK_INT + ")",
                17,
                muted
        );
        details.setGravity(Gravity.CENTER);
        root.addView(details);

        TextView safety = text(
                "O Bridge não remove bloqueios de movimento. O host do veículo continua responsável por permitir ou bloquear a interface conforme o estado do carro.",
                14,
                muted
        );
        safety.setGravity(Gravity.CENTER);
        safety.setPadding(0, 30, 0, 0);
        root.addView(safety);

        setContentView(root);
    }

    private TextView text(String value, int size, int color) {
        TextView v = new TextView(this);
        v.setText(value);
        v.setTextSize(size);
        v.setTextColor(color);
        v.setTextIsSelectable(true);
        return v;
    }
}
