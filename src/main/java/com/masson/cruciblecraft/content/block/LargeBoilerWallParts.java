package com.masson.cruciblecraft.content.block;

import java.util.Optional;

import com.masson.cruciblecraft.api.fluid.LongFluidHandler;
import com.masson.cruciblecraft.content.blockentity.LargeBoilerBlockEntity;
import com.masson.cruciblecraft.content.blockentity.MteInPlaceBlockEntity;
import com.masson.cruciblecraft.content.mte.MteInPlaceSpec;
import com.masson.cruciblecraft.content.multiblock.PortCapabilityGate;
import com.masson.cruciblecraft.content.multiblock.MultiblockStructureDefinition.PortType;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;

/**
 * GT6 dense boiler walls are both casing and directional fluid parts.
 * Assignment is supplied by the JSON structure (bottom = input, middle/top
 * = output) and is forwarded to the controller's shared long-capacity tanks.
 */
public final class LargeBoilerWallParts {
    private LargeBoilerWallParts() {}

    public static boolean isWall(MteInPlaceSpec spec) {
        return spec != null
                && (spec.meta() == 18022
                        || spec.meta() == 18027
                        || spec.meta() == 18026
                        || spec.meta() == 18023
                        || spec.meta() == 18025);
    }

    public static PortType defaultType(MteInPlaceSpec spec) {
        return PortType.ITEM_FLUID_IN;
    }

    public static boolean accepts(MteInPlaceSpec spec, PortType type) {
        return isWall(spec)
                && (type == PortType.ITEM_FLUID_IN
                        || type == PortType.FLUID_OUT)
                && (PortCapabilityGate.fluidFill(type)
                        || PortCapabilityGate.fluidDrain(type));
    }

    public static IFluidHandler fluids(MteInPlaceBlockEntity wall) {
        if (!accepts(wall.spec(), wall.portType())
                || wall.getLevel() == null) {
            return null;
        }
        Optional<BlockPos> controller = wall.controllerPosition();
        if (controller.isEmpty()) {
            return null;
        }
        BlockEntity entity = wall.getLevel().getBlockEntity(
                controller.orElseThrow());
        return entity instanceof LargeBoilerBlockEntity boiler
                ? boiler.fluidsForPort(wall.portType())
                : null;
    }

    public static LongFluidHandler longFluids(
            MteInPlaceBlockEntity wall) {
        if (!accepts(wall.spec(), wall.portType())
                || wall.getLevel() == null) {
            return null;
        }
        Optional<BlockPos> controller = wall.controllerPosition();
        if (controller.isEmpty()) {
            return null;
        }
        BlockEntity entity = wall.getLevel().getBlockEntity(
                controller.orElseThrow());
        return entity instanceof LargeBoilerBlockEntity boiler
                ? boiler.longFluidsForPort(wall.portType())
                : null;
    }
}
