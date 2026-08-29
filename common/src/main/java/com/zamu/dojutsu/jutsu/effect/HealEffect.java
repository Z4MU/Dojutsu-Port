package com.zamu.dojutsu.jutsu.effect;

import com.google.gson.JsonObject;
import com.zamu.dojutsu.core.NinjaSfx;
import com.zamu.dojutsu.jutsu.JutsuEntry;
import com.zamu.dojutsu.registry.ModSounds;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.GsonHelper;
import net.minecraft.world.entity.LivingEntity;

/**
 * Familia CURACION (F4/DJT-076). Piloto: {@code healing} — cura a la entidad viva <b>mas
 * cercana</b> en radio 3 (NO al caster: {@code tag=!d.caster}) con regeneracion 2s nivel 20;
 * "Nothing nearby" sin coste si no hay objetivo ({@code canApply}).
 *
 * <p>Parametros: {@code radius} (def. 3) · {@code effects} (lista de {@link MobEffectSpec};
 * el piloto usa regeneration 2s amplifier 20, el literal del datapack).
 *
 * <p>La puerta de <b>habilidad</b> {@code medical} del datapack (DJT-083) ya se aplica: {@code healing}
 * exige {@code medical >= 5} (el {@code >= 20} del datapack reescalado /4) y los jutsus medicos de DJT-109
 * (mystical_palm, wide_healing, regenerative_healing, creation_rebirth) usan esta misma familia con su gate.
 */
public final class HealEffect implements JutsuEffect {

    /** Parametros de la familia (puro y testeable). */
    public record Params(double radius, List<MobEffectSpec> effects) {

        public static Params parse(JsonObject json) {
            return new Params(
                    GsonHelper.getAsDouble(json, "radius", 3.0D),
                    MobEffectSpec.parseList(json, "effects"));
        }
    }

    @Override
    public boolean canApply(ServerLevel level, LivingEntity caster, JutsuEntry jutsu) {
        Params params = Params.parse(jutsu.effectParams().getAsJsonObject());
        return nearestTarget(level, caster, params).isPresent();
    }

    @Override
    public void apply(ServerLevel level, LivingEntity caster, JutsuEntry jutsu) {
        Params params = Params.parse(jutsu.effectParams().getAsJsonObject());
        nearestTarget(level, caster, params).ifPresent(target -> {
            for (MobEffectSpec spec : params.effects()) {
                spec.applyTo(target);
            }
            // Ancla visual (F11/DJT-239) sobre el OBJETIVO, no sobre el caster: en medicina lo que importa
            // ver es a quien estas curando.
            VfxService.playAt(level, jutsu, target, 1.0D);
            playHealSound(level, caster);
        });
    }

    /**
     * El {@code playsound medical_ninjutsu.healing} (F10/DJT-224). El datapack lo repite en los <b>seis</b>
     * {@code cast.mcfunction} medicos; aqui va una vez, en la familia que todos comparten.
     *
     * <p><b>Ahora si se copia el volumen literal</b> (F10/DJT-227). DJT-224 lo bajo a {@code 1.0} razonando
     * que un volumen mayor que 1 no suena mas fuerte sino que amplia el alcance, y que copiar el {@code 10}
     * habria hecho que se oyera a 160 bloques. Lo primero es cierto a medias y lo segundo ya no aplica:
     *
     * <ul>
     *   <li>El cliente <b>recorta la sonoridad a 1.0</b>, pero usa el volumen crudo para la
     *       <b>atenuacion por distancia</b>. Con {@code 10} la curacion suena plana en todo el radio; con
     *       {@code 1.0} se desvanece a los pocos bloques. No era el mismo sonido.</li>
     *   <li>El alcance ya no lo decide el volumen: {@link NinjaSfx} respeta el selector, asi que el 10 no
     *       puede escaparse a 160 bloques.</li>
     * </ul>
     *
     * <p><b>5 bloques</b>, que es lo que ponen cinco de los siete sitios. Los otros dos —{@code wide_healing}
     * y {@code creation_of_all_things}— usan 8; se unifica en 5 en vez de cablear una excepcion por jutsu en
     * una familia compartida, y la diferencia es quien lo oye entre el bloque 5 y el 8.
     */
    private static void playHealSound(ServerLevel level, LivingEntity caster) {
        NinjaSfx.at(level, caster.getX(), caster.getY(), caster.getZ(),
                ModSounds.MEDICAL_HEALING.get(), SoundSource.PLAYERS, 10.0F, 1.0F, 5.0D);
    }

    /** La entidad viva mas cercana al caster dentro del radio, excluyendo al propio caster. */
    private static Optional<LivingEntity> nearestTarget(ServerLevel level, LivingEntity caster, Params params) {
        return level.getEntitiesOfClass(LivingEntity.class,
                        caster.getBoundingBox().inflate(params.radius()),
                        // ArmorStand ES LivingEntity: el datapack los excluia via #dojutsu:ignore.
                        e -> e != caster && e.isAlive() && !e.isSpectator()
                                && !(e instanceof net.minecraft.world.entity.decoration.ArmorStand)).stream()
                .min(Comparator.comparingDouble(caster::distanceToSqr));
    }
}
