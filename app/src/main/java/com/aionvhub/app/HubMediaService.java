package com.aionvhub.app;

import android.media.MediaDescription;
import android.media.MediaMetadata;
import android.media.browse.MediaBrowser;
import android.media.browse.MediaBrowserService;
import android.media.session.MediaSession;
import android.media.session.PlaybackState;
import android.os.Bundle;
import java.util.ArrayList;
import java.util.List;

public class HubMediaService extends MediaBrowserService {
    private MediaSession mediaSession;

    @Override public void onCreate() {
        super.onCreate();
        mediaSession = new MediaSession(this, "AionVHubMedia");
        mediaSession.setCallback(new MediaSession.Callback() {
            @Override public void onPlay() {
                setPlaybackState(PlaybackState.STATE_PLAYING);
            }
            @Override public void onPause() {
                setPlaybackState(PlaybackState.STATE_PAUSED);
            }
            @Override public void onStop() {
                setPlaybackState(PlaybackState.STATE_STOPPED);
            }
        });
        setPlaybackState(PlaybackState.STATE_PAUSED);
        mediaSession.setActive(true);
        setSessionToken(mediaSession.getSessionToken());
    }

    private void setPlaybackState(int state) {
        long actions = PlaybackState.ACTION_PLAY | PlaybackState.ACTION_PAUSE | PlaybackState.ACTION_STOP;
        mediaSession.setPlaybackState(new PlaybackState.Builder()
                .setActions(actions)
                .setState(state, PlaybackState.PLAYBACK_POSITION_UNKNOWN, state == PlaybackState.STATE_PLAYING ? 1f : 0f)
                .build());
    }

    @Override public BrowserRoot onGetRoot(String clientPackageName, int clientUid, Bundle rootHints) {
        return new BrowserRoot("aion_root", null);
    }

    @Override public void onLoadChildren(String parentId, Result<List<MediaBrowser.MediaItem>> result) {
        List<MediaBrowser.MediaItem> items = new ArrayList<>();
        if ("aion_root".equals(parentId)) {
            MediaDescription description = new MediaDescription.Builder()
                    .setMediaId("connection_test")
                    .setTitle("AION V Hub")
                    .setSubtitle("Serviço de mídia / teste Android Auto")
                    .build();
            items.add(new MediaBrowser.MediaItem(description, MediaBrowser.MediaItem.FLAG_PLAYABLE));
        }
        result.sendResult(items);
    }

    @Override public void onDestroy() {
        if (mediaSession != null) {
            mediaSession.release();
        }
        super.onDestroy();
    }
}
