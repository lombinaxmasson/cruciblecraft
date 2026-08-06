package com.masson.cruciblecraft.registry;

import java.util.List;

import com.masson.cruciblecraft.api.energy.EnergyType;
import com.masson.cruciblecraft.machine.processing.MachineIdentityPolicy;
import com.masson.cruciblecraft.machine.processing.MachineVariant;
import com.masson.cruciblecraft.machine.processing.ProcessingMachineState;

import net.minecraft.resources.ResourceLocation;

/** Approved persisted machine-identity migrations. */
public final class ModMachineIdentityMigrations {
    public static final String LARGE_CENTRIFUGE_VARIANT_ID =
            "cruciblecraft:large_centrifuge";
    public static final String LARGE_CENTRIFUGE_PROFILE_ID =
            "cruciblecraft:large_centrifuge_profile";

    public static final MachineIdentityPolicy.Identity
            LEGACY_LARGE_CENTRIFUGE_IDENTITY =
                    legacyLargeCentrifugeIdentity();
    public static final MachineIdentityPolicy.Identity
            LARGE_CENTRIFUGE_IDENTITY =
                    new MachineIdentityPolicy.Identity(
                            LARGE_CENTRIFUGE_VARIANT_ID,
                            LARGE_CENTRIFUGE_PROFILE_ID,
                            LARGE_CENTRIFUGE_VARIANT_ID,
                            "KINETIC_ROTATION");
    public static final List<MachineIdentityPolicy.Migration>
            T16_LEGACY_TIER1_MIGRATIONS = List.of(
                    t16LegacyTier1Migration("lathe"),
                    t16LegacyTier1Migration("rollingmill"),
                    t16LegacyTier1Migration("wiremill"),
                    t16LegacyTier1Migration("shredder"),
                    t16LegacyTier1Migration("press"));
    public static final List<MachineIdentityPolicy.Migration>
            T17_LEGACY_TIER1_MIGRATIONS = List.of(
                    t17LegacyTier1Migration(
                            "distillery", EnergyType.ELECTRIC),
                    t17LegacyTier1Migration(
                            "drying", EnergyType.ELECTRIC),
                    t17LegacyTier1Migration(
                            "smelter", EnergyType.HEAT));

    private static final List<MachineIdentityPolicy.Migration> MIGRATIONS =
            java.util.stream.Stream.of(
                            List.of(
                            new MachineIdentityPolicy.Migration(
                                    LARGE_CENTRIFUGE_VARIANT_ID,
                                    LEGACY_LARGE_CENTRIFUGE_IDENTITY,
                                    LARGE_CENTRIFUGE_IDENTITY)),
                            T16_LEGACY_TIER1_MIGRATIONS,
                            T17_LEGACY_TIER1_MIGRATIONS)
                    .flatMap(List::stream)
                    .toList();

    public static MachineIdentityPolicy.Decision resolve(
            MachineVariant variant,
            ProcessingMachineState state) {
        return resolve(
                variant.id().toString(),
                identityOf(state),
                identityOf(variant));
    }

    public static MachineIdentityPolicy.Decision resolve(
            String variantId,
            MachineIdentityPolicy.Identity saved,
            MachineIdentityPolicy.Identity current) {
        return MachineIdentityPolicy.resolve(
                variantId, saved, current, MIGRATIONS);
    }

    public static MachineIdentityPolicy.Identity identityOf(
            ProcessingMachineState state) {
        return new MachineIdentityPolicy.Identity(
                state.machineKind(),
                state.tierProfile(),
                state.materialId(),
                state.energyIdentity());
    }

    public static MachineIdentityPolicy.Identity identityOf(
            MachineVariant variant) {
        return new MachineIdentityPolicy.Identity(
                variant.kind().id().toString(),
                variant.tier().id().toString(),
                variant.tier().materialId(),
                variant.tier().energyType().name());
    }

    /**
     * Reconstructs the exact tuple written before a source-backed variant
     * replaced a legacy processing spec.
     */
    public static MachineIdentityPolicy.Identity legacyIdentityOf(
            MachineVariant currentVariant) {
        return identityOf(MachineVariant.legacy(
                currentVariant.kind().behavior()));
    }

    /**
     * Derives the pre-tier tuple while retaining the historical host's actual
     * energy identity. T17 changed two electric hosts to adjacent Heat.
     */
    public static MachineIdentityPolicy.Identity legacyIdentityOf(
            MachineVariant currentVariant,
            EnergyType historicalEnergy) {
        MachineIdentityPolicy.Identity derived =
                legacyIdentityOf(currentVariant);
        return new MachineIdentityPolicy.Identity(
                derived.machineKind(),
                derived.tierProfile(),
                derived.materialId(),
                historicalEnergy.name());
    }

    private static MachineIdentityPolicy.Identity
            legacyLargeCentrifugeIdentity() {
        MachineVariant titanium = ModMachineVariants.require(
                ResourceLocation.fromNamespaceAndPath(
                        "cruciblecraft", "titanium_centrifuge"));
        return new MachineIdentityPolicy.Identity(
                LARGE_CENTRIFUGE_VARIANT_ID,
                titanium.tier().id().toString(),
                titanium.tier().materialId(),
                titanium.tier().energyType().name());
    }

    private static MachineIdentityPolicy.Migration
            t16LegacyTier1Migration(String path) {
        ResourceLocation id = ResourceLocation.fromNamespaceAndPath(
                "cruciblecraft", path);
        MachineVariant current = ModMachineVariants.require(id);
        if (!current.id().equals(current.kind().id())
                || !current.tier().materialId().equals(
                        "cruciblecraft:bronze")) {
            throw new IllegalStateException(
                    "T16 legacy migration target is not a tier-1 identity: "
                            + id);
        }
        return new MachineIdentityPolicy.Migration(
                id.toString(),
                legacyIdentityOf(current),
                identityOf(current));
    }

    private static MachineIdentityPolicy.Migration
            t17LegacyTier1Migration(
                    String path, EnergyType historicalEnergy) {
        ResourceLocation id = ResourceLocation.fromNamespaceAndPath(
                "cruciblecraft", path);
        MachineVariant current = ModMachineVariants.require(id);
        if (!current.id().equals(current.kind().id())
                || !current.tier().materialId().equals(
                        "cruciblecraft:steel")
                || current.tier().energyType() != EnergyType.HEAT) {
            throw new IllegalStateException(
                    "T17 legacy migration target is not a tier-1 Heat "
                            + "identity: " + id);
        }
        return new MachineIdentityPolicy.Migration(
                id.toString(),
                legacyIdentityOf(current, historicalEnergy),
                identityOf(current));
    }

    private ModMachineIdentityMigrations() {}
}
