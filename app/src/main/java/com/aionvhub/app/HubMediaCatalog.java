package com.aionvhub.app;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Catálogo pequeno e determinístico usado pelo MediaBrowserService.
 *
 * Mantemos a estrutura independente do Android para conseguir testar a árvore e
 * a busca no CI. A camada Android apenas transforma Entry em MediaItem.
 */
public final class HubMediaCatalog {
    public static final String ROOT_ID = "aion_root";
    public static final String DIAGNOSTICS_ID = "aion_diagnostics";

    public static final String CONNECTION_ID = "connection_test";
    public static final String VOICE_SEARCH_ID = "voice_search_test";
    public static final String HOST_HANDSHAKE_ID = "host_handshake";
    public static final String BRIDGE_HEALTH_ID = "bridge_health";
    public static final String SHIZUKU_STATUS_ID = "shizuku_status";

    private static final Map<String, Entry> ENTRIES = new LinkedHashMap<>();

    static {
        register(new Entry(
                DIAGNOSTICS_ID,
                "Diagnóstico do Bridge",
                "Estado da integração com Android Auto",
                true,
                "diagnostico bridge android auto host integracao"
        ));
        register(new Entry(
                CONNECTION_ID,
                "Teste de conexão",
                "Valida MediaSession e descoberta pelo host",
                false,
                "conexao android auto media session descoberta host"
        ));
        register(new Entry(
                VOICE_SEARCH_ID,
                "Pesquisa por voz",
                "Valida PLAY_FROM_SEARCH e pesquisa do catálogo",
                false,
                "voz voice pesquisa search android auto"
        ));
        register(new Entry(
                HOST_HANDSHAKE_ID,
                "Handshake do Android Auto",
                "Confirma se o host abriu o MediaBrowserService",
                false,
                "host handshake gearhead android auto media browser"
        ));
        register(new Entry(
                BRIDGE_HEALTH_ID,
                "Saúde do Media Bridge",
                "Valida catálogo, sessão e controles de reprodução",
                false,
                "bridge media catalogo sessao controles reproducao"
        ));
        register(new Entry(
                SHIZUKU_STATUS_ID,
                "Diagnóstico Shizuku",
                "Complementa o diagnóstico local privilegiado",
                false,
                "shizuku diagnostico adb shell privilegiado"
        ));
    }

    private HubMediaCatalog() {}

    private static void register(Entry entry) {
        ENTRIES.put(entry.id, entry);
    }

    public static Entry find(String id) {
        return ENTRIES.get(id);
    }

    public static List<Entry> children(String parentId) {
        List<Entry> out = new ArrayList<>();
        if (ROOT_ID.equals(parentId)) {
            add(out, DIAGNOSTICS_ID);
            add(out, CONNECTION_ID);
            add(out, VOICE_SEARCH_ID);
        } else if (DIAGNOSTICS_ID.equals(parentId)) {
            add(out, HOST_HANDSHAKE_ID);
            add(out, BRIDGE_HEALTH_ID);
            add(out, SHIZUKU_STATUS_ID);
        }
        return Collections.unmodifiableList(out);
    }

    public static List<Entry> search(String query) {
        String normalized = normalize(query);
        if (normalized.isEmpty()) {
            return children(ROOT_ID);
        }

        List<Entry> out = new ArrayList<>();
        for (Entry entry : ENTRIES.values()) {
            String haystack = normalize(entry.title + " " + entry.subtitle + " " + entry.keywords);
            if (haystack.contains(normalized)) {
                out.add(entry);
                continue;
            }

            boolean allTokens = true;
            for (String token : normalized.split("\\s+")) {
                if (!token.isEmpty() && !haystack.contains(token)) {
                    allTokens = false;
                    break;
                }
            }
            if (allTokens) out.add(entry);
        }
        return Collections.unmodifiableList(out);
    }

    private static void add(List<Entry> out, String id) {
        Entry e = find(id);
        if (e != null) out.add(e);
    }

    private static String normalize(String value) {
        return value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
    }

    public static final class Entry {
        public final String id;
        public final String title;
        public final String subtitle;
        public final boolean browsable;
        public final String keywords;

        Entry(String id, String title, String subtitle, boolean browsable, String keywords) {
            this.id = id;
            this.title = title;
            this.subtitle = subtitle;
            this.browsable = browsable;
            this.keywords = keywords;
        }
    }
}
