package com.masson.cruciblecraft.content.block;

import java.util.Optional;

import com.masson.cruciblecraft.api.energy.EnergyType;
import com.masson.cruciblecraft.content.blockentity.MteInPlaceBlockEntity;
import com.masson.cruciblecraft.content.mte.MteInPlaceSpec;
import com.masson.cruciblecraft.content.multiblock.CoilHosts;
import com.masson.cruciblecraft.content.multiblock.MultiblockStructureDefinition.PortType;
import com.masson.cruciblecraft.energy.vondagraagg.VonDaGraaggBlockEntity;
import com.masson.cruciblecraft.energy.vondagraagg.VonDaGraaggStructure;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;

/** GT6 18028 dense galvanized walls: {@code ONLY_ENERGY_IN} for Von Da Graagg. */
public final class GalvanizedGraaggWalls {
    private GalvanizedGraaggWalls() {}

    public static boolean isWall(MteInPlaceSpec spec) {
        return spec != null && spec.meta() == CoilHosts.DENSE_GALVANIZED_META;
    }

    public static boolean accepts(MteInPlaceSpec spec, PortType type) {
        return isWall(spec) && type == PortType.ENERGY_INPUT;
    }

    public static boolean forwardsEnergy(MteInPlaceBlockEntity wall) {
        return wall.mixerPortType() == PortType.ENERGY_INPUT && host(wall) != null;
    }

    public static long insertEnergy(
            MteInPlaceBlockEntity wall,
            EnergyType type,
            long size,
            long amount,
            boolean simulate) {
        if (!forwardsEnergy(wall) || type != EnergyType.ELECTRIC) {
            return 0L;
        }
        VonDaGraaggBlockEntity host = host(wall);
        return host == null
                ? 0L
                : host.insert(type, size, amount, null, simulate);
    }

    public static long storedEnergy(MteInPlaceBlockEntity wall, EnergyType type) {
        VonDaGraaggBlockEntity host = host(wall);
        return host == null ? 0L : host.stored(type);
    }

    public static long energyCapacity(MteInPlaceBlockEntity wall, EnergyType type) {
        VonDaGraaggBlockEntity host = host(wall);
        return host == null ? 0L : host.capacity(type);
    }

    static VonDaGraaggBlockEntity host(MteInPlaceBlockEntity wall) {
        Optional<BlockPos> controller = wall.mixerControllerPosition();
        Optional<net.minecraft.resources.ResourceLocation> structure =
                wall.mixerStructureId();
        Level level = wall.getLevel();
        if (level == null
                || controller.isEmpty()
                || structure.isEmpty()
                || !level.hasChunkAt(controller.get())) {
            return null;
        }
        if (!(level.getBlockEntity(controller.get())
                instanceof VonDaGraaggBlockEntity graagg)
                || !graagg.formed()
                || !structure.get().equals(VonDaGraaggStructure.STRUCTURE_ID)) {
            return null;
        }
        return graagg;
    }
}
