package com.zamu.dojutsu.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.zamu.dojutsu.entity.NinjaProjectile;
import com.zamu.dojutsu.items.JutsuModelData;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.client.renderer.entity.ThrownItemRenderer;
import net.minecraft.core.component.DataComponents;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomModelData;

/**
 * Renderer de los proyectiles del mod (F11/DJT-240). <b>Solo cliente.</b>
 *
 * <p><b>Por que hace falta.</b> Hasta ahora la entidad se pintaba con el {@code ThrownItemRenderer} de vanilla,
 * que gira el item <b>hacia la camara</b>. Para un sprite plano (una bola de nieve, un kunai) es lo correcto. Para
 * un modelo 3D con nariz y estela es un error: la nariz apuntaba siempre a quien la miraba, asi que el que
 * lanzaba la Bala de fuego veia el nucleo brillante de frente en vez de la estela alejandose. Lo detecto el
 * propio usuario en la primera prueba en partida.
 *
 * <p>Este renderer <b>orienta los modelos 3D de jutsu segun la direccion de vuelo</b> y deja todo lo demas
 * exactamente como estaba: los proyectiles con items de vanilla (shuriken de los shinobi, bolas de las bestias)
 * siguen girando hacia la camara. Si se orientaran tambien, un sprite plano visto de canto desapareceria.
 *
 * <p>Reutiliza el {@code ThrownItemRenderer} heredando de el, asi que no se registra ninguna {@code EntityType}
 * nueva (regla 5): solo se sustituye el renderer de una que ya existia.
 */
public final class NinjaProjectileRenderer extends ThrownItemRenderer<NinjaProjectile> {

    private final ItemRenderer items;

    public NinjaProjectileRenderer(EntityRendererProvider.Context context) {
        super(context);
        this.items = context.getItemRenderer();
    }

    @Override
    public void render(NinjaProjectile projectile, float entityYaw, float partialTick, PoseStack poseStack,
            MultiBufferSource buffers, int packedLight) {
        ItemStack stack = projectile.getItem();
        if (!isWorldModel(stack)) {
            super.render(projectile, entityYaw, partialTick, poseStack, buffers, packedLight); // sprite: a camara
            return;
        }
        if (projectile.tickCount < 2
                && entityRenderDispatcher.camera.getEntity().distanceToSqr(projectile) < 12.25D) {
            // Misma salvaguarda que el ThrownItemRenderer: recien lanzado, el proyectil esta dentro de la
            // cabeza de quien lo lanza. Orientado, ademas, la estela apunta justo a su camara.
            return;
        }
        poseStack.pushPose();
        float yaw = Mth.lerp(partialTick, projectile.yRotO, projectile.getYRot());
        float pitch = Mth.lerp(partialTick, projectile.xRotO, projectile.getXRot());
        poseStack.mulPose(ProjectileOrientation.noseAlongFlight(yaw, pitch));
        items.renderStatic(stack, ItemDisplayContext.GROUND, packedLight,
                net.minecraft.client.renderer.texture.OverlayTexture.NO_OVERLAY, poseStack, buffers,
                projectile.level(), projectile.getId());
        poseStack.popPose();
    }

    /** Lleva un modelo 3D de jutsu (rango 3000+), no un sprite de vanilla. */
    private static boolean isWorldModel(ItemStack stack) {
        CustomModelData data = stack.get(DataComponents.CUSTOM_MODEL_DATA);
        return data != null && JutsuModelData.isWorldModel(data.value());
    }
}
