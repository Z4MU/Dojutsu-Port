package com.zamu.dojutsu.registry;

import com.zamu.dojutsu.DojutsuMod;
import com.zamu.dojutsu.ai.ShinobiEntity;
import com.zamu.dojutsu.bijuu.entity.TailedBeastEntity;
import com.zamu.dojutsu.entity.AbilityAnchor;
import com.zamu.dojutsu.entity.ExplosiveTagEntity;
import com.zamu.dojutsu.entity.MarkerEntity;
import com.zamu.dojutsu.entity.NinjaProjectile;
import com.zamu.dojutsu.platform.PlatformServices;
import com.zamu.dojutsu.platform.Registrar;
import com.zamu.dojutsu.platform.RegistryEntry;
import dev.architectury.registry.level.entity.EntityAttributeRegistry;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;

/**
 * Registro de tipos de entidad del mod: las tres primitivas de EP-ENTITY (F4/DJT-070..072)
 * que comparten los sistemas de tecnica — proyectil, ancla de habilidad y marcador. Las
 * entidades de contenido (bestias, summons, shinobi IA) se anadiran aqui en F7/F8.
 *
 * <p>Toda entidad registrada necesita renderer en cliente (regla 5, o el cliente crashea):
 * se registran por loader en el client initializer (Fabric) / evento de renderers (NeoForge).
 * El placeholder {@code debug_marker} de F0 se retiro en DJT-072 (superado por
 * {@link MarkerEntity}).
 */
public final class ModEntities {

    private static final Registrar<EntityType<?>> ENTITIES =
            PlatformServices.REGISTRAR.create(Registries.ENTITY_TYPE);

    /**
     * Proyectil ninja reutilizable (F4/DJT-070): fisica propia, impacto por raycast, dueno
     * por UUID, transitorio. Tracking y update como los proyectiles lanzados vanilla.
     */
    public static final RegistryEntry<EntityType<NinjaProjectile>> NINJA_PROJECTILE =
            ENTITIES.register(DojutsuMod.id("ninja_projectile"),
                    () -> EntityType.Builder.of(NinjaProjectile::new, MobCategory.MISC)
                            .sized(0.25F, 0.25F)
                            .clientTrackingRange(4)
                            .updateInterval(10)
                            .build("ninja_projectile"));

    /**
     * Ancla posicional de habilidad activa (F4/DJT-071): tick propio + expiracion
     * garantizada. Viaja al cliente (F5/F6 colgaran visuales de ella); renderer Noop hoy.
     */
    public static final RegistryEntry<EntityType<AbilityAnchor>> ABILITY_ANCHOR =
            ENTITIES.register(DojutsuMod.id("ability_anchor"),
                    () -> EntityType.Builder.of(AbilityAnchor::new, MobCategory.MISC)
                            .sized(0.25F, 0.25F)
                            .clientTrackingRange(4)
                            .updateInterval(10)
                            .build("ability_anchor"));

    /**
     * Marcador generico transitorio (F4/DJT-071): posicion + caducidad, sin logica. Como el
     * Marker vanilla, no viaja al cliente (trackingRange 0): puro estado de servidor.
     */
    public static final RegistryEntry<EntityType<MarkerEntity>> MARKER =
            ENTITIES.register(DojutsuMod.id("marker"),
                    () -> EntityType.Builder.of(MarkerEntity::new, MobCategory.MISC)
                            .sized(0.0F, 0.0F)
                            .clientTrackingRange(0)
                            .build("marker"));

    /**
     * Bestia con Cola (F7/DJT-160): entidad <b>jefe</b> con barra nativa, la primera criatura viva del mod.
     * A diferencia de las tres primitivas, es un {@code Monster} con atributos (registrados aparte, ver
     * {@link #registerAttributes}) y con tracking amplio: se ve venir de lejos.
     */
    public static final RegistryEntry<EntityType<TailedBeastEntity>> TAILED_BEAST =
            ENTITIES.register(DojutsuMod.id("tailed_beast"),
                    () -> EntityType.Builder.of(TailedBeastEntity::new, MobCategory.MONSTER)
                            .sized(2.0F, 2.6F)
                            .fireImmune()
                            .clientTrackingRange(16)
                            .build("tailed_beast"));

    /**
     * Shinobi NPC (F8/DJT-185): el enemigo humano, en seis niveles de amenaza. Tamaño de jugador.
     */
    public static final RegistryEntry<EntityType<ShinobiEntity>> SHINOBI =
            ENTITIES.register(DojutsuMod.id("shinobi"),
                    () -> EntityType.Builder.of(ShinobiEntity::new, MobCategory.MONSTER)
                            .sized(0.6F, 1.95F)
                            .clientTrackingRange(10)
                            .build("shinobi"));

    /**
     * Sello explosivo colocado (F10/DJT-225): la mina de {@code items/explosive_tag}. Es la <b>unica entidad
     * persistente</b> del mod que no es una criatura — una trampa se deja puesta y se vuelve a por ella luego,
     * quiza tras reiniciar, asi que no podia ser un {@link AbilityAnchor} (que nunca se guarda). Se dibuja como
     * el papel que lleva encima, igual que el {@code armor_stand} del datapack.
     */
    public static final RegistryEntry<EntityType<ExplosiveTagEntity>> EXPLOSIVE_TAG =
            ENTITIES.register(DojutsuMod.id("explosive_tag"),
                    () -> EntityType.Builder.of(ExplosiveTagEntity::new, MobCategory.MISC)
                            .sized(0.25F, 0.25F)
                            .clientTrackingRange(8)
                            .updateInterval(20)
                            .build("explosive_tag"));

    private ModEntities() {
    }

    /** Confirma el registro. Llamar durante la inicializacion del mod. */
    public static void register() {
        ENTITIES.commit();
        registerAttributes();
    }

    /**
     * Registra los atributos de las entidades vivas. Va por el puerto de Architectury porque cada loader lo hace
     * distinto (NeoForge: {@code EntityAttributeCreationEvent}; Fabric:
     * {@code FabricDefaultAttributeRegistry}), y sin ellos el juego revienta al invocar la entidad.
     */
    private static void registerAttributes() {
        EntityAttributeRegistry.register(TAILED_BEAST, TailedBeastEntity::createAttributes);
        EntityAttributeRegistry.register(SHINOBI, ShinobiEntity::createAttributes);
    }
}
