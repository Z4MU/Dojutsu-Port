package com.zamu.dojutsu.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.zamu.dojutsu.bijuu.entity.TailedBeastEntity;
import com.zamu.dojutsu.domain.bijuu.BeastArt;
import com.zamu.dojutsu.domain.bijuu.BeastStats;
import com.zamu.dojutsu.domain.bijuu.TailedBeast;
import com.zamu.dojutsu.registry.ModItems;
import java.util.OptionalInt;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.core.component.DataComponents;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomModelData;

/**
 * Renderer de la Bestia con Cola (F7/DJT-160, cuerpos en F10/DJT-228).
 *
 * <h2>Dos caminos, y los dos son fieles</h2>
 * <ul>
 *   <li><b>Gyuki y Kurama</b> tienen cuerpo modelado en el resource pack original (965 y 1243 cubos): se
 *       dibujan con el suyo, montado sobre el item-vehiculo {@link ModItems#BEAST_BODY}.</li>
 *   <li><b>Las otras siete</b> se dibujan con el <b>ravager</b>. No es un apaño a la espera de nada: el
 *       datapack invoca un ravager de verdad y <b>no lo hace invisible</b> —esa linea esta comentada en
 *       {@code spawn_tb_entity}—, y de los diez modelos que su resource pack declara solo existen esos dos.
 *       Ver {@link BeastArt}.</li>
 * </ul>
 *
 * <p>Lo unico que el mod <b>no</b> reproduce a proposito es el cubo morado y negro de "modelo ausente" que en el
 * original flota sobre las siete bestias sin modelo. Copiar eso seria portar un fallo.
 *
 * <h2>El tamaño sale de un solo sitio</h2>
 * {@link BeastStats#renderScale} lo comparten el dibujo y la caja de colision. Cuando el numero vivia aqui, la
 * caja se quedaba en el tamaño fijo del {@code EntityType} y las bestias grandes <b>no recibian daño</b>: se
 * golpeaba el cuerpo dibujado y no habia nada solido detras.
 */
public class TailedBeastRenderer extends MobRenderer<TailedBeastEntity, TailedBeastModel> {

    private static final ResourceLocation TEXTURE =
            ResourceLocation.withDefaultNamespace("textures/entity/illager/ravager.png");

    /**
     * El modelo del cuerpo es un item, y los items se modelan mirando <b>hacia el observador</b>; la entidad
     * mira hacia donde camina. Media vuelta lo alinea con su rumbo.
     */
    private static final float ITEM_MODEL_YAW_OFFSET = 180.0F;

    /**
     * Los cuerpos vienen de un item de 16x16x16 y hay que llevarlos a tamaño de bestia. El factor sale de
     * cuadrar la altura dibujada con la de {@link BeastStats}, no de un gusto: si no coinciden vuelve el
     * problema de golpear donde no hay caja.
     */
    private static final float ITEM_MODEL_SCALE = 2.0F;

    public TailedBeastRenderer(EntityRendererProvider.Context context) {
        super(context, new TailedBeastModel(context.bakeLayer(ModelLayers.RAVAGER)), 1.5F);
    }

    @Override
    public void render(TailedBeastEntity entity, float entityYaw, float partialTick, PoseStack poseStack,
            MultiBufferSource buffers, int packedLight) {
        OptionalInt body = BeastArt.customModelData(entity.beast());
        if (body.isEmpty()) {
            super.render(entity, entityYaw, partialTick, poseStack, buffers, packedLight); // ravager
            return;
        }
        renderBody(entity, body.getAsInt(), entityYaw, partialTick, poseStack, buffers, packedLight);
        // El nombre y la barra de vida los pinta EntityRenderer.render, que aqui no se llama: se replica solo
        // esa parte para no perder la etiqueta de las dos bestias que si tienen cuerpo.
        if (shouldShowName(entity)) {
            renderNameTag(entity, entity.getDisplayName(), poseStack, buffers, packedLight, partialTick);
        }
    }

    /** Dibuja el cuerpo modelado: el item-vehiculo con el {@code CustomModelData} de esa bestia. */
    private void renderBody(TailedBeastEntity entity, int customModelData, float entityYaw, float partialTick,
            PoseStack poseStack, MultiBufferSource buffers, int packedLight) {
        poseStack.pushPose();
        float scale = BeastStats.renderScale(entity.beast()) * ITEM_MODEL_SCALE;
        poseStack.translate(0.0D, BeastStats.height(entity.beast()) / 2.0D, 0.0D);
        poseStack.mulPose(com.mojang.math.Axis.YP.rotationDegrees(-entityYaw + ITEM_MODEL_YAW_OFFSET));
        poseStack.scale(scale, scale, scale);
        Minecraft.getInstance().getItemRenderer().renderStatic(bodyStack(entity.beast(), customModelData),
                ItemDisplayContext.FIXED, packedLight, net.minecraft.client.renderer.texture.OverlayTexture.NO_OVERLAY,
                poseStack, buffers, entity.level(), entity.getId());
        poseStack.popPose();
    }

    /** El item-vehiculo con el modelo de esa bestia dentro. */
    private static ItemStack bodyStack(TailedBeast beast, int customModelData) {
        ItemStack stack = new ItemStack(ModItems.BEAST_BODY.get());
        stack.set(DataComponents.CUSTOM_MODEL_DATA, new CustomModelData(customModelData));
        return stack;
    }

    /**
     * La escala sale de {@link BeastStats#renderScale}, <b>no de una constante local</b> (F10, arreglo de la
     * verificacion en partida). Solo aplica al camino del ravager: el cuerpo modelado escala en
     * {@link #renderBody}, porque ahi la rotacion tiene que ir antes que el tamaño.
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
