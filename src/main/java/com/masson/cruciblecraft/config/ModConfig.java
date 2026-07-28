package com.masson.cruciblecraft.config;

import net.neoforged.neoforge.common.ModConfigSpec;

/**
 * Client-only preferences. Balance values belong in datapacks, not here (§5.5).
 */
public final class ModConfig {
    private static final ModConfigSpec.Builder CLIENT_BUILDER = new ModConfigSpec.Builder();

    public static final ModConfigSpec.ConfigValue<String> TEMPERATURE_UNIT = CLIENT_BUILDER
            .comment("Temperature display unit: C or F")
            .define("temperatureUnit", "C");

    public static final ModConfigSpec CLIENT_SPEC = CLIENT_BUILDER.build();

    private ModConfig() {}
}
