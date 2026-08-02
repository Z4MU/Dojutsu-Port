package com.zamu.dojutsu.client.render;

import com.zamu.dojutsu.ai.ShinobiEntity;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.HumanoidMobRenderer;
import net.minecraft.client.renderer.entity.layers.HumanoidArmorLayer;
import net.minecraft.resources.ResourceLocation;

/**
 * Renderer provisional del shinobi NPC (F8/DJT-185): reutiliza el <b>modelo humanoide y la textura del zombi</b>.
 *
 * <p>Es exactamente lo que hace el datapack —invoca un {@code zombie} con nombre— con la diferencia de que aqui
 * el zombi es solo el <b>aspecto</b> y no la entidad. El modelo humanoide y sus capas de armadura ya los registra
 * vanilla ({@link ModelLayers#ZOMBIE}), asi que no hace falta declarar capa propia en cada loader ni un solo
 * asset: <b>skins propias por aldea → F10</b>, con el resto del arte.
 *
 * <p>Ventaja de heredar de {@link HumanoidMobRenderer}: si en F10 se le pone armadura o el protector de frente,
 * la capa ya esta.
 */
public class ShinobiRenderer extends HumanoidMobRenderer<ShinobiEntity, HumanoidModel<ShinobiEntity>> {

    private static final ResourceLocation TEXTURE =
            ResourceLocation.withDefaultNamespace("textures/entity/zombie/zombie.png");

    public ShinobiRenderer(EntityRendererProvider.Context context) {
        super(context, new HumanoidModel<>(context.bakeLayer(ModelLayers.ZOMBIE)), 0.5F);
        this.addLayer(new HumanoidArmorLayer<>(this,
                new HumanoidModel<>(context.bakeLayer(ModelLayers.ZOMBIE_INNER_ARMOR)),
                new HumanoidModel<>(context.bakeLayer(ModelLayers.ZOMBIE_OUTER_ARMOR)),
                context.getModelManager()));
    }

    @Override
    public ResourceLocation getTextureLocation(ShinobiEntity entity) {
        return TEXTURE;
    }
}
