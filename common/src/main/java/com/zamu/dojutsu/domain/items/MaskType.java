package com.zamu.dojutsu.domain.items;

import java.util.Locale;
import java.util.Optional;

/**
 * Las seis <b>mascaras</b> {@code [S14]} (F8/DJT-170): tres ANBU y tres de Tobi. Verificadas en
 * {@code items/masks/*} y {@code give/masks/*} ({@code d.mask_id} 1..6).
 *
 * <p>Lo que hacen <b>aqui</b> es <b>ocultar tu nombre</b> ({@code team join MaskHidden}) y nada mas; se
 * distinguen solo por su aspecto, asi que son datos puros.
 *
 * <h2>En el datapack si dan armadura, y aqui no por una razon</h2>
 * Las seis del datapack llevan un {@code AttributeModifier} de {@code generic.armor} sobre el slot
 * {@code head}: <b>+2</b> las dos primeras de Tobi, <b>+3</b> la tercera y <b>+4</b> las tres ANBU. Ese
 * modificador solo cuenta <b>mientras el item ocupa el casco</b>, y el datapack lo ocupa
 * ({@code item replace entity @s armor.head}), lo que a cambio te <b>veta llevar casco de verdad</b>
 * ({@code masks/check_eye} aborta si el slot esta lleno).
 *
 * <p>Aqui la mascara es <b>estado, no casco</b> —decision explicita de DJT-170, que listaba "mascara en slot de
 * casco" entre los errores a evitar; ver {@code MaskService}—, asi que puedes llevar mascara <b>y</b> casco a la
 * vez. Arrastrar la armadura encima de eso no seria fidelidad sino un <b>buff</b>: +4 de armadura sin pagar el
 * slot es estrictamente mejor que el original. Si algun dia se quiere, es una decision de balance, no la
 * correccion de un olvido.
 *
 * <h2>Los ids van al reves de lo que parece</h2>
 * {@code d.mask_id} numera <b>Tobi del 1 al 3 y ANBU del 4 al 6</b> ({@code anbu_1} = 4 = "Anbu Mask (Itachi)"),
 * no al contrario. Verificado en tres sitios independientes: {@code items/masks/*} (que fija el score),
 * {@code give/masks/*} (que fija el {@code MaskId} del NBT) y {@code items/masks/unequip_give} (que reparte del
 * 1 al 6 empezando por Tobi).
 *
 * <p>El numero es <b>solo trazabilidad</b>: el unico sitio del datapack que mira el valor concreto es
 * {@code unequip_give}, para saber que mascara devolverte, y aqui ese reparto no existe porque <b>el tipo es el
 * item</b>. Lo demas ({@code byakugan/jougan/find_nearby}) solo pregunta {@code matches 1..}, o sea "lleva
 * mascara", sin importar cual.
 *
 * @param datapackId valor de {@code d.mask_id} (1..6); solo trazabilidad
 */
public enum MaskType {

    /** Mascara ANBU (variante 1); {@code give/masks/anbu_1}, "Anbu Mask (Itachi)". */
    ANBU_1(4),
    /** Mascara ANBU (variante 2); {@code give/masks/anbu_2}, "Anbu Mask (Kakashi)". */
    ANBU_2(5),
    /** Mascara ANBU (variante 3); {@code give/masks/anbu_3}, "Anbu Mask (Yamato)". */
    ANBU_3(6),
    /** Mascara de Tobi (espiral naranja). */
    TOBI_1(1),
    /** Mascara de Tobi (segunda). */
    TOBI_2(2),
    /** Mascara de Tobi (tercera). */
    TOBI_3(3);

    private final int datapackId;

    MaskType(int datapackId) {
        this.datapackId = datapackId;
    }

    /** Valor de {@code d.mask_id} (1..6). */
    public int datapackId() {
        return datapackId;
    }

    /** Id data-driven en minusculas ({@code "anbu_1"}, {@code "tobi_1"}). */
    public String id() {
        return name().toLowerCase(Locale.ROOT);
    }

    /** Resuelve una mascara por su id; vacio si no existe (estado tolerante). */
    public static Optional<MaskType> byId(String id) {
        for (MaskType mask : values()) {
            if (mask.id().equals(id)) {
                return Optional.of(mask);
            }
        }
        return Optional.empty();
    }
}
