package com.aionvhub.app;

import android.app.Activity;
import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

/** Configuração simples de uma fonte direta de mídia (HLS/MP3/AAC/MP4 etc.). */
public class StreamSettingsActivity extends Activity {
    private final int bg = Color.rgb(14,21,26);
    private final int text = Color.rgb(239,245,246);
    private final int muted = Color.rgb(164,180,187);
    private final int accent = Color.rgb(100,216,203);

    @Override protected void onCreate(Bundle state) {
        super.onCreate(state);
        buildUi();
    }

    private void buildUi() {
        ScrollView scroll = new ScrollView(this);
        scroll.setBackgroundColor(bg);
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(32,32,32,48);
        scroll.addView(root);
        setContentView(scroll);

        root.addView(label("Configurar IPTV / stream", 26, accent));
        root.addView(label(
                "Informe uma URL direta de mídia. Nesta primeira versão funcional, o Hub aceita um stream direto; suporte a listas M3U/EPG será ampliado nas próximas versões.",
                14,
                muted
        ));

        HubStreamStore.Config current = HubStreamStore.get(this);

        EditText title = new EditText(this);
        title.setHint("Nome, por exemplo: TV Sala / Rádio Favorita");
        title.setText(current.title);
        title.setTextColor(text);
        title.setHintTextColor(muted);
        root.addView(title);

        EditText url = new EditText(this);
        url.setHint("https://.../stream.m3u8 ou URL direta");
        url.setText(current.url);
        url.setTextColor(text);
        url.setHintTextColor(muted);
        url.setSingleLine(false);
        root.addView(url);

        CheckBox favorite = new CheckBox(this);
        favorite.setText("Mostrar também em Favoritos");
        favorite.setTextColor(text);
        favorite.setChecked(current.favorite);
        root.addView(favorite);

        root.addView(button("Salvar", v -> {
            String t = title.getText().toString().trim();
            String u = url.getText().toString().trim();
            if (!(u.startsWith("http://") || u.startsWith("https://"))) {
                Toast.makeText(this, "Use uma URL http:// ou https:// válida.", Toast.LENGTH_LONG).show();
                return;
            }
            HubStreamStore.save(this, t, u, favorite.isChecked());
            HubDiagnostics.event(this, "HUB stream configurado title=" + (t.isEmpty() ? "Minha transmissão" : t));
            Toast.makeText(this, "Fonte salva.", Toast.LENGTH_SHORT).show();
            finish();
        }));

        root.addView(button("Remover configuração", v -> {
            HubStreamStore.clear(this);
            HubDiagnostics.event(this, "HUB stream removido");
            Toast.makeText(this, "Fonte removida.", Toast.LENGTH_SHORT).show();
            finish();
        }));

        root.addView(label(
                "Uso no carro: o Android Auto recebe catálogo, áudio e controles de mídia. O player visual permanece no tablet e deve ser usado somente com o veículo estacionado.",
                14,
                muted
        ));
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
        p.setMargins(0,10,0,10);
        b.setLayoutParams(p);
        return b;
    }
}
