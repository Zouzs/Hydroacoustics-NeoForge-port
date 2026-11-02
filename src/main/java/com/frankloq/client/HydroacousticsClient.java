package com.frankloq.client;

import com.frankloq.Hydroacoustics;
import com.frankloq.util.HydroConfig;
import net.minecraft.client.Minecraft;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;

/**
 * Client-only event listeners and lifecycle management for NeoForge 1.21.1.
 */
@EventBusSubscriber(modid = Hydroacoustics.MOD_ID, value = Dist.CLIENT, bus = EventBusSubscriber.Bus.GAME)
public class HydroacousticsClient {

    private static HydroConfig config;

    public static HydroConfig getConfig() {
        if (config == null) {
            config = HydroConfig.load("default");
        }
        return config;
    }

    public static void setConfig(HydroConfig newConfig) {
        config = newConfig;
    }

    @SubscribeEvent
    public static void onClientLoggingIn(ClientPlayerNetworkEvent.LoggingIn event) {
        Minecraft mc = Minecraft.getInstance();
        String worldId = "unknown";
        if (mc.getCurrentServer() != null) {
            worldId = mc.getCurrentServer().ip.replaceAll("[^a-zA-Z0-9]", "_");
        } else if (mc.getSingleplayerServer() != null) {
            worldId = mc.getSingleplayerServer().getWorldData().getLevelName().replaceAll("[^a-zA-Z0-9]", "_");
        }
        config = HydroConfig.load(worldId);
        Hydroacoustics.LOGGER.info("[Hydroacoustics] Loaded client config for: {}", worldId);
    }
}
