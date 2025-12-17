package com.gobb1.antiuidup.mixins;

import com.gobb1.antiuidup.ContainerIntegrity;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.MenuProvider;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.OptionalInt;

@Mixin(ServerPlayer.class)
public abstract class ServerPlayer_OpenMenuMixin {

    @Inject(
            method = "openMenu(Lnet/minecraft/world/MenuProvider;)Ljava/util/OptionalInt;",
            at = @At("RETURN")
    )
    private void antiuidup$onOpenMenu(MenuProvider provider, CallbackInfoReturnable<OptionalInt> cir) {
        OptionalInt opened = cir.getReturnValue();
        if (opened != null && opened.isPresent()) {
            ContainerIntegrity.onMenuOpened((ServerPlayer) (Object) this, opened.getAsInt());
        }
    }
}
