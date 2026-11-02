package com.frankloq.client;

import com.frankloq.Hydroacoustics;
import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.entity.player.Player;

import java.util.UUID;

/**
 * 3D tickable sound instance playing continuous custom bubble loop glued
 * to the speaking player's head, with smooth 10-tick fade-out upon silence.
 */
public class BubbleSoundInstance extends AbstractTickableSoundInstance {
    private final Player player;
    private final UUID playerUuid;
    private final int stopDelayMs;

    private boolean isFading = false;
    private int fadeTicks = 0;
    private static final int MAX_FADE_TICKS = 10;

    public BubbleSoundInstance(Player player, UUID playerUuid, int stopDelayMs) {
        super(Hydroacoustics.CUSTOM_BUBBLES.get(), SoundSource.PLAYERS, SoundInstance.createUnseededRandom());
        this.player = player;
        this.playerUuid = playerUuid;
        this.stopDelayMs = stopDelayMs;
        this.looping = true;
        this.delay = 0;

        this.volume = HydroacousticsClient.getConfig().bubbleVolume;
        this.pitch = 0.8f + (this.random.nextFloat() * 0.4f);
        this.x = player.getX();
        this.y = player.getY() + 1.5;
        this.z = player.getZ();
    }

    @Override
    public void tick() {
        // Stop playing immediately if player disconnected or exited water
        if (this.player == null || this.player.isRemoved() || !this.player.isEyeInFluid(FluidTags.WATER)) {
            this.stop();
            return;
        }

        // Glue sound to the player's head position
        this.x = this.player.getX();
        this.y = this.player.getY() + 1.5;
        this.z = this.player.getZ();

        // Check if player is currently transmitting audio
        long lastPacketTime = ClientAudioHandler.getLastSpokeTime(this.playerUuid);
        boolean isTalking = (System.currentTimeMillis() - lastPacketTime) <= stopDelayMs;

        if (isTalking) {
            this.isFading = false;
            this.fadeTicks = 0;
            this.volume = HydroacousticsClient.getConfig().bubbleVolume;
        } else {
            this.isFading = true;
        }

        if (this.isFading) {
            this.fadeTicks++;
            float fadeRatio = 1.0f - ((float) this.fadeTicks / MAX_FADE_TICKS);
            if (fadeRatio <= 0.0f) {
                this.volume = 0.0f;
                this.stop();
            } else {
                this.volume = HydroacousticsClient.getConfig().bubbleVolume * fadeRatio;
            }
        }
    }
}
