package com.aionvhub.app;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/** Catálogo-base independente de Android para navegação e testes do Hub. */
public final class HubMediaCatalog {
    public static final String ROOT_ID = "aion_root";
    public static final String APPS_ID = "aion_apps";
    public static final String FAVORITES_ID = "aion_favorites";
    public static final String IPTV_ID = "aion_iptv";
    public static final String RADIOS_ID = "aion_radios";
    public static final String DIAGNOSTICS_ID = "aion_diagnostics";

    public static final String CUSTOM_STREAM_ID = "custom_stream";
    public static final String APPS_EMPTY_ID = "apps_empty";
    public static final String IPTV_SETUP_ID = "iptv_setup";
    public static final String FAVORITE_EMPTY_ID = "favorite_empty";
    public static final String RADIO_INFO_ID = "radio_info";

    public static final String CONNECTION_ID = "connection_test";
    public static final String VOICE_SEARCH_ID = "voice_search_test";
    public static final String HOST_HANDSHAKE_ID = "host_handshake";
    public static final String BRIDGE_HEALTH_ID = "bridge_health";
    public static final String SHIZUKU_STATUS_ID = "shizuku_status";

    private static final Map<String, Entry> ENTRIES = new LinkedHashMap<>();

    static {
        register(new Entry(APPS_ID, "Apps", "Aplicativos detectados no tablet", true,
                "apps aplicativos tablet instalados"));
        register(new Entry(FAVORITES_ID, "Favoritos", "Suas fontes preferidas", true,
                "favoritos favoritos stream radio iptv"));
        register(new Entry(IPTV_ID, "IPTV / Streams", "Transmissões configuradas no tablet", true,
                "iptv tv stream transmissao hls m3u8"));
        register(new Entry(RADIOS_ID, "Rádios", "Rádios e streams de áudio", true,
                "radio radios audio stream"));
        register(new Entry(DIAGNOSTICS_ID, "Diagnóstico", "Estado completo do AION V Hub", true,
                "diagnostico bridge android auto shizuku host"));

        register(new Entry(APPS_EMPTY_ID, "Nenhum app detectado",
                "Abra o Hub no tablet para atualizar a lista", false,
                "apps tablet vazio"));
        register(new Entry(IPTV_SETUP_ID, "Configurar IPTV no tablet",
                "Abra o AION V Hub no tablet para informar uma URL", false,
                "configurar iptv tablet url stream"));
        register(new Entry(FAVORITE_EMPTY_ID, "Nenhum favorito configurado",
                "Configure uma fonte no tablet e marque como favorita", false,
                "favorito configurar tablet"));
        register(new Entry(RADIO_INFO_ID, "Adicionar rádio no tablet",
                "Use Configurar IPTV / stream para uma URL de áudio", false,
                "radio adicionar tablet audio"));

        register(new Entry(CONNECTION_ID, "Teste de conexão",
                "Valida MediaSession e descoberta pelo host", false,
                "conexao android auto media session descoberta host"));
        register(new Entry(VOICE_SEARCH_ID, "Pesquisa por voz",
                "Valida PLAY_FROM_SEARCH e pesquisa do catálogo", false,
                "voz voice pesquisa search android auto"));
        register(new Entry(HOST_HANDSHAKE_ID, "Handshake do Android Auto",
                "Confirma se o host abriu o MediaBrowserService", false,
                "host handshake gearhead android auto media browser"));
        register(new Entry(BRIDGE_HEALTH_ID, "Saúde do Media Bridge",
                "Valida catálogo, sessão e controles de reprodução", false,
                "bridge media catalogo sessao controles reproducao"));
        register(new Entry(SHIZUKU_STATUS_ID, "Diagnóstico Shizuku",
                "Diagnóstico privilegiado opcional", false,
                "shizuku diagnostico adb shell privilegiado opcional"));
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
            add(out, APPS_ID);
            add(out, FAVORITES_ID);
            add(out, IPTV_ID);
            add(out, RADIOS_ID);
            add(out, DIAGNOSTICS_ID);
        } else if (APPS_ID.equals(parentId)) {
            add(out, APPS_EMPTY_ID);
        } else if (FAVORITES_ID.equals(parentId)) {
            add(out, FAVORITE_EMPTY_ID);
        } else if (IPTV_ID.equals(parentId)) {
            add(out, IPTV_SETUP_ID);
        } else if (RADIOS_ID.equals(parentId)) {
            add(out, RADIO_INFO_ID);
        } else if (DIAGNOSTICS_ID.equals(parentId)) {
            add(out, HOST_HANDSHAKE_ID);
            add(out, BRIDGE_HEALTH_ID);
            add(out, SHIZUKU_STATUS_ID);
            add(out, CONNECTION_ID);
            add(out, VOICE_SEARCH_ID);
        }
        return Collections.unmodifiableList(out);
    }

    public static List<Entry> search(String query) {
        String normalized = normalize(query);
        if (normalized.isEmpty()) return children(ROOT_ID);

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

    public static Entry customStream(String title) {
        return new Entry(
                CUSTOM_STREAM_ID,
                title == null || title.trim().isEmpty() ? "Minha transmissão" : title.trim(),
                "Fonte configurada no AION V Hub",
                false,
                "stream iptv audio video favorito personalizado"
        );
    }

    public static Entry externalApp(String packageName, String label) {
        String safePackage = packageName == null ? "" : packageName.trim();
        String safeLabel = label == null || label.trim().isEmpty() ? safePackage : label.trim();
        return new Entry(
                HubAppCatalog.mediaId(safePackage),
                safeLabel,
                "Disponível no tablet",
                false,
                "app aplicativo tablet " + safeLabel + " " + safePackage
        );
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
