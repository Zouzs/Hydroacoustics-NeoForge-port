package com.frankloq.util;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.neoforged.fml.loading.FMLPaths;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;

public class HydroConfig {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    private transient File configFile;

    // Configurable default DSP properties
    public float targetAlpha = 0.02f;
    public float targetGain = 3.0f;
    public float targetGargle = 0.5f;
    public float bubbleVolume = 0.08f;
    public float voiceDistance = 16.0f;

    public static HydroConfig load(String worldName) {
        // NeoForge standard config directory
        File dir = new File(FMLPaths.CONFIGDIR.get().toFile(), "hydroacoustics");
        if (!dir.exists()) dir.mkdirs();
        File file = new File(dir, "hydro_" + worldName + ".json");
        HydroConfig config = new HydroConfig();
        config.configFile = file;
        if (file.exists()) {
            try (FileReader reader = new FileReader(file)) {
                HydroConfig loaded = GSON.fromJson(reader, HydroConfig.class);
                if (loaded != null) {
                    loaded.configFile = file;
                    return loaded;
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        config.save();
        return config;
    }

    public void save() {
        if (this.configFile == null) return;
        try (FileWriter writer = new FileWriter(this.configFile)) {
            GSON.toJson(this, writer);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void reset() {
        this.targetAlpha = 0.02f;
        this.targetGain = 3.0f;
        this.targetGargle = 0.5f;
        this.bubbleVolume = 0.08f;
        this.voiceDistance = 16.0f;
        this.save();
    }
}
