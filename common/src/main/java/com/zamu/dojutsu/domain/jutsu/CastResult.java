package com.zamu.dojutsu.domain.jutsu;

import java.util.Objects;
import java.util.Optional;

/**
 * Resultado de intentar lanzar una tecnica: exito, o fallo con una razon tipada
 * ({@link CastFailureReason}).
 *
 * <p>Distingue explicitamente el fallo por chakra vs. cooldown vs. afinidad, para que el consumidor
 * (HUD, actionbar, IA) reaccione sin reinterpretar strings. Dominio puro.
 */
public final class CastResult {

    private static final CastResult SUCCESS = new CastResult(null);

    /** {@code null} indica exito. */
    private final CastFailureReason failureReason;

    private CastResult(CastFailureReason failureReason) {
        this.failureReason = failureReason;
    }

    /** Resultado de exito (singleton). */
    public static CastResult success() {
        return SUCCESS;
    }

    /** Resultado de fallo con su razon. */
    public static CastResult failure(CastFailureReason reason) {
        return new CastResult(Objects.requireNonNull(reason, "reason"));
    }

    public boolean isSuccess() {
        return failureReason == null;
    }

    public boolean isFailure() {
        return failureReason != null;
    }

    /** La razon del fallo, o vacio si fue exito. */
    public Optional<CastFailureReason> failureReason() {
        return Optional.ofNullable(failureReason);
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof CastResult other && failureReason == other.failureReason;
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(failureReason);
    }

    @Override
    public String toString() {
        return isSuccess() ? "CastResult[success]" : "CastResult[failure=" + failureReason + "]";
    }
}
