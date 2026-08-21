package com.aionvhub.app;

import android.app.Activity;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.view.Gravity;
import android.widget.LinearLayout;
import android.widget.MediaController;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.VideoView;

/**
 * Player visual local do tablet. Ele não é exposto como interface de vídeo no
 * Android Auto e não tenta remover restrições de movimento do veículo.
 */
public class VideoPlayerActivity extends Activity {
    private VideoView videoView;

    @Override protected void onCreate(Bundle state) {
        super.onCreate(state);

        HubStreamStore.Config config = HubStreamStore.get(this);
        if (!config.configured()) {
            Toast.makeText(this, "Nenhuma fonte configurada.", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(Color.BLACK);

        TextView warning = new TextView(this);
        warning.setText("Player de vídeo no tablet • use somente com o veículo estacionado");
        warning.setTextColor(Color.WHITE);
        warning.setTextSize(15);
        warning.setGravity(Gravity.CENTER);
        warning.setPadding(16,12,16,12);
        root.addView(warning, new LinearLayout.LayoutParams(-1,-2));

        videoView = new VideoView(this);
        LinearLayout.LayoutParams videoParams = new LinearLayout.LayoutParams(-1,0,1f);
        root.addView(videoView, videoParams);
        setContentView(root);

        MediaController controller = new MediaController(this);
        controller.setAnchorView(videoView);
        videoView.setMediaController(controller);
        videoView.setVideoURI(Uri.parse(config.url));
        videoView.setOnPreparedListener(mp -> {
            HubDiagnostics.event(this, "VIDEO preparado title=" + config.title);
            videoView.start();
        });
        videoView.setOnErrorListener((mp, what, extra) -> {
            HubDiagnostics.event(this, "VIDEO erro what=" + what + " extra=" + extra);
            Toast.makeText(this, "Não foi possível reproduzir esta fonte.", Toast.LENGTH_LONG).show();
            return true;
        });
        HubDiagnostics.event(this, "VIDEO abrindo title=" + config.title);
    }

    @Override protected void onPause() {
        if (videoView != null && videoView.isPlaying()) videoView.pause();
        super.onPause();
    }

    @Override protected void onDestroy() {
        if (videoView != null) videoView.stopPlayback();
        super.onDestroy();
    }
}
