package com.masson.cruciblecraft.content.block;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;

import com.masson.cruciblecraft.CrucibleCraft;
import com.masson.cruciblecraft.content.mte.MteInPlaceKind;
import com.masson.cruciblecraft.content.mte.MteInPlaceSpec;
import com.masson.cruciblecraft.material.MaterialCatalog;
import com.masson.cruciblecraft.registry.ModBlocks;

import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

/**
 * GT6 {@code MultiTileEntityCrucible} controllers and matching metal walls
 * ({@code NBT_DESIGN} 18009/18002/18007/18006/18003/18004/18005).
 */
public final class LargeCrucibleHosts {
    public static final String DEFAULT_MATERIAL = "steel";

    /** Visible GT6 large-crucible materials that already have CC wall/controller ids. */
    public static final List<String> MATERIALS = List.of(
            "steel",
            "stainless_steel",
            "invar",
            "titanium",
            "tungstensteel",
            "tungsten",
            "adamantium");

    private static final Set<String> MATERIAL_SET = new LinkedHashSet<>(MATERIALS);

    /** GT6 {@code NBT_ACIDPROOF} T on large crucibles. */
    private static final Set<String> ACID_PROOF = Set.of(
            "stainless_steel",
            "tungsten",
            "adamantium");

    private LargeCrucibleHosts() {}

    public static boolean isAllowed(String materialId) {
        return materialId != null && MATERIAL_SET.contains(materialId);
    }

    public static boolean isController(MteInPlaceSpec spec) {
        return spec != null && spec.gt6Class().contains("MultiTileEntityCrucible");
    }

    public static boolean isController(Block block) {
        return block instanceof LargeCrucibleBlock
                || (block instanceof MteInPlaceBlock inplace && isController(inplace.spec()));
    }

    public static boolean isWall(MteInPlaceSpec spec) {
        if (spec == null || !spec.registryPath().endsWith("/wall")) {
            return false;
        }
        return isAllowed(materialId(spec));
    }

    public static boolean isWall(Block block) {
        return block instanceof MteInPlaceBlock inplace && isWall(inplace.spec());
    }

    /** GT6 metal/wood/dense walls in Multiblock Machines, including non-casing parts. */
    public static boolean isCatalogWall(MteInPlaceSpec spec) {
        if (spec == null || spec.kind() != MteInPlaceKind.MULTIBLOCK_PART) {
            return false;
        }
        String path = spec.registryPath();
        return path.endsWith("/wall") || path.endsWith("_wall");
    }

    public static String materialId(MteInPlaceSpec spec) {
        return resolveMaterialId(token(spec.registryPath()));
    }

    public static Optional<String> bakedMaterial(BlockState state) {
        if (state.getBlock() instanceof MteInPlaceBlock inplace
                && (isController(inplace.spec()) || isCatalogWall(inplace.spec()))) {
            return Optional.of(materialId(inplace.spec()));
        }
        return Optional.empty();
    }

    public static boolean acidProof(String materialId) {
        return materialId != null && ACID_PROOF.contains(materialId);
    }

    public static Direction horizontalFacing(BlockState state) {
        if (state.hasProperty(ProcessingMachineBlock.FACING)) {
            return state.getValue(ProcessingMachineBlock.FACING);
        }
        if (state.hasProperty(MteInPlaceBlock.FACING)) {
            Direction facing = state.getValue(MteInPlaceBlock.FACING);
            return facing.getAxis().isHorizontal() ? facing : Direction.NORTH;
        }
        return Direction.NORTH;
    }

    public static int colorRgb(String materialId) {
        return MaterialCatalog.find(materialId)
                .map(material -> 0xFF000000 | material.colorRgb())
                .orElse(0xFF7F7F7F);
    }

    public static ResourceLocation controllerId(String materialId) {
        return ResourceLocation.fromNamespaceAndPath(
                CrucibleCraft.MODID, "multiblock/large_" + materialId + "_crucible");
    }

    public static ResourceLocation wallId(String materialId) {
        return ResourceLocation.fromNamespaceAndPath(
                CrucibleCraft.MODID, materialId + "/wall");
    }

    public static Block wallBlock(String materialId) {
        var holder = ModBlocks.mteInPlaceBlocksById().get(wallId(materialId));
        if (holder == null) {
            throw new IllegalStateException("Missing large-crucible wall for " + materialId);
        }
        return holder.get();
    }

    public static String token(String registryPath) {
        if (registryPath.startsWith("multiblock/large_")
                && registryPath.endsWith("_crucible")) {
            return registryPath.substring(
                    "multiblock/large_".length(),
                    registryPath.length() - "_crucible".length());
        }
        if (registryPath.endsWith("/wall")) {
            return registryPath.substring(0, registryPath.length() - "/wall".length());
        }
        if (registryPath.endsWith("_wall")) {
            String last = registryPath.substring(registryPath.lastIndexOf('/') + 1);
            if (last.startsWith("dense_")) {
                last = last.substring("dense_".length());
            }
            if (last.endsWith("_wall")) {
                last = last.substring(0, last.length() - "_wall".length());
            }
            return last;
        }
        return registryPath.substring(registryPath.lastIndexOf('/') + 1);
    }

    static String resolveMaterialId(String token) {
        String normalized = token == null ? "" : token.toLowerCase(Locale.ROOT);
        if (normalized.contains("galvanized")) {
            normalized = "steel_galvanized";
        }
        if (MaterialCatalog.contains(normalized)) {
            return normalized;
        }
        String elemental = normalized + "_elemental";
        return MaterialCatalog.contains(elemental) ? elemental : normalized;
    }
}
