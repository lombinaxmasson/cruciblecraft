package com.masson.cruciblecraft.worldgen;

import java.util.List;
import java.util.Optional;

/**
 * GT6 {@code Loader_Worldgen} {@code WorldgenOresBedrock} rows that run without
 * other mods: {@code GEN_FLOOR} plus {@code GEN_NETHER}. Hexorium, custom slots,
 * and planet/BTL/Erebus rows stay out.
 */
public final class BedrockOreCatalog {
    public static final List<Vein> VEINS = List.of(
            overworld("ore.bedrock.diamond", "diamond", 128000, IndicatorFlower.Family.B, 6),
            overworld("ore.bedrock.tungstate", "tungstate", 96000, IndicatorFlower.Family.B, 7),
            overworld("ore.bedrock.ferberite", "ferberite", 96000, IndicatorFlower.Family.B, 7),
            overworld("ore.bedrock.wolframite", "wolframite", 96000, IndicatorFlower.Family.B, 7),
            overworld("ore.bedrock.stolzite", "stolzite", 96000, IndicatorFlower.Family.B, 7),
            overworld("ore.bedrock.scheelite", "scheelite", 96000, IndicatorFlower.Family.B, 7),
            overworld("ore.bedrock.huebnerite", "huebnerite", 96000, IndicatorFlower.Family.B, 7),
            overworld("ore.bedrock.russelite", "russellite", 96000, IndicatorFlower.Family.B, 7),
            overworld("ore.bedrock.pinalite", "pinalite", 96000, IndicatorFlower.Family.B, 7),
            overworld("ore.bedrock.uraninite", "uraninite", 60000, IndicatorFlower.Family.A, 5),
            overworld("ore.bedrock.pitchblende", "pitchblende", 60000, IndicatorFlower.Family.B, 5),
            overworld("ore.bedrock.gold.a", "gold", 32000, IndicatorFlower.Family.A, 0),
            overworld("ore.bedrock.gold.b", "gold", 32000, IndicatorFlower.Family.B, 2),
            overworld("ore.bedrock.cooperite", "cooperite", 16000, IndicatorFlower.Family.A, 6),
            overworld("ore.bedrock.copper", "copper", 16000, IndicatorFlower.Family.B, 3),
            overworld("ore.bedrock.monazite", "monazite", 16000, IndicatorFlower.Family.A, 9),
            overworld("ore.bedrock.powellite", "powellite", 14000, IndicatorFlower.Family.A, 7),
            overworld("ore.bedrock.bastnasite", "bastnasite", 8000, IndicatorFlower.Family.A, 9),
            overworld("ore.bedrock.stibnite", "arsenopyrite", 8000, IndicatorFlower.Family.B, 0),
            overworld("ore.bedrock.redstone", "redstone", 7000, IndicatorFlower.Family.B, 4),
            overworld("ore.bedrock.vanadium", "vanadium_pentoxide", 6000, IndicatorFlower.Family.A, 7),
            overworld("ore.bedrock.galena", "galena", 6000, IndicatorFlower.Family.A, 1),
            overworld("ore.bedrock.coal", "coal", 5000, IndicatorFlower.Family.A, 7),
            overworld("ore.bedrock.graphite", "graphite", 5000, IndicatorFlower.Family.A, 7),
            overworld("ore.bedrock.stibnite.real", "stibnite", 4000, IndicatorFlower.Family.B, 1),
            overworld("ore.bedrock.hematite", "hematite", 4000, IndicatorFlower.Family.A, 7),
            overworld("ore.bedrock.sphalerite", "sphalerite", 3000, IndicatorFlower.Family.A, 3),
            overworld("ore.bedrock.smithsonite", "smithsonite", 3000, IndicatorFlower.Family.A, 3),
            overworld("ore.bedrock.pentlandite", "pentlandite", 3000, IndicatorFlower.Family.A, 4),
            overworld("ore.bedrock.saltpeter", "niter", 3000, IndicatorFlower.Family.A, 7),
            overworld("ore.bedrock.bauxite", "bauxite", 2000, IndicatorFlower.Family.A, 7),
            overworld("ore.bedrock.cassiterite", "cassiterite", 2000, IndicatorFlower.Family.A, 7),
            overworld("ore.bedrock.chalcopyrite", "chalcopyrite", 2000, IndicatorFlower.Family.A, 2),
            nether("ore.bedrock.voidquartz", "void_quartz", 4000),
            nether("ore.bedrock.glowstone", "glowstone", 4000),
            nether("ore.bedrock.gloomstone", "gloomstone", 4000),
            nether("ore.bedrock.efrine", "efrine", 2000),
            nether("ore.bedrock.netherquartz", "nether_quartz", 2000),
            nether("ore.bedrock.firestone", "firestone", 8000),
            nether("ore.bedrock.ancientdebris", "ancient_debris", 4000));

    public static final int OVERWORLD_COUNT = 33;
    public static final int NETHER_COUNT = 7;

    private BedrockOreCatalog() {}

    public static List<Vein> forNether(boolean nether) {
        return VEINS.stream().filter(vein -> vein.nether() == nether).toList();
    }

    private static Vein overworld(
            String gt6Name,
            String materialId,
            int probability,
            IndicatorFlower.Family family,
            int meta) {
        return new Vein(
                gt6Name,
                materialId,
                probability,
                false,
                Optional.of(IndicatorFlower.of(family, meta)));
    }

    private static Vein nether(String gt6Name, String materialId, int probability) {
        return new Vein(gt6Name, materialId, probability, true, Optional.empty());
    }

    /**
     * @param gt6Name GT6 worldgen object name. The second Stibnite row is
     *     {@code ore.bedrock.stibnite} in GT6 as well; CC keeps a distinct id so
     *     the arsenopyrite row and the real stibnite row both stay registered.
     */
    public record Vein(
            String gt6Name,
            String materialId,
            int probability,
            boolean nether,
            Optional<IndicatorFlower> flower) {
        public Vein {
            if (probability < 1) {
                throw new IllegalArgumentException(gt6Name);
            }
            flower = flower == null ? Optional.empty() : flower;
        }
    }
}
