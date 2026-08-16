package com.masson.cruciblecraft.registry;

import com.masson.cruciblecraft.CrucibleCraft;
import com.masson.cruciblecraft.api.energy.EnergyType;
import com.masson.cruciblecraft.content.multiblock.MultiblockControllerSpec;
import com.masson.cruciblecraft.machine.processing.MachineKindSpec;
import com.masson.cruciblecraft.machine.processing.MachineTierCatalog;
import com.masson.cruciblecraft.machine.processing.MachineVariant;
import com.masson.cruciblecraft.machine.processing.ProcessingMachineSpec;

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

    /**
     * T23 host spec for the distillation tower controller: port-fed heat
     * buffer (the GT6 tower base layer is nine heat transmitters), same
     * distillery layout and recipes (DESIGN_POLICY: CC collapses GT6
     * RM.DistillationTower and RM.Distillery into one map for v1). The
     * spec keeps its own id but declares the distillery recipe map so
     * MultiblockControllerSpec validation binds the right map.
     */
    public static final ProcessingMachineSpec DISTILLATION_TOWER_HOST =
            new ProcessingMachineSpec(
                    id("distillation_tower"),
                    ModRecipeMaps.DISTILLERY.id(),
                    () -> ModRecipeMaps.DISTILLERY,
                    ModProcessingMachines.DISTILLERY.items(),
                    ModProcessingMachines.DISTILLERY.fluids(),
                    new ProcessingMachineSpec.EnergySpec(
                            EnergyType.HEAT,
                            ProcessingMachineSpec.EnergyMode.BUFFERED,
                            4_096L,
                            1_024L),
                    new ProcessingMachineSpec.SidedIoPolicy(
                            ModProcessingMachines.DISTILLERY.sidedIo()
                                    .items(),
                            ModProcessingMachines.DISTILLERY.sidedIo()
                                    .fluids(),
                            (front, side) ->
                                    side != null
                                                    && side
                                                            == front
                                                                    .getOpposite()
                                            ? ProcessingMachineSpec
                                                    .CapabilityAccess.INPUT
                                            : ProcessingMachineSpec
                                                    .CapabilityAccess.NONE),
                    ModProcessingMachines.DISTILLERY.validator(),
                    ModProcessingMachines.DISTILLERY.buffering(),
                    ModProcessingMachines.DISTILLERY.ui());

    public static final MachineKindSpec DISTILLATION_TOWER_KIND =
            new MachineKindSpec(
                    id("distillation_tower"),
                    DISTILLATION_TOWER_HOST,
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
                    ModRecipeMaps.DISTILLERY.id(),
                    () -> DISTILLATION_TOWER_HOST)
                    .withVariant(() -> DISTILLATION_TOWER_VARIANT);

    private ModMultiblockControllers() {}

    private static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath(
                CrucibleCraft.MODID, path);
    }
}
