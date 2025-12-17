package com.gobb1.antiuidup;

import com.gobb1.antiuidup.config.AntiUiDupConfig;
import com.gobb1.antiuidup.logging.AuditLogger;
import com.gobb1.antiuidup.webhook.DiscordWebhook;
import com.gobb1.antiuidup.webhook.WebhookLimiter;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class ContainerIntegrity {
    private ContainerIntegrity() {}

    private static volatile AntiUiDupConfig CFG = new AntiUiDupConfig();
    private static final Map<UUID, State> STATES = new ConcurrentHashMap<>();

    private static final Set<String> IGNORED_VANILLA_MENU_SIMPLE_NAMES = Set.of(
            "CraftingMenu",
            "InventoryMenu",
            "FurnaceMenu",
            "BlastFurnaceMenu",
            "SmokerMenu",
            "BrewingStandMenu",
            "StonecutterMenu",
            "GrindstoneMenu",
            "SmithingMenu",
            "LoomMenu",
            "CartographyTableMenu",
            "EnchantmentMenu",
            "AnvilMenu",
            "BeaconMenu",
            "MerchantMenu",
            "HorseInventoryMenu",
            "LecternMenu",
            "CrafterMenu",
            "ChestMenu",
            "DispenserMenu",
            "HopperMenu",
            "ShulkerBoxMenu"
    );

    public static void applyConfig(AntiUiDupConfig cfg) {
        if (cfg != null) CFG = cfg;
    }

    private static long openGraceMs() { return Math.max(0L, CFG.openGraceMs); }
    private static long windowMs() { return Math.max(200L, CFG.windowMs); }
    private static int maxViolations() { return Math.max(1, CFG.maxViolationsInWindow); }

    private static void webhook(ServerPlayer p, String message) {
        if (!CFG.webhookEnabled) return;
        if (CFG.discordWebhookUrl == null || CFG.discordWebhookUrl.isBlank()) return;
        if (!WebhookLimiter.canSend(p.getUUID(), CFG.webhookCooldownSeconds)) return;

        String prefix = CFG.webhookIncludeServerName ? ("[" + CFG.serverName + "] ") : "";
        DiscordWebhook.send(CFG.discordWebhookUrl, prefix + message);
    }

    private static String playerTag(ServerPlayer p) {
        return p.getGameProfile().getName() + " (" + p.getUUID() + ")";
    }

    public static boolean hasOpenMenu(ServerPlayer p) {
        return p.containerMenu != p.inventoryMenu;
    }

    public static int currentContainerId(ServerPlayer p) {
        return p.containerMenu.containerId;
    }

    public static void onMenuClosed(ServerPlayer p) {
        State s = STATES.computeIfAbsent(p.getUUID(), k -> new State());
        s.lastKnownContainerId = -1;
        s.resetWindow();
    }

    public static void onMenuOpened(ServerPlayer p, int containerId) {
        State s = STATES.computeIfAbsent(p.getUUID(), k -> new State());
        s.lastKnownContainerId = containerId;
        s.lastMenuOpenAtMs = System.currentTimeMillis();
        s.resetWindow();
    }

    public static void onDisconnect(ServerPlayer p) {
        STATES.remove(p.getUUID());
        WebhookLimiter.clear(p.getUUID());
    }

    private static void forceClose(ServerPlayer p, String reason) {
        p.closeContainer();
        onMenuClosed(p);

        AuditLogger.info("[ContainerIntegrity] forceClose player=" + playerTag(p)
                + " reason=" + reason + " containerId=" + currentContainerId(p));
    }

    private static boolean isIgnoredVanillaMenu(ServerPlayer p) {
        String simple = p.containerMenu.getClass().getSimpleName();
        return IGNORED_VANILLA_MENU_SIMPLE_NAMES.contains(simple);
    }

    private static int addViolation(ServerPlayer p) {
        State s = STATES.computeIfAbsent(p.getUUID(), k -> new State());
        return s.addViolation(windowMs());
    }

    private static void maybeKick(ServerPlayer p, int v, String kickReason, String webhookReason) {
        if (v < maxViolations()) return;

        if (CFG.webhookOnKick) {
            webhook(p, "Kick por suspeita de desync: player=" + playerTag(p)
                    + " reason=" + webhookReason + " violations=" + v);
        }

        p.connection.disconnect(Component.literal(kickReason));
    }

    public static boolean validateContainerPacket(ServerPlayer p, int packetContainerId, String kind) {
        if (!CFG.enabled) return true;

        int expected = currentContainerId(p);
        if (packetContainerId == expected) return true;

        forceClose(p, "invalid_container_id:" + kind);

        int v = addViolation(p);

        String log = "[ContainerIntegrity] invalid_container_id player=" + playerTag(p)
                + " kind=" + kind
                + " packetId=" + packetContainerId
                + " expectedId=" + expected
                + " menu=" + p.containerMenu.getClass().getName()
                + " v=" + v;

        AuditLogger.warn(log);

        if (CFG.webhookOnInvalidContainerId) {
            webhook(p, "Suspeito: invalid_container_id\n"
                    + "player: " + playerTag(p) + "\n"
                    + "kind: " + kind + "\n"
                    + "packetId: " + packetContainerId + "\n"
                    + "expectedId: " + expected + "\n"
                    + "menu: " + p.containerMenu.getClass().getName() + "\n"
                    + "v: " + v);
        }

        maybeKick(p, v, "Desync/invalid container packets.", "invalid_container_id:" + kind);
        return false;
    }

    public static boolean onIllegalWorldAction(ServerPlayer p, String kind, boolean ignoreVanillaMenus) {
        if (!CFG.enabled) return false;
        if (!hasOpenMenu(p)) return false;

        if (ignoreVanillaMenus && isIgnoredVanillaMenu(p)) {
            return false;
        }

        State s = STATES.computeIfAbsent(p.getUUID(), k -> new State());
        long now = System.currentTimeMillis();
        long sinceOpen = now - s.lastMenuOpenAtMs;

        boolean isGrace = sinceOpen >= 0 && sinceOpen < openGraceMs();
        boolean isOpenNoiseKind = kind.equals("interact_entity") || kind.equals("use_item_on");

        if (isGrace && isOpenNoiseKind) {
            return true;
        }

        int containerId = currentContainerId(p);
        String menuClass = p.containerMenu.getClass().getName();

        p.closeContainer();
        onMenuClosed(p);

        int v = s.addViolation(windowMs());

        String log = "[ContainerIntegrity] illegal_world_action player=" + playerTag(p)
                + " kind=" + kind
                + " containerId=" + containerId
                + " menu=" + menuClass
                + " v=" + v;

        AuditLogger.warn(log);

        if (CFG.webhookOnIllegalWorldAction) {
            webhook(p, "Suspeito: illegal_world_action\n"
                    + "player: " + playerTag(p) + "\n"
                    + "kind: " + kind + "\n"
                    + "containerId: " + containerId + "\n"
                    + "menu: " + menuClass + "\n"
                    + "v: " + v);
        }

        maybeKick(p, v, "Desync/illegal actions while menu open.", "illegal_world_action:" + kind);
        return true;
    }

    public static void closeOnContextChange(ServerPlayer p, String reason) {
        if (!CFG.enabled) return;
        if (!hasOpenMenu(p)) return;

        AuditLogger.info("[ContainerIntegrity] context_close player=" + playerTag(p)
                + " reason=" + reason
                + " containerId=" + currentContainerId(p)
                + " menu=" + p.containerMenu.getClass().getName());

        p.closeContainer();
        onMenuClosed(p);
    }

    private static final class State {
        int lastKnownContainerId = -1;

        long windowStart = 0;
        int violationsInWindow = 0;

        long lastMenuOpenAtMs = 0;

        void resetWindow() {
            windowStart = System.currentTimeMillis();
            violationsInWindow = 0;
        }

        int addViolation(long windowMs) {
            long now = System.currentTimeMillis();
            if (now - windowStart > windowMs) {
                windowStart = now;
                violationsInWindow = 0;
            }
            violationsInWindow++;
            return violationsInWindow;
        }
    }
}
