package com.tbupgrades.extras.common;

import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.tbupgrades.extras.TravelersBackpackExtras;
import net.fabricmc.loader.api.FabricLoader;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * The addon's switches, in {@code config/travelersbackpackextras.json}.
 *
 * <p>Both settings change how strong the end game items are, so they are the kind of thing a pack
 * author wants to tune. Without Cloth Config installed the file is edited by hand and read once at
 * startup; an unreadable or partial file falls back to the defaults rather than failing the game.
 */
public final class TbxConfig {
    private static final String FILE_NAME = "travelersbackpackextras.json";

    private static boolean infiniteFluids = true;
    private static boolean appleGrantsFlight = true;

    private static boolean omegaFlight = true;
    private static double omegaFlightMax = 600.0D;
    private static double omegaFlightDrain = 1.0D;
    private static double omegaFlightRegen = 0.8D;

    private TbxConfig() {
    }

    /** Endless water and lava at netherite tier and above. */
    public static boolean infiniteFluids() {
        return infiniteFluids;
    }

    /** Whether the enchanted diamond golden apple grants creative flight. */
    public static boolean appleGrantsFlight() {
        return appleGrantsFlight;
    }

    /** Whether the omega upgrade grants its flight charge at all. */
    public static boolean omegaFlight() {
        return omegaFlight;
    }

    public static double omegaFlightMax() {
        return omegaFlightMax;
    }

    public static double omegaFlightDrain() {
        return omegaFlightDrain;
    }

    public static double omegaFlightRegen() {
        return omegaFlightRegen;
    }

    public static void setInfiniteFluids(boolean value) {
        infiniteFluids = value;
    }

    public static void setAppleGrantsFlight(boolean value) {
        appleGrantsFlight = value;
    }

    /** Writes the current values back, so an in-game change survives a restart. */
    public static void save() {
        Path file = FabricLoader.getInstance().getConfigDir().resolve(FILE_NAME);
        try {
            JsonObject json = new JsonObject();
            json.addProperty("_comment_infinite_fluids", "Endless water (>2 buckets) and lava (>10000 buckets) once a netherite or higher upgrade is installed.");
            json.addProperty("infinite_fluids", infiniteFluids);
            json.addProperty("_comment_apple_flight", "Whether the enchanted diamond golden apple grants 30 minutes of creative flight.");
            json.addProperty("apple_grants_creative_flight", appleGrantsFlight);
            json.addProperty("omega_flight", omegaFlight);
            json.addProperty("omega_flight_max", omegaFlightMax);
            json.addProperty("omega_flight_drain", omegaFlightDrain);
            json.addProperty("omega_flight_regen", omegaFlightRegen);
            Files.createDirectories(file.getParent());
            Files.writeString(file, new GsonBuilder().setPrettyPrinting().create().toJson(json), StandardCharsets.UTF_8);
        } catch (Exception e) {
            TravelersBackpackExtras.LOGGER.warn("[Extra Upgrades] could not write the config", e);
        }
    }

    public static void load() {
        Path file = FabricLoader.getInstance().getConfigDir().resolve(FILE_NAME);
        try {
            if (Files.exists(file)) {
                JsonObject json = JsonParser.parseString(Files.readString(file, StandardCharsets.UTF_8)).getAsJsonObject();
                infiniteFluids = read(json, "infinite_fluids", infiniteFluids);
                appleGrantsFlight = read(json, "apple_grants_creative_flight", appleGrantsFlight);
                omegaFlight = read(json, "omega_flight", omegaFlight);
                omegaFlightMax = readDouble(json, "omega_flight_max", omegaFlightMax);
                omegaFlightDrain = readDouble(json, "omega_flight_drain", omegaFlightDrain);
                omegaFlightRegen = readDouble(json, "omega_flight_regen", omegaFlightRegen);
                return;
            }
            Files.createDirectories(file.getParent());
            JsonObject json = new JsonObject();
            json.addProperty("_comment_infinite_fluids", "Endless water (>2 buckets) and lava (>10000 buckets) once a netherite or higher upgrade is installed.");
            json.addProperty("infinite_fluids", infiniteFluids);
            json.addProperty("_comment_apple_flight", "Whether the enchanted diamond golden apple grants 30 minutes of creative flight.");
            json.addProperty("apple_grants_creative_flight", appleGrantsFlight);
            json.addProperty("omega_flight", omegaFlight);
            json.addProperty("omega_flight_max", omegaFlightMax);
            json.addProperty("omega_flight_drain", omegaFlightDrain);
            json.addProperty("omega_flight_regen", omegaFlightRegen);
            Files.writeString(file, new GsonBuilder().setPrettyPrinting().create().toJson(json), StandardCharsets.UTF_8);
            TravelersBackpackExtras.LOGGER.info("[Extra Upgrades] wrote the default config to {}", file);
        } catch (Exception e) {
            TravelersBackpackExtras.LOGGER.warn("[Extra Upgrades] could not read the config, using defaults", e);
        }
    }

    private static double readDouble(JsonObject json, String key, double fallback) {
        return json.has(key) ? json.get(key).getAsDouble() : fallback;
    }

    private static boolean read(JsonObject json, String key, boolean fallback) {
        return json.has(key) ? json.get(key).getAsBoolean() : fallback;
    }
}