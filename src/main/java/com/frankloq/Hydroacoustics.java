package com.frankloq;

import com.frankloq.network.SyncConfigPayload;
import com.frankloq.util.HydroConfig;
import com.mojang.brigadier.arguments.FloatArgumentType;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Mod(Hydroacoustics.MOD_ID)
public class Hydroacoustics {
    public static final String MOD_ID = "hydroacoustics";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    // Modern DeferredRegister for SoundEvent on NeoForge 1.21.1
    public static final DeferredRegister<SoundEvent> SOUND_EVENTS =
            DeferredRegister.create(Registries.SOUND_EVENT, MOD_ID);

    public static final DeferredHolder<SoundEvent, SoundEvent> CUSTOM_BUBBLES = SOUND_EVENTS.register(
            "custom_bubbles",
            () -> SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath(MOD_ID, "custom_bubbles"))
    );

    // Master configuration on server
    public static HydroConfig serverConfig;

    public Hydroacoustics(IEventBus modEventBus) {
        LOGGER.info("[Hydroacoustics] Initializing Voice Chat Hydroacoustics for NeoForge 1.21.1...");

        // Register sounds with mod event bus
        SOUND_EVENTS.register(modEventBus);

        // Register custom network payload handlers with mod event bus
        modEventBus.addListener(this::registerPayloadHandlers);

        // Register game events with general NeoForge event bus
        NeoForge.EVENT_BUS.register(this);

        // Load server master configuration
        serverConfig = HydroConfig.load("server_master");
    }

    private void registerPayloadHandlers(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar registrar = event.registrar("1");
        registrar.playToClient(
                SyncConfigPayload.TYPE,
                SyncConfigPayload.STREAM_CODEC,
                SyncConfigPayload::handleClient
        );
        LOGGER.info("[Hydroacoustics] Registered network payload handler for {}", SyncConfigPayload.TYPE.id());
    }

    @SubscribeEvent
    public void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            sendSyncPacket(player);
        }
    }

    @SubscribeEvent
    public void onRegisterCommands(RegisterCommandsEvent event) {
        event.getDispatcher().register(
                Commands.literal("hydro")
                        .requires(source -> source.hasPermission(2))
                        .then(Commands.literal("mufflingStrength").then(Commands.argument("val", FloatArgumentType.floatArg(0.001f, 1.0f))
                                .executes(c -> updateAndSync(c.getSource(), "Muffling", FloatArgumentType.getFloat(c, "val"), 1))))
                        .then(Commands.literal("voiceVolume").then(Commands.argument("val", FloatArgumentType.floatArg(0.0f, 10.0f))
                                .executes(c -> updateAndSync(c.getSource(), "Voice Volume", FloatArgumentType.getFloat(c, "val"), 2))))
                        .then(Commands.literal("garglingEffect").then(Commands.argument("val", FloatArgumentType.floatArg(0.0f, 1.0f))
                                .executes(c -> updateAndSync(c.getSource(), "Gargle", FloatArgumentType.getFloat(c, "val"), 3))))
                        .then(Commands.literal("bubblingSound").then(Commands.argument("val", FloatArgumentType.floatArg(0.0f, 2.0f))
                                .executes(c -> updateAndSync(c.getSource(), "Bubble Vol", FloatArgumentType.getFloat(c, "val"), 4))))
                        .then(Commands.literal("voiceDistance").then(Commands.argument("val", FloatArgumentType.floatArg(1.0f, 100.0f))
                                .executes(c -> updateAndSync(c.getSource(), "Distance", FloatArgumentType.getFloat(c, "val"), 5))))
                        .then(Commands.literal("defaultValues")
                                .executes(context -> {
                                    serverConfig.reset();
                                    syncAllPlayers(context.getSource());
                                    context.getSource().sendSuccess(() -> Component.literal("§b[Hydroacoustics] §fServer values reset to default!"), false);
                                    return 1;
                                }))
        );
    }

    private int updateAndSync(CommandSourceStack source, String name, float val, int type) {
        if (type == 1) serverConfig.targetAlpha = val;
        if (type == 2) serverConfig.targetGain = val;
        if (type == 3) serverConfig.targetGargle = val;
        if (type == 4) serverConfig.bubbleVolume = val;
        if (type == 5) serverConfig.voiceDistance = val;
        serverConfig.save();
        syncAllPlayers(source);
        source.sendSuccess(() -> Component.literal("§b[Hydroacoustics] §fGlobal " + name + " set to: §a" + val), false);
        return 1;
    }

    private void syncAllPlayers(CommandSourceStack source) {
        for (ServerPlayer player : source.getServer().getPlayerList().getPlayers()) {
            sendSyncPacket(player);
        }
    }

    public static void sendSyncPacket(ServerPlayer player) {
        SyncConfigPayload payload = new SyncConfigPayload(
                serverConfig.targetAlpha,
                serverConfig.targetGain,
                serverConfig.targetGargle,
                serverConfig.bubbleVolume,
                serverConfig.voiceDistance
        );
        PacketDistributor.sendToPlayer(player, payload);
    }
}
