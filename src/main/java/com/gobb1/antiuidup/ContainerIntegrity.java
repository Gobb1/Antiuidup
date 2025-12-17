package com.gobb1.antiuidup;

import com.mojang.logging.LogUtils;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import org.slf4j.Logger;

import java.util.Set;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class ContainerIntegrity {
    private ContainerIntegrity() {}

    private static final Logger LOGGER = LogUtils.getLogger();
    private static final Map<UUID, State> STATES = new ConcurrentHashMap<>();

    private static final long OPEN_GRACE_MS = 350;

    private static final int MAX_VIOLATIONS_IN_WINDOW = 3;
    private static final long WINDOW_MS = 2500;

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

    public static boolean validateContainerPacket(ServerPlayer p, int packetContainerId, String kind) {
        int expected = currentContainerId(p);
        if (packetContainerId == expected) return true;

        forceClose(p, "invalid_container_id:" + kind);

        int v = addViolation(p);
        LOGGER.warn("[ContainerIntegrity] {} invalid {}: packetId={} expectedId={} v={}",
                p.getGameProfile().getName(), kind, packetContainerId, expected, v
        );

        if (v >= MAX_VIOLATIONS_IN_WINDOW) {
            p.connection.disconnect(Component.literal("Desync/invalid container packets."));
        }
        return false;
    }

    /**
     * Regra global: recebeu ação "do mundo" enquanto servidor acha que menu está aberto.
     * Fecha o menu no servidor, incrementa violações e retorna true para você CANCELAR o pacote.
     *
     * Use ignoreVanillaMenus=true só para checks que podem ter falso positivo.
     */
    public static boolean onIllegalWorldAction(ServerPlayer p, String kind, boolean ignoreVanillaMenus) {
        if (!hasOpenMenu(p)) return false;

        if (ignoreVanillaMenus && isIgnoredVanillaMenu(p)) return false;

        State s = STATES.computeIfAbsent(p.getUUID(), k -> new State());
        long now = System.currentTimeMillis();
        long sinceOpen = now - s.lastMenuOpenAtMs;

        boolean isGrace = sinceOpen >= 0 && sinceOpen < OPEN_GRACE_MS;
        boolean isOpenNoiseKind = kind.equals("interact_entity") || kind.equals("use_item_on");

        if (isGrace && isOpenNoiseKind) {
            return true;
        }

        int containerId = currentContainerId(p);
        p.closeContainer();
        onMenuClosed(p);

        int v = s.addViolation();
        LOGGER.warn("[ContainerIntegrity] {} illegal action while menu open: {} (containerId={}) v={}",
                p.getGameProfile().getName(), kind, containerId, v
        );

        if (v >= MAX_VIOLATIONS_IN_WINDOW) {
            p.connection.disconnect(Component.literal("Desync/illegal actions while menu open."));
        }
        return true;
    }

    public static void onDisconnect(ServerPlayer p) {
        STATES.remove(p.getUUID());
    }


    public static void closeOnContextChange(ServerPlayer p, String reason) {
        if (!hasOpenMenu(p)) return;

        LOGGER.info("[ContainerIntegrity] Closing menu for {} due to context change: {} (containerId={})",
                p.getGameProfile().getName(), reason, currentContainerId(p)
        );
        forceClose(p, "context_change:" + reason);
    }

    public static void onMenuOpened(ServerPlayer p, int containerId) {
        State s = STATES.computeIfAbsent(p.getUUID(), k -> new State());
        s.lastKnownContainerId = containerId;
        s.lastMenuOpenAtMs = System.currentTimeMillis();
        s.resetWindow();
    }

    private static void forceClose(ServerPlayer p, String reason) {
        p.closeContainer();
        onMenuClosed(p);
    }

    private static boolean isIgnoredVanillaMenu(ServerPlayer p) {
        String simple = p.containerMenu.getClass().getSimpleName();
        return IGNORED_VANILLA_MENU_SIMPLE_NAMES.contains(simple);
    }

    private static int addViolation(ServerPlayer p) {
        State s = STATES.computeIfAbsent(p.getUUID(), k -> new State());
        return s.addViolation();
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

        int addViolation() {
            long now = System.currentTimeMillis();
            if (now - windowStart > WINDOW_MS) {
                windowStart = now;
                violationsInWindow = 0;
            }
            violationsInWindow++;
            return violationsInWindow;
        }
    }
}
