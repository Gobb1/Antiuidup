package com.gobb1.antiuidup;

import com.gobb1.antiuidup.commands.AntiUiDupCommands;
import com.gobb1.antiuidup.config.AntiUiDupConfig;
import com.gobb1.antiuidup.logging.AuditLogger;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.server.MinecraftServer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Path;

public final class Antiuidup implements ModInitializer {

	public static Logger LOGGER = LoggerFactory.getLogger("Antiupdup");
	public static AntiUiDupConfig CONFIG;

	public static Path CONFIG_PATH;

	@Override
	public void onInitialize() {
		CONFIG_PATH = FabricLoader.getInstance().getConfigDir()
				.resolve("Antiuidup")
				.resolve("antiuidup.json");

		CONFIG = AntiUiDupConfig.loadOrCreate(CONFIG_PATH);

		Path logPath = FabricLoader.getInstance().getGameDir()
				.resolve("logs")
				.resolve("antiuidup.log");

		AuditLogger.init(logPath, CONFIG, LOGGER);
		ContainerIntegrity.applyConfig(CONFIG);

		ServerPlayConnectionEvents.DISCONNECT.register((handler, server) ->
				ContainerIntegrity.onDisconnect(handler.player)
		);

		CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
			AntiUiDupCommands.register(dispatcher);
		});

		LOGGER.info("[Antiupdup] Ready");
	}
}
