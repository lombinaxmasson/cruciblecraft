package com.masson.cruciblecraft.machine.generation;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import com.masson.cruciblecraft.api.energy.EnergyType;

import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;

import org.junit.jupiter.api.Test;

class FuelGeneratorIdentityPolicyTest {
    private static final FuelGeneratorSpec FUEL_ENGINE =
            new FuelGeneratorSpec(
                    ResourceLocation.parse("cruciblecraft:fuel_engine"),
                    () -> null,
                    8_000,
                    8_000,
                    1,
                    EnergyType.KINETIC_ROTATION,
                    16L,
                    1L,
                    65_536L,
                    10_000,
                    FuelGeneratorSpec.EnergyOutputFace.FRONT,
                    List.of(Direction.UP),
                    1);
    private static final FuelGeneratorSpec GAS_GENERATOR =
            new FuelGeneratorSpec(
                    ResourceLocation.parse(
                            "cruciblecraft:burning_gas_generator"),
                    () -> null,
                    16_000,
                    16_000,
                    2,
                    EnergyType.HEAT,
                    1L,
                    24L,
                    288_000L,
                    7_500,
                    FuelGeneratorSpec.EnergyOutputFace.UP,
                    List.of(Direction.NORTH, Direction.SOUTH),
                    2);

    @Test
    void completeCurrentIdentitiesAreAccepted() {
        for (FuelGeneratorSpec spec : List.of(
                FUEL_ENGINE, GAS_GENERATOR)) {
            FuelGeneratorIdentityPolicy.Identity current =
                    FuelGeneratorIdentityPolicy.current(spec);
            FuelGeneratorIdentityPolicy.Decision decision =
                    FuelGeneratorIdentityPolicy.resolve(spec, current);
            assertEquals(
                    FuelGeneratorIdentityPolicy.Resolution.ACCEPTED,
                    decision.resolution());
            assertEquals(current, decision.persistedIdentity());
            assertTrue(decision.quarantineReason().isEmpty());
        }
    }

    @Test
    void missingPartialAndWrongIdentitiesFailClosed() {
        FuelGeneratorIdentityPolicy.Decision missing =
                FuelGeneratorIdentityPolicy.resolve(FUEL_ENGINE, null);
        assertEquals(
                FuelGeneratorIdentityPolicy.Resolution.QUARANTINED,
                missing.resolution());
        assertEquals(
                FuelGeneratorIdentityPolicy.MISSING_SCHEMA_VERSION,
                missing.persistedIdentity().schemaVersion());
        assertTrue(missing.quarantineReason().isPresent());

        List<FuelGeneratorIdentityPolicy.Identity> rejected = List.of(
                new FuelGeneratorIdentityPolicy.Identity(
                        FuelGeneratorIdentityPolicy.MISSING_SCHEMA_VERSION,
                        "cruciblecraft:fuel_engine",
                        "KINETIC_ROTATION"),
                new FuelGeneratorIdentityPolicy.Identity(
                        1,
                        "",
                        "KINETIC_ROTATION"),
                new FuelGeneratorIdentityPolicy.Identity(
                        1,
                        "cruciblecraft:fuel_engine",
                        ""),
                new FuelGeneratorIdentityPolicy.Identity(
                        2,
                        "cruciblecraft:fuel_engine",
                        "KINETIC_ROTATION"),
                new FuelGeneratorIdentityPolicy.Identity(
                        1,
                        "cruciblecraft:fuel_engine_near",
                        "KINETIC_ROTATION"),
                new FuelGeneratorIdentityPolicy.Identity(
                        1,
                        "cruciblecraft:fuel_engine",
                        "ELECTRIC"),
                new FuelGeneratorIdentityPolicy.Identity(
                        1,
                        "cruciblecraft:burning_gas_generator_near",
                        "ELECTRIC"),
                new FuelGeneratorIdentityPolicy.Identity(
                        1,
                        "cruciblecraft:burning_gas_generator",
                        "KINETIC_ROTATION"),
                new FuelGeneratorIdentityPolicy.Identity(
                        0,
                        "cruciblecraft:burning_gas_generator",
                        "ELECTRIC"));
        for (FuelGeneratorIdentityPolicy.Identity identity : rejected) {
            FuelGeneratorSpec spec = identity.generatorId().contains(
                    "burning_gas_generator")
                    ? GAS_GENERATOR
                    : FUEL_ENGINE;
            FuelGeneratorIdentityPolicy.Decision decision =
                    FuelGeneratorIdentityPolicy.resolve(
                            spec, identity);
            assertEquals(
                    FuelGeneratorIdentityPolicy.Resolution.QUARANTINED,
                    decision.resolution());
            assertEquals(identity, decision.persistedIdentity());
            assertTrue(decision.quarantineReason().isPresent());
        }
    }
}
