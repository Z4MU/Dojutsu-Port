package com.zamu.dojutsu.dojutsu.ability;

import com.zamu.dojutsu.data.NinjaData;
import com.zamu.dojutsu.data.PlayerNinjaData;
import com.zamu.dojutsu.items.JutsuItem;
import com.zamu.dojutsu.jutsu.LastCastRegistry;
import java.util.Comparator;
import java.util.Optional;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;

/**
 * Copy Jutsu (Sharingan): el ojo que <b>copia la técnica que acaba de ver</b>. Es la habilidad más singular del
 * catálogo: no produce un efecto de combate, sino que <b>aprende un jutsu</b> — el único puente entre el sistema
 * de ojos (F5) y el de técnicas (F4). {@code [S05]}, {@code sharingan/copy_jutsu}.
 *
 * <p><b>No cuesta chakra ni fatiga</b> (verificado: {@code copy_jutsu.mcfunction} no cobra nada; el
 * {@code sharingan/cost} es el goteo pasivo del ojo). Su puerta real es <b>haber visto</b> un cast reciente y
 * tener la <b>mano vacía</b> ({@code unless entity @s[nbt={SelectedItem:{}}]} → si no, "Hand not empty").
 *
 * <p>El "haber visto" lo aporta {@link LastCastRegistry} (F4/DJT-079), construido precisamente como base de esta
 * copia: casts vivos, en la dimensión del observador y dentro del radio (el datapack usaba <b>20</b>). Eso
 * <b>corrige S06-R6</b>: el datapack marcaba {@code d.sharingan_seen} sin caducidad —el jutsu visto se recordaba
 * para siempre—; aquí el registro expira. Copiar entrega el jutsu como en {@code give/jutsu/<id>}: lo añade a los
 * conocidos y da su item.
 */
public final class CopyJutsuAbility implements EyeAbility {

    private static final int CHAKRA_COST = 0;
    private static final int STRAIN_COST = 0;      // el Sharingan base no añade eye_strain
    private static final int COOLDOWN_TICKS = 200; // sin cd propio en el datapack; acota el spam de la consulta
    private static final double SIGHT_RADIUS = 20.0;

    @Override
    public int chakraCost() {
        return CHAKRA_COST;
    }

    @Override
    public int strainCost() {
        return STRAIN_COST;
    }

    @Override
    public int cooldownTicks() {
        return COOLDOWN_TICKS;
    }

    @Override
    public void execute(EyeAbilityContext context) {
        ServerPlayer caster = context.caster();
        ServerLevel level = context.level();
        if (!caster.getMainHandItem().isEmpty()) {
            caster.sendSystemMessage(Component.translatable("ability.dojutsu.copy_jutsu.hand_not_empty"));
            return;
        }
        Optional<String> seen = nearestCopyableJutsu(caster, level);
        if (seen.isEmpty()) {
            caster.sendSystemMessage(Component.translatable("ability.dojutsu.copy_jutsu.nothing_seen"));
            return;
        }
        copy(caster, level, seen.get());
    }

    /** El jutsu del cast copiable más cercano al observador (vivo, en su dimensión, radio 20), o vacío. */
    private static Optional<String> nearestCopyableJutsu(ServerPlayer caster, ServerLevel level) {
        Vec3 pos = caster.position();
        return LastCastRegistry.recentCastsNear(level, pos, SIGHT_RADIUS, caster.getUUID()).stream()
                .min(Comparator.comparingDouble(record -> record.position().distanceToSqr(pos)))
                .map(LastCastRegistry.CastRecord::jutsuId);
    }

    private static void copy(ServerPlayer caster, ServerLevel level, String jutsuId) {
        PlayerNinjaData data = NinjaData.get(caster);
        Component jutsuName = Component.translatable("jutsu.dojutsu." + jutsuId);
        if (data.jutsu().knows(jutsuId)) {
            caster.sendSystemMessage(Component.translatable("ability.dojutsu.copy_jutsu.already_known", jutsuName));
            return;
        }
        data.setJutsu(data.jutsu().withKnown(jutsuId));
        ItemStack stack = JutsuItem.createStack(jutsuId); // equivale a give/jutsu/<id>
        if (!caster.getInventory().add(stack)) {
            caster.drop(stack, false);
        }
        caster.sendSystemMessage(Component.translatable("ability.dojutsu.copy_jutsu.copied", jutsuName));
        Vec3 pos = caster.position();
        level.sendParticles(ParticleTypes.ENCHANT, pos.x, pos.y + 1.5, pos.z, 30, 0.5, 0.5, 0.5, 0.5);
    }
}
