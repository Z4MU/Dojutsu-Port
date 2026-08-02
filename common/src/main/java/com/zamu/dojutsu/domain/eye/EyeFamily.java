package com.zamu.dojutsu.domain.eye;

/**
 * Familia logica de un ojo (dojutsu) del datapack {@code [S05]}.
 *
 * <p><b>Colapso 33 -> logicos (R4):</b> el datapack modela <b>33 indices</b> {@code Eye:1..33} (un item NBT
 * en el slot de casco), pero 9 de ellos son duplicados <b>Eternal Mangekyo (EMS)</b> de los Mangekyo base
 * (mismo {@code main}, distinta textura/coste). En el mod eso es <b>un ojo + el flag {@code eternal}</b> del
 * estado ({@link com.zamu.dojutsu.data.state.EyeState}), no un ojo aparte. Igualmente, los tres indices de
 * Sharingan (1/2/3 tomoe) son <b>un</b> ojo Sharingan con nivel interno (via {@code useLevels}).
 *
 * <p>La <b>familia</b> agrupa los ojos por linaje/dispatch: cada Mangekyo por personaje (Obito, Itachi...) es
 * un {@link com.zamu.dojutsu.dojutsu.EyeDefinition} distinto porque su arsenal de habilidades difiere (Kamui vs.
 * Amaterasu/Tsukuyomi), pero todos comparten {@code family = MANGEKYO}. El {@code useLevels} del estado se lleva
 * por familia (el datapack usa {@code d.sharingan_use}, {@code d.byakugan_use}...): mismo contador para todo el
 * linaje.
 *
 * <p><b>Enum del dominio a proposito:</b> las familias son un conjunto cerrado del canon (a diferencia de los
 * {@code EyeDefinition}, que son contenido data-driven y un pack puede ampliar). Dominio puro: sin imports de
 * Minecraft; el codec por nombre en minusculas vive en {@code EnumCodecs} (capa que conoce {@code Codec}).
 */
public enum EyeFamily {

    /** Byakugan (Hyuga/Otsutsuki): vision, palmas de vacio; evoluciona a Tenseigan. {@code Eye:1}. */
    BYAKUGAN,

    /** Sharingan base 1/2/3 tomoe (Uchiha): copia y genjutsu; evoluciona a Mangekyo o Rinnegan. {@code Eye:2..4}. */
    SHARINGAN,

    /** Mangekyo Sharingan por personaje (Obito, Madara, Sasuke, Itachi, Indra, Shin, Shisui, Fugaku, custom). */
    MANGEKYO,

    /** Rinne Sharingan (Kaguya/Arbol Divino): nodo especial de maximo nivel. {@code Eye:10}. */
    RINNE_SHARINGAN,

    /** Rinnegan (y sus variantes): los Seis Caminos. {@code Eye:5,6,29,30,31}. */
    RINNEGAN,

    /** Tenseigan (Otsutsuki con Byakugan): modo chakra, orbes buscadores de verdad. {@code Eye:16}. */
    TENSEIGAN,

    /** Ketsuryugan (Chinoike): tecnicas de sangre. {@code Eye:17}. */
    KETSURYUGAN,

    /** Isshiki (Otsutsuki): Sukunahikona, Daikokuten. {@code Eye:32}. */
    ISSHIKI,

    /** Jougan (Otsutsuki puro): restriccion corporal, onda expansiva. {@code Eye:33}. */
    JOUGAN
}
