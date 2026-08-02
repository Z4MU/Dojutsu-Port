package com.zamu.dojutsu.items;

import com.mojang.logging.LogUtils;
import com.zamu.dojutsu.DojutsuMod;
import com.zamu.dojutsu.data.NinjaData;
import com.zamu.dojutsu.data.PlayerNinjaData;
import com.zamu.dojutsu.data.state.NatureProfile;
import com.zamu.dojutsu.data.state.Skill;
import com.zamu.dojutsu.domain.items.DnaTable;
import com.zamu.dojutsu.domain.items.DnaType;
import com.zamu.dojutsu.natures.KekkeiGenkaiService;
import com.zamu.dojutsu.natures.NatureService;
import java.util.Optional;
import java.util.Random;
import java.util.random.RandomGenerator;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import org.slf4j.Logger;

/**
 * Identificacion e implante de <b>ADN</b> {@code [S14]} (F8/DJT-170): el camino de Orochimaru.
 *
 * <h2>Implantar es una apuesta cara</h2>
 * Solo prende {@value DnaTable#SUCCESS_CHANCE_PERCENT}% de las veces ({@code dna_rng} 1..3, gana el 1) y fallar
 * <b>consume el ADN</b> ademas de dejarte con nausea, debilidad y veneno. Un ADN raro se puede perder tres veces
 * seguidas.
 *
 * <h2>Salvo dos veces, que cuentan la misma historia</h2>
 * El azar no manda si <b>tu cuerpo ya acepto ese linaje</b>: Madera con celulas de Hashirama dentro, o celulas de
 * Hashirama siendo del clan <b>Senju</b> —el de Hashirama—. Son las dos unicas excepciones del datapack y las dos
 * son narrativas, no de balance.
 *
 * <h2>Lo que ya tienes no se te cobra</h2>
 * Si el ADN concede algo que ya posees, {@code idna_alr} anula las dos etiquetas y marca {@code d.dont_take}: no
 * cuenta como fallo y <b>el item no se consume</b>. Un no-intento, no un intento perdido — que es justo lo que
 * evita que el jugador queme un ADN raro por despiste.
 *
 * <p><b>S14-R2 (alcance del ADN):</b> auditado. El implante solo puede conceder lo que declara {@link DnaType},
 * y todo pasa por {@code NatureService}/{@code KekkeiGenkaiService}, que ya son idempotentes y emiten sus
 * eventos. No hay ninguna via por la que el ADN otorgue algo fuera de ese catalogo.
 */
public final class DnaService {

    private static final Logger LOGGER = LogUtils.getLogger();

    private DnaService() {
    }

    /**
     * Identifica un ADN sin marcar: revela al azar cual de los 26 es ({@code dna/get_id}).
     *
     * @return el tipo revelado, o vacio si la medicina no alcanza
     */
    public static Optional<DnaType> identify(ServerPlayer player, RandomGenerator random) {
        PlayerNinjaData data = NinjaData.get(player);
        int medical = data.progression().skill(Skill.MEDICAL).level();
        if (!DnaTable.canIdentify(medical)) {
            reject(player, "dna.dojutsu.needs_medical_identify");
            return Optional.empty();
        }
        DnaType revealed = DnaTable.rollIdentification(random);
        player.sendSystemMessage(Component.translatable("dna.dojutsu.identified",
                Component.translatable("dna.dojutsu.type." + revealed.id()).withStyle(ChatFormatting.AQUA))
                .withStyle(ChatFormatting.GREEN));
        return Optional.of(revealed);
    }

    /** Azar real; comodidad para el item y el comando. */
    public static Optional<DnaType> identify(ServerPlayer player) {
        return identify(player, new Random());
    }

    /**
     * Implanta un ADN ya identificado.
     *
     * @return el resultado; solo {@link DnaTable.ImplantOutcome#SUCCESS} y {@code FAILURE} consumen el ADN
     */
    public static DnaTable.ImplantOutcome implant(ServerPlayer player, DnaType type, RandomGenerator random) {
        PlayerNinjaData data = NinjaData.get(player);
        int medical = data.progression().skill(Skill.MEDICAL).level();
        String clan = data.clan().clan().orElse(null);
        boolean hashirama = data.natures().hashiramaDna();

        DnaTable.ImplantOutcome outcome = DnaTable.attemptImplant(
                type, medical, alreadyOwned(data, type), hashirama, clan, random);
        switch (outcome) {
            case LOW_MEDICAL -> reject(player, "dna.dojutsu.needs_medical_implant");
            case ALREADY_OWNED -> player.sendSystemMessage(
                    Component.translatable("dna.dojutsu.already_owned").withStyle(ChatFormatting.YELLOW));
            case FAILURE -> fail(player);
            case SUCCESS -> succeed(player, data, type);
        }
        return outcome;
    }

    /** Azar real; comodidad para el item y el comando. */
    public static DnaTable.ImplantOutcome implant(ServerPlayer player, DnaType type) {
        return implant(player, type, new Random());
    }

    /** Indica si el jugador ya posee lo que ese ADN concede ({@code idna_alr}). */
    public static boolean alreadyOwned(PlayerNinjaData data, DnaType type) {
        NatureProfile profile = data.natures();
        if (type.isHashiramaCells()) {
            return profile.hashiramaDna();
        }
        return type.nature().map(profile::has)
                .orElseGet(() -> type.kekkeiGenkai().map(profile::has).orElse(false));
    }

    /** Implante fallido: el cuerpo lo rechaza ({@code implant_dna}). */
    private static void fail(ServerPlayer player) {
        player.sendSystemMessage(Component.translatable("dna.dojutsu.failed").withStyle(ChatFormatting.RED));
        player.addEffect(new MobEffectInstance(MobEffects.CONFUSION, 600, 2));  // nausea 30s amp 2
        player.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 600, 2));   // weakness 30s amp 2
        player.addEffect(new MobEffectInstance(MobEffects.POISON, 200, 1));     // poison 10s amp 1
    }

    /** Implante exitoso: concede lo que el ADN lleve dentro. */
    private static void succeed(ServerPlayer player, PlayerNinjaData data, DnaType type) {
        if (type.isHashiramaCells()) {
            data.setNatures(data.natures().withHashiramaDna(true));
        } else {
            type.nature().ifPresent(nature -> NatureService.grant(player, nature));
            type.kekkeiGenkai().ifPresent(kg -> KekkeiGenkaiService.grant(player, kg));
        }
        player.sendSystemMessage(Component.translatable("dna.dojutsu.implanted",
                Component.translatable("dna.dojutsu.type." + type.id()).withStyle(ChatFormatting.AQUA))
                .withStyle(ChatFormatting.GREEN));
        player.playNotifySound(SoundEvents.PLAYER_LEVELUP, SoundSource.PLAYERS, 0.4f, 1.4f);
        LOGGER.info("[{}] {} se implanto el ADN {}.",
                DojutsuMod.MOD_NAME, player.getGameProfile().getName(), type.id());
    }

    /** Rechazo por requisito ({@code no_cast_med}): aviso y yunque, como el datapack. */
    private static void reject(ServerPlayer player, String key) {
        player.sendSystemMessage(Component.translatable(key).withStyle(ChatFormatting.RED));
        player.playNotifySound(SoundEvents.ANVIL_LAND, SoundSource.PLAYERS, 0.3f, 1f);
    }
}
