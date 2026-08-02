package com.zamu.dojutsu.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.zamu.dojutsu.bijuu.entity.TailedBeastEntity;
import com.zamu.dojutsu.domain.bijuu.BeastStats;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.resources.ResourceLocation;

/**
 * Renderer provisional de la Bestia con Cola (F7/DJT-160). Hornea la capa <b>del ravager vanilla</b>
 * ({@link ModelLayers#RAVAGER}) y usa su textura: ni capa propia por loader ni assets.
 *
 * <p><b>El tamaño si es propio</b>: la bestia crece con sus colas ({@link BeastStats#renderScale}), asi que un
 * Shukaku se ve grande y el Diez Colas, enorme. Es la unica pista visual de <i>cual</i> bestia tienes delante
 * hasta que lleguen los modelos — y basta para no confundirlas en combate.
 */
public class TailedBeastRenderer extends MobRenderer<TailedBeastEntity, TailedBeastModel> {

    private static final ResourceLocation TEXTURE =
            ResourceLocation.withDefaultNamespace("textures/entity/illager/ravager.png");

    public TailedBeastRenderer(EntityRendererProvider.Context context) {
        super(context, new TailedBeastModel(context.bakeLayer(ModelLayers.RAVAGER)), 1.5F);
    }

    /**
     * La escala sale de {@link BeastStats#renderScale}, <b>no de una constante local</b> (F10, arreglo de la
     * verificacion en partida). Cuando el numero vivia aqui, la caja de colision de la entidad se quedaba en el
     * tamaño fijo del {@code EntityType} y las bestias grandes <b>no recibian daño</b>: se golpeaba el cuerpo
     * dibujado y no habia nada solido detras. Compartir la funcion es lo que impide que vuelva a pasar.
     */
    @Override
    protected void scale(TailedBeastEntity entity, PoseStack poseStack, float partialTick) {
        float scale = BeastStats.renderScale(entity.beast());
        poseStack.scale(scale, scale, scale);
    }

    @Override
    public ResourceLocation getTextureLocation(TailedBeastEntity entity) {
        return TEXTURE;
    }
}
