package com.masson.cruciblecraft.content.item;

import java.util.Set;

import com.masson.cruciblecraft.machine.ToolMaterialRules;
import com.masson.cruciblecraft.machine.ToolMaterialRules.ToolKind;

import net.minecraft.core.BlockPos;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.DiggerItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Tier;
import net.minecraft.world.item.component.ItemAttributeModifiers;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.common.ItemAbility;

/** Shared stack-sensitive behavior for component-driven digging tools. */
public abstract class MaterialDiggerItem extends MaterialToolItem {
    private final TagKey<Block> mineableBlocks;
    private final float baseAttackDamage;
    private final float attackSpeed;
    private final Set<ItemAbility> abilities;

    protected MaterialDiggerItem(
            Properties properties,
            ToolKind kind,
            String nameKey,
            TagKey<Block> mineableBlocks,
            float baseAttackDamage,
            float attackSpeed,
            Set<ItemAbility> abilities) {
        super(properties, kind, nameKey);
        this.mineableBlocks = mineableBlocks;
        this.baseAttackDamage = baseAttackDamage;
        this.attackSpeed = attackSpeed;
        this.abilities = Set.copyOf(abilities);
    }

    @Override
    public final float getDestroySpeed(ItemStack stack, BlockState state) {
        return material(stack)
                .filter(ignored -> state.is(mineableBlocks))
                .map(materialId -> ToolMaterialRules.miningSpeed(
                        kind(), materialId))
                .orElse(1.0F);
    }

    @Override
    public final boolean isCorrectToolForDrops(
            ItemStack stack,
            BlockState state) {
        return material(stack)
                .map(materialId -> state.is(mineableBlocks)
                        && !state.is(ToolMaterialRules.miningTier(
                                kind(), materialId)
                                .getIncorrectBlocksForDrops()))
                .orElse(false);
    }

    @Override
    public final ItemAttributeModifiers getDefaultAttributeModifiers(
            ItemStack stack) {
        return material(stack)
                .map(materialId -> {
                    Tier tier = ToolMaterialRules.miningTier(
                            kind(), materialId);
                    return DiggerItem.createAttributes(
                            tier,
                            baseAttackDamage(tier),
                            attackSpeed);
                })
                .orElse(ItemAttributeModifiers.EMPTY);
    }

    protected float baseAttackDamage(Tier tier) {
        return baseAttackDamage;
    }

    @Override
    public final boolean mineBlock(
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
                    1, miningEntity, EquipmentSlot.MAINHAND);
        }
        return true;
    }

    @Override
    public final boolean hurtEnemy(
            ItemStack stack,
            LivingEntity target,
            LivingEntity attacker) {
        return canApplyDurabilityDamage(stack);
    }

    @Override
    public final void postHurtEnemy(
            ItemStack stack,
            LivingEntity target,
            LivingEntity attacker) {
        if (canApplyDurabilityDamage(stack)) {
            stack.hurtAndBreak(
                    2, attacker, EquipmentSlot.MAINHAND);
        }
    }

    @Override
    public final boolean canPerformAction(
            ItemStack stack,
            ItemAbility itemAbility) {
        return canApplyDurabilityDamage(stack)
                && abilities.contains(itemAbility);
    }
}
