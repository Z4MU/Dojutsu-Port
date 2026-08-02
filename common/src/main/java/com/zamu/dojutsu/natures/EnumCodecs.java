package com.zamu.dojutsu.natures;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.zamu.dojutsu.data.state.Skill;
import com.zamu.dojutsu.domain.eye.EyeFamily;
import com.zamu.dojutsu.domain.nature.KekkeiGenkai;
import com.zamu.dojutsu.domain.nature.NatureType;
import com.zamu.dojutsu.domain.resource.RegenFactor;
import java.util.Locale;

/**
 * Codecs <b>estrictos</b> por nombre para los enums de dominio, compartidos por los JSON de contenido y por los
 * sub-estados: el perfil elemental de {@code natures} ({@link KekkeiGenkaiDefinition}, {@link NatureRollTable}),
 * los clanes y los ojos ({@link EyeFamily}, F5). Es el utilitario unico de codecs de enum del proyecto (por eso
 * viven aqui tambien {@link #SKILL} y {@link #REGEN_FACTOR}, que no son "elementales").
 *
 * <p><b>Estrictos a proposito:</b> un nombre desconocido <b>falla</b> el parseo. Es lo contrario de los codecs
 * de {@code data.state}, que descartan lo desconocido para que un NBT corrupto no impida jugar: aqui una
 * referencia rota esta en un archivo de <b>contenido</b> y debe verse en el log, no colarse en silencio como un
 * KG sin componentes o un tramo de RNG que no otorga nada.
 *
 * <p>Vive en su propia clase (y no como constantes de los records que los usan) por dos motivos: evita duplicar
 * el helper, y evita el <b>ciclo de inicializacion estatica</b> que aparece cuando un record y su record
 * anidado se referencian los codecs entre si (el que se inicialice primero veria {@code null}).
 *
 * <p>Clase de utilidad no instanciable.
 */
public final class EnumCodecs {

    /** Codec de {@link NatureType} por su nombre en minusculas ({@code "fire"}, {@code "yin_yang"}). */
    public static final Codec<NatureType> NATURE = byName(NatureType.values(), "naturaleza");

    /** Codec de {@link KekkeiGenkai} por su nombre en minusculas ({@code "wood"}, {@code "jugo_clan"}). */
    public static final Codec<KekkeiGenkai> KEKKEI_GENKAI = byName(KekkeiGenkai.values(), "kekkei genkai");

    /** Codec de {@link Skill} por su nombre en minusculas ({@code "taijutsu"}, {@code "cc"}). Lo usan los clanes. */
    public static final Codec<Skill> SKILL = byName(Skill.values(), "habilidad");

    /** Codec de {@link RegenFactor} por su nombre en minusculas ({@code "clan_otsutsuki"}). Lo usan los clanes. */
    public static final Codec<RegenFactor> REGEN_FACTOR = byName(RegenFactor.values(), "factor de regen");

    /** Codec de {@link EyeFamily} por su nombre en minusculas ({@code "sharingan"}, {@code "mangekyo"}). F5. */
    public static final Codec<EyeFamily> EYE_FAMILY = byName(EyeFamily.values(), "familia de ojo");

    private EnumCodecs() {
    }

    private static <E extends Enum<E>> Codec<E> byName(E[] values, String what) {
        return Codec.STRING.comapFlatMap(name -> {
            for (E value : values) {
                if (value.name().toLowerCase(Locale.ROOT).equals(name)) {
                    return DataResult.success(value);
                }
            }
            return DataResult.error(() -> what + " desconocida: " + name);
        }, value -> value.name().toLowerCase(Locale.ROOT));
    }
}
