package com.zamu.dojutsu.domain.bijuu;

import java.util.EnumSet;
import java.util.OptionalInt;
import java.util.Set;

/**
 * Que Bestias con Cola tienen <b>cuerpo modelado</b> en el arte original (F10/DJT-228).
 *
 * <h2>El ravager no era una improvisacion del mod</h2>
 * Antes de este ticket, la ficha decia que las nueve bestias "toman prestada la malla del ravager" como apaño a
 * la espera de modelos propios. Lo que hace el datapack es <b>exactamente eso</b>:
 * {@code tailed_beasts/spawn/spawn_tb_entity} invoca un {@code ravager} de verdad —con 1024 de vida y 30 de
 * armadura— y le pone encima un {@code armor_stand} invisible que lleva un {@code knowledge_book} con
 * {@code CustomModelData} 14600..14609. La linea que haria invisible al ravager <b>esta comentada</b> en el
 * original, asi que el bicho se ve.
 *
 * <h2>Y de los diez modelos que declara, solo existen dos</h2>
 * El resource pack mapea los diez {@code CustomModelData} a {@code tailed_beast/1_tail} … {@code 10_tails},
 * pero <b>solo trae los ficheros de 8_tails y 9_tails</b>. Los otros ocho apuntan a modelos que no existen: en
 * el juego original, siete bestias y el Diez Colas se ven como un ravager con el cubo morado y negro de
 * "modelo ausente" flotando encima.
 *
 * <p>Por eso este catalogo tiene <b>dos entradas y no nueve</b>. Portar las siete que faltan no seria portar:
 * seria <b>autoria de arte nuevo</b>, que es otro trabajo y otra decision. Las que no estan aqui se siguen
 * dibujando con el ravager — que es lo que se ve en el datapack, sin el cubo de modelo ausente.
 */
public final class BeastArt {

    /**
     * Las dos que tienen cuerpo: Gyuki (965 cubos) y Kurama (1243), a 512x512. Son ademas las dos mas
     * reconocibles, lo que explica que fueran las primeras —y las unicas— que se llegaron a modelar.
     */
    private static final Set<TailedBeast> MODELLED =
            EnumSet.of(TailedBeast.EIGHT_TAILS, TailedBeast.NINE_TAILS);

    private BeastArt() {
    }

    /** Si esa bestia tiene cuerpo propio; {@code false} = se dibuja con el ravager, como en el original. */
    public static boolean hasBody(TailedBeast beast) {
        return MODELLED.contains(beast);
    }

    /**
     * El {@code CustomModelData} con el que su modelo cuelga del item-vehiculo. Se usa <b>el numero de colas</b>
     * y no el 146xx del datapack: aquel valor era global para todo el resource pack —convivia con doscientos
     * modelos mas colgados del mismo {@code knowledge_book}— y aqui el vehiculo es exclusivo de las bestias, asi
     * que el indice natural es el que ya las identifica.
     *
     * @return vacio si esa bestia no tiene cuerpo modelado
     */
    public static OptionalInt customModelData(TailedBeast beast) {
        return hasBody(beast) ? OptionalInt.of(beast.tails()) : OptionalInt.empty();
    }

    /** Cuantas bestias tienen cuerpo. Existe para que un test cuente sin enumerar. */
    public static int modelledCount() {
        return MODELLED.size();
    }
}
