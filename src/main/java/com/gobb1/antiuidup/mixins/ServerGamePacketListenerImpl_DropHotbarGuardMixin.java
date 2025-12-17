package com.gobb1.antiuidup.mixins;

import com.gobb1.antiuidup.ContainerIntegrity;
import net.minecraft.network.protocol.game.ServerboundPlayerActionPacket;
import net.minecraft.network.protocol.game.ServerboundSetCarriedItemPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = ServerGamePacketListenerImpl.class, priority = 2000)
public abstract class ServerGamePacketListenerImpl_DropHotbarGuardMixin {

    @Shadow public ServerPlayer player;

    @Inject(method = "handlePlayerAction", at = @At("HEAD"), cancellable = true)
    private void guardPlayerAction(ServerboundPlayerActionPacket packet, CallbackInfo ci) {
        ServerboundPlayerActionPacket.Action action = packet.getAction();

        if (action == ServerboundPlayerActionPacket.Action.DROP_ITEM
                || action == ServerboundPlayerActionPacket.Action.DROP_ALL_ITEMS
                || action == ServerboundPlayerActionPacket.Action.SWAP_ITEM_WITH_OFFHAND) {

            if (ContainerIntegrity.onIllegalWorldAction(player, "player_action:" + action.name(), true)) {
                ci.cancel();
            }
        }
    }

    @Inject(method = "handleSetCarriedItem", at = @At("HEAD"), cancellable = true)
    private void guardHotbarSwitch(ServerboundSetCarriedItemPacket packet, CallbackInfo ci) {
        boolean ignoreVanilla = true;

        if (ContainerIntegrity.onIllegalWorldAction(player, "hotbar_switch:" + packet.getSlot(), ignoreVanilla)) {
            ci.cancel();
        }
    }
}
