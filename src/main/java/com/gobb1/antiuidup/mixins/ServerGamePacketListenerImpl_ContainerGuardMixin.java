package com.gobb1.antiuidup.mixins;

import com.gobb1.antiuidup.ContainerIntegrity;
import net.minecraft.network.protocol.game.ServerboundContainerButtonClickPacket;
import net.minecraft.network.protocol.game.ServerboundContainerClickPacket;
import net.minecraft.network.protocol.game.ServerboundContainerClosePacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;


@Mixin(value = ServerGamePacketListenerImpl.class, priority = 2000)
public abstract class ServerGamePacketListenerImpl_ContainerGuardMixin {
    @Shadow public ServerPlayer player;

    @Inject(method = "handleContainerClick", at = @At("HEAD"), cancellable = true)
    private void guardContainerClick(ServerboundContainerClickPacket packet, CallbackInfo ci) {
        int id = packet.getContainerId();
        if (!ContainerIntegrity.validateContainerPacket(player, id, "ContainerClick")) {
            ci.cancel();
        }
    }

    @Inject(method = "handleContainerButtonClick", at = @At("HEAD"), cancellable = true)
    private void guardContainerButton(ServerboundContainerButtonClickPacket packet, CallbackInfo ci) {
        int id = packet.containerId();
        if (!ContainerIntegrity.validateContainerPacket(player, id, "ContainerButton")) {
            ci.cancel();
        }
    }

    @Inject(method = "handleContainerClose", at = @At("HEAD"))
    private void onContainerClose(ServerboundContainerClosePacket packet, CallbackInfo ci) {
        ContainerIntegrity.onMenuClosed(player);
    }
}

