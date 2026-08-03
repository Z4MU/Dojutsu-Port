package com.zamu.dojutsu.jutsu.effect;

import com.google.gson.JsonObject;
import com.zamu.dojutsu.jutsu.JutsuEntry;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.GsonHelper;
import net.minecraft.world.entity.LivingEntity;

/**
 * Familia GOLPE CARGADO (F4/DJT-085d, {@code [S06]}): al castear <b>arma</b> el golpe (no hace daño todavia); el
 * siguiente golpe cuerpo a cuerpo lo descarga ({@link PrimedStrikeManager}). Pilotos de rayo: lightning_bolt,
 * chidori, lightning_cutter, drilling_hand_of_sage_lightning ("Attack something to use Jutsu!").
 *
 * <p>Parametros ({@code effectParams}): {@code damage} (daño del golpe, def. 6) · {@code primingTicks} (ventana
 * para conectar el golpe en ticks, def. 30) · los de {@link EffectSfx} (F10/DJT-227).
 *
 * <p><b>El sonido suena al armar</b>, no al conectar el golpe: el datapack lo pone en {@code ready.mcfunction}.
 * Cinco de las once tecnicas de esta familia lo declaran; las otras seis iban mudas en el original y siguen
 * igual.
 */
public final class PrimedStrikeEffect implements JutsuEffect {

    /** Parametros de la familia (puro y testeable). */
    public record Params(float damage, int primingTicks, EffectSfx sfx) {

        public static Params parse(JsonObject json) {
            return new Params(
                    GsonHelper.getAsFloat(json, "damage", 6.0F),
                    GsonHelper.getAsInt(json, "primingTicks", 30),
                    EffectSfx.parse(json));
        }
    }

    @Override
    public void apply(ServerLevel level, LivingEntity caster, JutsuEntry jutsu) {
        if (!(caster instanceof ServerPlayer player)) {
            return; // el arma-y-golpea es de jugador; un caster IA lo tendra en F8
        }
        Params params = Params.parse(jutsu.effectParams().getAsJsonObject());
        PrimedStrikeManager.arm(player, jutsu.id(), params.damage(), params.primingTicks());
        params.sfx().play(level, player.getX(), player.getY(), player.getZ());
        player.displayClientMessage(Component.translatable("jutsu.dojutsu.primed"), true);
    }
}
