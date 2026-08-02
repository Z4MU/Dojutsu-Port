package com.zamu.dojutsu.sage;

import com.mojang.logging.LogUtils;
import com.zamu.dojutsu.DojutsuMod;
import com.zamu.dojutsu.core.PlayerTickSystem;
import com.zamu.dojutsu.core.TickPhase;
import com.zamu.dojutsu.data.NinjaData;
import com.zamu.dojutsu.data.PlayerNinjaData;
import com.zamu.dojutsu.data.state.SageState;
import com.zamu.dojutsu.domain.sage.NaturalEnergyTable;
import com.zamu.dojutsu.domain.sage.SageType;
import com.zamu.dojutsu.network.NinjaSync;
import com.zamu.dojutsu.progression.XpApi;
import java.util.Optional;
import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.phys.Vec3;
import org.slf4j.Logger;

/**
 * Reunion de <b>energia natural</b> {@code [S08]} (F6/DJT-135): estarse quieto acumula energia y, al llenarla,
 * te vuelves <b>sabio</b> del linaje que corresponda al bioma. Sistema por tick en {@link TickPhase#DOJUTSU}.
 *
 * <p><b>Quietud por umbral de velocidad, no por {@code sneak_time} (S08-R1):</b> el datapack marca al jugador
 * con un tag {@code d.moved} y cuenta {@code d.still_timer} mientras no lo tenga — un mecanismo fragil de
 * detectar. Aqui la quietud se mide directamente: el jugador esta quieto si su <b>velocidad</b> esta por debajo
 * de {@value #STILL_SPEED_THRESHOLD} bloques/tick. Moverse <b>reinicia</b> el acumulado, como el datapack.
 *
 * <p><b>Ritmo verificado</b> ({@link NaturalEnergyTable}): primero hay que asentarse
 * {@value NaturalEnergyTable#STILL_WAIT_TICKS} ticks; a partir de ahi se gana +1 de energia cada
 * {@code gatherTicks(nivel, aceite)} ticks (800 en nivel 0, 25 en nivel 5; la mitad con aceite de sapo), hasta
 * {@value NaturalEnergyTable#MAX_CHARGE}. Al llenarse, {@code become_sage}: +30 ninja_xp y el linaje del bioma.
 *
 * <p><b>Mapa bioma→linaje</b> (verificado en {@code become_sage.mcfunction}): pantano→Sapo, fortaleza→Serpiente,
 * bosque oscuro→Babosa. La fortaleza es una <b>estructura</b>, no un bioma, asi que aqui se aproxima por
 * <b>profundidad</b> (bajo tierra) — divergencia consciente, anotada.
 */
public final class NaturalEnergyChargingService implements PlayerTickSystem {

    private static final Logger LOGGER = LogUtils.getLogger();

    /** Velocidad (bloques/tick) por debajo de la cual se considera que el jugador esta quieto (S08-R1). */
    static final double STILL_SPEED_THRESHOLD = 0.01;

    /** Altura por debajo de la cual se considera "bajo tierra" (aproximacion de la fortaleza). */
    private static final int UNDERGROUND_Y = 40;

    /** XP al volverse sabio ({@code become_sage}: {@code ninja_xp += 30}). */
    private static final int BECOME_SAGE_XP = 30;

    @Override
    public TickPhase phase() {
        return TickPhase.DOJUTSU;
    }

    @Override
    public void tick(ServerPlayer player) {
        PlayerNinjaData data = NinjaData.get(player);
        SageState state = data.sage();
        if (state.fullyCharged() && state.sage()) {
            return; // ya es sabio y esta al maximo: nada que reunir (caso comun, sin trabajo por tick)
        }
        boolean still = isStill(player);
        SageState next = charge(state, still);
        if (next != state) {
            data.setSage(next);
        }
        if (!next.sage() && next.fullyCharged()) {
            becomeSage(player, data, next);
            return;
        }
        if (still && next.stillTicks() > NaturalEnergyTable.STILL_WAIT_TICKS) {
            ambience(player); // señal visual de que esta reuniendo
        }
    }

    /** El jugador esta quieto si su velocidad esta bajo el umbral (S08-R1: no {@code sneak_time}). */
    static boolean isStill(ServerPlayer player) {
        Vec3 velocity = player.getDeltaMovement();
        boolean slow = velocity.horizontalDistanceSqr() < STILL_SPEED_THRESHOLD * STILL_SPEED_THRESHOLD
                && Math.abs(velocity.y) < STILL_SPEED_THRESHOLD;
        return slow && player.onGround() && !player.isSprinting() && !player.isSwimming();
    }

    /**
     * Decision pura de la carga: moverse reinicia el acumulado; quieto lo suma y, pasada la espera previa, gana
     * energia cada {@code gatherTicks} ticks. Devuelve el <b>mismo estado</b> si no hay nada que cambiar.
     */
    static SageState charge(SageState state, boolean still) {
        if (!still) {
            return state.stillTicks() == 0 ? state : state.withStillTicks(0); // moverse interrumpe
        }
        if (state.fullyCharged()) {
            return state; // al maximo: seguir quieto no suma
        }
        int stillTicks = state.stillTicks() + 1;
        if (stillTicks <= NaturalEnergyTable.STILL_WAIT_TICKS) {
            return state.withStillTicks(stillTicks); // aun asentandose
        }
        int gathering = stillTicks - NaturalEnergyTable.STILL_WAIT_TICKS;
        if (gathering % NaturalEnergyTable.gatherTicks(state.level(), state.oiled()) != 0) {
            return state.withStillTicks(stillTicks); // entre pulsos
        }
        return state.withStillTicks(stillTicks)
                .withCharge(state.charge() + NaturalEnergyTable.CHARGE_PER_PULSE);
    }

    /** Al llenar la energia: se vuelve sabio del linaje del bioma, con su XP y aviso. */
    private static void becomeSage(ServerPlayer player, PlayerNinjaData data, SageState state) {
        SageType type = sageTypeFor(player);
        data.setSage(state.becomeSage(type));
        XpApi.addNinjaXp(player, BECOME_SAGE_XP);
        player.sendSystemMessage(Component.translatable("sage.dojutsu.became." + type.id())
                .withStyle(ChatFormatting.GOLD));
        ServerLevel level = player.serverLevel();
        level.playSound(null, player.blockPosition(), SoundEvents.BEACON_ACTIVATE, SoundSource.PLAYERS, 1.0f, 1.2f);
        Vec3 pos = player.position();
        level.sendParticles(ParticleTypes.HAPPY_VILLAGER, pos.x, pos.y + 1, pos.z, 60, 0.8, 1.0, 0.8, 0.1);
        NinjaSync.sendDelta(player);
        LOGGER.info("[{}] {} se volvio sabio de tipo {}.",
                DojutsuMod.MOD_NAME, player.getGameProfile().getName(), type.id());
    }

    /**
     * Linaje segun donde medita ({@code become_sage}): pantano→Sapo, bosque oscuro→Babosa, bajo tierra→Serpiente.
     * Sin coincidencia, Sapo (el linaje base del datapack). La <b>fortaleza</b> del original es una estructura;
     * se aproxima por profundidad para no depender del generador de estructuras (divergencia anotada).
     */
    private static SageType sageTypeFor(ServerPlayer player) {
        if (player.blockPosition().getY() < UNDERGROUND_Y) {
            return SageType.SNAKE; // in_stronghold ~ bajo tierra
        }
        return biomeId(player)
                .map(id -> {
                    if (id.contains("swamp") || id.contains("mangrove")) {
                        return SageType.TOAD;
                    }
                    if (id.contains("dark_forest")) {
                        return SageType.SLUG;
                    }
                    return SageType.TOAD;
                })
                .orElse(SageType.TOAD);
    }

    /** Id del bioma donde esta el jugador ({@code "minecraft:swamp"}), o vacio si no se puede resolver. */
    private static Optional<String> biomeId(ServerPlayer player) {
        var holder = player.serverLevel().getBiome(player.blockPosition());
        return holder.unwrapKey().map(key -> key.location().toString());
    }

    /** Particulas y sonido tenue mientras se reune energia (el titulo/actionbar del datapack). */
    private static void ambience(ServerPlayer player) {
        if (player.tickCount % 20 != 0) {
            return; // una vez por segundo, no cada tick
        }
        ServerLevel level = player.serverLevel();
        Vec3 pos = player.position();
        level.sendParticles(ParticleTypes.COMPOSTER, pos.x, pos.y + 1, pos.z, 4, 0.5, 0.8, 0.5, 0.01);
    }
}
