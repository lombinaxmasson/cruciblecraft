package com.masson.cruciblecraft.registry;

import com.masson.cruciblecraft.CrucibleCraft;
import com.masson.cruciblecraft.content.multiblock.MultiblockControllerSpec;
import com.masson.cruciblecraft.machine.processing.MachineKindSpec;
import com.masson.cruciblecraft.machine.processing.MachineTierCatalog;
import com.masson.cruciblecraft.machine.processing.MachineVariant;

import net.minecraft.resources.ResourceLocation;

/** Fixed controller identities; structure geometry remains entirely in JSON. */
public final class ModMultiblockControllers {
    public static final MachineKindSpec LARGE_CENTRIFUGE_KIND =
            new MachineKindSpec(
                    id("large_centrifuge"),
                    ModProcessingMachines.CENTRIFUGE,
                    MachineKindSpec.OverclockPolicy.CHEAP,
                    true);
    public static final MachineVariant LARGE_CENTRIFUGE_VARIANT =
            new MachineVariant(
                    id("large_centrifuge"),
                    LARGE_CENTRIFUGE_KIND,
                    MachineTierCatalog.requireControllerTierBand(
                            id("large_centrifuge_profile")));
    public static final MultiblockControllerSpec LARGE_CENTRIFUGE =
            new MultiblockControllerSpec(
                    id("large_centrifuge"),
                    id("large_centrifuge"),
                    ModRecipeMaps.CENTRIFUGE.id(),
                    () -> ModProcessingMachines.CENTRIFUGE)
                    .withVariant(() -> LARGE_CENTRIFUGE_VARIANT);

    public static final MachineKindSpec LARGE_MIXER_KIND =
            new MachineKindSpec(
                    id("large_mixer"),
                    ModProcessingMachines.MIXER,
                    MachineKindSpec.OverclockPolicy.CHEAP,
                    true);
    public static final MachineVariant LARGE_MIXER_VARIANT =
            new MachineVariant(
                    id("large_mixer"),
                    LARGE_MIXER_KIND,
                    MachineTierCatalog.requireControllerTierBand(
                            id("large_mixer_profile")));
    public static final MultiblockControllerSpec LARGE_MIXER =
            new MultiblockControllerSpec(
                    id("large_mixer"),
                    id("large_mixer"),
                    ModRecipeMaps.MIXER.id(),
                    () -> ModProcessingMachines.MIXER)
                    .withVariant(() -> LARGE_MIXER_VARIANT);

    public static final MachineKindSpec LARGE_ELECTROLYZER_KIND =
            new MachineKindSpec(
                    id("large_electrolyzer"),
                    ModProcessingMachines.ELECTROLYZER,
                    MachineKindSpec.OverclockPolicy.CHEAP,
                    true);
    public static final MachineVariant LARGE_ELECTROLYZER_VARIANT =
            new MachineVariant(
                    id("large_electrolyzer"),
                    LARGE_ELECTROLYZER_KIND,
                    MachineTierCatalog.requireControllerTierBand(
                            id("large_electrolyzer_profile")));
    public static final MultiblockControllerSpec LARGE_ELECTROLYZER =
            new MultiblockControllerSpec(
                    id("large_electrolyzer"),
                    id("large_electrolyzer"),
                    ModRecipeMaps.ELECTROLYZER.id(),
                    () -> ModProcessingMachines.ELECTROLYZER)
                    .withVariant(() -> LARGE_ELECTROLYZER_VARIANT);

    public static final MachineKindSpec LARGE_OVEN_KIND =
            new MachineKindSpec(
                    id("large_oven"),
                    ModProcessingMachines.LARGE_OVEN,
                    MachineKindSpec.OverclockPolicy.CHEAP,
                    true);
    public static final MachineVariant LARGE_OVEN_VARIANT =
            new MachineVariant(
                    id("large_oven"),
                    LARGE_OVEN_KIND,
                    MachineTierCatalog.requireControllerTierBand(
                            id("large_oven_profile")));
    public static final MultiblockControllerSpec LARGE_OVEN =
            new MultiblockControllerSpec(
                    id("large_oven"),
                    id("large_oven"),
                    ModRecipeMaps.OVEN.id(),
                    () -> ModProcessingMachines.LARGE_OVEN)
                    .withVariant(() -> LARGE_OVEN_VARIANT);

    public static final MachineKindSpec LARGE_BATH_KIND =
            new MachineKindSpec(
                    id("large_bath"),
                    ModProcessingMachines.BATH,
                    MachineKindSpec.OverclockPolicy.STANDARD,
                    false);
    public static final MachineVariant LARGE_BATH_VARIANT =
            new MachineVariant(
                    id("large_bath"),
                    LARGE_BATH_KIND,
                    MachineTierCatalog.requireControllerTierBand(
                            id("large_bath_profile")));
    public static final MultiblockControllerSpec LARGE_BATH =
            new MultiblockControllerSpec(
                    id("large_bath"),
                    id("large_bath"),
                    ModRecipeMaps.BATH.id(),
                    () -> ModProcessingMachines.BATH)
                    .withVariant(() -> LARGE_BATH_VARIANT);

    public static final MachineKindSpec LARGE_COAGULATOR_KIND =
            new MachineKindSpec(
                    id("large_coagulator"),
                    ModProcessingMachines.COAGULATOR,
                    MachineKindSpec.OverclockPolicy.STANDARD,
                    false);
    public static final MachineVariant LARGE_COAGULATOR_VARIANT =
            new MachineVariant(
                    id("large_coagulator"),
                    LARGE_COAGULATOR_KIND,
                    MachineTierCatalog.requireControllerTierBand(
                            id("large_coagulator_profile")));
    public static final MultiblockControllerSpec LARGE_COAGULATOR =
            new MultiblockControllerSpec(
                    id("large_coagulator"),
                    id("large_coagulator"),
                    ModRecipeMaps.COAGULATOR.id(),
                    () -> ModProcessingMachines.COAGULATOR)
                    .withVariant(() -> LARGE_COAGULATOR_VARIANT);

    public static final MachineKindSpec LARGE_AUTOCLAVE_KIND =
            new MachineKindSpec(
                    id("large_autoclave"),
                    ModProcessingMachines.AUTOCLAVE,
                    MachineKindSpec.OverclockPolicy.STANDARD,
                    false);
    public static final MachineVariant LARGE_AUTOCLAVE_VARIANT =
            new MachineVariant(
                    id("large_autoclave"),
                    LARGE_AUTOCLAVE_KIND,
                    MachineTierCatalog.requireControllerTierBand(
                            id("large_autoclave_profile")));
    public static final MultiblockControllerSpec LARGE_AUTOCLAVE =
            new MultiblockControllerSpec(
                    id("large_autoclave"),
                    id("large_autoclave"),
                    ModRecipeMaps.AUTOCLAVE.id(),
                    () -> ModProcessingMachines.AUTOCLAVE)
                    .withVariant(() -> LARGE_AUTOCLAVE_VARIANT);

    public static final MachineKindSpec LARGE_FERMENTER_KIND =
            new MachineKindSpec(
                    id("large_fermenter"),
                    ModProcessingMachines.FERMENTER,
                    MachineKindSpec.OverclockPolicy.CHEAP,
                    true);
    public static final MachineVariant LARGE_FERMENTER_VARIANT =
            new MachineVariant(
                    id("large_fermenter"),
                    LARGE_FERMENTER_KIND,
                    MachineTierCatalog.requireControllerTierBand(
                            id("large_fermenter_profile")));
    public static final MultiblockControllerSpec LARGE_FERMENTER =
            new MultiblockControllerSpec(
                    id("large_fermenter"),
                    id("large_fermenter"),
                    ModRecipeMaps.FERMENTER.id(),
                    () -> ModProcessingMachines.FERMENTER)
                    .withVariant(() -> LARGE_FERMENTER_VARIANT);

    public static final MachineKindSpec LARGE_CRUSHER_KIND =
            new MachineKindSpec(
                    id("large_crusher"),
                    ModProcessingMachines.LARGE_CRUSHER,
                    MachineKindSpec.OverclockPolicy.CHEAP,
                    true);
    public static final MachineVariant LARGE_CRUSHER_VARIANT =
            new MachineVariant(
                    id("large_crusher"),
                    LARGE_CRUSHER_KIND,
                    MachineTierCatalog.requireControllerTierBand(
                            id("large_crusher_profile")));
    public static final MultiblockControllerSpec LARGE_CRUSHER =
            new MultiblockControllerSpec(
                    id("large_crusher"),
                    id("large_crusher"),
                    ModRecipeMaps.CRUSHER.id(),
                    () -> ModProcessingMachines.CRUSHER)
                    .withVariant(() -> LARGE_CRUSHER_VARIANT);

    public static final MachineKindSpec LARGE_SHREDDER_KIND =
            new MachineKindSpec(
                    id("large_shredder"),
                    ModProcessingMachines.LARGE_SHREDDER,
                    MachineKindSpec.OverclockPolicy.CHEAP,
                    true);
    public static final MachineVariant LARGE_SHREDDER_VARIANT =
            new MachineVariant(
                    id("large_shredder"),
                    LARGE_SHREDDER_KIND,
                    MachineTierCatalog.requireControllerTierBand(
                            id("large_shredder_profile")));
    public static final MultiblockControllerSpec LARGE_SHREDDER =
            new MultiblockControllerSpec(
                    id("large_shredder"),
                    id("large_shredder"),
                    ModRecipeMaps.SHREDDER.id(),
                    () -> ModProcessingMachines.SHREDDER)
                    .withVariant(() -> LARGE_SHREDDER_VARIANT);

    public static final MachineKindSpec DISTILLATION_TOWER_KIND =
            new MachineKindSpec(
                    id("distillation_tower"),
                    ModProcessingMachines.DISTILLATION_TOWER,
                    MachineKindSpec.OverclockPolicy.CHEAP,
                    true);
    public static final MachineVariant DISTILLATION_TOWER_VARIANT =
            new MachineVariant(
                    id("distillation_tower"),
                    DISTILLATION_TOWER_KIND,
                    MachineTierCatalog.requireControllerTierBand(
                            id("distillation_tower_profile")));
    public static final MultiblockControllerSpec DISTILLATION_TOWER =
            new MultiblockControllerSpec(
                    id("distillation_tower"),
                    id("distillation_tower"),
                    ModRecipeMaps.DISTILLATION_TOWER.id(),
                    () -> ModProcessingMachines.DISTILLATION_TOWER)
                    .withVariant(() -> DISTILLATION_TOWER_VARIANT);

    public static final MachineKindSpec CRYO_DISTILLATION_TOWER_KIND =
            new MachineKindSpec(
                    id("cryo_distillation_tower"),
                    ModProcessingMachines.CRYO_DISTILLATION_TOWER,
                    MachineKindSpec.OverclockPolicy.CHEAP,
                    true);
    public static final MachineVariant CRYO_DISTILLATION_TOWER_VARIANT =
            new MachineVariant(
                    id("cryo_distillation_tower"),
                    CRYO_DISTILLATION_TOWER_KIND,
                    MachineTierCatalog.requireControllerTierBand(
                            id("cryo_distillation_tower_profile")));
    public static final MultiblockControllerSpec CRYO_DISTILLATION_TOWER =
            new MultiblockControllerSpec(
                    id("cryo_distillation_tower"),
                    id("distillation_tower"),
                    ModRecipeMaps.CRYO_DISTILLATION_TOWER.id(),
                    () -> ModProcessingMachines.CRYO_DISTILLATION_TOWER)
                    .withVariant(() -> CRYO_DISTILLATION_TOWER_VARIANT);

    public static final MachineKindSpec LARGE_MATTER_FABRICATOR_KIND =
            new MachineKindSpec(
                    id("large_matter_fabricator"),
                    ModProcessingMachines.LARGE_MATTER_FABRICATOR,
                    MachineKindSpec.OverclockPolicy.CHEAP,
                    true);
    public static final MachineVariant LARGE_MATTER_FABRICATOR_VARIANT =
            new MachineVariant(
                    id("large_matter_fabricator"),
                    LARGE_MATTER_FABRICATOR_KIND,
                    MachineTierCatalog.requireControllerTierBand(
                            id("large_matter_fabricator_profile")));
    public static final MultiblockControllerSpec LARGE_MATTER_FABRICATOR =
            new MultiblockControllerSpec(
                    id("large_matter_fabricator"),
                    id("large_matter_fabricator"),
                    ModRecipeMaps.MASSFAB.id(),
                    () -> ModProcessingMachines.LARGE_MATTER_FABRICATOR)
                    .withVariant(() -> LARGE_MATTER_FABRICATOR_VARIANT);

    private ModMultiblockControllers() {}

    private static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath(
                CrucibleCraft.MODID, path);
    }
}
