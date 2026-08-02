package com.zamu.dojutsu.data.state;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.zamu.dojutsu.domain.story.StoryTable;
import java.util.Optional;

/**
 * Sub-estado de los <b>eventos narrativos</b> {@code [S19]} (F9/DJT-200): el encuentro con tu Sabio y las mitades
 * de su chakra que ya tienes. Sub-estado enchufable de {@link com.zamu.dojutsu.data.PlayerNinjaData}.
 *
 * <h2>Un solo juego de campos, porque solo puede haber un sabio</h2>
 * El datapack lleva los dos por separado ({@code d.hagoromo*} y {@code d.hamura*}), pero los clanes de Hagoromo y
 * Hamura son <b>disjuntos</b>: ningun ninja puede encontrarse con los dos. Duplicar los campos seria mantener dos
 * mitades de las que una siempre esta vacia; aqui se guarda <b>cual</b> sabio es y un solo juego de progreso.
 *
 * <h2>Las dos mitades son acumulables</h2>
 * {@code yin} y {@code yang} son <b>banderas independientes</b>, no una eleccion excluyente:
 * {@code gain_other_half} permite recibir mas adelante la que falte. Guardarlo como un enum "camino elegido"
 * habria hecho imposible tener las dos.
 *
 * @param sage           id del sabio que le corresponde y con el que ya interactuo; vacio = aun ninguno
 * @param stage          etapa del encuentro: 0 sin conocerlo, 1 ya hablo, 2 ya eligio ({@code d.hagoromo})
 * @param failedAttempts intentos en los que el azar no salio ({@code d.hagoromo_try}); sube la probabilidad
 * @param yin            ya tiene la mitad Yin de su chakra ({@code d.hago_yin} / {@code d.hamu_yin})
 * @param yang           ya tiene la mitad Yang ({@code d.hago_yang} / {@code d.hamu_yang})
 */
public record StoryState(Optional<String> sage, int stage, int failedAttempts, boolean yin, boolean yang) {

    /** Ninja que aun no ha visto a nadie. */
    public static final StoryState DEFAULT = new StoryState(Optional.empty(), 0, 0, false, false);

    /** Codec del sub-estado; campos ausentes caen a {@link #DEFAULT}. */
    public static final Codec<StoryState> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.STRING.optionalFieldOf("sage").forGetter(StoryState::sage),
            Codec.INT.optionalFieldOf("stage", 0).forGetter(StoryState::stage),
            Codec.INT.optionalFieldOf("failedAttempts", 0).forGetter(StoryState::failedAttempts),
            Codec.BOOL.optionalFieldOf("yin", false).forGetter(StoryState::yin),
            Codec.BOOL.optionalFieldOf("yang", false).forGetter(StoryState::yang)
    ).apply(instance, StoryState::new));

    /** Normaliza: etapa y contador dentro de rango (NBT corrupto no rompe el tick). */
    public StoryState {
        stage = Math.min(Math.max(stage, 0), StoryTable.STAGE_DONE);
        failedAttempts = Math.max(failedAttempts, 0);
    }

    /**
     * Indica si el sabio ya agoto sus dos apariciones y no volvera ({@code hagoromo_2} veta el {@code try}).
     *
     * <p>Ojo: <b>no</b> significa "ya eligio". La etapa la sube el encuentro, no la eleccion — quien rechaza
     * vuelve a etapa cero y puede volver a encontrarselo.
     */
    public boolean isDone() {
        return stage >= StoryTable.STAGE_DONE;
    }

    /** Indica si ya tiene alguna mitad del chakra de su sabio. */
    public boolean hasAnyHalf() {
        return yin || yang;
    }

    /** Indica si ya tiene <b>las dos</b> mitades del chakra de su sabio. */
    public boolean hasBothHalves() {
        return yin && yang;
    }

    /** Copia contando un intento en el que el azar no salio (y con el la probabilidad sube). */
    public StoryState withFailedAttempt() {
        return new StoryState(sage, stage, StoryTable.nextAttempts(failedAttempts), yin, yang);
    }

    /**
     * Copia tras aparecerse el sabio ({@code start}): <b>sube la etapa</b> —es el encuentro el que la sube, no la
     * eleccion— y deja el contador de intentos como lo deja el datapack
     * ({@link StoryTable#attemptsAfterEncounter()}).
     */
    public StoryState withEncounter(String sageId) {
        return new StoryState(Optional.of(sageId), stage + 1, StoryTable.attemptsAfterEncounter(), yin, yang);
    }

    /** Copia con la mitad Yin concedida ({@code d.hago_yin 1}); la etapa no la toca la eleccion. */
    public StoryState withYin() {
        return new StoryState(sage, stage, failedAttempts, true, yang);
    }

    /** Copia con la mitad Yang concedida ({@code d.hago_yang 1}); la etapa no la toca la eleccion. */
    public StoryState withYang() {
        return new StoryState(sage, stage, failedAttempts, yin, true);
    }

    /**
     * Copia tras rechazar el camino: {@code no_path} pone {@code d.hagoromo 0}, o sea que <b>vuelve a la etapa
     * cero</b> y el sabio podra volver a ofrecerselo.
     */
    public StoryState withRefusal() {
        return new StoryState(sage, 0, failedAttempts, yin, yang);
    }
}
