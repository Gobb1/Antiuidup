package com.gobb1.antiuidup;


import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;

import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.server.MinecraftServer;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public final class Antiuidup implements ModInitializer {

	public static Logger LOGGER = LoggerFactory.getLogger("Antiupdup");

	@Override
	public void onInitialize() {

		ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> {
			ContainerIntegrity.onDisconnect(handler.player);
		});

		ServerLifecycleEvents.SERVER_STARTED.register(Antiuidup::onServerStarted);
	}

	private static void onServerStarted(MinecraftServer it) {
		LOGGER.info("[Antiupdup] Ready");
	}

}
