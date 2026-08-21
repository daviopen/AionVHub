package com.aionvhub.app;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class HubAppCatalogTest {

    @Test
    public void mediaIdRoundTripPreservesPackageName() {
        String id = HubAppCatalog.mediaId("com.example.player");
        assertEquals("app:com.example.player", id);
        assertTrue(HubAppCatalog.isMediaId(id));
        assertEquals("com.example.player", HubAppCatalog.packageFromMediaId(id));
    }

    @Test
    public void invalidMediaIdsAreRejected() {
        assertFalse(HubAppCatalog.isMediaId(null));
        assertFalse(HubAppCatalog.isMediaId(""));
        assertFalse(HubAppCatalog.isMediaId("app:"));
        assertFalse(HubAppCatalog.isMediaId("com.example.player"));
        assertNull(HubAppCatalog.packageFromMediaId("invalid"));
    }

    @Test
    public void launchableAppKeepsPackageAndLabel() {
        HubAppCatalog.LaunchableApp app = new HubAppCatalog.LaunchableApp("com.example.tv", "Minha TV");
        assertEquals("com.example.tv", app.packageName);
        assertEquals("Minha TV", app.label);
    }
}
