package com.zamu.dojutsu.jutsu.effect;

import com.google.gson.JsonObject;
import com.zamu.dojutsu.domain.jutsu.JutsuDefinition;
import java.util.Map;
import java.util.Optional;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.GsonHelper;

/**
 * El <b>ancla visual</b> de un jutsu (F11/DJT-239): que se dibuja, donde y cuanto dura. Hermano de
 * {@link EffectSfx}, y por el mismo motivo — se declara en el JSON de la tecnica, no en un {@code switch} por
 * id dentro del codigo.
 *
 * <h2>El problema que resuelve</h2>
 * De las 128 tecnicas, <b>solo 23 dibujaban algo</b>: las de proyectil, porque son una entidad real volando.
 * Las <b>54 de area</b> no emitian ni una particula — un Katon que abrasa todo en 5 bloques funcionaba, hacia
 * dano, y visualmente no pasaba <b>nada</b>. Lo mismo con los 11 golpes cargados, los 7 buffs y las 6
 * curaciones. No era un defecto del porte: el datapack tenia el mismo agujero.
 *
 * <p>Sin un sitio donde colgar un visual, los modelos 3D que se hagan despues no tienen donde aparecer. Esto
 * es ese sitio.
 *
 * <h2>Dos capas</h2>
 * <ul>
 *   <li><b>Particulas</b>, que funcionan hoy y sin arte. Si la tecnica no declara ninguna se <b>deduce de su
 *       afinidad</b>: un jutsu de fuego echa llamas, uno de agua salpica. 109 de las 128 tecnicas declaran
 *       afinidad, asi que la inmensa mayoria pasa a verse sin escribir un solo dato nuevo.</li>
 *   <li><b>Un modelo</b>, opcional: el id de un item que se muestra en el mundo con un {@code ItemDisplay} de
 *       vanilla. Es el enganche para los modelos por tecnica que lleguen despues, y <b>no necesita un
 *       renderer propio</b> — importa, porque la regla 5 del repo obliga a dar renderer a toda EntityType
 *       registrada, y aqui no se registra ninguna: la entidad es de vanilla.</li>
 * </ul>
 *
 * @param particle      id de particula (texto, sin resolver); vacio = se deduce de la afinidad del jutsu
 * @param count         cuantas particulas
 * @param spread        radio de dispersion en bloques; 0 = usar el radio del propio efecto
 * @param speed         velocidad que vanilla da a la particula
 * @param model         id del item a mostrar como {@code ItemDisplay}, o vacio para no mostrar ninguno
 * @param modelScale    escala del modelo
 * @param modelOffsetY  altura sobre el punto de anclaje
 * @param lifetimeTicks cuanto vive el modelo antes de retirarse
 */
public record EffectVfx(Optional<String> particle, int count, double spread, double speed,
        Optional<String> model, float modelScale, double modelOffsetY, int lifetimeTicks) {

    /** Vida por defecto del modelo: 1,5 s. Suficiente para leerlo sin que se quede colgado en escena. */
    public static final int DEFAULT_LIFETIME_TICKS = 30;

    /** Sin nada declarado: se deducira la particula de la afinidad y no habra modelo. */
    public static final EffectVfx DEFAULT =
            new EffectVfx(Optional.empty(), 24, 0.0D, 0.05D, Optional.empty(), 1.0F, 1.0D,
                    DEFAULT_LIFETIME_TICKS);

    /**
     * Particula por afinidad, para que una tecnica sin datos de arte ya se vea.
     *
     * <p>Son elecciones de vanilla deliberadamente sobrias: el objetivo es <b>que se note que pasa algo</b>,
     * no imitar el anime. Cuando llegue el arte, cada tecnica declarara lo suyo y esto dejara de usarse.
     */
    private static final Map<String, String> BY_NATURE = Map.of(
            "fire", "minecraft:flame",
            "water", "minecraft:splash",
            "lightning", "minecraft:electric_spark",
            "wind", "minecraft:cloud",
            "earth", "minecraft:crit",
            "yin", "minecraft:smoke",
            "yang", "minecraft:end_rod",
            "yin_yang", "minecraft:portal");

    /** Lo que echa una tecnica sin afinidad (universal, kenjutsu, medica). */
    static final String NEUTRAL = "minecraft:enchant";

    /** Lee los campos {@code vfx*} del JSON del efecto. Todos opcionales: sin ninguno, {@link #DEFAULT}. */
    static EffectVfx parse(JsonObject json) {
        return new EffectVfx(
                Optional.ofNullable(GsonHelper.getAsString(json, "vfxParticle", null)),
                GsonHelper.getAsInt(json, "vfxCount", DEFAULT.count()),
                GsonHelper.getAsDouble(json, "vfxSpread", DEFAULT.spread()),
                GsonHelper.getAsDouble(json, "vfxSpeed", DEFAULT.speed()),
                Optional.ofNullable(GsonHelper.getAsString(json, "vfxModel", null)),
                GsonHelper.getAsFloat(json, "vfxModelScale", DEFAULT.modelScale()),
                GsonHelper.getAsDouble(json, "vfxModelOffsetY", DEFAULT.modelOffsetY()),
                GsonHelper.getAsInt(json, "vfxLifetimeTicks", DEFAULT.lifetimeTicks()));
    }

    /**
     * Particula a usar: la declarada, o la de la afinidad del jutsu.
     *
     * <p>Un id declarado que no exista cae a la deducida en vez de tumbar el cast — mismo criterio tolerante
     * que {@link EffectSfx} con los sonidos.
     */
    net.minecraft.core.particles.ParticleOptions particleFor(JutsuDefinition definition) {
        return resolve(particle.orElse(null))
                .orElseGet(() -> resolve(particleIdFor(definition)).orElse(ParticleTypes.ENCHANT));
    }

    /** Busca una particula registrada por id; vacio si el id es nulo, mal formado o no existe. */
    private static Optional<net.minecraft.core.particles.ParticleOptions> resolve(String id) {
        return Optional.ofNullable(id)
                .map(ResourceLocation::tryParse)
                .flatMap(net.minecraft.core.registries.BuiltInRegistries.PARTICLE_TYPE::getOptional)
                .filter(net.minecraft.core.particles.ParticleOptions.class::isInstance)
                .map(net.minecraft.core.particles.ParticleOptions.class::cast);
    }

    /**
     * <b>La decision</b>: que particula corresponde a este jutsu, como id de texto.
     *
     * <p>Devuelve texto y no una particula ya resuelta para que sea <b>pura</b> — sin registros de Minecraft
     * de por medio se puede probar con tests, que es la convencion de este repo. Resolverla es glue, y vive en
     * {@link #particleFor}.
     */
    String particleIdFor(JutsuDefinition definition) {
        if (particle.isPresent()) {
            return particle.get();
        }
        return definition.affinityRequirement()
                .map(requirement -> BY_NATURE.getOrDefault(requirement.natureId(), NEUTRAL))
                .orElse(NEUTRAL);
    }

    /** Radio efectivo de dispersion: el declarado, o el del propio efecto si no se declaro. */
    double spreadOr(double effectRadius) {
        return spread > 0 ? spread : Math.max(0.5D, effectRadius);
    }
}
