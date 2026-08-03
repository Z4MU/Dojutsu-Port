package com.zamu.dojutsu.registry;

import com.zamu.dojutsu.DojutsuMod;
import com.zamu.dojutsu.platform.PlatformServices;
import com.zamu.dojutsu.platform.Registrar;
import com.zamu.dojutsu.platform.RegistryEntry;
import net.minecraft.core.registries.Registries;
import net.minecraft.sounds.SoundEvent;

/**
 * Registro de sonidos del mod (F10/DJT-224): los <b>28 eventos</b> del resource pack original.
 *
 * <p>Aqui se va el {@code ninja_debug} de F0: era un evento registrado <b>sin fichero de audio</b>, asi que
 * cada arranque del cliente imprimia {@code Missing sound for event: dojutsu:ninja_debug}. No lo llamaba nadie
 * y ya hay 28 sonidos de verdad; mantenerlo solo dejaba un aviso permanente en el log.
 *
 * <h2>Los nombres se conservan literales a proposito</h2>
 * Parecen redundantes bajo el namespace del mod ({@code dojutsu:dojutsu.byakugan.activate}), y la tentacion es
 * renombrarlos a algo como {@code eye.byakugan.activate}. No se hace: <b>el datapack los invoca por ese nombre
 * exacto</b> —{@code playsound dojutsu.rinnegan.activate} aparece 20 veces, {@code dojutsu.sharingan.mangekyo.activate}
 * 16— asi que mantenerlos deja la correspondencia 1:1 con las llamadas originales y con el {@code sounds.json}
 * del RP. Renombrar habria roto la unica pista que dice que sonido va donde.
 *
 * <h2>Correccion (F10/DJT-227): los 28 se usan, ninguno sobra</h2>
 * Esta ficha afirmaba que ocho eran "arte producido y nunca cableado". <b>Era falso</b>, y el fallo estaba en
 * como se busco: el datapack invoca buena parte de sus sonidos <b>con el namespace delante</b>
 * ({@code playsound minecraft:dojutsu.isshiki.reappear}), asi que un grep de {@code playsound dojutsu.} los
 * dejaba fuera. Contando bien salen <b>92 llamadas</b> y los <b>28</b> aparecen en alguna.
 *
 * <p>Los ocho tenian su sitio desde el principio: {@code susanoo.activate_1} en {@code susanoo/activate} y
 * {@code deactivate}; {@code jutsu.chidori} y {@code jutsu.rasengan} en el {@code ready} de sus golpes
 * cargados; {@code jutsu.rasenshuriken} en {@code rasenshuriken/throw}; {@code isshiki.reappear} en
 * {@code sukunahikona_self/end}; y {@code frog_call}, {@code white_rage_technique} y {@code ear_ringing} en
 * las invocaciones de sabio.
 *
 * <p>La leccion no es el grep: es que <b>un recuento equivocado se guarda como hecho</b> y deja de mirarse.
 * El inventario completo, sonido a sonido y con su estado, esta en {@code docs/sonido.md}.
 */
public final class ModSounds {

    private static final Registrar<SoundEvent> SOUNDS = PlatformServices.REGISTRAR.create(Registries.SOUND_EVENT);

    // --- Ojos (S05): activacion y tecnicas ------------------------------------------------------------------

    /** Byakugan al activarse. */
    public static final RegistryEntry<SoundEvent> BYAKUGAN_ACTIVATE = event("dojutsu.byakugan.activate");
    /** Jougan al activarse. */
    public static final RegistryEntry<SoundEvent> JOUGAN_ACTIVATE = event("dojutsu.jougan.activate");
    /** Tenseigan al activarse. */
    public static final RegistryEntry<SoundEvent> TENSEIGAN_ACTIVATE = event("dojutsu.tenseigan.activate");
    /** Ketsuryugan al activarse. */
    public static final RegistryEntry<SoundEvent> KETSURYUGAN_ACTIVATE = event("dojutsu.ketsuryugan.activate");
    /** Isshiki al activarse. */
    public static final RegistryEntry<SoundEvent> ISSHIKI_ACTIVATE = event("dojutsu.isshiki.activate");
    /** Isshiki al reaparecer (sin cablear en el datapack). */
    public static final RegistryEntry<SoundEvent> ISSHIKI_REAPPEAR = event("dojutsu.isshiki.reappear");
    /** Sharingan cegando a un objetivo. */
    public static final RegistryEntry<SoundEvent> SHARINGAN_BLIND = event("dojutsu.sharingan.blind");
    /** Genjutsu del Sharingan. */
    public static final RegistryEntry<SoundEvent> SHARINGAN_GENJUTSU = event("dojutsu.sharingan.genjutsu");
    /** Mangekyo al activarse (el mas usado del datapack tras el Rinnegan). */
    public static final RegistryEntry<SoundEvent> MANGEKYO_ACTIVATE = event("dojutsu.sharingan.mangekyo.activate");
    /** Susanoo al invocarse (sin cablear en el datapack). */
    public static final RegistryEntry<SoundEvent> SUSANOO_ACTIVATE =
            event("dojutsu.sharingan.mangekyo.susanoo.activate_1");
    /** Rinnegan al activarse. */
    public static final RegistryEntry<SoundEvent> RINNEGAN_ACTIVATE = event("dojutsu.rinnegan.activate");
    /** Amenotejikara (intercambio de posicion). */
    public static final RegistryEntry<SoundEvent> AMENOTEJIKARA = event("dojutsu.rinnegan.amenotejikara");
    /** Bansho Tenin (atraccion). */
    public static final RegistryEntry<SoundEvent> BANSHO_TENIN = event("dojutsu.rinnegan.bansho_tenin");
    /** Rinne Sharingan al activarse. */
    public static final RegistryEntry<SoundEvent> RINNE_SHARINGAN_ACTIVATE =
            event("dojutsu.rinne_sharingan.activate");
    /** Amenominaka (traslado dimensional). */
    public static final RegistryEntry<SoundEvent> AMENOMINAKA = event("dojutsu.rinne_sharingan.amenominaka");

    // --- Jutsus (S06) ---------------------------------------------------------------------------------------

    /** Naturaleza tierra. */
    public static final RegistryEntry<SoundEvent> JUTSU_EARTH = event("jutsu.earth");
    /** Naturaleza viento. */
    public static final RegistryEntry<SoundEvent> JUTSU_WIND = event("jutsu.wind");
    /** Naturaleza rayo. */
    public static final RegistryEntry<SoundEvent> JUTSU_LIGHTNING = event("jutsu.lightning");
    /** Mokuton (madera). */
    public static final RegistryEntry<SoundEvent> JUTSU_WOOD = event("jutsu.wood");
    /** Chidori (sin cablear en el datapack). */
    public static final RegistryEntry<SoundEvent> JUTSU_CHIDORI = event("jutsu.chidori");
    /** Rasengan (sin cablear en el datapack). */
    public static final RegistryEntry<SoundEvent> JUTSU_RASENGAN = event("jutsu.rasengan");
    /** Rasenshuriken (sin cablear en el datapack). */
    public static final RegistryEntry<SoundEvent> JUTSU_RASENSHURIKEN = event("jutsu.rasenshuriken");
    /** Invocacion. */
    public static final RegistryEntry<SoundEvent> JUTSU_SUMMONING = event("jutsu.summoning");
    /** Llamada de sapo (sin cablear en el datapack). */
    public static final RegistryEntry<SoundEvent> JUTSU_FROG_CALL = event("jutsu.frog_call");
    /** Cancion de flauta (genjutsu de Tayuya). */
    public static final RegistryEntry<SoundEvent> JUTSU_FLUTE_SONG = event("jutsu.flute_song");
    /** Tecnica de la Furia Blanca (sin cablear en el datapack). */
    public static final RegistryEntry<SoundEvent> JUTSU_WHITE_RAGE = event("jutsu.white_rage_technique");

    // --- Varios ---------------------------------------------------------------------------------------------

    /** Ninjutsu medico curando. */
    public static final RegistryEntry<SoundEvent> MEDICAL_HEALING = event("medical_ninjutsu.healing");
    /** Pitido de oidos (sin cablear en el datapack). */
    public static final RegistryEntry<SoundEvent> EAR_RINGING = event("misc.ear_ringing");

    private ModSounds() {
    }

    /** Declara un evento de rango variable con el id del mod; el fichero vive en {@code sounds.json}. */
    private static RegistryEntry<SoundEvent> event(String name) {
        return SOUNDS.register(DojutsuMod.id(name),
                () -> SoundEvent.createVariableRangeEvent(DojutsuMod.id(name)));
    }

    /** Confirma el registro. Llamar durante la inicializacion del mod. */
    public static void register() {
        SOUNDS.commit();
    }
}
