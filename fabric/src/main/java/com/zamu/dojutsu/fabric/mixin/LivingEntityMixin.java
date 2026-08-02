package com.zamu.dojutsu.fabric.mixin;

import com.zamu.dojutsu.chakra.FoodChakraHandler;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Mixin de Fabric que dispara la reposicion de chakra al <b>terminar de comer</b>. Necesario porque fabric-api
 * no expone un evento de "fin de uso de item" (NeoForge si: {@code LivingEntityUseItemEvent.Finish}, ver
 * {@code DojutsuNeoForgeServerEvents}). Ver {@code [ADD/05]} y DJT-032b.
 *
 * <p>Se engancha al inicio de {@code completeUsingItem} (que solo se invoca cuando el uso del item ha
 * completado su duracion) y, solo para {@link ServerPlayer} (lado servidor), delega en el handler comun con el
 * item que se estaba usando. El propio handler filtra a comida.
 */
@Mixin(LivingEntity.class)
public abstract class LivingEntityMixin {

    @Inject(method = "completeUsingItem", at = @At("HEAD"))
    private void dojutsu$onCompleteUsingItem(CallbackInfo ci) {
        if ((Object) this instanceof ServerPlayer player) {
            FoodChakraHandler.onFinishEating(player, player.getUseItem());
        }
    }
}
