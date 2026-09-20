package com.masson.cruciblecraft.content.item;

import java.util.Optional;

import com.masson.cruciblecraft.api.material.MaterialLookup;
import com.masson.cruciblecraft.machine.ToolMaterialRules.ToolKind;
import com.masson.cruciblecraft.material.prefix.MaterialPrefixCatalog;

import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

/**
 * GT6 {@code ToolStats.getBrokenItem}: scrapGt of the tool material, count
 * {@code 1+RNG.nextInt(1+(int)(4*mMaterialAmount/U))} when amount ≥ U/4.
 */
public final class ToolBreakScrap {
    private ToolBreakScrap() {}

    public static void hurtAndBreak(
            ItemStack stack,
            int amount,
            LivingEntity entity,
            EquipmentSlot slot) {
        if (entity == null) {
            return;
        }
        ItemStack snapshot = stack.copy();
        stack.hurtAndBreak(amount, entity, slot);
        if (stack.isEmpty()) {
            give(entity, forBrokenTool(snapshot, entity.getRandom()));
        }
    }

    public static ItemStack forBrokenTool(ItemStack tool) {
        return forBrokenTool(tool, RandomSource.create());
    }

    public static ItemStack forBrokenTool(ItemStack tool, RandomSource random) {
        if (!(tool.getItem() instanceof MaterialToolItem materialTool)) {
            return ItemStack.EMPTY;
        }
        Optional<String> materialId = materialTool.material(tool);
        if (materialId.isEmpty()) {
            return ItemStack.EMPTY;
        }
        int units = materialUnits(materialTool.kind());
        int bound = scrapRandomBound(units);
        if (bound <= 0) {
            return ItemStack.EMPTY;
        }
        int count = Math.min(64, 1 + random.nextInt(bound));
        return MaterialPrefixCatalog.find("scrap")
                .flatMap(prefix -> MaterialLookup.tryStack(
                        materialId.orElseThrow(), prefix, count))
                .orElse(ItemStack.EMPTY);
    }

    /** {@code 1+(int)(4*mMaterialAmount/U)}; 0 means GT6 returns no scrap. */
    static int scrapRandomBound(int materialUnits) {
        if (materialUnits <= 0) {
            return 0;
        }
        return 1 + (4 * materialUnits);
    }

    private static int materialUnits(ToolKind kind) {
        return switch (kind) {
            case PLUNGER, FLINT_AND_TINDER -> 0;
            case WRENCH, WIRE_CUTTER, BUTCHERY_KNIFE, CLUB -> 4;
            case ROLLING_PIN -> 2;
            case SCOOP -> 3;
            case BRANCH_CUTTER -> 5;
            default -> 1;
        };
    }

    private static void give(LivingEntity entity, ItemStack scrap) {
        if (scrap.isEmpty()) {
            return;
        }
        if (entity instanceof Player player) {
            if (!player.addItem(scrap)) {
                player.drop(scrap, false);
            }
            return;
        }
        entity.spawnAtLocation(scrap);
    }
}
