package com.aionvhub.app;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.graphics.Shader;
import android.graphics.drawable.Drawable;

/**
 * Gera artwork e ícones leves em runtime, evitando assets binários pesados.
 * A identidade visual usa grafite + ciano, próxima da linguagem visual do AION V.
 */
public final class HubArtwork {
    private static final int BG = Color.rgb(12, 18, 22);
    private static final int SURFACE = Color.rgb(24, 34, 40);
    private static final int ACCENT = Color.rgb(75, 211, 205);
    private static final int TEXT = Color.rgb(244, 247, 248);

    private HubArtwork() {}

    public static Bitmap brand(int size) {
        int s = Math.max(64, size);
        Bitmap bitmap = Bitmap.createBitmap(s, s, Bitmap.Config.ARGB_8888);
        Canvas c = new Canvas(bitmap);
        Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);

        p.setShader(new LinearGradient(0, 0, s, s,
                new int[]{Color.rgb(21, 42, 47), Color.rgb(18, 29, 35), BG},
                null, Shader.TileMode.CLAMP));
        c.drawRoundRect(new RectF(0, 0, s, s), s * .12f, s * .12f, p);
        p.setShader(null);

        float cx = s * .5f;
        float top = s * .20f;
        float bottom = s * .58f;
        Path a = new Path();
        a.moveTo(cx, top);
        a.lineTo(s * .27f, bottom);
        a.lineTo(s * .39f, bottom);
        a.lineTo(cx, s * .39f);
        a.lineTo(s * .61f, bottom);
        a.lineTo(s * .73f, bottom);
        a.close();
        p.setColor(ACCENT);
        c.drawPath(a, p);

        p.setColor(TEXT);
        p.setTextAlign(Paint.Align.CENTER);
        p.setTypeface(android.graphics.Typeface.create(android.graphics.Typeface.DEFAULT, android.graphics.Typeface.BOLD));
        p.setTextSize(s * .105f);
        c.drawText("AION V", cx, s * .74f, p);
        p.setColor(ACCENT);
        p.setTextSize(s * .062f);
        p.setLetterSpacing(.08f);
        c.drawText("HUB", cx, s * .84f, p);
        return bitmap;
    }

    public static Bitmap forEntry(Context context, String mediaId, int size) {
        if (HubAppCatalog.isMediaId(mediaId)) {
            String pkg = HubAppCatalog.packageFromMediaId(mediaId);
            return appIcon(context, pkg, size);
        }
        if (HubMediaCatalog.APPS_ID.equals(mediaId)) return section(size, 1);
        if (HubMediaCatalog.FAVORITES_ID.equals(mediaId)) return section(size, 2);
        if (HubMediaCatalog.IPTV_ID.equals(mediaId) || HubMediaCatalog.CUSTOM_STREAM_ID.equals(mediaId)) return section(size, 3);
        if (HubMediaCatalog.RADIOS_ID.equals(mediaId)) return section(size, 4);
        if (HubMediaCatalog.DIAGNOSTICS_ID.equals(mediaId)) return section(size, 5);
        return brand(size);
    }

    public static Bitmap appIcon(Context context, String packageName, int size) {
        int s = Math.max(32, size);
        try {
            Drawable d = context.getPackageManager().getApplicationIcon(packageName);
            Bitmap out = Bitmap.createBitmap(s, s, Bitmap.Config.ARGB_8888);
            Canvas c = new Canvas(out);
            int pad = Math.max(2, s / 12);
            d.setBounds(pad, pad, s - pad, s - pad);
            d.draw(c);
            return out;
        } catch (Exception ignored) {
            return section(s, 1);
        }
    }

    private static Bitmap section(int size, int kind) {
        int s = Math.max(32, size);
        Bitmap bitmap = Bitmap.createBitmap(s, s, Bitmap.Config.ARGB_8888);
        Canvas c = new Canvas(bitmap);
        Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
        p.setColor(SURFACE);
        c.drawRoundRect(new RectF(0, 0, s, s), s * .22f, s * .22f, p);
        p.setColor(ACCENT);
        p.setStyle(Paint.Style.STROKE);
        p.setStrokeWidth(Math.max(2f, s * .07f));
        p.setStrokeCap(Paint.Cap.ROUND);

        float m = s * .24f;
        float e = s - m;
        float mid = s * .5f;

        if (kind == 1) { // apps: grade 2x2
            float r = s * .11f;
            p.setStyle(Paint.Style.FILL);
            c.drawRoundRect(new RectF(m, m, mid - s * .04f, mid - s * .04f), r, r, p);
            c.drawRoundRect(new RectF(mid + s * .04f, m, e, mid - s * .04f), r, r, p);
            c.drawRoundRect(new RectF(m, mid + s * .04f, mid - s * .04f, e), r, r, p);
            c.drawRoundRect(new RectF(mid + s * .04f, mid + s * .04f, e, e), r, r, p);
        } else if (kind == 2) { // favorito: estrela
            p.setStyle(Paint.Style.FILL);
            Path star = new Path();
            for (int i = 0; i < 10; i++) {
                double a = -Math.PI / 2 + i * Math.PI / 5;
                float r = (i % 2 == 0) ? s * .28f : s * .12f;
                float x = mid + (float) Math.cos(a) * r;
                float y = mid + (float) Math.sin(a) * r;
                if (i == 0) star.moveTo(x, y); else star.lineTo(x, y);
            }
            star.close();
            c.drawPath(star, p);
        } else if (kind == 3) { // stream: play
            p.setStyle(Paint.Style.FILL);
            Path play = new Path();
            play.moveTo(s * .39f, s * .30f);
            play.lineTo(s * .73f, mid);
            play.lineTo(s * .39f, s * .70f);
            play.close();
            c.drawPath(play, p);
        } else if (kind == 4) { // radio: antena + ondas
            p.setStyle(Paint.Style.STROKE);
            c.drawLine(mid, s * .46f, mid, s * .73f, p);
            c.drawCircle(mid, s * .39f, s * .055f, p);
            c.drawArc(new RectF(s * .31f, s * .22f, s * .69f, s * .60f), -58, 116, false, p);
            c.drawArc(new RectF(s * .20f, s * .11f, s * .80f, s * .71f), -52, 104, false, p);
        } else { // diagnóstico: pulso
            p.setStyle(Paint.Style.STROKE);
            Path pulse = new Path();
            pulse.moveTo(m, mid);
            pulse.lineTo(s * .39f, mid);
            pulse.lineTo(s * .46f, s * .32f);
            pulse.lineTo(s * .56f, s * .68f);
            pulse.lineTo(s * .64f, mid);
            pulse.lineTo(e, mid);
            c.drawPath(pulse, p);
        }
        return bitmap;
    }
}
