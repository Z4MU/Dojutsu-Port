package com.zamu.dojutsu.client.render;

import com.zamu.dojutsu.bijuu.entity.TailedBeastEntity;
import net.minecraft.client.model.HierarchicalModel;
import net.minecraft.client.model.geom.ModelPart;

/**
 * Modelo provisional de la Bestia con Cola (F7/DJT-160): <b>reutiliza la malla del ravager vanilla</b>.
 *
 * <p>El datapack hace exactamente esto —un {@code ravager} con un armor stand encima sosteniendo un item con
 * {@code CustomModelData}— y su autor lo deja anotado como pendiente. Aqui se toma la misma decision de forma
 * explicita y mucho mas barata: el renderer hornea {@code ModelLayers.RAVAGER}, que <b>ya registra vanilla</b>,
 * asi que no hace falta declarar una capa propia en cada loader ni un solo asset. La bestia se ve, se pelea y se
 * mata desde el primer dia; los <b>modelos reales</b> son deuda de arte anotada para F10, como el Susanoo.
 *
 * <p>El modelo es estatico a proposito: animar una malla prestada seria trabajo tirado cuando llegue la propia.
 */
public class TailedBeastModel extends HierarchicalModel<TailedBeastEntity> {

    private final ModelPart root;

    public TailedBeastModel(ModelPart root) {
        this.root = root;
    }

    @Override
    public ModelPart root() {
        return root;
    }

    @Override
    public void setupAnim(TailedBeastEntity entity, float limbSwing, float limbSwingAmount, float ageInTicks,
            float netHeadYaw, float headPitch) {
        // sin animacion: la malla es prestada (ver javadoc de la clase)
    }
}
