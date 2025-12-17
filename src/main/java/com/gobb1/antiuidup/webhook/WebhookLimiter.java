package com.gobb1.antiuidup.webhook;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class WebhookLimiter {
    private WebhookLimiter() {}

    private static final Map<UUID, Long> LAST_SENT = new ConcurrentHashMap<>();

    public static boolean canSend(UUID uuid, int cooldownSeconds) {
        long now = System.currentTimeMillis();
        long cd = Math.max(0, cooldownSeconds) * 1000L;
        Long last = LAST_SENT.get(uuid);
        if (last != null && now - last < cd) return false;
        LAST_SENT.put(uuid, now);
        return true;
    }

    public static void clear(UUID uuid) {
        LAST_SENT.remove(uuid);
    }
}
