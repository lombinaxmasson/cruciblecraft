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

    private ModMultiblockControllers() {}

    private static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath(
                CrucibleCraft.MODID, path);
    }
}
