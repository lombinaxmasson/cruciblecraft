package com.masson.cruciblecraft.content.item;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import com.masson.cruciblecraft.machine.ToolMaterialRules.ToolKind;
import com.masson.cruciblecraft.registry.ModItems;
import com.masson.cruciblecraft.registry.ModRecipeMaps;

import net.minecraft.world.item.ItemStack;

/**
 * EMI-free projection of the tool variants that have a live assembler
 * route: {@code ToolMaterialRules.isAllowed} intersected with the expanded
 * {@code tool/assembler/<tool>/<route>/<material>} entries — the same filter
 * the GameTest pins. Materials without a route stay out of the creative tab
 * and EMI by design; no "uncraftable" marking is invented.
 *
 * <p>The result is cached per assembler epoch, so the TOOLS tab and the EMI
 * plugin pay the enumeration cost once per recipe reload.
 */
public final class ToolDisplayPlan {
    private static volatile Snapshot cache =
            new Snapshot(Long.MIN_VALUE, List.of());

    private ToolDisplayPlan() {}

    public static List<String> routedMaterials(
            Collection<String> assemblerEntryPaths, String tool) {
        String prefix = "tool/assembler/" + tool + "/";
        return assemblerEntryPaths.stream()
                .filter(path -> path.startsWith(prefix))
                .map(path -> path.substring(path.lastIndexOf('/') + 1))
                .distinct()
                .sorted()
                .toList();
    }

    public static List<ItemStack> routedVariantStacks() {
        long epoch = ModRecipeMaps.ASSEMBLER.runtimeEpoch();
        Snapshot current = cache;
        if (current.epoch == epoch) {
            return current.stacks;
        }
        synchronized (ToolDisplayPlan.class) {
            current = cache;
            if (current.epoch == epoch) {
                return current.stacks;
            }
            List<ItemStack> stacks = compute();
            cache = new Snapshot(epoch, stacks);
            return stacks;
        }
    }

    private static List<ItemStack> compute() {
        List<String> paths = ModRecipeMaps.ASSEMBLER.entries().stream()
                .map(entry -> entry.id().getPath())
                .toList();
        List<ItemStack> stacks = new ArrayList<>();
        for (ToolKind kind : ToolKind.values()) {
            MaterialToolItem item = itemFor(kind);
            for (String materialId : routedMaterials(
                    paths, kind.serializedName())) {
                stacks.add(item.variant(materialId));
            }
        }
        return List.copyOf(stacks);
    }

    private static MaterialToolItem itemFor(ToolKind kind) {
        return switch (kind) {
            case PICKAXE -> ModItems.MATERIAL_PICKAXE.get();
            case SHOVEL -> ModItems.MATERIAL_SHOVEL.get();
            case AXE -> ModItems.MATERIAL_AXE.get();
            case HOE -> ModItems.MATERIAL_HOE.get();
            case SWORD -> ModItems.MATERIAL_SWORD.get();
            case SMITHING_HAMMER -> ModItems.SMITHING_HAMMER.get();
            case FILE -> ModItems.MATERIAL_FILE.get();
            case CHISEL -> ModItems.MATERIAL_CHISEL.get();
            case SAW -> ModItems.MATERIAL_SAW.get();
            case SCREWDRIVER -> ModItems.MATERIAL_SCREWDRIVER.get();
            case WRENCH -> ModItems.MATERIAL_WRENCH.get();
            case MONKEY_WRENCH -> ModItems.MATERIAL_MONKEY_WRENCH.get();
            case WIRE_CUTTER -> ModItems.MATERIAL_WIRE_CUTTER.get();
            case KNIFE -> ModItems.MATERIAL_KNIFE.get();
            case CLUB -> ModItems.MATERIAL_CLUB.get();
            case SPADE -> ModItems.MATERIAL_SPADE.get();
            case DOUBLE_AXE -> ModItems.MATERIAL_DOUBLE_AXE.get();
            case SENSE -> ModItems.MATERIAL_SENSE.get();
            case PLOW -> ModItems.MATERIAL_PLOW.get();
            case CONSTRUCTION_PICK -> ModItems.MATERIAL_CONSTRUCTION_PICK.get();
            case GEM_PICK -> ModItems.MATERIAL_GEM_PICK.get();
            case BUILDER_WAND -> ModItems.MATERIAL_BUILDER_WAND.get();
            case UNIVERSAL_SPADE -> ModItems.MATERIAL_UNIVERSAL_SPADE.get();
            case CROWBAR -> ModItems.MATERIAL_CROWBAR.get();
            case PLUNGER -> ModItems.MATERIAL_PLUNGER.get();
            case SCOOP -> ModItems.MATERIAL_SCOOP.get();
            case BUTCHERY_KNIFE -> ModItems.MATERIAL_BUTCHERY_KNIFE.get();
            case BRANCH_CUTTER -> ModItems.MATERIAL_BRANCH_CUTTER.get();
            case SCISSORS -> ModItems.MATERIAL_SCISSORS.get();
            case PINCERS -> ModItems.MATERIAL_PINCERS.get();
            case SOFT_HAMMER -> ModItems.MATERIAL_SOFT_HAMMER.get();
            case BENDING_CYLINDER -> ModItems.MATERIAL_BENDING_CYLINDER.get();
            case BENDING_CYLINDER_SMALL ->
                    ModItems.MATERIAL_BENDING_CYLINDER_SMALL.get();
            case HAND_DRILL -> ModItems.MATERIAL_HAND_DRILL.get();
            case ROLLING_PIN -> ModItems.MATERIAL_ROLLING_PIN.get();
            case FLINT_AND_TINDER -> ModItems.MATERIAL_FLINT_AND_TINDER.get();
            case POCKET_MULTITOOL -> ModItems.MATERIAL_POCKET_MULTITOOL.get();
        };
    }

    private record Snapshot(long epoch, List<ItemStack> stacks) {}
}
