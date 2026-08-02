package com.zamu.dojutsu.network;

import com.zamu.dojutsu.DojutsuMod;
import com.zamu.dojutsu.data.state.OnboardingState;
import io.netty.buffer.ByteBuf;
import java.util.HashMap;
import java.util.Map;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/**
 * Payload Servidor -&gt; Cliente con el estado ninja <b>observable</b> del jugador.
 *
 * <p>{@code full=true} es un snapshot completo (se envia en join/respawn/cambio de dimension);
 * {@code full=false} es un delta (solo tras un cambio). Transporta el estado observable: la fase de
 * {@link OnboardingState} (F1), el chakra actual/maximo (F2, HUD) y la progresion observable (F2, menu/HUD).
 * Mientras haya pocos campos, el delta reenvia todo el observable; el delta por-campo se optimizara cuando
 * crezcan (deuda documentada).
 *
 * @param full        si es un snapshot completo ({@code true}) o un delta ({@code false})
 * @param onboarding  fase de onboarding observable
 * @param chakra      chakra normal actual (para el HUD)
 * @param maxChakra   chakra normal maximo (para el HUD)
 * @param progression proyeccion observable de la progresion (puntos, ninja_xp, niveles)
 * @param cooldowns   cooldowns de jutsu observables: {@code jutsuId -> tick de mundo disponible} (HUD de cd,
 *                    F4/DJT-081); el cliente calcula el restante con su propio tick de mundo
 */
public record SyncNinjaDataPayload(boolean full, OnboardingState.Phase onboarding, int chakra, int maxChakra,
                                   ProgressionSnapshot progression, Map<String, Long> cooldowns)
        implements CustomPacketPayload {

    public static final Type<SyncNinjaDataPayload> TYPE =
            new Type<>(DojutsuMod.id("sync_ninja_data"));

    private static final StreamCodec<ByteBuf, OnboardingState.Phase> PHASE_CODEC = ByteBufCodecs.VAR_INT.map(
            ordinal -> {
                OnboardingState.Phase[] values = OnboardingState.Phase.values();
                return (ordinal >= 0 && ordinal < values.length) ? values[ordinal] : OnboardingState.Phase.NOT_STARTED;
            },
            phase -> phase.ordinal());

    private static final StreamCodec<ByteBuf, Map<String, Long>> COOLDOWNS_CODEC =
            ByteBufCodecs.map(HashMap::new, ByteBufCodecs.STRING_UTF8, ByteBufCodecs.VAR_LONG);

    public static final StreamCodec<RegistryFriendlyByteBuf, SyncNinjaDataPayload> CODEC = StreamCodec.composite(
            ByteBufCodecs.BOOL, SyncNinjaDataPayload::full,
            PHASE_CODEC, SyncNinjaDataPayload::onboarding,
            ByteBufCodecs.VAR_INT, SyncNinjaDataPayload::chakra,
            ByteBufCodecs.VAR_INT, SyncNinjaDataPayload::maxChakra,
            ProgressionSnapshot.STREAM_CODEC, SyncNinjaDataPayload::progression,
            COOLDOWNS_CODEC, SyncNinjaDataPayload::cooldowns,
            SyncNinjaDataPayload::new);

    public SyncNinjaDataPayload {
        cooldowns = Map.copyOf(cooldowns);
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
