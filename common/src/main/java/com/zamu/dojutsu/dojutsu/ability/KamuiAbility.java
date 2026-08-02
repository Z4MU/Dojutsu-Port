package com.zamu.dojutsu.dojutsu.ability;

import com.zamu.dojutsu.chakra.ChakraService;
import com.zamu.dojutsu.data.NinjaData;
import com.zamu.dojutsu.data.PlayerNinjaData;
import com.zamu.dojutsu.dojutsu.EyeStrainService;
import java.util.UUID;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.phys.Vec3;

/**
 * Kamui (Mangekyō de Obito): el jutsu espacio-tiempo — <b>marca un punto</b> y luego <b>te transporta</b> a el.
 * Es la tecnica insignia del Mangekyō de Obito y la primera del roster por personaje (lote 2). {@code [S05]},
 * {@code sharingan/mangekyo/obito/kamui_set} + {@code kamui_tp}.
 *
 * <p><b>Marca y vuelve, en una sola habilidad:</b> el datapack separa "Kamui (Set Location)" y "Kamui" en dos
 * slots; aqui una unica habilidad alterna — el primer lanzamiento <b>marca</b> tu posicion (gratis), el siguiente
 * te <b>teletransporta</b> a ella. Fiel a los costes verificados del {@code kamui_tp}: el teletransporte cuesta
 * <b>20 chakra</b>, <b>+2 eye_strain</b> (exento con ojo eterno) y tiene <b>cooldown 300 ticks</b>
 * ({@code mangekyo_time 300}) <b>compartido</b> con {@link KamuiOthersAbility} via {@link KamuiWaypoints}.
 *
 * <p><b>Auto-gestiona su coste</b> (como {@link CopyJutsuAbility}): declara coste 0 al pipeline porque las dos
 * fases tienen costes distintos (marcar=gratis, volver=20), y cobra el teletransporte dentro de {@link #execute}.
 * Soporta cambio de dimension.
 */
public final class KamuiAbility implements EyeAbility {

    static final int TELEPORT_CHAKRA = 20;
    static final int TELEPORT_STRAIN = 2;
    static final int COOLDOWN_TICKS = 300; // mangekyo_time 300 (compartido con Kamui Others)

    @Override
    public int chakraCost() {
        return 0; // marcar es gratis; el teletransporte cobra 20 dentro de execute (dos fases, dos costes)
    }

    @Override
    public int strainCost() {
        return 0; // la fatiga (+2) la aplica el teletransporte dentro de execute
    }

    @Override
    public void execute(EyeAbilityContext context) {
        ServerPlayer caster = context.caster();
        UUID id = caster.getUUID();
        KamuiWaypoints.Waypoint waypoint = KamuiWaypoints.get(id);
        if (waypoint == null) {
            markLocation(caster, id);
        } else {
            teleport(caster, id, waypoint);
        }
    }

    /** Primer lanzamiento: marca la posicion actual (gratis). */
    private static void markLocation(ServerPlayer caster, UUID id) {
        KamuiWaypoints.set(id, new KamuiWaypoints.Waypoint(caster.level().dimension(), caster.position(),
                caster.getYRot(), caster.getXRot()));
        caster.sendSystemMessage(Component.translatable("ability.dojutsu.kamui.marked"));
        caster.level().playSound(null, caster.blockPosition(), SoundEvents.ENDERMAN_TELEPORT,
                SoundSource.PLAYERS, 0.6f, 1.4f);
    }

    /** Segundo lanzamiento: teletransporta al punto marcado, cobrando chakra/fatiga y con cooldown. */
    private static void teleport(ServerPlayer caster, UUID id, KamuiWaypoints.Waypoint waypoint) {
        long now = caster.serverLevel().getGameTime();
        if (KamuiWaypoints.onCooldown(id, now)) {
            caster.sendSystemMessage(Component.translatable("ability.dojutsu.kamui.cooldown"));
            return; // aun en enfriamiento: no gasta nada (el datapack lo tacha)
        }
        PlayerNinjaData data = NinjaData.get(caster);
        if (!ChakraService.consume(data, TELEPORT_CHAKRA)) {
            caster.sendSystemMessage(Component.translatable("ability.dojutsu.kamui.no_chakra"));
            return; // sin chakra: no castea (el datapack tampoco)
        }
        if (!data.eye().eternal()) {
            EyeStrainService.addStrain(data, TELEPORT_STRAIN); // unless wearing_eternal add eye_strain 2
        }
        ServerLevel destination = caster.server.getLevel(waypoint.dimension());
        if (destination != null) {
            KamuiEffects.poof(caster);
            caster.teleportTo(destination, waypoint.pos().x, waypoint.pos().y, waypoint.pos().z,
                    waypoint.yaw(), waypoint.pitch());
            KamuiEffects.poof(caster);
        }
        KamuiWaypoints.clear(id); // el punto se consume al volver (hay que remarcar)
        KamuiWaypoints.setReadyAt(id, now + COOLDOWN_TICKS);
    }

    /** Olvida el punto marcado y el cooldown de un jugador (al desconectar): no sobreviven a la sesion. */
    public static void forget(UUID playerId) {
        KamuiWaypoints.forget(playerId);
    }

    /** Efectos de teletransporte de Kamui (sonido + particulas), compartidos con {@link KamuiOthersAbility}. */
    static final class KamuiEffects {
        private KamuiEffects() {
        }

        static void poof(ServerPlayer at) {
            ServerLevel level = at.serverLevel();
            Vec3 pos = at.position();
            level.playSound(null, at.blockPosition(), SoundEvents.ENDERMAN_TELEPORT, SoundSource.PLAYERS, 0.8f, 1.0f);
            level.sendParticles(ParticleTypes.PORTAL, pos.x, pos.y + 1, pos.z, 40, 0.4, 0.8, 0.4, 0.3);
        }
    }
}
