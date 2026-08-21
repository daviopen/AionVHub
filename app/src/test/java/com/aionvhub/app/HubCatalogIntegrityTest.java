package com.aionvhub.app;

import org.junit.Test;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class HubCatalogIntegrityTest {

    @Test
    public void rootIdsAreUniqueAndBrowsable() {
        List<HubMediaCatalog.Entry> root = HubMediaCatalog.children(HubMediaCatalog.ROOT_ID);
        Set<String> ids = new HashSet<>();
        for (HubMediaCatalog.Entry entry : root) {
            assertTrue(entry.browsable);
            assertTrue("duplicate id: " + entry.id, ids.add(entry.id));
        }
        assertEquals(5, ids.size());
    }

    @Test
    public void emptySearchReturnsRootNavigation() {
        List<HubMediaCatalog.Entry> result = HubMediaCatalog.search("   ");
        assertEquals(HubMediaCatalog.children(HubMediaCatalog.ROOT_ID).size(), result.size());
        assertEquals(HubMediaCatalog.APPS_ID, result.get(0).id);
    }

    @Test
    public void customStreamFallsBackToFriendlyTitle() {
        HubMediaCatalog.Entry entry = HubMediaCatalog.customStream("   ");
        assertEquals("Minha transmissão", entry.title);
        assertFalse(entry.browsable);
    }

    @Test
    public void externalAppsRemainPlayableHandoffEntries() {
        HubMediaCatalog.Entry entry = HubMediaCatalog.externalApp("com.example.video", "Player Externo");
        assertEquals(HubAppCatalog.mediaId("com.example.video"), entry.id);
        assertFalse(entry.browsable);
        assertTrue(entry.keywords.contains("app"));
    }
}
