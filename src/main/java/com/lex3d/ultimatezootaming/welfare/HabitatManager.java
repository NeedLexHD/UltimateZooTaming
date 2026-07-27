package com.lex3d.ultimatezootaming.welfare;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.animal.WaterAnimal;
import net.minecraftforge.registries.ForgeRegistries;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

/**
 * Habitat + regime par type de mob : assignation AUTO (heuristique) + OVERRIDES
 * admin persistes en JSON dans le dossier du monde (via /zootame habitats).
 * dietOverride : 0=auto 1=herbivore 2=carnivore 3=piscivore 4=omnivore.
 */
public class HabitatManager {

    public record Entry(int habitat, int diet) {}

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static Map<String, Entry> overrides = new HashMap<>();
    private static Path file;

    public static void load(MinecraftServer server) {
        file = server.getWorldPath(net.minecraft.world.level.storage.LevelResource.ROOT)
                .resolve("ultimatezootame-habitats.json");
        try {
            if (Files.exists(file)) {
                overrides = GSON.fromJson(Files.readString(file),
                        new TypeToken<Map<String, Entry>>(){}.getType());
                if (overrides == null) overrides = new HashMap<>();
            }
        } catch (IOException e) {
            overrides = new HashMap<>();
        }
    }

    public static void save() {
        if (file == null) return;
        try {
            Files.writeString(file, GSON.toJson(overrides));
        } catch (IOException ignored) {}
    }

    public static void set(String typeId, int habitat, int diet) {
        overrides.put(typeId, new Entry(habitat, diet));
        save();
    }

    public static Map<String, Entry> all() {
        return overrides;
    }

    /** Profil d'habitat effectif du mob : override sinon heuristique AUTO. */
    public static HabitatProfile profileOf(LivingEntity mob) {
        String id = key(mob.getType());
        Entry e = overrides.get(id);
        if (e != null && e.habitat() > 0 && e.habitat() < HabitatProfile.values().length) {
            return HabitatProfile.values()[e.habitat()];
        }
        // Heuristique auto
        if (mob instanceof WaterAnimal) return HabitatProfile.AQUATIC;
        if (mob.fireImmune()) return HabitatProfile.NETHER;
        return HabitatProfile.AUTO; // -> le calculateur applique la regle generique
    }

    /** Regime force du mob (0 = auto/heuristique croquettes existante). */
    public static int dietOverrideOf(EntityType<?> type) {
        Entry e = overrides.get(key(type));
        return e == null ? 0 : e.diet();
    }

    public static String key(EntityType<?> type) {
        ResourceLocation rl = ForgeRegistries.ENTITY_TYPES.getKey(type);
        return rl == null ? "unknown" : rl.toString();
    }
}
