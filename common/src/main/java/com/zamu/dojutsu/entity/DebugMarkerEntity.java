package com.zamu.dojutsu.entity;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;

/**
 * Entidad marcador mínima usada como placeholder para validar el registro de
 * {@code EntityType} (DJT-006). No define datos sincronizados ni persistencia; no debe
 * spawnearse en cliente hasta que exista un renderer.
 */
public class DebugMarkerEntity extends Entity {

    public DebugMarkerEntity(EntityType<?> type, Level level) {
        super(type, level);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        // Sin datos sincronizados.
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag compound) {
        // Sin estado persistente.
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag compound) {
        // Sin estado persistente.
    }
}
