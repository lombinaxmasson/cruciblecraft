package com.masson.cruciblecraft.content.block;

import java.util.Optional;

import com.masson.cruciblecraft.content.blockentity.MteInPlaceBlockEntity;
import com.masson.cruciblecraft.content.mte.MteInPlaceSpec;
import com.masson.cruciblecraft.content.multiblock.MultiblockControllerBinding;
import com.masson.cruciblecraft.content.multiblock.MultiblockPortHost;
import com.masson.cruciblecraft.content.multiblock.MultiblockStructureDefinition.PortType;
import com.masson.cruciblecraft.machine.processing.ProcessingMachineSpec;
import com.masson.cruciblecraft.machine.processing.SidedFluidHandler;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;

/** GT6 18001/18002/18003/18004/18005/18006/18007 and dense tank walls. */
public final class TankWallParts {
    private TankWallParts() {}

    public static boolean isWall(MteInPlaceSpec spec) {
        if (spec == null) {
            return false;
        }
        return switch (spec.meta()) {
            case 18001, 18002, 18003, 18004, 18005, 18006, 18007,
                    18022, 18023, 18024, 18025, 18026, 18027 -> true;
            default -> false;
        };
    }

    public static PortType defaultType(MteInPlaceSpec spec) {
        return PortType.FLUID;
    }

    public static boolean accepts(MteInPlaceSpec spec, PortType type) {
        return isWall(spec) && type == PortType.FLUID;
    }

    public static IFluidHandler fluids(MteInPlaceBlockEntity wall) {
        if (!accepts(wall.spec(), wall.portType())) {
            return null;
        }
        MultiblockPortHost host = host(wall);
        if (host == null) {
            return null;
        }
        return new SidedFluidHandler(
                host.tanks(),
                host.fluidInputTanks(),
                host.fluidOutputTanks(),
                ProcessingMachineSpec.CapabilityAccess.BOTH,
                () -> {});
    }

    static MultiblockPortHost host(MteInPlaceBlockEntity wall) {
        Optional<BlockPos> controller = wall.controllerPosition();
        Optional<ResourceLocation> structure = wall.structureId();
        Level level = wall.getLevel();
        if (level == null
                || controller.isEmpty()
                || structure.isEmpty()
                || !level.hasChunkAt(controller.orElseThrow())) {
            return null;
        }
        if (!(level.getBlockEntity(controller.orElseThrow())
                instanceof MultiblockControllerBinding binding)
                || !binding.structureValid()
                || !structure.get().equals(binding.structureId())) {
            return null;
        }
        return binding.portHost();
    }
}
