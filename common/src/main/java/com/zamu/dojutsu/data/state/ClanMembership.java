package com.zamu.dojutsu.data.state;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.Optional;

/**
 * Sub-estado de pertenencia a clan del ninja ({@code d.clan} del datapack, {@code [S04]}).
 *
 * <p>El clan se identifica por su <b>id data-driven</b> ({@code "uchiha"}), nunca por el indice numerico del
 * datapack ({@code d.clan = 2}): los clanes son contenido ({@code ClanRegistry}) y un pack puede anadir mas.
 * {@code Optional.empty()} = sin clan ({@code d.clan = 0}).
 *
 * <p>{@code initialized} = "los bonus del clan ya se aplicaron" ({@code [S04]}: el join solo actua
 * {@code if d.clan matches 0}). Es la guarda de <b>una sola vez</b> que usara {@code ClanService.join}
 * (DJT-057) para no re-aplicar bonus ni tras caminos raros (relogin a mitad, re-join).
 *
 * <p>{@code jutsuTime} es el contador del <b>desbloqueo progresivo</b> de jutsus de clan
 * ({@code d.<clan>_time} del datapack, DJT-061): ticks de juego acumulados hacia el siguiente jutsu. Lo avanza
 * {@code ClanJutsuUnlockService} (capado al umbral del clan); lo consumira la entrega real de F4.
 *
 * <p>Value object <b>inmutable</b> (patron de sub-estado enchufable de
 * {@link com.zamu.dojutsu.data.PlayerNinjaData}): {@link #DEFAULT} + {@link #CODEC} + withers.
 *
 * @param clan        id del clan al que pertenece, o vacio si no tiene
 * @param initialized si los bonus de ese clan ya se aplicaron (idempotencia del join)
 * @param jutsuTime   ticks acumulados hacia el siguiente jutsu de clan ({@code d.<clan>_time})
 */
public record ClanMembership(Optional<String> clan, boolean initialized, int jutsuTime) {

    /** Sin clan (el estado de un jugador nuevo; el onboarding de DJT-059 lo cambia). */
    public static final ClanMembership DEFAULT = new ClanMembership(Optional.empty(), false, 0);

    /** Codec del sub-estado (compound tolerante: campos ausentes -> sin clan). */
    public static final Codec<ClanMembership> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.STRING.optionalFieldOf("clan").forGetter(ClanMembership::clan),
            Codec.BOOL.optionalFieldOf("initialized", false).forGetter(ClanMembership::initialized),
            Codec.INT.optionalFieldOf("jutsuTime", 0).forGetter(ClanMembership::jutsuTime)
    ).apply(instance, ClanMembership::new));

    /** Sanea NBT corrupto: el contador nunca es negativo. */
    public ClanMembership {
        jutsuTime = Math.max(0, jutsuTime);
    }

    /** Indica si el ninja pertenece a algun clan. */
    public boolean hasClan() {
        return clan.isPresent();
    }

    /** Indica si pertenece a un clan concreto. */
    public boolean isIn(String clanId) {
        return clan.filter(clanId::equals).isPresent();
    }

    /** Copia perteneciendo a un clan, con los bonus marcados como aplicados y el contador a 0 (join, DJT-057). */
    public ClanMembership withClan(String clanId) {
        return new ClanMembership(Optional.of(clanId), true, 0);
    }

    /** Copia con otro contador de desbloqueo progresivo. La usa {@code ClanJutsuUnlockService} (DJT-061). */
    public ClanMembership withJutsuTime(int newJutsuTime) {
        return new ClanMembership(clan, initialized, newJutsuTime);
    }
}
