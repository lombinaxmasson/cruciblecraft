package com.masson.cruciblecraft.content.item;

import com.masson.cruciblecraft.machine.ToolMaterialRules;
import com.masson.cruciblecraft.machine.ToolMaterialRules.ToolKind;

import net.minecraft.core.BlockPos;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.item.component.ItemAttributeModifiers;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.common.ItemAbilities;
import net.neoforged.neoforge.common.ItemAbility;

/** Component-driven sword without persisted tool or attribute components. */
public final class MaterialSwordItem extends MaterialToolItem {
    public MaterialSwordItem(Properties properties) {
        super(
                properties,
                ToolKind.SWORD,
                "item.cruciblecraft.material_sword");
    }

    @Override
    public ItemAttributeModifiers getDefaultAttributeModifiers(
            ItemStack stack) {
        return material(stack)
                .map(materialId -> SwordItem.createAttributes(
                        ToolMaterialRules.miningTier(
                                ToolKind.SWORD, materialId),
                        3.0F,
                        -2.4F))
                .orElse(ItemAttributeModifiers.EMPTY);
    }

    @Override
    public float getDestroySpeed(ItemStack stack, BlockState state) {
        if (material(stack).isEmpty()) {
            return 1.0F;
        }
        if (state.is(Blocks.COBWEB)) {
            return 15.0F;
        }
        return state.is(BlockTags.SWORD_EFFICIENT) ? 1.5F : 1.0F;
    }

    @Override
    public boolean isCorrectToolForDrops(
            ItemStack stack,
            BlockState state) {
        return material(stack).isPresent() && state.is(Blocks.COBWEB);
    }

    @Override
    public boolean canAttackBlock(
            BlockState state,
            Level level,
            BlockPos pos,
            Player player) {
        return !player.isCreative();
    }

    @Override
    public boolean mineBlock(
            ItemStack stack,
            Level level,
            BlockState state,
            BlockPos pos,
            LivingEntity miningEntity) {
        if (!canApplyDurabilityDamage(stack)) {
            return false;
        }
        if (!level.isClientSide
                && state.getDestroySpeed(level, pos) != 0.0F) {
            stack.hurtAndBreak(
                    2, miningEntity, EquipmentSlot.MAINHAND);
        }
        return true;
    }

    @Override
    public boolean hurtEnemy(
            ItemStack stack,
            LivingEntity target,
            LivingEntity attacker) {
        return canApplyDurabilityDamage(stack);
    }

    @Override
    public void postHurtEnemy(
            ItemStack stack,
            LivingEntity target,
            LivingEntity attacker) {
        if (canApplyDurabilityDamage(stack)) {
            stack.hurtAndBreak(
                    1, attacker, EquipmentSlot.MAINHAND);
        }
    }

    @Override
    public boolean canPerformAction(
            ItemStack stack,
            ItemAbility itemAbility) {
        return canApplyDurabilityDamage(stack)
                && ItemAbilities.DEFAULT_SWORD_ACTIONS.contains(itemAbility);
    }
}
