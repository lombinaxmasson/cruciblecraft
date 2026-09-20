package com.masson.cruciblecraft.content.item.tool;

import com.masson.cruciblecraft.content.item.MaterialToolItem;
import com.masson.cruciblecraft.content.item.ToolBreakScrap;
import com.masson.cruciblecraft.machine.ToolMaterialRules;
import com.masson.cruciblecraft.registry.ModComponents;

import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

/**
 * GT6 {@code MultiItemTool.doDamage}: electric tools always spend EU; durability
 * only on {@code 1/max(10, quality*20)}. CC stores raw {@code mToolDurability},
 * so durability ticks are {@code max(1, gt6Amount / 100)}.
 */
public final class ElectricToolCharge {
    private ElectricToolCharge() {}

    public static boolean isElectric(ItemStack stack) {
        return capacity(stack) > 0L;
    }

    public static long charge(ItemStack stack) {
        return stack.getOrDefault(ModComponents.ELECTRIC_CHARGE.get(), 0L);
    }

    public static long capacity(ItemStack stack) {
        return stack.getOrDefault(ModComponents.ELECTRIC_CAPACITY.get(), 0L);
    }

    public static long voltage(ItemStack stack) {
        return stack.getOrDefault(ModComponents.ELECTRIC_VOLTAGE.get(), 0L);
    }

    public static void applyEmpty(
            ItemStack stack, long capacity, long voltage) {
        stack.set(ModComponents.ELECTRIC_CHARGE.get(), 0L);
        stack.set(ModComponents.ELECTRIC_CAPACITY.get(), Math.max(0L, capacity));
        stack.set(ModComponents.ELECTRIC_VOLTAGE.get(), Math.max(0L, voltage));
    }

    public static void setCharge(ItemStack stack, long value) {
        long cap = capacity(stack);
        if (cap <= 0L) {
            return;
        }
        stack.set(
                ModComponents.ELECTRIC_CHARGE.get(),
                Math.max(0L, Math.min(cap, value)));
    }

    public static boolean hasCharge(ItemStack stack) {
        return !isElectric(stack) || charge(stack) > 0L;
    }

    public static boolean spend(
            ItemStack stack,
            int gt6Amount,
            LivingEntity entity,
            EquipmentSlot slot) {
        if (entity instanceof Player player && player.getAbilities().instabuild) {
            return true;
        }
        return spendInternal(stack, gt6Amount, entity.getRandom(), entity, slot);
    }

    public static boolean spendCraft(ItemStack stack, int gt6Amount) {
        return spendInternal(stack, gt6Amount, RandomSource.create(), null, null);
    }

    private static boolean spendInternal(
            ItemStack stack,
            int gt6Amount,
            RandomSource random,
            LivingEntity entity,
            EquipmentSlot slot) {
        if (gt6Amount < 0) {
            return false;
        }
        boolean electric = isElectric(stack);
        if (electric) {
            long stored = charge(stack);
            if (stored <= 0L) {
                return false;
            }
            setCharge(stack, stored - Math.min(stored, gt6Amount));
        }
        int quality = 1;
        if (stack.getItem() instanceof MaterialToolItem tool) {
            quality = tool.material(stack)
                    .map(id -> ToolMaterialRules.requireStats(tool.kind(), id)
                            .quality())
                    .orElse(1);
        }
        boolean rollDurability = !electric
                || random.nextInt(Math.max(10, quality * 20)) == 0;
        if (rollDurability && gt6Amount > 0) {
            int vanilla = Math.max(1, gt6Amount / 100);
            if (entity != null && slot != null) {
                ToolBreakScrap.hurtAndBreak(stack, vanilla, entity, slot);
            } else {
                int next = stack.getDamageValue() + vanilla;
                if (next >= stack.getMaxDamage()) {
                    stack.setCount(0);
                } else {
                    stack.setDamageValue(next);
                }
            }
        }
        return true;
    }
}
