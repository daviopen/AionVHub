package com.aionvhub.app;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Parser simples de playlists M3U/M3U8 para o catálogo IPTV do Hub. */
public final class IptvPlaylistParser {
    private static final Pattern ATTR = Pattern.compile("([A-Za-z0-9_-]+)=\"([^\"]*)\"");

    private IptvPlaylistParser() {}

    public static List<Channel> parse(String content) {
        if (content == null || content.trim().isEmpty()) return Collections.emptyList();

        List<Channel> out = new ArrayList<>();
        String pendingName = null;
        String pendingLogo = "";
        String pendingGroup = "";
        String pendingId = "";

        String[] lines = content.replace("\r", "").split("\n");
        for (String raw : lines) {
            String line = raw == null ? "" : raw.trim();
            if (line.isEmpty()) continue;

            if (line.startsWith("#EXTINF:")) {
                pendingName = extractName(line);
                pendingLogo = attr(line, "tvg-logo");
                pendingGroup = attr(line, "group-title");
                pendingId = attr(line, "tvg-id");
                continue;
            }

            if (line.startsWith("#")) continue;
            if (!(line.startsWith("http://") || line.startsWith("https://"))) continue;

            if (pendingName == null || pendingName.trim().isEmpty()) {
                pendingName = "Canal IPTV";
            }
            out.add(new Channel(pendingName.trim(), line, pendingLogo, pendingGroup, pendingId));
            pendingName = null;
            pendingLogo = "";
            pendingGroup = "";
            pendingId = "";
        }
        return Collections.unmodifiableList(out);
    }

    private static String extractName(String extinf) {
        int comma = extinf.indexOf(',');
        return comma >= 0 && comma + 1 < extinf.length() ? extinf.substring(comma + 1).trim() : "Canal IPTV";
    }

    private static String attr(String extinf, String key) {
        Matcher m = ATTR.matcher(extinf);
        while (m.find()) {
            if (key.equalsIgnoreCase(m.group(1))) return m.group(2) == null ? "" : m.group(2).trim();
        }
        return "";
    }

    public static final class Channel {
        public final String name;
        public final String url;
        public final String logoUrl;
        public final String group;
        public final String tvgId;

        Channel(String name, String url, String logoUrl, String group, String tvgId) {
            this.name = name == null ? "Canal IPTV" : name;
            this.url = url == null ? "" : url;
            this.logoUrl = logoUrl == null ? "" : logoUrl;
            this.group = group == null ? "" : group;
            this.tvgId = tvgId == null ? "" : tvgId;
        }
    }
}
