package com.aionvhub.app;

import android.content.Intent;
import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.media.MediaBrowserServiceCompat;
import android.support.v4.media.MediaBrowserCompat;
import android.support.v4.media.MediaDescriptionCompat;
import android.support.v4.media.MediaMetadataCompat;
import android.support.v4.media.session.MediaSessionCompat;
import android.support.v4.media.session.PlaybackStateCompat;
import java.util.ArrayList;
import java.util.List;

public class HubMediaService extends MediaBrowserServiceCompat {
    private MediaSessionCompat mediaSession;

    @Override public void onCreate() {
        super.onCreate();
        HubDiagnostics.event(this, "MEDIA-COMPAT onCreate");
        try {
            mediaSession = new MediaSessionCompat(this, "AionVHubMedia");
            HubDiagnostics.event(this, "MEDIA-COMPAT MediaSession criada");

            mediaSession.setCallback(new MediaSessionCompat.Callback() {
                @Override public void onPlay() {
                    HubDiagnostics.event(HubMediaService.this, "MEDIA-COMPAT onPlay");
                    setPlaybackState(PlaybackStateCompat.STATE_PLAYING);
                }

                @Override public void onPlayFromMediaId(String mediaId, Bundle extras) {
                    HubDiagnostics.event(HubMediaService.this, "MEDIA-COMPAT onPlayFromMediaId: " + mediaId);
                    publishTestMetadata(mediaId, "AION V Hub", "Teste Android Auto");
                    setPlaybackState(PlaybackStateCompat.STATE_PLAYING);
                }

                @Override public void onPlayFromSearch(String query, Bundle extras) {
                    String safeQuery = query == null ? "" : query.trim();
                    HubDiagnostics.event(HubMediaService.this, "MEDIA-COMPAT onPlayFromSearch: " + safeQuery);
                    String title = safeQuery.isEmpty() ? "AION V Hub" : "Busca: " + safeQuery;
                    publishTestMetadata("search_test", title, "Pesquisa por voz Android Auto");
                    setPlaybackState(PlaybackStateCompat.STATE_PLAYING);
                }

                @Override public void onPause() {
                    HubDiagnostics.event(HubMediaService.this, "MEDIA-COMPAT onPause");
                    setPlaybackState(PlaybackStateCompat.STATE_PAUSED);
                }

                @Override public void onStop() {
                    HubDiagnostics.event(HubMediaService.this, "MEDIA-COMPAT onStop");
                    setPlaybackState(PlaybackStateCompat.STATE_STOPPED);
                }
            });

            publishTestMetadata("connection_test", "AION V Hub", "Diagnóstico Android Auto");

            setPlaybackState(PlaybackStateCompat.STATE_PAUSED);
            mediaSession.setActive(true);
            setSessionToken(mediaSession.getSessionToken());
            HubDiagnostics.event(this, "MEDIA-COMPAT session token publicado");
        } catch (Throwable t) {
            HubDiagnostics.event(this, "MEDIA-COMPAT ERRO onCreate: " + t.getClass().getSimpleName() + " " + String.valueOf(t.getMessage()));
            throw t;
        }
    }

    private void publishTestMetadata(String mediaId, String title, String artist) {
        if (mediaSession == null) return;
        mediaSession.setMetadata(new MediaMetadataCompat.Builder()
                .putString(MediaMetadataCompat.METADATA_KEY_MEDIA_ID, mediaId)
                .putString(MediaMetadataCompat.METADATA_KEY_TITLE, title)
                .putString(MediaMetadataCompat.METADATA_KEY_ARTIST, artist)
                .build());
    }

    private void setPlaybackState(int state) {
        if (mediaSession == null) return;
        long actions = PlaybackStateCompat.ACTION_PLAY |
                PlaybackStateCompat.ACTION_PLAY_FROM_MEDIA_ID |
                PlaybackStateCompat.ACTION_PLAY_FROM_SEARCH |
                PlaybackStateCompat.ACTION_PAUSE |
                PlaybackStateCompat.ACTION_STOP;
        mediaSession.setPlaybackState(new PlaybackStateCompat.Builder()
                .setActions(actions)
                .setState(state, PlaybackStateCompat.PLAYBACK_POSITION_UNKNOWN,
                        state == PlaybackStateCompat.STATE_PLAYING ? 1f : 0f)
                .build());
    }

    @Nullable
    @Override
    public BrowserRoot onGetRoot(@NonNull String clientPackageName, int clientUid, @Nullable Bundle rootHints) {
        HubDiagnostics.event(this, "MEDIA-COMPAT onGetRoot cliente=" + clientPackageName + " uid=" + clientUid);
        return new BrowserRoot("aion_root", null);
    }

    @Override
    public void onLoadChildren(@NonNull String parentId, @NonNull Result<List<MediaBrowserCompat.MediaItem>> result) {
        HubDiagnostics.event(this, "MEDIA-COMPAT onLoadChildren parent=" + parentId);
        List<MediaBrowserCompat.MediaItem> items = new ArrayList<>();
        if ("aion_root".equals(parentId)) {
            MediaDescriptionCompat description = new MediaDescriptionCompat.Builder()
                    .setMediaId("connection_test")
                    .setTitle("AION V Hub")
                    .setSubtitle("Teste de integração Android Auto")
                    .build();
            items.add(new MediaBrowserCompat.MediaItem(description, MediaBrowserCompat.MediaItem.FLAG_PLAYABLE));
        }
        result.sendResult(items);
        HubDiagnostics.event(this, "MEDIA-COMPAT onLoadChildren resultado=" + items.size());
    }

    @Override public int onStartCommand(Intent intent, int flags, int startId) {
        HubDiagnostics.event(this, "MEDIA-COMPAT onStartCommand");
        return super.onStartCommand(intent, flags, startId);
    }

    @Override public void onDestroy() {
        HubDiagnostics.event(this, "MEDIA-COMPAT onDestroy");
        if (mediaSession != null) mediaSession.release();
        super.onDestroy();
    }
}
