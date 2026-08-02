package com.zamu.dojutsu.dojutsu.ability;

import com.zamu.dojutsu.chakra.ChakraService;
import com.zamu.dojutsu.data.NinjaData;
import com.zamu.dojutsu.data.PlayerNinjaData;
import com.zamu.dojutsu.dojutsu.EyeStrainService;
import java.util.List;
import java.util.UUID;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;

/**
 * Kamui (Others) (Mangekyō de Obito): manda a las <b>entidades cercanas</b> al punto marcado de Kamui — el uso
 * ofensivo del jutsu espacio-tiempo (desterrar/reubicar a los enemigos). Segundo slot del Kamui de Obito, fiel a
 * {@code sharingan/mangekyo/obito/kamui_others}. {@code [S05]}.
 *
 * <p><b>Reutiliza la marca de {@link KamuiAbility}</b> (el mismo {@code d.kamui_tp}) via {@link KamuiWaypoints}:
 * hay que haber marcado un punto primero (con la habilidad Kamui). Teletransporta a las entidades vivas en
 * {@code distance 0..6} (excepto al lanzador) a ese punto.
 *
 * <p>Costes verificados ({@code kamui_others}): <b>20 chakra</b>, <b>+3 eye_strain</b> (una mas que el
 * teletransporte propio; exento con ojo eterno) y el <b>cooldown 300t compartido</b> de Kamui. Auto-gestiona su
 * coste dentro de {@link #execute} (contrato 0/0), como {@link KamuiAbility}.
 */
public final class KamuiOthersAbility implements EyeAbility {

    private static final int CHAKRA = KamuiAbility.TELEPORT_CHAKRA; // 20 (mismo que el propio)
    private static final int STRAIN = 3;   // kamui_others: +3 eye_strain (una mas que el propio)
    private static final double RADIUS = 6.0; // distance 0..6 del datapack

    @Override
    public int chakraCost() {
        return 0; // auto-gestionado dentro de execute (necesita marca + cooldown compartido)
    }

    @Override
    public int strainCost() {
        return 0;
    }

    @Override
    public void execute(EyeAbilityContext context) {
        ServerPlayer caster = context.caster();
        ServerLevel level = context.level();
        UUID id = caster.getUUID();
        KamuiWaypoints.Waypoint waypoint = KamuiWaypoints.get(id);
        if (waypoint == null) {
            caster.sendSystemMessage(Component.translatable("ability.dojutsu.kamui.no_mark"));
            return; // sin marca no hay a donde mandarlos (hay que usar Kamui primero)
        }
        if (waypoint.dimension() != level.dimension()) {
            // el destierro es local (como el tp @e ... @s del datapack, con la marca en el mismo mundo)
            caster.sendSystemMessage(Component.translatable("ability.dojutsu.kamui.wrong_dimension"));
            return;
        }
        long now = level.getGameTime();
        if (KamuiWaypoints.onCooldown(id, now)) {
            caster.sendSystemMessage(Component.translatable("ability.dojutsu.kamui.cooldown"));
            return;
        }
        List<LivingEntity> targets = EyeAbilityEffects.livingAround(level, caster, caster.position(), RADIUS);
        if (targets.isEmpty()) {
            caster.sendSystemMessage(Component.translatable("ability.dojutsu.no_target"));
            return; // nadie cerca: no gasta nada (fiel al espiritu de no_cast_nby)
        }
        PlayerNinjaData data = NinjaData.get(caster);
        if (!ChakraService.consume(data, CHAKRA)) {
            caster.sendSystemMessage(Component.translatable("ability.dojutsu.kamui.no_chakra"));
            return;
        }
        if (!data.eye().eternal()) {
            EyeStrainService.addStrain(data, STRAIN);
        }
        KamuiAbility.KamuiEffects.poof(caster);
        for (LivingEntity target : targets) {
            target.teleportTo(waypoint.pos().x, waypoint.pos().y, waypoint.pos().z);
        }
        KamuiWaypoints.setReadyAt(id, now + KamuiAbility.COOLDOWN_TICKS);
    }
}
