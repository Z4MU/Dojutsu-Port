package com.zamu.dojutsu.dojutsu.susanoo;

import com.zamu.dojutsu.domain.susanoo.SusanooCostTable;
import com.zamu.dojutsu.domain.susanoo.SusanooTier;
import com.zamu.dojutsu.entity.AbilityAnchor;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

/**
 * Avatar del Susanoo {@code [S24]} (DJT-121): la coraza visible que envuelve al portador y <b>crece por
 * etapas</b>. Es un {@link AbilityAnchor} que sigue al dueno y se descarta al apagar el modo — el propio
 * contrato del ancla contempla este caso ("un sistema que necesite un ancla mientras el modo este activo crea
 * el ancla con la vida del modo o la descarta el mismo al desactivar").
 *
 * <p><b>Corrige el armor stand del datapack (RA-3):</b> alli el avatar era un {@code armor_stand} tageado
 * {@code d.narr} con un {@code knowledge_book{CustomModelData:<personaje><etapa>}} en la cabeza, que quedaba
 * colgado si su limpieza no corria. Aqui es una entidad con <b>caducidad por construccion</b>.
 *
 * <p><b>Alcance honesto:</b> esto es la <b>presencia</b> del avatar (entidad, seguimiento, silueta por etapa,
 * despawn), no sus modelos 3D. Los siete modelos por personaje ({@code CustomModelData} 1000..7000 + etapa) son
 * <b>trabajo de assets</b> aun sin portar, como lo fueron en su dia los modelos de los items de jutsu; hasta
 * entonces la silueta se dibuja con particulas, que es como se representan el resto de entidades del mod
 * (ninguna tiene renderer todavia).
 *
 * <p>La <b>etapa</b> (1..nivel dominado) decide el tamano de la silueta; el <b>nivel</b> decide coste y
 * proteccion (ver {@link SusanooCostTable}). Es la separacion del datapack: {@code set_model} mira
 * {@code susanoo_stg}, {@code effects} mira {@code susanoo_lvl}.
 */
final class SusanooAvatar {

    /** Vida del ancla si nadie la descarta: red de seguridad, el servicio la descarta al apagar el modo. */
    private static final int SAFETY_LIFETIME_TICKS = 20 * 60 * 10; // 10 min

    /** Altura de la silueta por etapa (bloques): de la caja toracica al Susanoo perfecto. */
    private static final double[] HEIGHT_BY_STAGE = {2.5, 3.2, 4.0, 5.0, 6.5, 8.0};

    /** Radio de la silueta por etapa (bloques). */
    private static final double[] RADIUS_BY_STAGE = {1.0, 1.3, 1.6, 2.0, 2.6, 3.2};

    private SusanooAvatar() {
    }

    /**
     * Invoca el avatar sobre el jugador. El ancla se mueve con el y dibuja su silueta cada tick leyendo la
     * <b>etapa actual</b> del servicio, asi que subir de etapa se ve sin reinvocar nada.
     */
    static AbilityAnchor spawn(ServerPlayer owner) {
        ServerLevel level = owner.serverLevel();
        AbilityAnchor avatar = AbilityAnchor.create(level, owner, SAFETY_LIFETIME_TICKS)
                .withTickCallback(anchor -> follow(anchor, owner));
        level.addFreshEntity(avatar);
        return avatar;
    }

    /** Sigue al dueno y dibuja la silueta de su etapa. Si el dueno ya no esta, el ancla se descarta. */
    private static void follow(AbilityAnchor anchor, ServerPlayer owner) {
        if (owner.isRemoved() || !owner.isAlive() || owner.level() != anchor.level()) {
            anchor.discard(); // huerfano: no dejamos avatares colgados (el pecado del armor stand)
            return;
        }
        anchor.setPos(owner.getX(), owner.getY(), owner.getZ());
        if (anchor.level() instanceof ServerLevel level) {
            drawShell(level, owner, SusanooService.stageOf(owner.getUUID()),
                    SusanooCostTable.tier(SusanooService.levelOf(owner)));
        }
    }

    /**
     * Dibuja la coraza: un anillo de particulas a la altura de la etapa, mas un nucleo. El color sale del
     * nivel — el datapack pintaba un {@code dust} distinto por personaje ({@code effects.mcfunction}); hasta
     * que existan los modelos por personaje, se distingue la <b>etapa</b>, que es lo que cambia al progresar.
     */
    private static void drawShell(ServerLevel level, ServerPlayer owner, int stage, SusanooTier tier) {
        int index = Math.min(Math.max(stage, 1), HEIGHT_BY_STAGE.length) - 1;
        double height = HEIGHT_BY_STAGE[index];
        double radius = RADIUS_BY_STAGE[index];
        Vec3 base = owner.position();
        DustParticleOptions dust = new DustParticleOptions(colorOf(tier), 1.5f);

        int points = 12 + index * 4; // la silueta se puebla al crecer
        for (int i = 0; i < points; i++) {
            double angle = 2 * Math.PI * i / points;
            double x = base.x + Math.cos(angle) * radius;
            double z = base.z + Math.sin(angle) * radius;
            level.sendParticles(dust, x, base.y + 0.2, z, 1, 0, 0, 0, 0);
            level.sendParticles(dust, x, base.y + height, z, 1, 0, 0, 0, 0);
        }
        level.sendParticles(ParticleTypes.SOUL_FIRE_FLAME,
                base.x, base.y + height / 2, base.z, 3, radius / 2, height / 3, radius / 2, 0.01);
    }

    /** Color de la coraza por nivel: del violeta de la caja toracica al dorado del Susanoo perfecto. */
    private static Vector3f colorOf(SusanooTier tier) {
        return switch (tier.level()) {
            case 1 -> new Vector3f(0.7f, 0.4f, 0.9f);
            case 2 -> new Vector3f(0.9f, 0.4f, 0.0f);
            case 3 -> new Vector3f(0.3f, 0.6f, 1.0f);
            case 4 -> new Vector3f(0.3f, 0.3f, 1.0f);
            case 5 -> new Vector3f(0.3f, 0.8f, 0.45f);
            default -> new Vector3f(0.98f, 0.89f, 0.43f);
        };
    }
}
