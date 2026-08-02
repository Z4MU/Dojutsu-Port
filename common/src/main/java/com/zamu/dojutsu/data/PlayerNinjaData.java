package com.zamu.dojutsu.data;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.zamu.dojutsu.data.state.ChakraState;
import com.zamu.dojutsu.data.state.ClanMembership;
import com.zamu.dojutsu.data.state.EmpowermentState;
import com.zamu.dojutsu.data.state.ItemFlags;
import com.zamu.dojutsu.data.state.JinchurikiState;
import com.zamu.dojutsu.data.state.EyeState;
import com.zamu.dojutsu.data.state.JutsuKnowledge;
import com.zamu.dojutsu.data.state.NatureProfile;
import com.zamu.dojutsu.data.state.OnboardingState;
import com.zamu.dojutsu.data.state.ProgressionState;
import com.zamu.dojutsu.data.state.SageState;
import com.zamu.dojutsu.data.state.SixPathsState;
import com.zamu.dojutsu.data.state.StoryState;
import com.zamu.dojutsu.data.state.SusanooState;
import com.zamu.dojutsu.data.state.ThrowingState;
import com.zamu.dojutsu.data.state.VisionState;
import java.util.Objects;

/**
 * Agregado raiz del estado por jugador (DDD): reemplaza los ~800 scoreboards del datapack por una unica
 * fuente de verdad, persistente (via {@code AttachmentService}) y versionada.
 *
 * <p><b>Empieza minimo</b> (evita el bloqueo de un monolito): hoy solo {@link #dataVersion()} y
 * {@link OnboardingState}. Cada fase posterior anade su sub-estado siguiendo el <b>patron de sub-estado
 * enchufable</b>:
 * <ol>
 *   <li>crear un value object inmutable con una constante {@code DEFAULT} y un {@code CODEC} de compound;</li>
 *   <li>anadir un campo privado aqui, con getter y una <b>puerta de escritura</b> (un setter/mutador — nunca
 *       campos publicos mutables);</li>
 *   <li>anadir una linea al {@link #CODEC} con {@code SubState.CODEC.optionalFieldOf("clave", SubState.DEFAULT)}.</li>
 * </ol>
 * {@code optionalFieldOf} hace el estado <b>tolerante</b>: los saves viejos sin ese campo toman el default y
 * los campos futuros desconocidos se ignoran, sin reescribir el contrato.
 *
 * <p><b>Versionado desde el dia 1:</b> {@link #CURRENT_DATA_VERSION} es el esquema actual; un NBT sin el
 * campo se lee como {@link #INITIAL_DATA_VERSION} (esquema previo al versionado) para que la cadena de
 * migracion (DJT-018) sepa que debe migrarlo. <b>Reset</b> = instancia nueva (DJT-026).
 *
 * <p>Puede usar tipos de Mojang/Minecraft ({@code Codec}, NBT): es capa {@code data}, no {@code domain}.
 */
public final class PlayerNinjaData {

    /** Esquema previo al versionado: valor asumido cuando el NBT no trae {@code dataVersion}. */
    public static final int INITIAL_DATA_VERSION = 0;

    /** Version del esquema que produce el codigo actual. */
    public static final int CURRENT_DATA_VERSION = 1;

    /**
     * Codec del agregado. Cada sub-estado entra con {@code optionalFieldOf(default)} (tolerancia). El
     * {@code dataVersion} ausente cae a {@link #INITIAL_DATA_VERSION} (save legado, a migrar).
     */
    public static final Codec<PlayerNinjaData> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.INT.optionalFieldOf("dataVersion", INITIAL_DATA_VERSION).forGetter(PlayerNinjaData::dataVersion),
            OnboardingState.CODEC.optionalFieldOf("onboarding", OnboardingState.DEFAULT).forGetter(PlayerNinjaData::onboarding),
            ChakraState.CODEC.optionalFieldOf("chakra", ChakraState.DEFAULT).forGetter(PlayerNinjaData::chakra),
            ProgressionState.CODEC.optionalFieldOf("progression", ProgressionState.DEFAULT).forGetter(PlayerNinjaData::progression),
            NatureProfile.CODEC.optionalFieldOf("natures", NatureProfile.DEFAULT).forGetter(PlayerNinjaData::natures),
            ClanMembership.CODEC.optionalFieldOf("clan", ClanMembership.DEFAULT).forGetter(PlayerNinjaData::clan),
            JutsuKnowledge.CODEC.optionalFieldOf("jutsu", JutsuKnowledge.DEFAULT).forGetter(PlayerNinjaData::jutsu),
            EyeState.CODEC.optionalFieldOf("eye", EyeState.DEFAULT).forGetter(PlayerNinjaData::eye),
            SusanooState.CODEC.optionalFieldOf("susanoo", SusanooState.DEFAULT).forGetter(PlayerNinjaData::susanoo),
            VisionState.CODEC.optionalFieldOf("vision", VisionState.DEFAULT).forGetter(PlayerNinjaData::vision),
            EmpowermentState.CODEC.optionalFieldOf("empowerment", EmpowermentState.DEFAULT).forGetter(PlayerNinjaData::empowerment),
            SageState.CODEC.optionalFieldOf("sage", SageState.DEFAULT).forGetter(PlayerNinjaData::sage),
            JinchurikiState.CODEC.optionalFieldOf("jinchuriki", JinchurikiState.DEFAULT).forGetter(PlayerNinjaData::jinchuriki),
            ItemFlags.CODEC.optionalFieldOf("items", ItemFlags.DEFAULT).forGetter(PlayerNinjaData::items),
            ThrowingState.CODEC.optionalFieldOf("throwing", ThrowingState.DEFAULT).forGetter(PlayerNinjaData::throwing),
            StoryState.CODEC.optionalFieldOf("story", StoryState.DEFAULT).forGetter(PlayerNinjaData::story)
    ).apply(instance, PlayerNinjaData::new));

    private int dataVersion;
    private OnboardingState onboarding;
    private ChakraState chakra;
    private ProgressionState progression;
    private NatureProfile natures;
    private ClanMembership clan;
    private JutsuKnowledge jutsu;
    private EyeState eye;
    private SusanooState susanoo;
    private VisionState vision;
    private EmpowermentState empowerment;
    private SageState sage;
    private JinchurikiState jinchuriki;
    private ItemFlags items;
    private ThrowingState throwing;
    private StoryState story;

    /** Seguimiento de campos observables cambiados para el delta sync (transitorio, no se serializa). */
    private final DirtyTracker dirtyTracker = new DirtyTracker();

    /** Crea el estado de un jugador nuevo, con defaults y en la version actual del esquema. */
    public PlayerNinjaData() {
        this(CURRENT_DATA_VERSION, OnboardingState.DEFAULT, ChakraState.DEFAULT, ProgressionState.DEFAULT,
                NatureProfile.DEFAULT, ClanMembership.DEFAULT, JutsuKnowledge.DEFAULT, EyeState.DEFAULT,
                SusanooState.DEFAULT, VisionState.DEFAULT, EmpowermentState.DEFAULT, SageState.DEFAULT,
                JinchurikiState.DEFAULT, ItemFlags.DEFAULT, ThrowingState.DEFAULT, StoryState.DEFAULT);
    }

    /** Constructor de reconstruccion (usado por el {@link #CODEC}). */
    private PlayerNinjaData(int dataVersion, OnboardingState onboarding, ChakraState chakra,
            ProgressionState progression, NatureProfile natures, ClanMembership clan, JutsuKnowledge jutsu,
            EyeState eye, SusanooState susanoo, VisionState vision, EmpowermentState empowerment, SageState sage,
            JinchurikiState jinchuriki, ItemFlags items, ThrowingState throwing, StoryState story) {
        this.dataVersion = dataVersion;
        this.onboarding = onboarding;
        this.chakra = chakra;
        this.progression = progression;
        this.natures = natures;
        this.clan = clan;
        this.jutsu = jutsu;
        this.eye = eye;
        this.susanoo = susanoo;
        this.vision = vision;
        this.empowerment = empowerment;
        this.sage = sage;
        this.jinchuriki = jinchuriki;
        this.items = items;
        this.throwing = throwing;
        this.story = story;
    }

    public int dataVersion() {
        return dataVersion;
    }

    public OnboardingState onboarding() {
        return onboarding;
    }

    public ChakraState chakra() {
        return chakra;
    }

    public ProgressionState progression() {
        return progression;
    }

    public NatureProfile natures() {
        return natures;
    }

    public ClanMembership clan() {
        return clan;
    }

    public JutsuKnowledge jutsu() {
        return jutsu;
    }

    public EyeState eye() {
        return eye;
    }

    /** Maestria del Susanoo (nivel dominado y uso acumulado) {@code [S24]}. */
    public SusanooState susanoo() {
        return susanoo;
    }

    /** Preferencias de vision ocular (chakra vision silenciada o no) {@code [S25]}. */
    public VisionState vision() {
        return vision;
    }

    /** Modos de potenciacion: sello maldito, Ocho Puertas y SOAH {@code [S10/S11/S12]} (F6). */
    public EmpowermentState empowerment() {
        return empowerment;
    }

    /** Modo sabio: linaje, nivel y energia natural reunida {@code [S08]} (F6). */
    public SageState sage() {
        return sage;
    }

    /** Bestia con cola sellada dentro del ninja {@code [S09]} (F7). */
    public JinchurikiState jinchuriki() {
        return jinchuriki;
    }

    /** Efectos de item que viven en el ninja: hoy, la mascara puesta {@code [S14]} (F8). */
    public ItemFlags items() {
        return items;
    }

    /** Armas arrojadizas: la marca del Vuelo del Dios del Trueno y la maestria {@code [S16]} (F8). */
    public ThrowingState throwing() {
        return throwing;
    }

    /** Eventos narrativos: el encuentro con tu Sabio y las mitades de su chakra {@code [S19]} (F9). */
    public StoryState story() {
        return story;
    }

    /**
     * Chakra de los Seis Caminos: el nodo donde convergen las vias de alto nivel {@code [S08/S19]} (F9/DJT-210).
     *
     * <p><b>Vive anidado en {@link SageState}</b>, no como sub-estado propio: el agregado ya esta en los
     * <b>16 componentes</b> que admite {@code RecordCodecBuilder.group}, que es un techo duro de DataFixerUpper.
     * De los sub-estados existentes el sabio es el anfitrion correcto — el datapack archiva estas funciones bajo
     * {@code sage/six_paths/} y {@code d.is_sage 2} es literalmente estado de sabio. Este atajo mantiene la
     * lectura igual de directa para quien lo consume.
     */
    public SixPathsState sixPaths() {
        return sage.sixPaths();
    }

    /** Seguimiento de campos observables cambiados (para el delta sync). Transitorio. */
    public DirtyTracker dirtyTracker() {
        return dirtyTracker;
    }

    // --- Puertas de escritura (una por campo; las llaman los servicios, no se exponen campos) ---

    /** Reemplaza el sub-estado de onboarding y lo marca como observable-sucio. */
    public void setOnboarding(OnboardingState onboarding) {
        this.onboarding = Objects.requireNonNull(onboarding, "onboarding");
        dirtyTracker.markDirty(ObservableField.ONBOARDING);
    }

    /**
     * Reemplaza el sub-estado de chakra. La llama {@code ChakraService}. Marca el campo como observable-sucio
     * <b>solo si cambia su proyeccion observable</b> (chakra/maxChakra, lo que ve el HUD): asi la regeneracion,
     * que actualiza el timer cada tick y el chakra natural (no observables), no genera deltas de red inutiles.
     */
    public void setChakra(ChakraState chakra) {
        Objects.requireNonNull(chakra, "chakra");
        if (this.chakra.chakra() != chakra.chakra() || this.chakra.maxChakra() != chakra.maxChakra()) {
            dirtyTracker.markDirty(ObservableField.CHAKRA);
        }
        this.chakra = chakra;
    }

    /**
     * Reemplaza el sub-estado de progresion y lo marca como observable-sucio. La llaman los servicios de
     * progresion (skill points, nivelacion, rango, unlocks).
     *
     * <p>A diferencia del chakra (que escribe un timer cada tick), la progresion solo cambia en eventos
     * discretos (gasto, XP, hito), asi que marcar siempre es correcto y no genera deltas por tick.
     */
    public void setProgression(ProgressionState progression) {
        this.progression = Objects.requireNonNull(progression, "progression");
        dirtyTracker.markDirty(ObservableField.PROGRESSION);
    }

    /**
     * Reemplaza el perfil elemental (naturalezas, afinidades, KG). La llamara {@code NatureService} (DJT-051),
     * la unica puerta prevista para el perfil.
     *
     * <p><b>No marca observable</b>: hoy ningun consumidor de cliente lee el perfil, y "solo lo observable cruza
     * la red" ({@code [ADD/07]}). Cuando el cliente lo necesite (DJT-060, pantalla de creacion), se anade
     * {@code ObservableField.NATURES} y su marcado aqui — cambio de una linea, igual que con la progresion.
     */
    public void setNatures(NatureProfile natures) {
        this.natures = Objects.requireNonNull(natures, "natures");
    }

    /**
     * Reemplaza la pertenencia a clan. La llamara {@code ClanService} (DJT-057), la unica puerta prevista.
     *
     * <p><b>No marca observable</b> todavia (mismo criterio que {@code setNatures}): el consumidor de cliente
     * llega con la pantalla de creacion (DJT-060), que anadira el campo observable que necesite.
     */
    public void setClan(ClanMembership clan) {
        this.clan = Objects.requireNonNull(clan, "clan");
    }

    /**
     * Reemplaza el conocimiento de jutsus. La llaman {@code JutsuLearningService} (aprendizaje), el
     * pipeline de cast (cooldowns, DJT-077) y el reset atomico.
     *
     * <p>Marca observable <b>solo si cambian los cooldowns</b> (lo que ve el HUD de cd, DJT-081): igual
     * que {@code setChakra}, un cambio no observable (aprender un jutsu, avanzar el progreso del learner)
     * no genera delta de red inutil. {@code known}/{@code learnProgress} no cruzan la red.
     */
    public void setJutsu(JutsuKnowledge jutsu) {
        Objects.requireNonNull(jutsu, "jutsu");
        if (!this.jutsu.cooldowns().equals(jutsu.cooldowns())) {
            dirtyTracker.markDirty(ObservableField.JUTSU_COOLDOWNS);
        }
        this.jutsu = jutsu;
    }

    /**
     * Reemplaza el estado de ojos (dojutsu). La llamaran los servicios de F5 (despertar/evolucion DJT-114/115,
     * strain DJT-116, menu DJT-117), la unica puerta prevista para {@link EyeState}.
     *
     * <p><b>No marca observable</b> todavia (mismo criterio que {@code setNatures}/{@code setClan}): en DJT-113
     * ningun consumidor de cliente lee el ojo. El menu ocular (DJT-117) y el HUD anadiran su
     * {@code ObservableField.EYE} y el marcado aqui cuando lo necesiten — cambio de una linea.
     */
    public void setEye(EyeState eye) {
        this.eye = Objects.requireNonNull(eye, "eye");
    }

    /**
     * Reemplaza el estado del Susanoo (maestria: nivel y uso). La llama {@code SusanooService} (DJT-120).
     *
     * <p><b>No marca observable</b> (mismo criterio que {@code setEye}): el nivel no cruza la red todavia; el
     * HUD del Susanoo (DJT-121) anadira su {@code ObservableField} cuando lo necesite.
     */
    public void setSusanoo(SusanooState susanoo) {
        this.susanoo = Objects.requireNonNull(susanoo, "susanoo");
    }

    /**
     * Reemplaza las preferencias de vision (chakra vision silenciada) {@code [S25]}. La llama
     * {@code ChakraVisionService} (DJT-125).
     *
     * <p><b>No marca observable:</b> la vision la aplica el servidor por el glow nativo; el cliente no lee este
     * estado.
     */
    public void setVision(VisionState vision) {
        this.vision = Objects.requireNonNull(vision, "vision");
    }

    /**
     * Reemplaza el estado de los modos de potenciacion (sello maldito, puertas, SOAH) {@code [S10/S11/S12]}.
     * La llaman los servicios de F6.
     *
     * <p><b>No marca observable</b> (mismo criterio que {@code setEye}/{@code setSusanoo}): ningun consumidor de
     * cliente lo lee todavia; el HUD de puertas/sello anadira su {@code ObservableField} cuando lo necesite.
     */
    public void setEmpowerment(EmpowermentState empowerment) {
        this.empowerment = Objects.requireNonNull(empowerment, "empowerment");
    }

    /**
     * Reemplaza el estado del modo sabio {@code [S08]}. La llaman {@code NaturalEnergyChargingService} (carga) y
     * {@code SageService} (F6/DJT-136).
     *
     * <p><b>No marca observable</b> (mismo criterio que los demas sub-estados de F5/F6): el HUD de energia
     * natural anadira su {@code ObservableField} cuando lo necesite — cambio de una linea.
     */
    public void setSage(SageState sage) {
        this.sage = Objects.requireNonNull(sage, "sage");
    }

    /**
     * Reemplaza el estado de jinchuriki {@code [S09]}. La llama {@code SealingService} (F7/DJT-165).
     *
     * <p><b>No marca observable</b> (mismo criterio que los demas sub-estados de F5/F6/F7): el chakra maximo que
     * presta la bestia si viaja al cliente, pero por su propio {@code ChakraState}.
     */
    public void setJinchuriki(JinchurikiState jinchuriki) {
        this.jinchuriki = Objects.requireNonNull(jinchuriki, "jinchuriki");
    }

    /**
     * Reemplaza los flags de item {@code [S14]}. La llama {@code MaskService} (F8/DJT-170).
     *
     * <p><b>No marca observable</b>: que llevas mascara lo ve el resto del mundo por el equipo del scoreboard
     * (nametag oculto), no por el sync del HUD.
     */
    public void setItems(ItemFlags items) {
        this.items = Objects.requireNonNull(items, "items");
    }

    /**
     * Reemplaza el estado de arrojadizos {@code [S16]}. La llama {@code FtgMarkerService} (F8/DJT-180).
     *
     * <p><b>No marca observable</b>: la marca no se dibuja en el HUD todavia; cuando lo haga, una linea.
     */
    public void setThrowing(ThrowingState throwing) {
        this.throwing = Objects.requireNonNull(throwing, "throwing");
    }

    /**
     * Reemplaza el estado narrativo {@code [S19]}. La llama {@code SageEncounterService} (F9/DJT-200).
     */
    public void setStory(StoryState story) {
        this.story = Objects.requireNonNull(story, "story");
    }

    /**
     * Reemplaza el estado de los Seis Caminos {@code [S08/S19]}. La llama {@code SixPathsService} (F9/DJT-210),
     * que es el <b>unico</b> sitio que lo concede o retira: las vias lo activan, no lo reimplementan.
     */
    public void setSixPaths(SixPathsState sixPaths) {
        this.sage = this.sage.withSixPaths(Objects.requireNonNull(sixPaths, "sixPaths"));
    }

    /** Fija la version del esquema. Uso previsto: la cadena de migracion (DJT-018). */
    public void setDataVersion(int dataVersion) {
        this.dataVersion = dataVersion;
    }

    @Override
    public String toString() {
        return "PlayerNinjaData[dataVersion=" + dataVersion + ", onboarding=" + onboarding
                + ", chakra=" + chakra + ", progression=" + progression + ", natures=" + natures
                + ", clan=" + clan + ", jutsu=" + jutsu + ", eye=" + eye + "]";
    }
}
