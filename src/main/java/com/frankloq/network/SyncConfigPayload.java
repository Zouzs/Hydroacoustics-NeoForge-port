package com.frankloq.network;

import com.frankloq.Hydroacoustics;
import com.frankloq.client.HydroacousticsClient;
import com.frankloq.util.HydroConfig;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/**
 * Modern CustomPacketPayload for NeoForge 1.21.1 network communication.
 * Synchronizes server audio filter configuration to connected clients.
 */
public record SyncConfigPayload(
        float alpha,
        float gain,
        float gargle,
        float bubble,
        float dist
) implements CustomPacketPayload {

    public static final CustomPacketPayload.Type<SyncConfigPayload> TYPE =
            new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(Hydroacoustics.MOD_ID, "sync_config"));

    public static final StreamCodec<ByteBuf, SyncConfigPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.FLOAT, SyncConfigPayload::alpha,
            ByteBufCodecs.FLOAT, SyncConfigPayload::gain,
            ByteBufCodecs.FLOAT, SyncConfigPayload::gargle,
            ByteBufCodecs.FLOAT, SyncConfigPayload::bubble,
            ByteBufCodecs.FLOAT, SyncConfigPayload::dist,
            SyncConfigPayload::new
    );

    @Override
    public CustomPacketPayload.Type<SyncConfigPayload> type() {
        return TYPE;
    }

    public static void handleClient(SyncConfigPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            HydroConfig config = HydroacousticsClient.getConfig();
            if (config != null) {
                config.targetAlpha = payload.alpha();
                config.targetGain = payload.gain();
                config.targetGargle = payload.gargle();
                config.bubbleVolume = payload.bubble();
                config.voiceDistance = payload.dist();
                config.save();
                Hydroacoustics.LOGGER.info("[Hydroacoustics] Synchronized audio config from server.");
            }
        });
    }
}
