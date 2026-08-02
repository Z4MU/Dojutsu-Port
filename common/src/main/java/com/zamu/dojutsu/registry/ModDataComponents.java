package com.zamu.dojutsu.registry;

import com.zamu.dojutsu.DojutsuMod;
import com.zamu.dojutsu.items.ContractComponent;
import com.zamu.dojutsu.items.DnaComponent;
import com.zamu.dojutsu.items.JutsuComponent;
import com.zamu.dojutsu.items.LearnerComponent;
import com.zamu.dojutsu.items.WeaponComponent;
import com.zamu.dojutsu.platform.PlatformServices;
import com.zamu.dojutsu.platform.Registrar;
import com.zamu.dojutsu.platform.RegistryEntry;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.Registries;

/**
 * Registro de data components del mod (1.21). Primer uso del registro
 * {@code Registries.DATA_COMPONENT_TYPE} a traves del puerto {@code RegistrarService} (RM-6: validar en
 * runtime en ambos loaders).
 */
public final class ModDataComponents {

    private static final Registrar<DataComponentType<?>> COMPONENTS =
            PlatformServices.REGISTRAR.create(Registries.DATA_COMPONENT_TYPE);

    /** Componente del Skill Learner: que elemento ensena el item (persistente y sincronizado al cliente). */
    public static final RegistryEntry<DataComponentType<LearnerComponent>> LEARNER =
            COMPONENTS.register(DojutsuMod.id("learner"), () -> DataComponentType.<LearnerComponent>builder()
                    .persistent(LearnerComponent.CODEC)
                    .networkSynchronized(LearnerComponent.STREAM_CODEC)
                    .build());

    /** Componente del item de jutsu (F4/DJT-075): que tecnica porta el item. */
    public static final RegistryEntry<DataComponentType<JutsuComponent>> JUTSU =
            COMPONENTS.register(DojutsuMod.id("jutsu"), () -> DataComponentType.<JutsuComponent>builder()
                    .persistent(JutsuComponent.CODEC)
                    .networkSynchronized(JutsuComponent.STREAM_CODEC)
                    .build());

    /** Componente del arma ninja (F4/DJT-082): tipo, daño base y req de kenjutsu de sus tecnicas. */
    public static final RegistryEntry<DataComponentType<WeaponComponent>> WEAPON =
            COMPONENTS.register(DojutsuMod.id("weapon"), () -> DataComponentType.<WeaponComponent>builder()
                    .persistent(WeaponComponent.CODEC)
                    .networkSynchronized(WeaponComponent.STREAM_CODEC)
                    .build());

    /** Componente del ADN (F8/DJT-170): que lleva dentro la muestra, o vacio si aun no se ha identificado. */
    public static final RegistryEntry<DataComponentType<DnaComponent>> DNA =
            COMPONENTS.register(DojutsuMod.id("dna"), () -> DataComponentType.<DnaComponent>builder()
                    .persistent(DnaComponent.CODEC)
                    .networkSynchronized(DnaComponent.STREAM_CODEC)
                    .build());

    /** Componente del contrato (F8/DJT-175): especie firmada, o vacio si esta en blanco. */
    public static final RegistryEntry<DataComponentType<ContractComponent>> CONTRACT =
            COMPONENTS.register(DojutsuMod.id("contract"), () -> DataComponentType.<ContractComponent>builder()
                    .persistent(ContractComponent.CODEC)
                    .networkSynchronized(ContractComponent.STREAM_CODEC)
                    .build());

    private ModDataComponents() {
    }

    /** Confirma el registro. Llamar durante la inicializacion del mod. */
    public static void register() {
        COMPONENTS.commit();
    }
}
