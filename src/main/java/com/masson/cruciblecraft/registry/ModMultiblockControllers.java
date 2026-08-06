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
                    MachineTierCatalog.requireControllerProfile(
                            id("large_centrifuge_profile")));
    public static final MultiblockControllerSpec LARGE_CENTRIFUGE =
            new MultiblockControllerSpec(
                    id("large_centrifuge"),
                    id("large_centrifuge"),
                    ModRecipeMaps.CENTRIFUGE.id(),
                    () -> ModProcessingMachines.CENTRIFUGE)
                    .withVariant(() -> LARGE_CENTRIFUGE_VARIANT);

    private ModMultiblockControllers() {}

    private static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath(
                CrucibleCraft.MODID, path);
    }
}
