package com.zamu.dojutsu.dojutsu.ability;

import com.zamu.dojutsu.core.NinjaSfx;
import com.zamu.dojutsu.registry.ModSounds;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import java.util.function.Supplier;

/**
 * Sonido de cada habilidad ocular al castearse (F10/DJT-227). Es <b>una tabla</b>, invocada desde el unico
 * punto de casteo ({@link EyeAbilityService#cast}), no una linea repetida en treinta clases.
 *
 * <h2>No es una regla por familia, y comprobarlo cambio el diseno</h2>
 * La tentacion era "cada ojo tiene su sonido y suena al usar cualquiera de sus habilidades". El datapack
 * <b>no</b> hace eso: el {@code cost.mcfunction} compartido del Mangekyo no toca nada, y el {@code playsound}
 * vive en el coste de <b>cada</b> tecnica. La diferencia se ve en los que se quedan callados —
 *
 * <ul>
 *   <li><b>Amaterasu</b> (Itachi) no suena. Su carpeta no tiene un solo {@code playsound}.</li>
 *   <li>Las <b>tres del Byakugan</b> tampoco: {@code dojutsu.byakugan.activate} sale unicamente en
 *       {@code awaken} y {@code evolve}, nunca al usar una tecnica.</li>
 *   <li>Las <b>dos del Tenseigan</b>, igual.</li>
 *   <li><b>Kamui</b> suena, pero con un sonido <b>vanilla</b> ({@code entity.enderman.teleport}), no con el del
 *       Mangekyo; va en su propia clase.</li>
 * </ul>
 *
 * <p>Haberlo puesto en el pipeline como regla de familia habria dado voz a cuatro tecnicas que el original
 * dejo mudas y habria pisado la de Obito. Se declara habilidad por habilidad porque asi esta declarado.
 *
 * <h2>Volumen y alcance salen del datapack, no de la intuicion</h2>
 * Las del Mangekyo van a <b>0.1</b> y las del Rinnegan a <b>0.5</b>, ambas en 10 bloques; el aturdimiento del
 * Sharingan va a <b>10 en 5 bloques</b>, que es otra cosa —fuerte y cerca— y por eso {@link NinjaSfx} respeta
 * el alcance del selector en vez de dejar que lo derive del volumen.
 *
 * <h2>La puerta solo la piden las del Mangekyo</h2>
 * Sus once llamadas llevan {@code unless predicate dojutsu:sfx_delay}; las del Rinnegan y las del Ketsuryugan
 * no. Se respeta: son las del Mangekyo las que se encadenan rapido.
 */
final class EyeAbilitySfx {

    /**
     * Un {@code playsound} del datapack.
     *
     * @param sound  evento
     * @param source canal ({@code player} o {@code master} en el original)
     * @param volume volumen literal
     * @param pitch  tono literal
     * @param range  alcance del selector, en bloques
     * @param gated  si la llamada iba tras {@code unless predicate dojutsu:sfx_delay}
     */
    private record Spec(Supplier<SoundEvent> sound, SoundSource source, float volume, float pitch, double range,
            boolean gated) {
    }

    private static final Map<String, Spec> BY_ABILITY = new HashMap<>();

    private EyeAbilitySfx() {
    }

    /**
     * La tabla, construida una vez. Los sonidos se guardan como <b>lambda</b> y no como
     * {@code ModSounds.X} directo: leer ese campo dispararia la inicializacion de {@link ModSounds}, que
     * necesita la plataforma y no existe en los tests de dominio. Asi la tabla se puede montar —y comprobar
     * quien suena y quien no— sin Minecraft delante.
     */
    private static Map<String, Spec> table() {
        if (!BY_ABILITY.isEmpty()) {
            return BY_ABILITY;
        }
        // Mangekyo: 0.1 en 10 bloques, con puerta. Ocho tecnicas con la misma linea en ocho ficheros.
        for (String id : new String[] {"tsukuyomi", "tengai_shinsei", "hinokagutsuchi", "kotoamatsukami",
                "beams_of_light", "chakra_blades", "phase_shift", "flame_control"}) {
            BY_ABILITY.put(id, new Spec(() -> ModSounds.MANGEKYO_ACTIVATE.get(), SoundSource.PLAYERS, 0.1F,
                    1.0F, NinjaSfx.RANGE_10, true));
        }
        // Rinnegan: 0.5 en 10 bloques, sin puerta (las cuatro porteadas de los Seis Caminos).
        for (String id : new String[] {"shinra_tensei", "asura_path", "preta_path", "chibaku_tensei"}) {
            BY_ABILITY.put(id, new Spec(() -> ModSounds.RINNEGAN_ACTIVATE.get(), SoundSource.PLAYERS, 0.5F,
                    1.0F, NinjaSfx.RANGE_10, false));
        }
        // Ketsuryugan: 0.5 en 10 bloques, sin puerta.
        for (String id : new String[] {"blood_eruption", "iron_blood_fist", "rising_blood_spear",
                "exploding_human"}) {
            BY_ABILITY.put(id, new Spec(() -> ModSounds.KETSURYUGAN_ACTIVATE.get(), SoundSource.PLAYERS, 0.5F,
                    1.0F, NinjaSfx.RANGE_10, false));
        }
        // Las tres con sonido propio.
        BY_ABILITY.put("bansho_tenin", new Spec(() -> ModSounds.BANSHO_TENIN.get(), SoundSource.PLAYERS, 0.5F,
                1.0F, NinjaSfx.RANGE_10, false));
        BY_ABILITY.put("amenotejikara", new Spec(() -> ModSounds.AMENOTEJIKARA.get(), SoundSource.PLAYERS,
                0.5F, 1.0F, 5.0D, false));
        // El aturdimiento es el unico que va por master y fuerte: sharingan/stun/summon.
        BY_ABILITY.put("genjutsu_stun", new Spec(() -> ModSounds.SHARINGAN_GENJUTSU.get(), SoundSource.MASTER,
                10.0F, 1.0F, 5.0D, false));
        return BY_ABILITY;
    }

    /** Toca el sonido de esa habilidad, si el datapack tocaba alguno. Llamado tras ejecutar el efecto. */
    static void play(ServerPlayer caster, String abilityId) {
        Optional.ofNullable(table().get(abilityId)).ifPresent(spec -> {
            if (spec.gated() && !NinjaSfx.pass(caster)) {
                return;
            }
            NinjaSfx.at(caster, spec.sound().get(), spec.source(), spec.volume(), spec.pitch(), spec.range());
        });
    }

    /** Si esa habilidad suena. Para poder comprobar la tabla sin mundo. */
    static boolean hasSound(String abilityId) {
        return table().containsKey(abilityId);
    }
}
