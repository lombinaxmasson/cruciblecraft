package com.masson.cruciblecraft.registry;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;

import com.masson.cruciblecraft.api.energy.EnergyType;
import com.masson.cruciblecraft.machine.processing.ProcessingMachineSpec;

import net.minecraft.SharedConstants;
import net.minecraft.server.Bootstrap;
import net.neoforged.fml.loading.LoadingModList;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

class ProcessingMachineEnergyAuditTest {
    private static final Map<String, EnergyType> EXPECTED = Map.ofEntries(
            Map.entry("bronze_crusher", EnergyType.KINETIC_PUSH),
            Map.entry("sluice", EnergyType.KINETIC_ROTATION),
            Map.entry("bath", EnergyType.KINETIC),
            Map.entry("centrifuge", EnergyType.KINETIC_ROTATION),
            Map.entry("shredder", EnergyType.KINETIC_ROTATION),
            Map.entry("sifter", EnergyType.KINETIC_PUSH),
            Map.entry("mortar", EnergyType.KINETIC),
            Map.entry("smelter", EnergyType.HEAT),
            Map.entry("extruder", EnergyType.KINETIC),
            Map.entry("cutter", EnergyType.KINETIC),
            Map.entry("lathe", EnergyType.KINETIC_ROTATION),
            Map.entry("rollingmill", EnergyType.KINETIC_ROTATION),
            Map.entry("rollbender", EnergyType.KINETIC),
            Map.entry("wiremill", EnergyType.KINETIC_ROTATION),
            Map.entry("bender", EnergyType.KINETIC),
            Map.entry("assembler", EnergyType.KINETIC),
            Map.entry("welder", EnergyType.KINETIC),
            Map.entry("press", EnergyType.KINETIC_PUSH),
            Map.entry("electrolyzer", EnergyType.ELECTRIC),
            Map.entry("mixer", EnergyType.ELECTRIC),
            Map.entry("distillery", EnergyType.HEAT),
            Map.entry("autoclave", EnergyType.ELECTRIC),
            Map.entry("drying", EnergyType.HEAT),
            Map.entry("compressor", EnergyType.ELECTRIC),
            Map.entry("generifier", EnergyType.ELECTRIC));

    @BeforeAll
    static void bootstrapMinecraft() {
        LoadingModList.of(List.of(), List.of(), List.of(), List.of(), Map.of());
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();
    }

    @Test
    void allTwentyFiveSpecsKeepTheirExplicitEnergyValues() {
        Map<String, EnergyType> actual = allSpecs().collect(
                java.util.stream.Collectors.toUnmodifiableMap(
                        spec -> spec.id().getPath(),
                        spec -> spec.energy().type()));
        assertEquals(25, actual.size());
        assertEquals(EXPECTED, actual);
        assertEquals(EnergyType.KINETIC, actual.get("extruder"));
        assertEquals(EnergyType.ELECTRIC, actual.get("compressor"));
    }

    @Test
    void legacyKineticSetRemainsExactlyEightWithoutExpansion() {
        Set<String> actual = allSpecs()
                .filter(spec -> spec.energy().type() == EnergyType.KINETIC)
                .map(spec -> spec.id().getPath())
                .collect(java.util.stream.Collectors.toUnmodifiableSet());
        assertEquals(
                Set.of(
                        "assembler",
                        "bath",
                        "bender",
                        "cutter",
                        "extruder",
                        "mortar",
                        "rollbender",
                        "welder"),
                actual);
    }

    private static Stream<ProcessingMachineSpec> allSpecs() {
        return Stream.concat(
                Stream.of(ModProcessingMachines.CRUSHER),
                ModProcessingMachines.CONFIGURED_MACHINES.stream());
    }
}
