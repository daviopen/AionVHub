package com.aionvhub.app;

import org.junit.Test;

import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class HubMediaCatalogTest {

    @Test
    public void rootHasBrowsableDiagnosticsAndPlayableItems() {
        List<HubMediaCatalog.Entry> root = HubMediaCatalog.children(HubMediaCatalog.ROOT_ID);

        assertEquals(3, root.size());
        assertEquals(HubMediaCatalog.DIAGNOSTICS_ID, root.get(0).id);
        assertTrue(root.get(0).browsable);
        assertFalse(root.get(1).browsable);
        assertFalse(root.get(2).browsable);
    }

    @Test
    public void diagnosticsSectionHasExpectedChildren() {
        List<HubMediaCatalog.Entry> diagnostics = HubMediaCatalog.children(HubMediaCatalog.DIAGNOSTICS_ID);

        assertEquals(3, diagnostics.size());
        assertEquals(HubMediaCatalog.HOST_HANDSHAKE_ID, diagnostics.get(0).id);
        assertEquals(HubMediaCatalog.BRIDGE_HEALTH_ID, diagnostics.get(1).id);
        assertEquals(HubMediaCatalog.SHIZUKU_STATUS_ID, diagnostics.get(2).id);
        for (HubMediaCatalog.Entry entry : diagnostics) {
            assertFalse(entry.browsable);
        }
    }

    @Test
    public void searchIsCaseInsensitiveAndFindsAndroidAutoItems() {
        List<HubMediaCatalog.Entry> results = HubMediaCatalog.search("ANDROID AUTO");

        assertFalse(results.isEmpty());
        assertTrue(contains(results, HubMediaCatalog.CONNECTION_ID));
        assertTrue(contains(results, HubMediaCatalog.HOST_HANDSHAKE_ID));
    }

    @Test
    public void searchFindsShizukuDiagnostic() {
        List<HubMediaCatalog.Entry> results = HubMediaCatalog.search("shizuku");

        assertEquals(1, results.size());
        assertEquals(HubMediaCatalog.SHIZUKU_STATUS_ID, results.get(0).id);
    }

    @Test
    public void unknownParentReturnsEmptyListAndKnownItemCanBeLoaded() {
        assertTrue(HubMediaCatalog.children("unknown").isEmpty());

        HubMediaCatalog.Entry item = HubMediaCatalog.find(HubMediaCatalog.CONNECTION_ID);
        assertNotNull(item);
        assertEquals("Teste de conexão", item.title);
    }

    private static boolean contains(List<HubMediaCatalog.Entry> entries, String id) {
        for (HubMediaCatalog.Entry entry : entries) {
            if (id.equals(entry.id)) return true;
        }
        return false;
    }
}
