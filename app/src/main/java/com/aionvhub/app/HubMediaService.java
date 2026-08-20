package com.aionvhub.app;

import android.app.PendingIntent;
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

/**
 * Media Browser do AION V Hub.
 *
 * A v0.7 usa uma integração mais completa com o contrato clássico do Android
 * Auto: BrowserRoot com capabilities, árvore navegável, onLoadItem, onSearch,
 * MediaSession e PLAY_FROM_SEARCH. A implementação é própria e mantém o foco
 * em diagnóstico/compatibilidade do Hub.
 */
public class HubMediaService extends MediaBrowserServiceCompat {
    private static final String EXTRA_MEDIA_SEARCH_SUPPORTED = "android.media.browse.SEARCH_SUPPORTED";
    private static final String CONTENT_STYLE_SUPPORTED = "android.media.browse.CONTENT_STYLE_SUPPORTED";
    private static final String CONTENT_STYLE_PLAYABLE_HINT = "android.media.browse.CONTENT_STYLE_PLAYABLE_HINT";
    private static final String CONTENT_STYLE_BROWSABLE_HINT = "android.media.browse.CONTENT_STYLE_BROWSABLE_HINT";
    private static final int CONTENT_STYLE_LIST = 1;

    private MediaSessionCompat mediaSession;

    @Override
    public void onCreate() {
        super.onCreate();
        HubDiagnostics.event(this, "MEDIA-V2 onCreate");

        try {
            mediaSession = new MediaSessionCompat(this, "AionVHubMediaV2");
            mediaSession.setFlags(
                    MediaSessionCompat.FLAG_HANDLES_MEDIA_BUTTONS |
                    MediaSessionCompat.FLAG_HANDLES_TRANSPORT_CONTROLS
            );

            Intent openHub = new Intent(this, MainActivity.class)
                    .addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP | Intent.FLAG_ACTIVITY_CLEAR_TOP);
            PendingIntent sessionActivity = PendingIntent.getActivity(
                    this,
                    70,
                    openHub,
                    PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
            );
            mediaSession.setSessionActivity(sessionActivity);

            mediaSession.setCallback(new MediaSessionCompat.Callback() {
                @Override
                public void onPrepare() {
                    HubDiagnostics.event(HubMediaService.this, "MEDIA-V2 onPrepare");
                    setPlaybackState(PlaybackStateCompat.STATE_PAUSED);
                }

                @Override
                public void onPlay() {
                    HubDiagnostics.event(HubMediaService.this, "MEDIA-V2 onPlay");
                    setPlaybackState(PlaybackStateCompat.STATE_PLAYING);
                }

                @Override
                public void onPlayFromMediaId(String mediaId, Bundle extras) {
                    HubDiagnostics.event(HubMediaService.this, "MEDIA-V2 onPlayFromMediaId=" + mediaId);
                    HubMediaCatalog.Entry entry = HubMediaCatalog.find(mediaId);
                    if (entry == null) {
                        publishMetadata(mediaId, "AION V Hub", "Item solicitado pelo Android Auto");
                    } else if (entry.browsable) {
                        publishMetadata(entry.id, entry.title, entry.subtitle);
                        setPlaybackState(PlaybackStateCompat.STATE_PAUSED);
                        return;
                    } else {
                        publishMetadata(entry.id, entry.title, entry.subtitle);
                    }
                    setPlaybackState(PlaybackStateCompat.STATE_PLAYING);
                }

                @Override
                public void onPlayFromSearch(String query, Bundle extras) {
                    String safeQuery = query == null ? "" : query.trim();
                    HubDiagnostics.event(HubMediaService.this, "MEDIA-V2 onPlayFromSearch=" + safeQuery);

                    HubMediaCatalog.Entry entry = firstPlayable(HubMediaCatalog.search(safeQuery));
                    if (entry != null) {
                        publishMetadata(entry.id, entry.title, entry.subtitle);
                    } else {
                        String title = safeQuery.isEmpty() ? "AION V Hub" : "Busca: " + safeQuery;
                        publishMetadata(HubMediaCatalog.VOICE_SEARCH_ID, title, "Pesquisa do Android Auto");
                    }
                    setPlaybackState(PlaybackStateCompat.STATE_PLAYING);
                }

                @Override
                public void onPause() {
                    HubDiagnostics.event(HubMediaService.this, "MEDIA-V2 onPause");
                    setPlaybackState(PlaybackStateCompat.STATE_PAUSED);
                }

                @Override
                public void onStop() {
                    HubDiagnostics.event(HubMediaService.this, "MEDIA-V2 onStop");
                    setPlaybackState(PlaybackStateCompat.STATE_STOPPED);
                }
            });

            publishMetadata(
                    HubMediaCatalog.CONNECTION_ID,
                    "AION V Hub",
                    "Media Browser v2 • diagnóstico Android Auto"
            );
            setPlaybackState(PlaybackStateCompat.STATE_PAUSED);

            setSessionToken(mediaSession.getSessionToken());
            mediaSession.setActive(true);
            HubDiagnostics.event(this, "MEDIA-V2 session token publicado");
        } catch (Throwable t) {
            HubDiagnostics.event(
                    this,
                    "MEDIA-V2 ERRO onCreate: " + t.getClass().getSimpleName() + " " + String.valueOf(t.getMessage())
            );
            throw t;
        }
    }

    @Nullable
    @Override
    public BrowserRoot onGetRoot(
            @NonNull String clientPackageName,
            int clientUid,
            @Nullable Bundle rootHints
    ) {
        // Mantém a assinatura antiga do evento para o diagnóstico já existente
        // na MainActivity, acrescentando o marcador v2 para diferenciar a nova arquitetura.
        HubDiagnostics.event(
                this,
                "MEDIA-COMPAT onGetRoot cliente=" + clientPackageName + " uid=" + clientUid + " v2"
        );

        Bundle extras = new Bundle();
        extras.putBoolean(EXTRA_MEDIA_SEARCH_SUPPORTED, true);
        extras.putBoolean(CONTENT_STYLE_SUPPORTED, true);
        extras.putInt(CONTENT_STYLE_BROWSABLE_HINT, CONTENT_STYLE_LIST);
        extras.putInt(CONTENT_STYLE_PLAYABLE_HINT, CONTENT_STYLE_LIST);
        return new BrowserRoot(HubMediaCatalog.ROOT_ID, extras);
    }

    @Override
    public void onLoadChildren(
            @NonNull String parentId,
            @NonNull Result<List<MediaBrowserCompat.MediaItem>> result
    ) {
        HubDiagnostics.event(this, "MEDIA-V2 onLoadChildren parent=" + parentId);

        List<MediaBrowserCompat.MediaItem> items = new ArrayList<>();
        for (HubMediaCatalog.Entry entry : HubMediaCatalog.children(parentId)) {
            items.add(toMediaItem(entry));
        }

        result.sendResult(items);
        HubDiagnostics.event(this, "MEDIA-V2 onLoadChildren resultado=" + items.size());
    }

    @Override
    public void onLoadItem(
            @NonNull String itemId,
            @NonNull Result<MediaBrowserCompat.MediaItem> result
    ) {
        HubDiagnostics.event(this, "MEDIA-V2 onLoadItem id=" + itemId);
        HubMediaCatalog.Entry entry = HubMediaCatalog.find(itemId);
        result.sendResult(entry == null ? null : toMediaItem(entry));
    }

    @Override
    public void onSearch(
            @NonNull String query,
            Bundle extras,
            @NonNull Result<List<MediaBrowserCompat.MediaItem>> result
    ) {
        String safeQuery = query.trim();
        HubDiagnostics.event(this, "MEDIA-V2 onSearch query=" + safeQuery);

        List<MediaBrowserCompat.MediaItem> items = new ArrayList<>();
        for (HubMediaCatalog.Entry entry : HubMediaCatalog.search(safeQuery)) {
            items.add(toMediaItem(entry));
        }

        result.sendResult(items);
        HubDiagnostics.event(this, "MEDIA-V2 onSearch resultado=" + items.size());
    }

    private MediaBrowserCompat.MediaItem toMediaItem(HubMediaCatalog.Entry entry) {
        MediaDescriptionCompat description = new MediaDescriptionCompat.Builder()
                .setMediaId(entry.id)
                .setTitle(entry.title)
                .setSubtitle(entry.subtitle)
                .build();

        int flag = entry.browsable
                ? MediaBrowserCompat.MediaItem.FLAG_BROWSABLE
                : MediaBrowserCompat.MediaItem.FLAG_PLAYABLE;
        return new MediaBrowserCompat.MediaItem(description, flag);
    }

    private HubMediaCatalog.Entry firstPlayable(List<HubMediaCatalog.Entry> entries) {
        for (HubMediaCatalog.Entry entry : entries) {
            if (!entry.browsable) return entry;
        }
        return null;
    }

    private void publishMetadata(String mediaId, String title, String subtitle) {
        if (mediaSession == null) return;
        mediaSession.setMetadata(new MediaMetadataCompat.Builder()
                .putString(MediaMetadataCompat.METADATA_KEY_MEDIA_ID, mediaId)
                .putString(MediaMetadataCompat.METADATA_KEY_TITLE, title)
                .putString(MediaMetadataCompat.METADATA_KEY_ARTIST, subtitle)
                .putString(MediaMetadataCompat.METADATA_KEY_DISPLAY_TITLE, title)
                .putString(MediaMetadataCompat.METADATA_KEY_DISPLAY_SUBTITLE, subtitle)
                .build());
    }

    private void setPlaybackState(int state) {
        if (mediaSession == null) return;

        long actions = PlaybackStateCompat.ACTION_PREPARE |
                PlaybackStateCompat.ACTION_PLAY |
                PlaybackStateCompat.ACTION_PLAY_FROM_MEDIA_ID |
                PlaybackStateCompat.ACTION_PLAY_FROM_SEARCH |
                PlaybackStateCompat.ACTION_PAUSE |
                PlaybackStateCompat.ACTION_STOP;

        mediaSession.setPlaybackState(new PlaybackStateCompat.Builder()
                .setActions(actions)
                .setState(
                        state,
                        PlaybackStateCompat.PLAYBACK_POSITION_UNKNOWN,
                        state == PlaybackStateCompat.STATE_PLAYING ? 1f : 0f
                )
                .build());
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        String action = intent == null ? "null" : String.valueOf(intent.getAction());
        HubDiagnostics.event(this, "MEDIA-V2 onStartCommand action=" + action);
        return super.onStartCommand(intent, flags, startId);
    }

    @Override
    public void onDestroy() {
        HubDiagnostics.event(this, "MEDIA-V2 onDestroy");
        if (mediaSession != null) {
            mediaSession.setActive(false);
            mediaSession.release();
        }
        super.onDestroy();
    }
}
