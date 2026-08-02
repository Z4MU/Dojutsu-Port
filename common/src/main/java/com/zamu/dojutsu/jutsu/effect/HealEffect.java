package com.zamu.dojutsu.jutsu.effect;

import com.google.gson.JsonObject;
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
            playHealSound(level, caster);
        });
    }

    /**
     * El {@code playsound medical_ninjutsu.healing} (F10/DJT-224). El datapack lo repite en los <b>seis</b>
     * {@code cast.mcfunction} medicos; aqui va una vez, en la familia que todos comparten.
     *
     * <p><b>El volumen no se copia literal.</b> El datapack pone {@code 10}, pero en Minecraft un volumen
     * mayor que 1 no suena mas fuerte: <b>solo amplia el alcance</b> (16 x volumen = 160 bloques). Lo que
     * acotaba de verdad quien lo oia era el selector {@code @a[distance=0..5]}. Con {@code 1.0} se obtiene el
     * mismo volumen percibido en la distancia que importa; copiar el 10 habria hecho que se oyera a 160
     * bloques a un ninja curandose.
     */
    private static void playHealSound(ServerLevel level, LivingEntity caster) {
        level.playSound(null, caster.getX(), caster.getY(), caster.getZ(),
                ModSounds.MEDICAL_HEALING.get(), SoundSource.PLAYERS, 1.0F, 1.0F);
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
