package com.masson.cruciblecraft.content.block;

import java.util.List;

import com.masson.cruciblecraft.api.unit.MaterialUnits;
import com.masson.cruciblecraft.content.blockentity.AnvilBlockEntity;
import com.masson.cruciblecraft.content.item.SmithingHammerItem;
import com.masson.cruciblecraft.api.material.MaterialPrefixes;
import com.masson.cruciblecraft.api.material.MaterialLookup;
import com.masson.cruciblecraft.machine.MachineMaterialRules;
import com.masson.cruciblecraft.recipe.AnvilStrikeContext;
import com.masson.cruciblecraft.registry.ModComponents;
import com.masson.cruciblecraft.registry.ModItems;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

public final class AnvilBlock extends Block implements EntityBlock {
    public static final DirectionProperty FACING = BlockStateProperties.HORIZONTAL_FACING;
    private static final VoxelShape SHAPE = Shapes.or(
            Block.box(2, 0, 2, 14, 4, 14),
            Block.box(5, 4, 5, 11, 10, 11),
            Block.box(1, 10, 1, 15, 16, 15));

    public AnvilBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH));
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return defaultBlockState().setValue(FACING, context.getHorizontalDirection());
    }

    @Override
    protected ItemInteractionResult useItemOn(
            ItemStack stack,
            BlockState state,
            Level level,
            BlockPos pos,
            Player player,
            InteractionHand hand,
            BlockHitResult hitResult) {
        boolean hammer = stack.getItem() instanceof SmithingHammerItem;
        boolean material = MaterialUnits.resolve(stack).isPresent();
        if (!hammer && !material) {
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        }
        if (level.isClientSide) {
            boolean acceptedFace = hammer
                    ? strikeContext(state, pos, hitResult).mode().isPresent()
                    : hitResult.getDirection() == Direction.UP;
            return acceptedFace
                    ? ItemInteractionResult.SUCCESS
                    : ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        }
        if (!(level.getBlockEntity(pos) instanceof AnvilBlockEntity anvil)) {
            return ItemInteractionResult.FAIL;
        }
        if (anvil.materialQuarantined()) {
            player.displayClientMessage(
                    Component.translatable(
                            "message.cruciblecraft.anvil_material_quarantined",
                            anvil.quarantinedMaterialId()),
                    true);
            return ItemInteractionResult.SUCCESS;
        }

        if (hammer) {
            AnvilStrikeContext context = strikeContext(state, pos, hitResult);
            var mode = context.mode();
            if (mode.isEmpty()) {
                return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
            }
            SmithingHammerItem hammerItem = (SmithingHammerItem) stack.getItem();
            var hammerMaterial = hammerItem.material(stack);
            if (hammerMaterial.isEmpty()) {
                player.displayClientMessage(
                        Component.translatable(
                                "message.cruciblecraft.invalid_hammer_material",
                                stack.get(ModComponents.TOOL_MATERIAL)),
                        true);
                return ItemInteractionResult.SUCCESS;
            }
            var strike = anvil.strike(
                    mode.get(),
                    MachineMaterialRules.processingTier(
                            MachineMaterialRules.Device.HAMMER,
                            hammerMaterial.orElseThrow()));
            if (strike.isEmpty()) {
                player.displayClientMessage(
                        Component.translatable("message.cruciblecraft.anvil_no_recipe"),
                        true);
                return ItemInteractionResult.SUCCESS;
            }

            if (!player.getAbilities().instabuild) {
                stack.hurtAndBreak(
                        1,
                        player,
                        hand == InteractionHand.MAIN_HAND
                                ? EquipmentSlot.MAINHAND
                                : EquipmentSlot.OFFHAND);
            }
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
            return ItemInteractionResult.SUCCESS;
        }

        AnvilStrikeContext context = strikeContext(state, pos, hitResult);
        if (hitResult.getDirection() != Direction.UP) {
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        }
        if (anvil.insert(context.topSlot(), stack)) {
            if (!player.getAbilities().instabuild) {
                stack.shrink(stack.getCount());
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

    @Override
    protected InteractionResult useWithoutItem(
            BlockState state,
            Level level,
            BlockPos pos,
            Player player,
            BlockHitResult hitResult) {
        if (!level.isClientSide && level.getBlockEntity(pos) instanceof AnvilBlockEntity anvil) {
            ItemStack returned;
            if (hitResult.getDirection() == Direction.UP) {
                returned = anvil.extract(strikeContext(state, pos, hitResult).topSlot());
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

    @Override
    protected void onRemove(
            BlockState state,
            Level level,
            BlockPos pos,
            BlockState newState,
            boolean movedByPiston) {
        if (state.getBlock() != newState.getBlock()
                && level.getBlockEntity(pos) instanceof AnvilBlockEntity anvil) {
            anvil.dropContents();
        }
        super.onRemove(state, level, pos, newState, movedByPiston);
    }

    @Override
    protected VoxelShape getShape(
            BlockState state,
            BlockGetter level,
            BlockPos pos,
            CollisionContext context) {
        return SHAPE;
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new AnvilBlockEntity(pos, state);
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING);
    }

    @Override
    protected List<ItemStack> getDrops(BlockState state, LootParams.Builder params) {
        List<ItemStack> drops = super.getDrops(state, params);
        if (params.getOptionalParameter(LootContextParams.BLOCK_ENTITY)
                instanceof AnvilBlockEntity anvil) {
            for (ItemStack drop : drops) {
                if (drop.is(ModItems.ANVIL.get())) {
                    drop.set(ModComponents.MACHINE_MATERIAL, anvil.materialId());
                    drop.set(ModComponents.MACHINE_DURABILITY, anvil.durabilityComponent());
                }
            }
        }
        return drops;
    }

    private static AnvilStrikeContext strikeContext(
            BlockState state,
            BlockPos pos,
            BlockHitResult hit) {
        var location = hit.getLocation();
        return new AnvilStrikeContext(
                AnvilStrikeContext.Facing.valueOf(state.getValue(FACING).name()),
                AnvilStrikeContext.HitFace.valueOf(hit.getDirection().name()),
                location.x - pos.getX(),
                location.z - pos.getZ());
    }

    private static ItemStack scrapFor(String materialId) {
        if ("stone".equals(materialId)) {
            return new ItemStack(Items.COBBLESTONE, 2);
        }
        return MaterialLookup.item(materialId, MaterialPrefixes.INGOT)
                .map(item -> new ItemStack(item, 1))
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
