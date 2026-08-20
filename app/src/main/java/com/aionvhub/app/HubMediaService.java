package com.aionvhub.app;

import android.content.Intent;
import android.media.MediaDescription;
import android.media.MediaMetadata;
import android.media.browse.MediaBrowser;
import android.media.session.MediaSession;
import android.media.session.PlaybackState;
import android.os.Bundle;
import android.service.media.MediaBrowserService;
import java.util.ArrayList;
import java.util.List;

public class HubMediaService extends MediaBrowserService {
    private MediaSession mediaSession;

    @Override public void onCreate() {
        super.onCreate();
        HubDiagnostics.log(this, "MEDIA onCreate");
        try {
            mediaSession = new MediaSession(this, "AionVHubMedia");
            HubDiagnostics.log(this, "MEDIA MediaSession criada");

            mediaSession.setCallback(new MediaSession.Callback() {
                @Override public void onPlay() {
                    HubDiagnostics.log(HubMediaService.this, "MEDIA onPlay");
                    setPlaybackState(PlaybackState.STATE_PLAYING);
                }
                @Override public void onPlayFromMediaId(String mediaId, Bundle extras) {
                    HubDiagnostics.log(HubMediaService.this, "MEDIA onPlayFromMediaId: " + mediaId);
                    mediaSession.setMetadata(new MediaMetadata.Builder()
                            .putString(MediaMetadata.METADATA_KEY_MEDIA_ID, mediaId)
                            .putString(MediaMetadata.METADATA_KEY_TITLE, "AION V Hub")
                            .putString(MediaMetadata.METADATA_KEY_ARTIST, "Teste Android Auto")
                            .build());
                    setPlaybackState(PlaybackState.STATE_PLAYING);
                }
                @Override public void onPause() {
                    HubDiagnostics.log(HubMediaService.this, "MEDIA onPause");
                    setPlaybackState(PlaybackState.STATE_PAUSED);
                }
                @Override public void onStop() {
                    HubDiagnostics.log(HubMediaService.this, "MEDIA onStop");
                    setPlaybackState(PlaybackState.STATE_STOPPED);
                }
            });

            mediaSession.setMetadata(new MediaMetadata.Builder()
                    .putString(MediaMetadata.METADATA_KEY_MEDIA_ID, "connection_test")
                    .putString(MediaMetadata.METADATA_KEY_TITLE, "AION V Hub")
                    .putString(MediaMetadata.METADATA_KEY_ARTIST, "Diagnóstico Android Auto")
                    .build());

            setPlaybackState(PlaybackState.STATE_PAUSED);
            mediaSession.setActive(true);
            setSessionToken(mediaSession.getSessionToken());
            HubDiagnostics.log(this, "MEDIA session token publicado");
        } catch (Throwable t) {
            HubDiagnostics.log(this, "MEDIA ERRO onCreate: " + t.getClass().getSimpleName() + " " + String.valueOf(t.getMessage()));
            throw t;
        }
    }

    private void setPlaybackState(int state) {
        if (mediaSession == null) return;
        long actions = PlaybackState.ACTION_PLAY |
                PlaybackState.ACTION_PLAY_FROM_MEDIA_ID |
                PlaybackState.ACTION_PAUSE |
                PlaybackState.ACTION_STOP;
        mediaSession.setPlaybackState(new PlaybackState.Builder()
                .setActions(actions)
                .setState(state, PlaybackState.PLAYBACK_POSITION_UNKNOWN,
                        state == PlaybackState.STATE_PLAYING ? 1f : 0f)
                .build());
    }

    @Override public BrowserRoot onGetRoot(String clientPackageName, int clientUid, Bundle rootHints) {
        HubDiagnostics.log(this, "MEDIA onGetRoot cliente=" + clientPackageName + " uid=" + clientUid);
        try {
            return new BrowserRoot("aion_root", null);
        } catch (Throwable t) {
            HubDiagnostics.log(this, "MEDIA ERRO onGetRoot: " + t.getClass().getSimpleName());
            throw t;
        }
    }

    @Override public void onLoadChildren(String parentId, Result<List<MediaBrowser.MediaItem>> result) {
        HubDiagnostics.log(this, "MEDIA onLoadChildren parent=" + parentId);
        try {
            List<MediaBrowser.MediaItem> items = new ArrayList<>();
            if ("aion_root".equals(parentId)) {
                MediaDescription description = new MediaDescription.Builder()
                        .setMediaId("connection_test")
                        .setTitle("AION V Hub")
                        .setSubtitle("Teste de integração Android Auto")
                        .build();
                items.add(new MediaBrowser.MediaItem(description, MediaBrowser.MediaItem.FLAG_PLAYABLE));
            }
            result.sendResult(items);
            HubDiagnostics.log(this, "MEDIA onLoadChildren resultado=" + items.size());
        } catch (Throwable t) {
            HubDiagnostics.log(this, "MEDIA ERRO onLoadChildren: " + t.getClass().getSimpleName() + " " + String.valueOf(t.getMessage()));
            throw t;
        }
    }

    @Override public int onStartCommand(Intent intent, int flags, int startId) {
        HubDiagnostics.log(this, "MEDIA onStartCommand");
        return super.onStartCommand(intent, flags, startId);
    }

    @Override public void onDestroy() {
        HubDiagnostics.log(this, "MEDIA onDestroy");
        if (mediaSession != null) mediaSession.release();
        super.onDestroy();
    }
}
