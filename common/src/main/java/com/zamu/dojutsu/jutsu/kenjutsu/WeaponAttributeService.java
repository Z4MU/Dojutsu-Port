package com.zamu.dojutsu.jutsu.kenjutsu;

import com.zamu.dojutsu.DojutsuMod;
import com.zamu.dojutsu.core.DomainEvents;
import com.zamu.dojutsu.data.NinjaData;
import com.zamu.dojutsu.data.state.Skill;
import com.zamu.dojutsu.domain.event.SkillLevelChangedEvent;
import com.zamu.dojutsu.items.WeaponComponent;
import com.zamu.dojutsu.registry.ModDataComponents;
import dev.architectury.utils.GameInstance;
import java.util.Locale;
import net.minecraft.core.component.DataComponents;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EquipmentSlotGroup;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemAttributeModifiers;

/**
 * Atributos de las armas ninja (F4/DJT-082, {@code [S07]}). Escribe el {@code ItemAttributeModifiers} vanilla
 * del arma — daño base + <b>proficiencia de kenjutsu</b> + penalizacion de velocidad — de modo que MC lo aplique
 * y <b>revierta solo</b> al equipar/desequipar.
 *
 * <p><b>Corrige S07-R2 (el "atributo pegado"):</b> el datapack aplicaba el modifier al <b>jugador</b> por tick
 * ({@code dmg_calc}) y lo quitaba con otra funcion ({@code remove_attributes}); si la retirada no corria, el
 * bonus se quedaba pegado. Aqui el modifier vive en el <b>item</b> con un id estable: reescribirlo lo reemplaza
 * (no acumula) y desequipar lo retira por construccion — imposible que se pegue.
 *
 * <p><b>Reactivo, no polling:</b> se recalculan las armas del inventario al <b>subir el nivel de kenjutsu</b>
 * ({@link SkillLevelChangedEvent}) y al <b>entrar</b> el jugador (cubre armas obtenidas offline). No hay barrido
 * por tick.
 *
 * <p><b>Escala (regla 16, divergencia documentada):</b> las bandas del datapack estan a {@code d.kenjutsu}
 * 400/800/1200/1600/2000 (+1..+5), pero el mod capa las habilidades a nivel 500 desde F1 ({@code XpCurve}). Se
 * reescalan ÷4 → 100/200/300/400/500, repartiendo los 5 tiers por todo el rango. La penalizacion de velocidad
 * −0.03 (mientras portas arma) desaparece al alcanzar el 4º tier (kenjutsu ≥ 400 en escala del mod = 1600 en el
 * datapack).
 */
public final class WeaponAttributeService {

    /** Id estable del modifier de daño del arma (reescribir reemplaza; nunca acumula). */
    private static final net.minecraft.resources.ResourceLocation DAMAGE_ID =
            DojutsuMod.id("weapon_damage");

    /** Id estable del modifier de la penalizacion de velocidad por portar arma. */
    private static final net.minecraft.resources.ResourceLocation SLOW_ID =
            DojutsuMod.id("weapon_slow");

    /** Penalizacion de velocidad al portar un arma sin dominio de kenjutsu ({@code [S07]}). */
    private static final double SLOW_AMOUNT = -0.03D;

    /** Nivel de kenjutsu (escala del mod, ÷4) que elimina la penalizacion de velocidad (1600 datapack). */
    private static final int SLOW_REMOVED_AT = 400;

    private WeaponAttributeService() {
    }

    /** Suscribe la actualizacion reactiva. Llamar una vez en el arranque. */
    public static void register() {
        DomainEvents.bus().subscribe(SkillLevelChangedEvent.class, event -> {
            if (!Skill.KENJUTSU.name().toLowerCase(Locale.ROOT).equals(event.skillId())) {
                return;
            }
            MinecraftServer server = GameInstance.getServer();
            if (server == null) {
                return;
            }
            ServerPlayer player = server.getPlayerList().getPlayer(event.player());
            if (player != null) {
                updateInventory(player);
            }
        });
    }

    /**
     * Bonus de daño por proficiencia de kenjutsu (F4/DJT-082). Puro y testeable. Bandas reescaladas ÷4 del
     * datapack: {@code <100 → +0; 100-199 → +1; 200-299 → +2; 300-399 → +3; 400-499 → +4; 500 → +5}.
     *
     * @param kenjutsuLevel nivel de kenjutsu en la escala del mod (0..500)
     */
    public static int proficiencyBonus(int kenjutsuLevel) {
        if (kenjutsuLevel >= 500) {
            return 5;
        }
        if (kenjutsuLevel < 100) {
            return 0;
        }
        return kenjutsuLevel / 100; // 100-199→1, 200-299→2, 300-399→3, 400-499→4
    }

    /** Indica si a este nivel de kenjutsu el arma aun impone la penalizacion de velocidad. */
    public static boolean hasSpeedPenalty(int kenjutsuLevel) {
        return kenjutsuLevel < SLOW_REMOVED_AT;
    }

    /**
     * Escribe en el stack el {@code ItemAttributeModifiers} correspondiente al nivel de kenjutsu dado: daño
     * (base + proficiencia) y, si no hay dominio, la penalizacion de velocidad. Solo actua sobre stacks con
     * {@link WeaponComponent}. Idempotente: reescribir con el mismo nivel produce el mismo resultado.
     */
    public static void applyModifiers(ItemStack stack, int kenjutsuLevel) {
        WeaponComponent weapon = stack.get(ModDataComponents.WEAPON.get());
        if (weapon == null) {
            return;
        }
        double damage = weapon.baseDamage() + proficiencyBonus(kenjutsuLevel);
        ItemAttributeModifiers.Builder builder = ItemAttributeModifiers.builder()
                .add(Attributes.ATTACK_DAMAGE,
                        new AttributeModifier(DAMAGE_ID, damage, AttributeModifier.Operation.ADD_VALUE),
                        EquipmentSlotGroup.MAINHAND);
        if (hasSpeedPenalty(kenjutsuLevel)) {
            builder.add(Attributes.MOVEMENT_SPEED,
                    new AttributeModifier(SLOW_ID, SLOW_AMOUNT, AttributeModifier.Operation.ADD_VALUE),
                    EquipmentSlotGroup.MAINHAND);
        }
        stack.set(DataComponents.ATTRIBUTE_MODIFIERS, builder.build());
    }

    /** Recalcula todas las armas del inventario del jugador (mochila + hotbar + off-hand) a su nivel actual. */
    public static void updateInventory(ServerPlayer player) {
        int level = NinjaData.get(player).progression().skill(Skill.KENJUTSU).level();
        for (ItemStack stack : player.getInventory().items) {
            applyModifiers(stack, level);
        }
        for (ItemStack stack : player.getInventory().offhand) {
            applyModifiers(stack, level);
        }
    }
}
