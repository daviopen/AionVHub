package com.aionvhub.app;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Set;

/** Regras puras de ordenação dos menus exibidos na central. */
public final class HubCarMenuPolicy {
    private static final List<String> ORDER = Collections.unmodifiableList(Arrays.asList(
            HubMediaCatalog.APPS_ID,
            HubMediaCatalog.FAVORITES_ID,
            HubMediaCatalog.IPTV_ID,
            HubMediaCatalog.RADIOS_ID,
            HubMediaCatalog.DIAGNOSTICS_ID
    ));

    private HubCarMenuPolicy() {}

    public static List<String> all() {
        return ORDER;
    }

    public static List<String> filter(Set<String> enabled) {
        if (enabled == null) return ORDER;
        List<String> out = new ArrayList<>();
        for (String id : ORDER) if (enabled.contains(id)) out.add(id);
        return Collections.unmodifiableList(out);
    }
}
