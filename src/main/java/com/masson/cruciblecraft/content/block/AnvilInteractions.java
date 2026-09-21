package com.masson.cruciblecraft.content.block;

import com.masson.cruciblecraft.api.material.MaterialLookup;
import com.masson.cruciblecraft.api.material.MaterialPrefixes;
import com.masson.cruciblecraft.api.tool.ToolAction;
import com.masson.cruciblecraft.api.tool.ToolResult;
import com.masson.cruciblecraft.api.unit.MaterialUnits;
import com.masson.cruciblecraft.content.blockentity.AnvilBlockEntity;
import com.masson.cruciblecraft.content.item.MaterialToolItem;
import com.masson.cruciblecraft.content.item.tool.ToolClick;
import com.masson.cruciblecraft.machine.MachineMaterialRules;
import com.masson.cruciblecraft.machine.MachineMaterialRules.Device;
import com.masson.cruciblecraft.recipe.AnvilStrikeContext;
import com.masson.cruciblecraft.registry.ModComponents;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

/** Shared GT6 anvil click / hammer / extract path for live and unique hosts. */
public final class AnvilInteractions {
    private AnvilInteractions() {}

    public static boolean canPlaceHeld(AnvilBlockEntity anvil, ItemStack stack) {
        if (AnvilHosts.isHammer(stack)) {
            return anvil.workpiece(0).isEmpty() && anvil.workpiece(1).isEmpty();
        }
        return !MaterialUnits.resolve(stack).isEmpty();
    }

    public static ItemInteractionResult useItemOn(
            ItemStack stack,
            Level level,
            BlockPos pos,
            Player player,
            BlockHitResult hitResult) {
        if (!(level.getBlockEntity(pos) instanceof AnvilBlockEntity anvil)
                || !canPlaceHeld(anvil, stack)) {
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        }
        if (level.isClientSide) {
            return hitResult.getDirection() == Direction.UP
                    ? ItemInteractionResult.SUCCESS
                    : ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        }
        if (anvil.materialQuarantined()) {
            player.displayClientMessage(
                    Component.translatable(
                            "message.cruciblecraft.anvil_material_quarantined",
                            anvil.quarantinedMaterialId()),
                    true);
            return ItemInteractionResult.SUCCESS;
        }
        if (hitResult.getDirection() != Direction.UP) {
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        }
        AnvilStrikeContext context = strikeContext(anvil.getBlockState(), pos, hitResult);
        int moved = anvil.insertOrMerge(context.topSlot(), stack);
        if (moved > 0) {
            if (!player.getAbilities().instabuild) {
                stack.shrink(moved);
            }
            player.displayClientMessage(
                    Component.translatable("message.cruciblecraft.anvil_inserted"),
                    true);
        } else {
            player.displayClientMessage(
                    Component.translatable("message.cruciblecraft.anvil_rejected"),
                    true);
        }
        return ItemInteractionResult.SUCCESS;
    }

    public static ToolResult useTool(ToolAction action, UseOnContext context) {
        if (action != ToolAction.HAMMER) {
            return ToolResult.PASS;
        }
        Level level = context.getLevel();
        BlockPos pos = context.getClickedPos();
        BlockHitResult hit = ToolClick.hit(context);
        AnvilStrikeContext strikeContext = strikeContext(level.getBlockState(pos), pos, hit);
        if (strikeContext.mode().isEmpty()) {
            return ToolResult.PASS;
        }
        Player player = context.getPlayer();
        if (player == null) {
            return ToolResult.PASS;
        }
        if (level.isClientSide) {
            return ToolResult.SUCCESS;
        }
        if (!(level.getBlockEntity(pos) instanceof AnvilBlockEntity anvil)) {
            return ToolResult.PASS;
        }
        if (anvil.workpiece(0).isEmpty() && anvil.workpiece(1).isEmpty()) {
            return ToolResult.PASS;
        }
        if (anvil.materialQuarantined()) {
            player.displayClientMessage(
                    Component.translatable(
                            "message.cruciblecraft.anvil_material_quarantined",
                            anvil.quarantinedMaterialId()),
                    true);
            return ToolResult.SUCCESS;
        }
        ItemStack stack = context.getItemInHand();
        if (!(stack.getItem() instanceof MaterialToolItem hammer)) {
            return ToolResult.PASS;
        }
        var hammerMaterial = hammer.material(stack);
        if (hammerMaterial.isEmpty()) {
            player.displayClientMessage(
                    Component.translatable(
                            "message.cruciblecraft.invalid_hammer_material",
                            stack.get(ModComponents.TOOL_MATERIAL)),
                    true);
            return ToolResult.SUCCESS;
        }
        var strike = anvil.strike(
                strikeContext.mode().orElseThrow(),
                MachineMaterialRules.processingTier(
                        Device.HAMMER,
                        hammerMaterial.orElseThrow()));
        if (strike.isEmpty()) {
            player.displayClientMessage(
                    Component.translatable("message.cruciblecraft.anvil_no_recipe"),
                    true);
            return ToolResult.SUCCESS;
        }
        ToolClick.hurt(context);
        level.playSound(
                null,
                pos,
                SoundEvents.ANVIL_LAND,
                SoundSource.BLOCKS,
                0.45f,
                1.1f + level.random.nextFloat() * 0.2f);
        if (strike.get().completed()) {
            for (ItemStack overflow : strike.get().overflow()) {
                if (!player.addItem(overflow)) {
                    dropAbove(level, pos, overflow);
                }
            }
            player.displayClientMessage(
                    Component.translatable(
                            "message.cruciblecraft.anvil_completed",
                            strike.get().result().getHoverName()),
                    true);
            if (strike.get().anvilExhausted()) {
                anvil.dropContents();
                dropAbove(level, pos, scrapFor(anvil.materialId()));
                level.destroyBlock(pos, false);
            }
        } else {
            player.displayClientMessage(
                    Component.translatable(
                            "message.cruciblecraft.anvil_progress",
                            strike.get().progress(),
                            strike.get().required()),
                    true);
        }
        return ToolResult.SUCCESS;
    }

    public static InteractionResult useWithoutItem(
            Level level,
            BlockPos pos,
            Player player,
            BlockHitResult hitResult) {
        if (!level.isClientSide && level.getBlockEntity(pos) instanceof AnvilBlockEntity anvil) {
            ItemStack returned;
            if (hitResult.getDirection() == Direction.UP) {
                returned = anvil.extract(
                        strikeContext(anvil.getBlockState(), pos, hitResult).topSlot());
            } else if (hitResult.getDirection().getAxis().isHorizontal()) {
                returned = anvil.splitBetweenSlots();
            } else {
                returned = ItemStack.EMPTY;
            }
            if (!returned.isEmpty() && !player.addItem(returned)) {
                dropAbove(level, pos, returned);
            }
        }
        return InteractionResult.SUCCESS;
    }

    public static AnvilStrikeContext strikeContext(
            BlockState state,
            BlockPos pos,
            BlockHitResult hit) {
        var location = hit.getLocation();
        return new AnvilStrikeContext(
                AnvilStrikeContext.Facing.valueOf(AnvilHosts.horizontalFacing(state).name()),
                AnvilStrikeContext.HitFace.valueOf(hit.getDirection().name()),
                location.x - pos.getX(),
                location.z - pos.getZ());
    }

    private static ItemStack scrapFor(String materialId) {
        if ("stone".equals(materialId)) {
            return new ItemStack(Items.COBBLESTONE, 2);
        }
        return MaterialLookup.tryStack(materialId, MaterialPrefixes.INGOT, 1)
                .orElseGet(() -> new ItemStack(Items.IRON_NUGGET, 4));
    }

    private static void dropAbove(Level level, BlockPos pos, ItemStack stack) {
        if (!stack.isEmpty()) {
            net.minecraft.world.Containers.dropItemStack(
                    level,
                    pos.getX() + 0.5,
                    pos.getY() + 1.2,
                    pos.getZ() + 0.5,
                    stack);
        }
    }
}
