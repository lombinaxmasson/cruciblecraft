package com.masson.cruciblecraft.registry;

import java.util.List;

import com.masson.cruciblecraft.content.multiblock.MultiblockControllerPlugin;
import com.masson.cruciblecraft.content.multiblock.MultiblockControllerPluginRegistry;

import net.minecraft.resources.ResourceLocation;

/**
 * Registers the whitelisted multiblock controller plugins. Every plugin is
 * consumed by at least one real controller; new plugins must be added here
 * AND to the disk mirror data/cruciblecraft/multiblock_plugins.json (tests
 * enforce the bidirectional equality and freeze the id set).
 */
public final class ModMultiblockPlugins {
    /** Shared processing host gated by the JSON structure. Consumed by
     * large_centrifuge (existing) and distillation_tower (T23 C1) — the
     * two real consumers justify the shared behavior. */
    public static final ResourceLocation PROCESSING_HOST =
            ResourceLocation.fromNamespaceAndPath(
                    "cruciblecraft", "processing_host");

    /** Matcher supply is one item and one fluid per shared host, never
     * counted per physical port block (T15e rule). Consumed by every
     * multi-port structure controller. */
    public static final ResourceLocation SHARED_PORT_SUPPLY =
            ResourceLocation.fromNamespaceAndPath(
                    "cruciblecraft", "shared_port_supply");

    /** Plugin set persisted and resolved by the large centrifuge
     * controller (quarantine on mismatch). */
    public static final List<ResourceLocation> LARGE_CENTRIFUGE_PLUGINS =
            List.of(PROCESSING_HOST, SHARED_PORT_SUPPLY);

    /** Plugin set persisted and resolved by the distillation tower
     * controller — the second real processing_host consumer. */
    public static final List<ResourceLocation>
            DISTILLATION_TOWER_PLUGINS =
                    List.of(PROCESSING_HOST, SHARED_PORT_SUPPLY);

    /** energy_input ports on this controller carry HU, not the KU
     * default — declared by the large boiler. */
    public static final ResourceLocation HEAT_ENERGY_INPUT =
            ResourceLocation.fromNamespaceAndPath(
                    "cruciblecraft", "heat_energy_input");

    /** water + HU -> steam continuous conversion (SteamConversion
     * constants, source-backed). Consumed by the large boiler. */
    public static final ResourceLocation STEAM_CONVERSION =
            ResourceLocation.fromNamespaceAndPath(
                    "cruciblecraft", "steam_conversion");

    /** Plugin set persisted and resolved by the large boiler
     * controller (conversion host, HU energy identity). */
    public static final List<ResourceLocation> LARGE_BOILER_PLUGINS =
            List.of(
                    HEAT_ENERGY_INPUT,
                    STEAM_CONVERSION,
                    SHARED_PORT_SUPPLY);

    /** bulk fluid storage with bidirectional ports, no recipe
     * transaction, no energy. Consumed by the 3x3x3 tank. */
    public static final ResourceLocation STORAGE_HOST =
            ResourceLocation.fromNamespaceAndPath(
                    "cruciblecraft", "storage_host");

    /** Plugin set persisted and resolved by the 3x3x3 tank controller. */
    public static final List<ResourceLocation> TANK_PLUGINS =
            List.of(STORAGE_HOST, SHARED_PORT_SUPPLY);

    public static void register() {
        MultiblockControllerPluginRegistry.register(plugin(PROCESSING_HOST));
        MultiblockControllerPluginRegistry.register(
                plugin(SHARED_PORT_SUPPLY));
        MultiblockControllerPluginRegistry.register(
                plugin(HEAT_ENERGY_INPUT));
        MultiblockControllerPluginRegistry.register(
                plugin(STEAM_CONVERSION));
        MultiblockControllerPluginRegistry.register(
                plugin(STORAGE_HOST));
    }

    private static MultiblockControllerPlugin plugin(
            ResourceLocation id) {
        return () -> id;
    }

    private ModMultiblockPlugins() {}
}
