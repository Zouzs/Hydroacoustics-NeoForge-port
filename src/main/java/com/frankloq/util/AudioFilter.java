package com.frankloq.util;

/**
 * Real-time Digital Signal Processing (DSP) filter for underwater acoustic simulation.
 * Applies a 2-pole cascaded low-pass filter (muffling), an LFO amplitude modulator (gargle/bubbling),
 * and dynamic volume gain with soft clipping protection.
 */
public class AudioFilter {
    private float previousSample1 = 0.0f;
    private float previousSample2 = 0.0f;
    private float lfoPhase = 0.0f;

    // Live filter state tracking
    private float currentAlpha = 1.0f;    // 1.0 = No muffling (surface cutoff)
    private float currentGain = 1.0f;     // 1.0 = Normal volume
    private float currentGargle = 0.0f;   // 0.0 = No chopping

    public AudioFilter() {}

    /**
     * Applies the DSP filter while smoothly lerping values over time to prevent pops/clicks.
     *
     * @param audioData    Raw 48kHz 16-bit PCM samples
     * @param targetAlpha  Low-pass filter coefficient (lower = more muffled)
     * @param targetGain   Volume multiplier
     * @param targetGargle Depth of the 15Hz bubbling amplitude modulation
     * @return true if the filter has smoothly returned to normal surface levels
     */
    public boolean applyLerpedFilter(short[] audioData, float targetAlpha, float targetGain, float targetGargle) {
        // At 48kHz, 0.0001f creates a smooth ~0.3s glide
        float lerpSpeed = 0.0001f;
        float lfoStep = (15.0f * 2.0f * (float) Math.PI) / 48000.0f;

        for (int i = 0; i < audioData.length; i++) {
            // Smoothly glide towards targets
            currentAlpha += (targetAlpha - currentAlpha) * lerpSpeed;
            currentGain += (targetGain - currentGain) * lerpSpeed;
            currentGargle += (targetGargle - currentGargle) * lerpSpeed;

            float currentSample = audioData[i];

            // Apply 15Hz gargle / chopping
            if (currentGargle > 0.001f) {
                lfoPhase += lfoStep;
                if (lfoPhase > Math.PI * 2) lfoPhase -= (float) (Math.PI * 2);
                float lfoValue = 1.0f - (currentGargle * 0.5f * (1.0f + (float) Math.sin(lfoPhase)));
                currentSample *= lfoValue;
            } else {
                lfoPhase = 0.0f;
            }

            // Apply 2-pole cascaded low-pass filter (muffling)
            previousSample1 = previousSample1 + currentAlpha * (currentSample - previousSample1);
            previousSample2 = previousSample2 + currentAlpha * (previousSample1 - previousSample2);

            // Apply gain and clipping protection
            float amplifiedSample = previousSample2 * currentGain;
            if (amplifiedSample > Short.MAX_VALUE) amplifiedSample = Short.MAX_VALUE;
            else if (amplifiedSample < Short.MIN_VALUE) amplifiedSample = Short.MIN_VALUE;

            audioData[i] = (short) amplifiedSample;
        }

        return Math.abs(currentAlpha - 1.0f) < 0.001f &&
                Math.abs(currentGain - 1.0f) < 0.001f &&
                Math.abs(currentGargle - 0.0f) < 0.001f;
    }

    public void reset() {
        this.previousSample1 = 0.0f;
        this.previousSample2 = 0.0f;
        this.lfoPhase = 0.0f;
        this.currentAlpha = 1.0f;
        this.currentGain = 1.0f;
        this.currentGargle = 0.0f;
    }
}
