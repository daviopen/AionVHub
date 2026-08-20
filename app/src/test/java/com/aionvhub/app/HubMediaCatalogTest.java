package com.aionvhub.app;

import org.junit.Test;

import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class HubMediaCatalogTest {

    @Test
    public void rootHasFunctionalHubSections() {
        List<HubMediaCatalog.Entry> root = HubMediaCatalog.children(HubMediaCatalog.ROOT_ID);

        assertEquals(4, root.size());
        assertEquals(HubMediaCatalog.FAVORITES_ID, root.get(0).id);
        assertEquals(HubMediaCatalog.IPTV_ID, root.get(1).id);
        assertEquals(HubMediaCatalog.RADIOS_ID, root.get(2).id);
        assertEquals(HubMediaCatalog.DIAGNOSTICS_ID, root.get(3).id);
        for (HubMediaCatalog.Entry entry : root) assertTrue(entry.browsable);
    }

    @Test
    public void diagnosticsSectionPreservesCompleteBridgeChecks() {
        List<HubMediaCatalog.Entry> diagnostics = HubMediaCatalog.children(HubMediaCatalog.DIAGNOSTICS_ID);

        assertEquals(5, diagnostics.size());
        assertEquals(HubMediaCatalog.HOST_HANDSHAKE_ID, diagnostics.get(0).id);
        assertEquals(HubMediaCatalog.BRIDGE_HEALTH_ID, diagnostics.get(1).id);
        assertEquals(HubMediaCatalog.SHIZUKU_STATUS_ID, diagnostics.get(2).id);
        for (HubMediaCatalog.Entry entry : diagnostics) assertFalse(entry.browsable);
    }

    @Test
    public void iptvAndFavoritesHaveSafeFallbackItems() {
        List<HubMediaCatalog.Entry> iptv = HubMediaCatalog.children(HubMediaCatalog.IPTV_ID);
        List<HubMediaCatalog.Entry> favorites = HubMediaCatalog.children(HubMediaCatalog.FAVORITES_ID);

        assertEquals(1, iptv.size());
        assertEquals(HubMediaCatalog.IPTV_SETUP_ID, iptv.get(0).id);
        assertEquals(1, favorites.size());
        assertEquals(HubMediaCatalog.FAVORITE_EMPTY_ID, favorites.get(0).id);
    }

    @Test
    public void searchIsCaseInsensitiveAndFindsAndroidAutoDiagnostics() {
        List<HubMediaCatalog.Entry> results = HubMediaCatalog.search("ANDROID AUTO");

        assertFalse(results.isEmpty());
        assertTrue(contains(results, HubMediaCatalog.CONNECTION_ID));
        assertTrue(contains(results, HubMediaCatalog.HOST_HANDSHAKE_ID));
    }

    @Test
    public void searchFindsShizukuDiagnostic() {
        List<HubMediaCatalog.Entry> results = HubMediaCatalog.search("shizuku");

        assertFalse(results.isEmpty());
        assertTrue(contains(results, HubMediaCatalog.SHIZUKU_STATUS_ID));
    }

    @Test
    public void customStreamEntryIsPlayable() {
        HubMediaCatalog.Entry item = HubMediaCatalog.customStream("Minha TV");
        assertNotNull(item);
        assertEquals(HubMediaCatalog.CUSTOM_STREAM_ID, item.id);
        assertEquals("Minha TV", item.title);
        assertFalse(item.browsable);
    }

    private static boolean contains(List<HubMediaCatalog.Entry> entries, String id) {
        for (HubMediaCatalog.Entry entry : entries) {
            if (id.equals(entry.id)) return true;
        }
        return false;
    }
}
