package com.zamu.dojutsu.data.state;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.Optional;

/**
 * Sub-estado del <b>chakra de los Seis Caminos</b> {@code [S08/S19]} (F9/DJT-210): el nodo donde convergen las
 * vias de alto nivel. Sub-estado enchufable de {@link com.zamu.dojutsu.data.PlayerNinjaData}.
 *
 * <h2>Value object propio, pero anidado en {@link SageState}</h2>
 * Lo conceden <b>tres subsistemas distintos</b> (eventos narrativos, sellado del Diez Colas, item), gatea cosas
 * que no son el modo sabio (orbes buscadores de la verdad, jutsus de los Seis Caminos) y tiene <b>vida propia</b>:
 * puede estar prestado. Por eso es un value object aparte y no tres campos sueltos.
 *
 * <p>Que <b>viva dentro</b> de {@link SageState} en vez de colgar del agregado no es diseño sino un techo:
 * {@code RecordCodecBuilder.group} admite <b>16 componentes</b> y {@code PlayerNinjaData} ya los agota. De los
 * sub-estados existentes el sabio es el anfitrion correcto — el datapack archiva estas funciones bajo
 * {@code sage/six_paths/} y {@code d.is_sage 2} es literalmente estado de sabio. Se lee por
 * {@code PlayerNinjaData.sixPaths()}, asi que ningun consumidor ve el anidamiento.
 *
 * <h2>Prestado no es lo mismo que concedido</h2>
 * {@link #loanSource} es la traduccion de {@code d.tmp_sp_chakra}: vacio = permanente, presente = lo presta esa
 * fuente (la bestia, en el unico caso real) y se retira al extraerla, igual que las naturalezas y KG prestados
 * de F3. El datapack lo barre en el mismo {@code natures/remove_tmp} que aquellos: el chakra de los Seis Caminos
 * <b>ya era</b> parte de su sistema de prestamos.
 *
 * <p>Value object <b>inmutable</b> (regla 10b): se reemplaza con withers.
 *
 * @param chakra     tiene el chakra de los Seis Caminos ({@code d.sp_chakra}), permanente o prestado
 * @param loanSource quien lo presta ({@code d.tmp_sp_chakra}); vacio = es suyo y no se le puede quitar
 * @param sageMode   tiene desbloqueado el Modo Sabio de los Seis Caminos ({@code d.is_sage 2})
 */
public record SixPathsState(boolean chakra, Optional<String> loanSource, boolean sageMode) {

    /** Ninja que no ha llegado a los Seis Caminos. */
    public static final SixPathsState DEFAULT = new SixPathsState(false, Optional.empty(), false);

    /** Codec del sub-estado; campos ausentes caen a {@link #DEFAULT}. */
    public static final Codec<SixPathsState> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.BOOL.optionalFieldOf("chakra", false).forGetter(SixPathsState::chakra),
            Codec.STRING.optionalFieldOf("loanSource").forGetter(SixPathsState::loanSource),
            Codec.BOOL.optionalFieldOf("sageMode", false).forGetter(SixPathsState::sageMode)
    ).apply(instance, SixPathsState::new));

    /** Normaliza: sin chakra no puede haber prestamo colgando (NBT corrupto no deja un prestamo huerfano). */
    public SixPathsState {
        if (!chakra) {
            loanSource = Optional.empty();
        }
    }

    /** Indica si lo tiene <b>prestado</b> y por tanto se le puede retirar. */
    public boolean isLoaned() {
        return chakra && loanSource.isPresent();
    }

    /** Indica si esa fuente concreta es quien se lo presta. */
    public boolean isLoanedBy(String source) {
        return loanSource.filter(source::equals).isPresent();
    }

    /**
     * Copia con el chakra concedido <b>en propiedad</b> ({@code obtain_six_paths_chakra}). Si lo tenia prestado,
     * pasa a ser suyo: el prestamo se consolida en vez de duplicarse.
     */
    public SixPathsState granted() {
        return new SixPathsState(true, Optional.empty(), sageMode);
    }

    /** Copia con el chakra <b>prestado</b> por esa fuente ({@code tmp_sp_chakra}). */
    public SixPathsState loanedBy(String source) {
        return new SixPathsState(true, Optional.of(source), sageMode);
    }

    /** Copia sin el chakra ({@code take_sp_chakra}); el Modo Sabio desbloqueado no se pierde. */
    public SixPathsState revoked() {
        return new SixPathsState(false, Optional.empty(), sageMode);
    }

    /** Copia con el Modo Sabio de los Seis Caminos desbloqueado ({@code d.is_sage 2}). */
    public SixPathsState withSageMode() {
        return new SixPathsState(chakra, loanSource, true);
    }
}
