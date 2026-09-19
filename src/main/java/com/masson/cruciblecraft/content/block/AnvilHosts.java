package com.masson.cruciblecraft.content.block;

import java.util.Locale;
import java.util.Map;
import java.util.Optional;

import com.masson.cruciblecraft.content.item.MaterialToolItem;
import com.masson.cruciblecraft.content.mte.MteInPlaceSpec;
import com.masson.cruciblecraft.machine.MachineMaterialRules;
import com.masson.cruciblecraft.machine.MachineMaterialRules.Device;
import com.masson.cruciblecraft.machine.ToolMaterialRules.ToolKind;
import com.masson.cruciblecraft.material.MaterialCatalog;

import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * GT6 {@code MultiTileEntityAnvil} identity, collision and iconset helpers.
 * Unique in-place anvils stay distinct BlockItems; they share this backend.
 */
public final class AnvilHosts {
    /** GT6 {@code MT.Stone} solid RGB (205, 205, 205). */
    public static final int STONE_COLOR = 0xFFCDCDCD;

    private static final VoxelShape NORTH_SOUTH = Block.box(0.0, 0.0, 4.0, 16.0, 12.0, 12.0);
    private static final VoxelShape EAST_WEST = Block.box(4.0, 0.0, 0.0, 12.0, 12.0, 16.0);

    /**
     * Durability published on GT6 {@code Loader_MultiTileEntities} anvil
     * registrations. Live {@code Device.ANVIL} catalog values win when present.
     */
    private static final Map<String, Long> GT6_DURABILITY = Map.ofEntries(
            Map.entry("stone", 10_000L),
            Map.entry("lead", 800_000L),
            Map.entry("bronze", 1_000_000L),
            Map.entry("arsenic_copper", 1_000_000L),
            Map.entry("arsenic_bronze", 2_000_000L),
            Map.entry("syrmorite", 2_000_000L),
            Map.entry("ironwood", 7_500_000L),
            Map.entry("steel", 10_000_000L),
            Map.entry("desh", 12_500_000L),
            Map.entry("efrine", 20_000_000L),
            Map.entry("thaumium", 25_000_000L),
            Map.entry("black_steel", 30_000_000L),
            Map.entry("blue_steel", 40_000_000L),
            Map.entry("red_steel", 50_000_000L),
            Map.entry("vanadium_steel", 70_000_000L),
            Map.entry("octine", 80_000_000L),
            Map.entry("fiery_steel", 90_000_000L),
            Map.entry("titanium", 100_000_000L),
            Map.entry("netherite", 150_000_000L),
            Map.entry("void_metal", 300_000_000L),
            Map.entry("titanium_gold", 400_000_000L),
            Map.entry("tungstensteel", 1_000_000_000L),
            Map.entry("tungsten", 2_000_000_000L),
            Map.entry("iridium", 10_000_000_000L),
            Map.entry("adamantium", 1_000_000_000_000L),
            Map.entry("draconium", 1_000_000_000_000L),
            Map.entry("draconium_awakened", 2_000_000_000_000L));

    public enum Iconset {
        STONE,
        METALLIC,
        WOOD
    }

    private AnvilHosts() {}

    public static boolean isAnvil(MteInPlaceSpec spec) {
        return spec != null && spec.gt6Class().contains("MultiTileEntityAnvil");
    }

    public static boolean isAnvil(Block block) {
        return block instanceof AnvilBlock
                || (block instanceof MteInPlaceBlock inplace && isAnvil(inplace.spec()));
    }

    public static boolean isHammer(ItemStack stack) {
        return stack.getItem() instanceof MaterialToolItem tool
                && tool.kind() == ToolKind.SMITHING_HAMMER;
    }

    public static String materialId(MteInPlaceSpec spec) {
        return resolveMaterialId(token(spec.registryPath()));
    }

    public static Optional<String> bakedMaterial(BlockState state) {
        if (state.getBlock() instanceof MteInPlaceBlock inplace && isAnvil(inplace.spec())) {
            return Optional.of(materialId(inplace.spec()));
        }
        return Optional.empty();
    }

    public static long maxDurability(String materialId) {
        if (MachineMaterialRules.isAllowed(Device.ANVIL, materialId)) {
            return MachineMaterialRules.anvilMaxDurability(materialId);
        }
        Long published = GT6_DURABILITY.get(materialId);
        if (published != null) {
            return published;
        }
        if (materialId.endsWith("_elemental")) {
            published = GT6_DURABILITY.get(
                    materialId.substring(0, materialId.length() - "_elemental".length()));
            if (published != null) {
                return published;
            }
        }
        return MachineMaterialRules.STONE_ANVIL_DURABILITY;
    }

    public static Iconset iconset(String materialId) {
        if ("stone".equals(materialId)) {
            return Iconset.STONE;
        }
        if ("ironwood".equals(materialId)) {
            return Iconset.WOOD;
        }
        return Iconset.METALLIC;
    }

    public static Iconset iconset(MteInPlaceSpec spec) {
        return iconset(materialId(spec));
    }

    public static boolean metallic(String materialId) {
        return iconset(materialId) == Iconset.METALLIC;
    }

    public static VoxelShape shape(Direction facing) {
        return facing.getAxis() == Direction.Axis.X ? EAST_WEST : NORTH_SOUTH;
    }

    public static Direction horizontalFacing(BlockState state) {
        if (state.hasProperty(AnvilBlock.FACING)) {
            return state.getValue(AnvilBlock.FACING);
        }
        if (state.hasProperty(MteInPlaceBlock.FACING)) {
            Direction facing = state.getValue(MteInPlaceBlock.FACING);
            return facing.getAxis().isHorizontal() ? facing : Direction.NORTH;
        }
        return Direction.NORTH;
    }

    public static int colorRgb(String materialId) {
        if ("stone".equals(materialId)) {
            return STONE_COLOR;
        }
        return MaterialCatalog.find(materialId)
                .map(material -> 0xFF000000 | material.colorRgb())
                .orElse(STONE_COLOR);
    }

    static String token(String registryPath) {
        if (registryPath.endsWith("/anvil")) {
            return registryPath.substring(0, registryPath.length() - "/anvil".length());
        }
        String last = registryPath.substring(registryPath.lastIndexOf('/') + 1);
        if (last.endsWith("_anvil")) {
            return last.substring(0, last.length() - "_anvil".length());
        }
        return last;
    }

    static String resolveMaterialId(String token) {
        String normalized = token.toLowerCase(Locale.ROOT);
        if ("awakened_draconium".equals(normalized)) {
            normalized = "draconium_awakened";
        }
        if (MaterialCatalog.contains(normalized)) {
            return normalized;
        }
        String elemental = normalized + "_elemental";
        return MaterialCatalog.contains(elemental) ? elemental : normalized;
    }
}
