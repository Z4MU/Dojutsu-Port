package com.zamu.dojutsu.data.state;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.Optional;
import net.minecraft.core.GlobalPos;

/**
 * Sub-estado de <b>armas arrojadizas</b> {@code [S16]} (F8/DJT-180): la marca del Vuelo del Dios del Trueno y la
 * maestria de la tecnica. Sub-estado enchufable de {@link com.zamu.dojutsu.data.PlayerNinjaData}.
 *
 * <h2>La marca vive aqui, y eso resuelve S16 entero</h2>
 * En el datapack la marca <b>es el propio armor stand clavado</b>, asi que depende del chunk. Y como un chunk
 * descargado se lleva la marca por delante, el autor lo parchea con <b>{@code forceload add}</b>:
 * {@code throwing/move} fuerza la carga del chunk donde se clava el kunai y {@code teleport}/{@code reset} lo
 * liberan. Funciona, pero significa que cada marca <b>mantiene un chunk cargado indefinidamente</b> — y si algo
 * mata el kunai por otra via, el forceload se queda huerfano para siempre.
 *
 * <p>Aqui la marca es <b>coordenadas en el estado del jugador</b> ({@link GlobalPos}: dimension + posicion), que
 * viaja en el Attachment. No depende de ningun chunk, funciona entre dimensiones por construccion, y no hay nada
 * que liberar. El kunai clavado pasa a ser solo la <b>parte visible</b> de la marca, no la marca.
 *
 * <h2>Una sola marca, y no por simplificar</h2>
 * Es un {@code Optional}, no una lista, porque el datapack permite <b>exactamente una</b>: {@code reset} mata la
 * anterior en cuanto lanzas otro kunai. Y teletransportarse la consume.
 *
 * <p>Value object <b>inmutable</b> (regla 10b): se reemplaza con withers.
 *
 * @param ftgMark  posicion y dimension de la marca activa; vacio = no hay nada marcado
 * @param ftgLevel nivel de la tecnica 0..30 ({@code d.ftg_lvl}); abarata el salto
 * @param ftgXp    usos acumulados hacia el siguiente nivel ({@code d.ftg_xp})
 */
public record ThrowingState(Optional<GlobalPos> ftgMark, int ftgLevel, int ftgXp) {

    /** Ninja sin nada marcado y sin practica. */
    public static final ThrowingState DEFAULT = new ThrowingState(Optional.empty(), 0, 0);

    /** Codec del sub-estado; campos ausentes caen a {@link #DEFAULT}. */
    public static final Codec<ThrowingState> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            GlobalPos.CODEC.optionalFieldOf("ftgMark").forGetter(ThrowingState::ftgMark),
            Codec.INT.optionalFieldOf("ftgLevel", 0).forGetter(ThrowingState::ftgLevel),
            Codec.INT.optionalFieldOf("ftgXp", 0).forGetter(ThrowingState::ftgXp)
    ).apply(instance, ThrowingState::new));

    /** Normaliza: nivel dentro de rango y XP no negativa (NBT corrupto no rompe el tick). */
    public ThrowingState {
        ftgLevel = Math.min(Math.max(ftgLevel, 0), com.zamu.dojutsu.domain.throwing.FtgTable.MAX_LEVEL);
        ftgXp = Math.max(ftgXp, 0);
    }

    /** Indica si tiene algo marcado. */
    public boolean hasMark() {
        return ftgMark.isPresent();
    }

    /** Copia con una marca nueva (reemplaza la anterior: solo puede haber una). */
    public ThrowingState withMark(GlobalPos mark) {
        return new ThrowingState(Optional.of(mark), ftgLevel, ftgXp);
    }

    /** Copia sin marca (al saltar, que la consume, o al lanzar otro kunai). */
    public ThrowingState withoutMark() {
        return new ThrowingState(Optional.empty(), ftgLevel, ftgXp);
    }

    /** Copia con otra XP de tecnica. */
    public ThrowingState withFtgXp(int xp) {
        return new ThrowingState(ftgMark, ftgLevel, xp);
    }

    /** Copia subiendo un nivel de tecnica y descontando su coste en XP. */
    public ThrowingState withFtgLevelUp() {
        return new ThrowingState(ftgMark, ftgLevel + 1,
                Math.max(ftgXp - com.zamu.dojutsu.domain.throwing.FtgTable.XP_PER_LEVEL, 0));
    }
}
