package com.masson.cruciblecraft.content.block;

import java.util.Locale;
import java.util.List;
import java.util.stream.Collectors;

import com.masson.cruciblecraft.api.unit.MaterialUnits;
import com.masson.cruciblecraft.content.blockentity.CrucibleBlockEntity;
import com.masson.cruciblecraft.heat.ItemHeat;
import com.masson.cruciblecraft.registry.ModBlockEntities;
import com.masson.cruciblecraft.registry.ModComponents;
import com.masson.cruciblecraft.registry.ModItems;
import com.masson.cruciblecraft.fluid.CrucibleInteractionMessages;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.BlockHitResult;
import net.neoforged.neoforge.fluids.FluidUtil;

import org.jetbrains.annotations.Nullable;

public class CrucibleBlock extends Block implements EntityBlock {
    public CrucibleBlock(Properties properties) {
        super(properties);
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
        if (level.getBlockEntity(pos) instanceof CrucibleBlockEntity fluidCrucible
                && FluidUtil.interactWithFluidHandler(
                        player,
                        hand,
                        fluidCrucible.externalFluids())) {
            return ItemInteractionResult.sidedSuccess(level.isClientSide);
        }
        var materialEntry = MaterialUnits.resolve(stack);
        if (materialEntry.isEmpty()) {
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        }
        if (level.isClientSide) {
            return ItemInteractionResult.SUCCESS;
        }
        if (!(level.getBlockEntity(pos) instanceof CrucibleBlockEntity crucible)) {
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        }

        var result = crucible.insert(
                materialEntry.orElseThrow(),
                ItemHeat.temperature(stack, level.getGameTime()));
        if (result == com.masson.cruciblecraft.fluid.CrucibleTransferCoordinator.InsertResult.SUCCESS) {
            if (!player.getAbilities().instabuild) {
                stack.shrink(1);
            }
            player.displayClientMessage(
                    Component.translatable("message.cruciblecraft.material_inserted"),
                    true);
        } else {
            player.displayClientMessage(
                    Component.translatable(CrucibleInteractionMessages.key(result)),
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
        if (!level.isClientSide && level.getBlockEntity(pos) instanceof CrucibleBlockEntity crucible) {
            String contents = crucible.composition().entrySet().stream()
                    .map(entry -> entry.getKey() + ": " + entry.getValue() + " u")
                    .collect(Collectors.joining(", "));
            player.displayClientMessage(
                    Component.translatable(
                            "message.cruciblecraft.crucible_status",
                            String.format(Locale.ROOT, "%.1f", crucible.temperatureCelsius()),
                            contents.isEmpty() ? "-" : contents),
                    true);
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new CrucibleBlockEntity(pos, state);
    }

    @Override
    protected List<ItemStack> getDrops(BlockState state, LootParams.Builder params) {
        List<ItemStack> drops = super.getDrops(state, params);
        if (!(params.getOptionalParameter(LootContextParams.BLOCK_ENTITY)
                instanceof CrucibleBlockEntity crucible)) {
            return drops;
        }
        for (ItemStack drop : drops) {
            if (drop.is(ModItems.CRUCIBLE.get())) {
                drop.set(ModComponents.MACHINE_MATERIAL, crucible.casingMaterialId());
            }
        }
        return drops;
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(
            Level level,
            BlockState state,
            BlockEntityType<T> type) {
        return createTicker(
                type,
                ModBlockEntities.CRUCIBLE.get(),
                level.isClientSide
                        ? CrucibleBlockEntity::clientTick
                        : CrucibleBlockEntity::serverTick);
    }

    @SuppressWarnings("unchecked")
    private static <T extends BlockEntity, E extends BlockEntity> BlockEntityTicker<T> createTicker(
            BlockEntityType<T> actual,
            BlockEntityType<E> expected,
            BlockEntityTicker<? super E> ticker) {
        return actual == expected ? (BlockEntityTicker<T>) ticker : null;
    }
}
