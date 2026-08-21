package com.aionvhub.app;

import android.app.PendingIntent;
import android.content.Intent;
import android.graphics.Bitmap;
import android.media.AudioAttributes;
import android.media.MediaPlayer;
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
import java.util.Locale;

/**
 * Media Browser do AION V Hub.
 *
 * Mantém o caminho validado no Android Auto 17.3 e oferece catálogo funcional,
 * apps detectados dinamicamente, artwork, fonte configurável e reprodução via MediaSession.
 */
public class HubMediaService extends MediaBrowserServiceCompat {
    private static final String EXTRA_MEDIA_SEARCH_SUPPORTED = "android.media.browse.SEARCH_SUPPORTED";
    private static final String CONTENT_STYLE_SUPPORTED = "android.media.browse.CONTENT_STYLE_SUPPORTED";
    private static final String CONTENT_STYLE_PLAYABLE_HINT = "android.media.browse.CONTENT_STYLE_PLAYABLE_HINT";
    private static final String CONTENT_STYLE_BROWSABLE_HINT = "android.media.browse.CONTENT_STYLE_BROWSABLE_HINT";
    private static final int CONTENT_STYLE_LIST = 1;

    private MediaSessionCompat mediaSession;
    private MediaPlayer player;
    private boolean playerPrepared;
    private String currentMediaId;

    @Override public void onCreate() {
        super.onCreate();
        HubDiagnostics.event(this, "MEDIA-V2 onCreate");

        try {
            mediaSession = new MediaSessionCompat(this, "AionVHubMediaV2");
            mediaSession.setFlags(
                    MediaSessionCompat.FLAG_HANDLES_MEDIA_BUTTONS |
                    MediaSessionCompat.FLAG_HANDLES_TRANSPORT_CONTROLS
            );

            Intent openHub = new Intent(this, HomeActivity.class)
                    .addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP | Intent.FLAG_ACTIVITY_CLEAR_TOP);
            PendingIntent sessionActivity = PendingIntent.getActivity(
                    this,
                    80,
                    openHub,
                    PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
            );
            mediaSession.setSessionActivity(sessionActivity);

            mediaSession.setCallback(new MediaSessionCompat.Callback() {
                @Override public void onPrepare() {
                    HubDiagnostics.event(HubMediaService.this, "MEDIA-V2 onPrepare");
                    setPlaybackState(PlaybackStateCompat.STATE_PAUSED);
                }

                @Override public void onPlay() {
                    HubDiagnostics.event(HubMediaService.this, "MEDIA-V2 onPlay");
                    if (player != null && playerPrepared) {
                        player.start();
                        setPlaybackState(PlaybackStateCompat.STATE_PLAYING);
                    } else if (HubMediaCatalog.CUSTOM_STREAM_ID.equals(currentMediaId)) {
                        playConfiguredStream();
                    } else {
                        setPlaybackState(PlaybackStateCompat.STATE_PLAYING);
                    }
                }

                @Override public void onPlayFromMediaId(String mediaId, Bundle extras) {
                    HubDiagnostics.event(HubMediaService.this, "MEDIA-V2 onPlayFromMediaId=" + mediaId);

                    if (HubAppCatalog.isMediaId(mediaId)) {
                        String packageName = HubAppCatalog.packageFromMediaId(mediaId);
                        HubAppCatalog.LaunchableApp app = HubAppCatalog.find(HubMediaService.this, packageName);
                        String title = app == null ? packageName : app.label;

                        boolean launched = false;
                        try {
                            launched = app != null && HubAppCatalog.launch(HubMediaService.this, packageName);
                        } catch (Throwable t) {
                            HubDiagnostics.event(HubMediaService.this,
                                    "APP handoff ERRO package=" + packageName + " " + t.getClass().getSimpleName());
                        }

                        if (launched) {
                            publishMetadata(mediaId, title, "Aberto no tablet pelo AION V Hub");
                            HubDiagnostics.event(HubMediaService.this,
                                    "APP handoff OK package=" + packageName);
                        } else {
                            publishMetadata(mediaId, title, "Android bloqueou a abertura no tablet");
                            HubDiagnostics.event(HubMediaService.this,
                                    "APP handoff BLOQUEADO package=" + packageName);
                        }
                        setPlaybackState(PlaybackStateCompat.STATE_PAUSED);
                        return;
                    }

                    if (HubMediaCatalog.CUSTOM_STREAM_ID.equals(mediaId)) {
                        currentMediaId = mediaId;
                        playConfiguredStream();
                        return;
                    }

                    HubMediaCatalog.Entry entry = findEntry(mediaId);
                    if (entry == null) {
                        publishMetadata(mediaId, "AION V Hub", "Item solicitado pelo Android Auto");
                    } else {
                        publishMetadata(entry.id, entry.title, entry.subtitle);
                    }
                    setPlaybackState(PlaybackStateCompat.STATE_PAUSED);
                }

                @Override public void onPlayFromSearch(String query, Bundle extras) {
                    String safeQuery = query == null ? "" : query.trim();
                    HubDiagnostics.event(HubMediaService.this, "MEDIA-V2 onPlayFromSearch=" + safeQuery);

                    HubStreamStore.Config config = HubStreamStore.get(HubMediaService.this);
                    String normalized = safeQuery.toLowerCase(Locale.ROOT);
                    if (config.configured() && (
                            normalized.isEmpty() ||
                            config.title.toLowerCase(Locale.ROOT).contains(normalized) ||
                            normalized.contains("iptv") || normalized.contains("stream") || normalized.contains("transmissao")
                    )) {
                        currentMediaId = HubMediaCatalog.CUSTOM_STREAM_ID;
                        playConfiguredStream();
                        return;
                    }

                    HubMediaCatalog.Entry entry = firstPlayable(HubMediaCatalog.search(safeQuery));
                    if (entry != null) {
                        publishMetadata(entry.id, entry.title, entry.subtitle);
                    } else {
                        String title = safeQuery.isEmpty() ? "AION V Hub" : "Busca: " + safeQuery;
                        publishMetadata(HubMediaCatalog.VOICE_SEARCH_ID, title, "Pesquisa do Android Auto");
                    }
                    setPlaybackState(PlaybackStateCompat.STATE_PAUSED);
                }

                @Override public void onPause() {
                    HubDiagnostics.event(HubMediaService.this, "MEDIA-V2 onPause");
                    if (player != null && playerPrepared && player.isPlaying()) player.pause();
                    setPlaybackState(PlaybackStateCompat.STATE_PAUSED);
                }

                @Override public void onStop() {
                    HubDiagnostics.event(HubMediaService.this, "MEDIA-V2 onStop");
                    releasePlayer();
                    setPlaybackState(PlaybackStateCompat.STATE_STOPPED);
                }
            });

            publishMetadata(HubMediaCatalog.ROOT_ID, "AION V Hub", "Apps • Favoritos • IPTV • Rádios");
            setPlaybackState(PlaybackStateCompat.STATE_PAUSED);

            setSessionToken(mediaSession.getSessionToken());
            mediaSession.setActive(true);
            HubDiagnostics.event(this, "MEDIA-V2 session token publicado");
        } catch (Throwable t) {
            HubDiagnostics.event(this, "MEDIA-V2 ERRO onCreate: " + t.getClass().getSimpleName() + " " + String.valueOf(t.getMessage()));
            throw t;
        }
    }

    @Nullable
    @Override public BrowserRoot onGetRoot(@NonNull String clientPackageName, int clientUid, @Nullable Bundle rootHints) {
        HubDiagnostics.event(this, "MEDIA-COMPAT onGetRoot cliente=" + clientPackageName + " uid=" + clientUid + " v2");
        Bundle extras = new Bundle();
        extras.putBoolean(EXTRA_MEDIA_SEARCH_SUPPORTED, true);
        extras.putBoolean(CONTENT_STYLE_SUPPORTED, true);
        extras.putInt(CONTENT_STYLE_BROWSABLE_HINT, CONTENT_STYLE_LIST);
        extras.putInt(CONTENT_STYLE_PLAYABLE_HINT, CONTENT_STYLE_LIST);
        return new BrowserRoot(HubMediaCatalog.ROOT_ID, extras);
    }

    @Override public void onLoadChildren(@NonNull String parentId, @NonNull Result<List<MediaBrowserCompat.MediaItem>> result) {
        HubDiagnostics.event(this, "MEDIA-V2 onLoadChildren parent=" + parentId);
        List<HubMediaCatalog.Entry> entries = childrenFor(parentId);
        List<MediaBrowserCompat.MediaItem> items = new ArrayList<>();
        for (HubMediaCatalog.Entry entry : entries) items.add(toMediaItem(entry));
        result.sendResult(items);
        HubDiagnostics.event(this, "MEDIA-V2 onLoadChildren resultado=" + items.size());
    }

    @Override public void onLoadItem(@NonNull String itemId, @NonNull Result<MediaBrowserCompat.MediaItem> result) {
        HubDiagnostics.event(this, "MEDIA-V2 onLoadItem id=" + itemId);
        HubMediaCatalog.Entry entry = findEntry(itemId);
        result.sendResult(entry == null ? null : toMediaItem(entry));
    }

    @Override public void onSearch(@NonNull String query, Bundle extras, @NonNull Result<List<MediaBrowserCompat.MediaItem>> result) {
        String safeQuery = query.trim();
        HubDiagnostics.event(this, "MEDIA-V2 onSearch query=" + safeQuery);

        List<MediaBrowserCompat.MediaItem> items = new ArrayList<>();
        HubStreamStore.Config config = HubStreamStore.get(this);
        String normalized = safeQuery.toLowerCase(Locale.ROOT);

        if (config.configured() && (
                normalized.isEmpty() ||
                config.title.toLowerCase(Locale.ROOT).contains(normalized) ||
                normalized.contains("iptv") || normalized.contains("stream")
        )) {
            items.add(toMediaItem(HubMediaCatalog.customStream(config.title)));
        }

        if (!normalized.isEmpty()) {
            for (HubAppCatalog.LaunchableApp app : HubAppCatalog.listLaunchable(this)) {
                String haystack = (app.label + " " + app.packageName).toLowerCase(Locale.ROOT);
                if (haystack.contains(normalized)) {
                    items.add(toMediaItem(HubMediaCatalog.externalApp(app.packageName, app.label)));
                }
            }
        }

        for (HubMediaCatalog.Entry entry : HubMediaCatalog.search(safeQuery)) items.add(toMediaItem(entry));
        result.sendResult(items);
        HubDiagnostics.event(this, "MEDIA-V2 onSearch resultado=" + items.size());
    }

    private List<HubMediaCatalog.Entry> childrenFor(String parentId) {
        HubStreamStore.Config config = HubStreamStore.get(this);
        List<HubMediaCatalog.Entry> out = new ArrayList<>();

        if (HubMediaCatalog.APPS_ID.equals(parentId)) {
            List<HubAppCatalog.LaunchableApp> apps = HubAppCatalog.listLaunchable(this);
            if (apps.isEmpty()) {
                out.addAll(HubMediaCatalog.children(parentId));
            } else {
                for (HubAppCatalog.LaunchableApp app : apps) {
                    out.add(HubMediaCatalog.externalApp(app.packageName, app.label));
                }
            }
            return out;
        }

        if (HubMediaCatalog.IPTV_ID.equals(parentId)) {
            if (config.configured()) out.add(HubMediaCatalog.customStream(config.title));
            else out.addAll(HubMediaCatalog.children(parentId));
            return out;
        }

        if (HubMediaCatalog.FAVORITES_ID.equals(parentId)) {
            if (config.configured() && config.favorite) out.add(HubMediaCatalog.customStream(config.title));
            else out.addAll(HubMediaCatalog.children(parentId));
            return out;
        }

        out.addAll(HubMediaCatalog.children(parentId));
        return out;
    }

    private HubMediaCatalog.Entry findEntry(String id) {
        if (HubAppCatalog.isMediaId(id)) {
            String packageName = HubAppCatalog.packageFromMediaId(id);
            HubAppCatalog.LaunchableApp app = HubAppCatalog.find(this, packageName);
            return app == null ? null : HubMediaCatalog.externalApp(app.packageName, app.label);
        }
        if (HubMediaCatalog.CUSTOM_STREAM_ID.equals(id)) {
            HubStreamStore.Config config = HubStreamStore.get(this);
            return config.configured() ? HubMediaCatalog.customStream(config.title) : null;
        }
        return HubMediaCatalog.find(id);
    }

    private MediaBrowserCompat.MediaItem toMediaItem(HubMediaCatalog.Entry entry) {
        Bitmap icon = HubArtwork.forEntry(this, entry.id, 56);
        MediaDescriptionCompat description = new MediaDescriptionCompat.Builder()
                .setMediaId(entry.id)
                .setTitle(entry.title)
                .setSubtitle(entry.subtitle)
                .setIconBitmap(icon)
                .build();
        int flag = entry.browsable ? MediaBrowserCompat.MediaItem.FLAG_BROWSABLE : MediaBrowserCompat.MediaItem.FLAG_PLAYABLE;
        return new MediaBrowserCompat.MediaItem(description, flag);
    }

    private HubMediaCatalog.Entry firstPlayable(List<HubMediaCatalog.Entry> entries) {
        for (HubMediaCatalog.Entry entry : entries) if (!entry.browsable) return entry;
        return null;
    }

    private void playConfiguredStream() {
        HubStreamStore.Config config = HubStreamStore.get(this);
        if (!config.configured()) {
            publishMetadata(HubMediaCatalog.IPTV_SETUP_ID, "Configure uma fonte no tablet", "AION V Hub");
            setPlaybackError("Nenhuma URL configurada");
            return;
        }

        releasePlayer();
        currentMediaId = HubMediaCatalog.CUSTOM_STREAM_ID;
        publishMetadata(currentMediaId, config.title, "AION V Hub • stream configurado");
        setPlaybackState(PlaybackStateCompat.STATE_BUFFERING);
        HubDiagnostics.event(this, "MEDIA-V2 stream preparando title=" + config.title);

        try {
            player = new MediaPlayer();
            player.setAudioAttributes(new AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                    .build());
            player.setDataSource(config.url);
            player.setOnPreparedListener(mp -> {
                playerPrepared = true;
                mp.start();
                setPlaybackState(PlaybackStateCompat.STATE_PLAYING);
                HubDiagnostics.event(HubMediaService.this, "MEDIA-V2 stream PLAYING");
            });
            player.setOnCompletionListener(mp -> {
                setPlaybackState(PlaybackStateCompat.STATE_STOPPED);
                HubDiagnostics.event(HubMediaService.this, "MEDIA-V2 stream concluído");
            });
            player.setOnErrorListener((mp, what, extra) -> {
                HubDiagnostics.event(HubMediaService.this, "MEDIA-V2 stream ERRO what=" + what + " extra=" + extra);
                setPlaybackError("Falha ao reproduzir a fonte configurada");
                releasePlayer();
                return true;
            });
            player.prepareAsync();
        } catch (Exception e) {
            HubDiagnostics.event(this, "MEDIA-V2 stream EXCEPTION " + e.getClass().getSimpleName());
            setPlaybackError("Não foi possível abrir a fonte");
            releasePlayer();
        }
    }

    private void publishMetadata(String mediaId, String title, String subtitle) {
        if (mediaSession == null) return;
        Bitmap artwork = HubArtwork.forEntry(this, mediaId, 256);
        mediaSession.setMetadata(new MediaMetadataCompat.Builder()
                .putString(MediaMetadataCompat.METADATA_KEY_MEDIA_ID, mediaId)
                .putString(MediaMetadataCompat.METADATA_KEY_TITLE, title)
                .putString(MediaMetadataCompat.METADATA_KEY_ARTIST, subtitle)
                .putString(MediaMetadataCompat.METADATA_KEY_DISPLAY_TITLE, title)
                .putString(MediaMetadataCompat.METADATA_KEY_DISPLAY_SUBTITLE, subtitle)
                .putBitmap(MediaMetadataCompat.METADATA_KEY_ALBUM_ART, artwork)
                .putBitmap(MediaMetadataCompat.METADATA_KEY_ART, artwork)
                .putBitmap(MediaMetadataCompat.METADATA_KEY_DISPLAY_ICON, artwork)
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
                .setState(state, PlaybackStateCompat.PLAYBACK_POSITION_UNKNOWN,
                        state == PlaybackStateCompat.STATE_PLAYING ? 1f : 0f)
                .build());
    }

    private void setPlaybackError(String message) {
        if (mediaSession == null) return;
        mediaSession.setPlaybackState(new PlaybackStateCompat.Builder()
                .setActions(PlaybackStateCompat.ACTION_PLAY | PlaybackStateCompat.ACTION_STOP)
                .setErrorMessage(message)
                .setState(PlaybackStateCompat.STATE_ERROR, PlaybackStateCompat.PLAYBACK_POSITION_UNKNOWN, 0f)
                .build());
    }

    private void releasePlayer() {
        playerPrepared = false;
        if (player != null) {
            try { player.reset(); } catch (Exception ignored) {}
            try { player.release(); } catch (Exception ignored) {}
            player = null;
        }
    }

    @Override public int onStartCommand(Intent intent, int flags, int startId) {
        String action = intent == null ? "null" : String.valueOf(intent.getAction());
        HubDiagnostics.event(this, "MEDIA-V2 onStartCommand action=" + action);
        return super.onStartCommand(intent, flags, startId);
    }

    @Override public void onDestroy() {
        HubDiagnostics.event(this, "MEDIA-V2 onDestroy");
        releasePlayer();
        if (mediaSession != null) {
            mediaSession.setActive(false);
            mediaSession.release();
        }
        super.onDestroy();
    }
}
