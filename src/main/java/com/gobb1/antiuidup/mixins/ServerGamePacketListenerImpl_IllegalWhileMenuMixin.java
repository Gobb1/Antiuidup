package com.gobb1.antiuidup.mixins;

import com.gobb1.antiuidup.ContainerIntegrity;
import net.minecraft.network.protocol.game.ServerboundInteractPacket;
import net.minecraft.network.protocol.game.ServerboundUseItemOnPacket;
import net.minecraft.network.protocol.game.ServerboundUseItemPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = ServerGamePacketListenerImpl.class, priority = 2000)
public abstract class ServerGamePacketListenerImpl_IllegalWhileMenuMixin {

    @Shadow public ServerPlayer player;

    @Inject(method = "handleUseItemOn", at = @At("HEAD"), cancellable = true)
    private void guardUseItemOn(ServerboundUseItemOnPacket packet, CallbackInfo ci) {
        if (ContainerIntegrity.onIllegalWorldAction(player, "use_item_on", true)) {
            ci.cancel();
        }
    }

    @Inject(method = "handleUseItem", at = @At("HEAD"), cancellable = true)
    private void guardUseItem(ServerboundUseItemPacket packet, CallbackInfo ci) {
        if (ContainerIntegrity.onIllegalWorldAction(player, "use_item", true)) {
            ci.cancel();
        }
    }

    @Inject(method = "handleInteract", at = @At("HEAD"), cancellable = true)
    private void guardInteract(ServerboundInteractPacket packet, CallbackInfo ci) {
        if (ContainerIntegrity.onIllegalWorldAction(player, "interact_entity", true)) {
            ci.cancel();
        }
    }
}
