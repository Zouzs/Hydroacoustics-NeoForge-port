package com.frankloq.client;

import com.frankloq.util.AudioFilter;
import com.frankloq.util.HydroConfig;
import de.maxhenkel.voicechat.api.events.ClientReceiveSoundEvent;
import de.maxhenkel.voicechat.api.events.EventRegistration;
import net.minecraft.client.Minecraft;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.entity.player.Player;

import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Intercepts incoming voice chat audio packets and applies underwater DSP muffling,
 * gargle modulation, distance attenuation, and head-attached bubble sound instances.
 */
public class ClientAudioHandler {
    private static final ConcurrentHashMap<UUID, AudioFilter> playerFilters = new ConcurrentHashMap<>();
    private static final ConcurrentHashMap<UUID, Long> lastSpokeTime = new ConcurrentHashMap<>();
    private static final ConcurrentHashMap<UUID, BubbleSoundInstance> activeBubbleSounds = new ConcurrentHashMap<>();

    public static final int BUBBLE_STOP_DELAY_MS = 100;

    public static long getLastSpokeTime(UUID uuid) {
        return lastSpokeTime.getOrDefault(uuid, 0L);
    }

    public static void register(EventRegistration registration) {
        registration.registerEvent(ClientReceiveSoundEvent.EntitySound.class, event -> {
            processAudio(event.getId(), event.getRawAudio());
        });
        registration.registerEvent(ClientReceiveSoundEvent.LocationalSound.class, event -> {
            processAudio(event.getId(), event.getRawAudio());
        });
    }

    private static void processAudio(UUID speakerUuid, short[] rawAudio) {
        Minecraft client = Minecraft.getInstance();
        if (client.player == null || client.level == null) return;

        // In 1.21.1 Mojang mappings: player.isEyeInFluid(FluidTags.WATER) checks if player head is submerged
        boolean listenerUnderwater = client.player.isEyeInFluid(FluidTags.WATER);
        boolean speakerUnderwater = false;
        double distance = 0.0;

        Player speaker = client.level.getPlayerByUUID(speakerUuid);
        if (speaker == null && speakerUuid.equals(client.player.getUUID())) {
            speaker = client.player;
        }

        if (speaker != null) {
            speakerUnderwater = speaker.isEyeInFluid(FluidTags.WATER);
            distance = client.player.distanceTo(speaker);
        }

        HydroConfig config = HydroacousticsClient.getConfig();

        // Base surface targets
        float targetAlpha = 1.0f;
        float targetGain = 1.0f;
        float targetGargle = 0.0f;
        boolean shouldPlayBubbles = false;

        // Apply underwater effects if either listener or speaker is submerged
        if (listenerUnderwater || speakerUnderwater) {
            targetAlpha = config.targetAlpha;

            // Dynamic distance falloff
            float baseGain = config.targetGain;
            float maxDist = config.voiceDistance;
            float distanceMultiplier = 1.0f;
            if (distance > 0.1) {
                distanceMultiplier = 1.0f - ((float) distance / maxDist);
                if (distanceMultiplier < 0.0f) {
                    distanceMultiplier = 0.0f;
                }
                distanceMultiplier = distanceMultiplier * distanceMultiplier;
            }
            targetGain = baseGain * distanceMultiplier;

            if (speakerUnderwater) {
                targetGargle = config.targetGargle;
                shouldPlayBubbles = true;
            } else {
                targetGargle = 0.0f;
                shouldPlayBubbles = false;
            }
        }

        // Handle the bubble sound instance
        if (shouldPlayBubbles) {
            lastSpokeTime.put(speakerUuid, System.currentTimeMillis());
            if (speaker != null) {
                BubbleSoundInstance currentSound = activeBubbleSounds.get(speakerUuid);
                if (currentSound == null || currentSound.isStopped()) {
                    final Player finalSpeaker = speaker;
                    client.execute(() -> {
                        BubbleSoundInstance checkSound = activeBubbleSounds.get(speakerUuid);
                        if (checkSound == null || checkSound.isStopped()) {
                            BubbleSoundInstance newSound = new BubbleSoundInstance(finalSpeaker, speakerUuid, BUBBLE_STOP_DELAY_MS);
                            activeBubbleSounds.put(speakerUuid, newSound);
                            client.getSoundManager().play(newSound);
                        }
                    });
                }
            }
        }

        // Apply smooth lerping DSP filter
        AudioFilter filter = playerFilters.computeIfAbsent(speakerUuid, k -> new AudioFilter());
        boolean isFullySurfaceLevel = filter.applyLerpedFilter(rawAudio, targetAlpha, targetGain, targetGargle);

        // Cleanup filter state when back at surface
        if (!listenerUnderwater && !speakerUnderwater && isFullySurfaceLevel) {
            filter.reset();
        }
    }
}
