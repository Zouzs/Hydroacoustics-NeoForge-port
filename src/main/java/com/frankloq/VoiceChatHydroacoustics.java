package com.frankloq;

import com.frankloq.client.ClientAudioHandler;
import de.maxhenkel.voicechat.api.ForgeVoicechatPlugin;
import de.maxhenkel.voicechat.api.VoicechatApi;
import de.maxhenkel.voicechat.api.VoicechatPlugin;
import de.maxhenkel.voicechat.api.events.EventRegistration;
import net.neoforged.fml.loading.FMLEnvironment;

/**
 * Simple Voice Chat Plugin implementation for NeoForge 1.21.1.
 * Registered via both META-INF/services SPI and @ForgeVoicechatPlugin annotation.
 */
@ForgeVoicechatPlugin
public class VoiceChatHydroacoustics implements VoicechatPlugin {

    @Override
    public String getPluginId() {
        return Hydroacoustics.MOD_ID;
    }

    @Override
    public void initialize(VoicechatApi api) {
        Hydroacoustics.LOGGER.info("[Hydroacoustics] Voice Chat Hydroacoustics Plugin Initialized!");
    }

    @Override
    public void registerEvents(EventRegistration registration) {
        // Only load the audio interception if running on a physical client
        if (FMLEnvironment.dist.isClient()) {
            ClientAudioHandler.register(registration);
        }
    }
}
