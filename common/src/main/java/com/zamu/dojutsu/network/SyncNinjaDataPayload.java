package com.zamu.dojutsu.network;

import com.zamu.dojutsu.DojutsuMod;
import com.zamu.dojutsu.data.state.OnboardingState;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/**
 * Payload Servidor -&gt; Cliente con el estado ninja <b>observable</b> del jugador.
 *
 * <p>{@code full=true} es un snapshot completo (se envia en join/respawn/cambio de dimension);
 * {@code full=false} es un delta (solo tras un cambio). Transporta el estado observable: la fase de
 * {@link OnboardingState} (F1), el chakra actual/maximo (F2, HUD), la progresion observable (F2, menu/HUD)
 * y el conocimiento de jutsus (F4 enfriamientos + F11 aprendidos, ver {@link JutsuSnapshot}).
 * Mientras haya pocos campos, el delta reenvia todo el observable; el delta por-campo se optimizara cuando
 * crezcan (deuda documentada). La <b>excepcion</b> ya existe: la lista de jutsus aprendidos viaja solo cuando
 * cambia, porque es la unica lo bastante grande para que reenviarla duela ({@link JutsuSnapshot#known()}).
 *
 * <p><b>Nota para quien anada campos:</b> {@code StreamCodec.composite} admite <b>seis</b> componentes y los
 * seis estan usados. Lo siguiente que haya que sincronizar va <b>dentro</b> del snapshot que le corresponda
 * ({@link ProgressionSnapshot}, {@link JutsuSnapshot}) o en uno nuevo, no como septimo componente.
 *
 * @param full        si es un snapshot completo ({@code true}) o un delta ({@code false})
 * @param onboarding  fase de onboarding observable
 * @param chakra      chakra normal actual (para el HUD)
 * @param maxChakra   chakra normal maximo (para el HUD)
 * @param progression proyeccion observable de la progresion (puntos, ninja_xp, niveles)
 * @param jutsu       proyeccion observable de los jutsus: enfriamientos (F4/DJT-081) y aprendidos
 *                    (F11/DJT-231). Ver {@link JutsuSnapshot}
 */
public record SyncNinjaDataPayload(boolean full, OnboardingState.Phase onboarding, int chakra, int maxChakra,
                                   ProgressionSnapshot progression, JutsuSnapshot jutsu)
        implements CustomPacketPayload {

    public static final Type<SyncNinjaDataPayload> TYPE =
            new Type<>(DojutsuMod.id("sync_ninja_data"));

    private static final StreamCodec<ByteBuf, OnboardingState.Phase> PHASE_CODEC = ByteBufCodecs.VAR_INT.map(
            ordinal -> {
                OnboardingState.Phase[] values = OnboardingState.Phase.values();
                return (ordinal >= 0 && ordinal < values.length) ? values[ordinal] : OnboardingState.Phase.NOT_STARTED;
            },
            phase -> phase.ordinal());

    public static final StreamCodec<RegistryFriendlyByteBuf, SyncNinjaDataPayload> CODEC = StreamCodec.composite(
            ByteBufCodecs.BOOL, SyncNinjaDataPayload::full,
            PHASE_CODEC, SyncNinjaDataPayload::onboarding,
            ByteBufCodecs.VAR_INT, SyncNinjaDataPayload::chakra,
            ByteBufCodecs.VAR_INT, SyncNinjaDataPayload::maxChakra,
            ProgressionSnapshot.STREAM_CODEC, SyncNinjaDataPayload::progression,
            JutsuSnapshot.STREAM_CODEC, SyncNinjaDataPayload::jutsu,
            SyncNinjaDataPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
