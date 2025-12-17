package com.gobb1.antiuidup.commands;

import com.gobb1.antiuidup.Antiuidup;
import com.gobb1.antiuidup.ContainerIntegrity;
import com.gobb1.antiuidup.config.AntiUiDupConfig;
import com.gobb1.antiuidup.logging.AuditLogger;
import com.mojang.brigadier.CommandDispatcher;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;

public final class AntiUiDupCommands {
    private AntiUiDupCommands() {}

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(
                Commands.literal("antiuidup")
                        .requires(src -> src.hasPermission(2))
                        .then(Commands.literal("reload")
                                .executes(ctx -> {
                                    try {
                                        AntiUiDupConfig cfg = AntiUiDupConfig.loadOrCreate(Antiuidup.CONFIG_PATH);
                                        Antiuidup.CONFIG = cfg;

                                        ContainerIntegrity.applyConfig(cfg);
                                        AuditLogger.applyConfig(cfg);

                                        ctx.getSource().sendSuccess(() ->
                                                Component.literal("[AntiUiDup] Config recarregada com sucesso."), true);

                                        AuditLogger.info("[AntiUiDup] Reload executado por " + ctx.getSource().getTextName());
                                        return 1;
                                    } catch (Exception e) {
                                        ctx.getSource().sendFailure(Component.literal("[AntiUiDup] Falha ao recarregar config: " + e.getMessage()));
                                        return 0;
                                    }
                                })
                        )
        );
    }
}
