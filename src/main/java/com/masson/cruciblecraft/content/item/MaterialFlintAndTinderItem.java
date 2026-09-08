package com.masson.cruciblecraft.content.item;

import com.masson.cruciblecraft.machine.ToolMaterialRules.ToolKind;

import net.minecraft.advancements.CriteriaTriggers;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseFireBlock;
import net.minecraft.world.level.block.CampfireBlock;
import net.minecraft.world.level.block.CandleBlock;
import net.minecraft.world.level.block.CandleCakeBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gameevent.GameEvent;

/** GT6 flint and tinder: light campfires and place fire like flint and steel. */
public final class MaterialFlintAndTinderItem extends MaterialToolItem {
    public MaterialFlintAndTinderItem(Properties properties) {
        super(
                properties,
                ToolKind.FLINT_AND_TINDER,
                "item.cruciblecraft.material_flint_and_tinder");
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        InteractionResult tool = super.useOn(context);
        if (tool.consumesAction()) {
            return tool;
        }
        if (!canApplyDurabilityDamage(context.getItemInHand())) {
            return InteractionResult.PASS;
        }
        Level level = context.getLevel();
        BlockPos pos = context.getClickedPos();
        BlockState state = level.getBlockState(pos);
        if (CampfireBlock.canLight(state)
                || CandleBlock.canLight(state)
                || CandleCakeBlock.canLight(state)) {
            level.playSound(
                    context.getPlayer(),
                    pos,
                    SoundEvents.FLINTANDSTEEL_USE,
                    SoundSource.BLOCKS,
                    1.0F,
                    level.getRandom().nextFloat() * 0.4F + 0.8F);
            level.setBlock(
                    pos,
                    state.setValue(
                            net.minecraft.world.level.block.state.properties
                                    .BlockStateProperties.LIT,
                            true),
                    11);
            hurt(context);
            return InteractionResult.sidedSuccess(level.isClientSide);
        }
        BlockPos above = pos.relative(context.getClickedFace());
        if (!BaseFireBlock.canBePlacedAt(
                level, above, context.getHorizontalDirection())) {
            return InteractionResult.FAIL;
        }
        level.playSound(
                context.getPlayer(),
                above,
                SoundEvents.FLINTANDSTEEL_USE,
                SoundSource.BLOCKS,
                1.0F,
                level.getRandom().nextFloat() * 0.4F + 0.8F);
        level.setBlock(above, BaseFireBlock.getState(level, above), 11);
        level.gameEvent(
                context.getPlayer(), GameEvent.BLOCK_PLACE, above);
        if (context.getPlayer() instanceof ServerPlayer serverPlayer) {
            CriteriaTriggers.PLACED_BLOCK.trigger(
                    serverPlayer, above, context.getItemInHand());
        }
        hurt(context);
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    private static void hurt(UseOnContext context) {
        if (context.getPlayer() != null) {
            context.getItemInHand().hurtAndBreak(
                    1,
                    context.getPlayer(),
                    LivingEntity.getSlotForHand(context.getHand()));
        }
    }
}
