package com.masson.cruciblecraft.registry;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.masson.cruciblecraft.api.energy.EnergyType;
import com.masson.cruciblecraft.machine.processing.MachineDeliveryCatalog;
import com.masson.cruciblecraft.machine.processing.ProcessingMachineSpec;

import net.minecraft.SharedConstants;
import net.minecraft.server.Bootstrap;
import net.neoforged.fml.loading.LoadingModList;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

class ProcessingMachineEnergyAuditTest {
    @BeforeAll
    static void bootstrapMinecraft() {
        LoadingModList.of(List.of(), List.of(), List.of(), List.of(), Map.of());
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();
    }

    @Test
    void allConfiguredSpecsKeepTheirExplicitEnergyValues() {
        Map<String, EnergyType> expected = MachineDeliveryCatalog.hosts().stream()
                .collect(Collectors.toUnmodifiableMap(
                        host -> host.id().getPath(),
                        MachineDeliveryCatalog.Host::energyType));
        Map<String, EnergyType> actual = allSpecs().collect(
                Collectors.toUnmodifiableMap(
                        spec -> spec.id().getPath(),
                        spec -> spec.energy().type()));
        assertEquals(expected.size(), actual.size());
        assertEquals(expected, actual);
        assertEquals(EnergyType.HEAT, actual.get("extruder"));
        assertEquals(EnergyType.KINETIC_PUSH, actual.get("compressor"));
        assertEquals(EnergyType.TIME, actual.get("autoclave"));
    }

    @Test
    void liveSpecsMatchDeliveryEnergyMode() {
        allSpecs().forEach(spec -> {
            var host = MachineDeliveryCatalog.require(spec.id());
            assertEquals(
                    host.energyMode(),
                    spec.energy().mode(),
                    spec.id().toString());
        });
    }

    @Test
    void legacyKineticSetRemainsTheOpeningOnlyHosts() {
        Set<String> actual = allSpecs()
                .filter(spec -> spec.energy().type() == EnergyType.KINETIC)
                .map(spec -> spec.id().getPath())
                .collect(Collectors.toUnmodifiableSet());
        assertEquals(
                Set.of(
                        "assembler",
                        "bender",
                        "mortar"),
                actual);
    }

    private static Stream<ProcessingMachineSpec> allSpecs() {
        return Stream.concat(
                Stream.of(ModProcessingMachines.CRUSHER),
                ModProcessingMachines.CONFIGURED_MACHINES.stream());
    }
}
