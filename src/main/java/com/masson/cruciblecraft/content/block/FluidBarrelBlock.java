package com.masson.cruciblecraft.content.block;

import java.util.List;

import org.jetbrains.annotations.Nullable;

import com.masson.cruciblecraft.api.tool.ToolAction;
import com.masson.cruciblecraft.api.tool.ToolInteractable;
import com.masson.cruciblecraft.api.tool.ToolResult;
import com.masson.cruciblecraft.content.blockentity.FluidBarrelBlockEntity;
import com.masson.cruciblecraft.content.fluidbarrel.FluidBarrelContents;
import com.masson.cruciblecraft.content.fluidbarrel.FluidBarrelFluids;
import com.masson.cruciblecraft.content.fluidbarrel.FluidBarrelProfile;
import com.masson.cruciblecraft.content.fluidbarrel.FluidBarrelText;
import com.masson.cruciblecraft.content.item.tool.ToolClick;
import com.masson.cruciblecraft.registry.ModBlockEntities;
import com.masson.cruciblecraft.registry.ModComponents;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.fluids.FluidStack;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.BlockHitResult;

/**
 * One GT6 wooden barrel, plastic canister, metal drum, or logistics tank.
 * Decorative covers are not hosted on this block.
 */
public final class FluidBarrelBlock extends Block
        implements EntityBlock, ToolInteractable {
    private final FluidBarrelProfile profile;

    public FluidBarrelBlock(FluidBarrelProfile profile, Properties properties) {
        super(properties);
        this.profile = profile;
    }

    public FluidBarrelProfile profile() {
        return profile;
    }

    @Override
    public MutableComponent getName() {
        return FluidBarrelText.name(profile);
    }

    @Override
    public void appendHoverText(
            ItemStack stack,
            Item.TooltipContext context,
            List<Component> tooltip,
            TooltipFlag flag) {
        FluidBarrelContents contents = stack.getOrDefault(
                ModComponents.FLUID_BARREL.get(), FluidBarrelContents.EMPTY);
        if (!contents.fluidId().isEmpty()) {
            tooltip.add(Component.translatable(
                    "tooltip.cruciblecraft.fluid_barrel.contents",
                    fluidName(contents.fluidId()),
                    contents.amount(),
                    profile.capacity()));
            if (contents.sealed() && contents.amount() > 0L) {
                tooltip.add(Component.translatable(
                        "tooltip.cruciblecraft.fluid_barrel.sealed_progress",
                        contents.sealedTime()));
            }
        } else {
            tooltip.add(Component.translatable(
                    "tooltip.cruciblecraft.fluid_barrel.capacity",
                    profile.capacity()));
        }
        tooltip.add(Component.translatable(
                "tooltip.cruciblecraft.fluid_barrel.no_gui"));
        tooltip.add(Component.translatable(
                "tooltip.cruciblecraft.fluid_barrel.no_power"));
        if (profile.onlySimple()) {
            tooltip.add(Component.translatable(
                    "tooltip.cruciblecraft.fluid_barrel.only_simple"));
        }
        if (profile.gasProof()) {
            tooltip.add(Component.translatable(
                    "tooltip.cruciblecraft.fluid_barrel.gas_proof"));
        }
        if (profile.acidProof()) {
            tooltip.add(Component.translatable(
                    "tooltip.cruciblecraft.fluid_barrel.acid_proof"));
        }
        if (profile.plasmaProof()) {
            tooltip.add(Component.translatable(
                    "tooltip.cruciblecraft.fluid_barrel.plasma_proof"));
        }
        if (profile.magicProof()) {
            tooltip.add(Component.translatable(
                    "tooltip.cruciblecraft.fluid_barrel.magic_proof"));
        }
        if (profile.meltingPoint() < Long.MAX_VALUE) {
            tooltip.add(Component.translatable(
                    "tooltip.cruciblecraft.fluid_barrel.meltdown",
                    profile.meltingPoint()));
        }
        tooltip.add(Component.translatable(
                "tooltip.cruciblecraft.fluid_barrel.wrench"));
        tooltip.add(Component.translatable(
                "tooltip.cruciblecraft.fluid_barrel.hammer"));
        tooltip.add(Component.translatable(
                "tooltip.cruciblecraft.fluid_barrel.glass"));
        super.appendHoverText(stack, context, tooltip, flag);
    }

    private static Component fluidName(String fluidId) {
        Fluid fluid = FluidBarrelFluids.fluid(fluidId);
        if (fluid == Fluids.EMPTY) {
            return Component.literal(fluidId);
        }
        return new FluidStack(fluid, 1).getHoverName();
    }

    @Override
    public void setPlacedBy(
            Level level,
            BlockPos pos,
            BlockState state,
            @Nullable LivingEntity placer,
            ItemStack stack) {
        if (level.getBlockEntity(pos) instanceof FluidBarrelBlockEntity barrel) {
            barrel.readFromItem(stack);
        }
    }

    @Override
    public ItemStack getCloneItemStack(
            LevelReader level, BlockPos pos, BlockState state) {
        ItemStack stack = new ItemStack(this);
        if (level.getBlockEntity(pos) instanceof FluidBarrelBlockEntity barrel) {
            barrel.writeToItem(stack);
        }
        return stack;
    }

    @Override
    protected List<ItemStack> getDrops(
            BlockState state, LootParams.Builder params) {
        ItemStack stack = new ItemStack(this);
        if (params.getOptionalParameter(LootContextParams.BLOCK_ENTITY)
                instanceof FluidBarrelBlockEntity barrel) {
            barrel.writeToItem(stack);
        }
        return List.of(stack);
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
        if (!ToolClick.canDispatch(stack)) {
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        }
        return ToolClick.useItemOn(stack, level, player, hand, hitResult);
    }

    @Override
    public ToolResult useTool(ToolAction action, UseOnContext context) {
        if (!(context.getLevel().getBlockEntity(context.getClickedPos())
                instanceof FluidBarrelBlockEntity barrel)) {
            return ToolResult.PASS;
        }
        return barrel.useTool(action, context);
    }

    @Override
    public int getFlammability(
            BlockState state,
            BlockGetter level,
            BlockPos pos,
            Direction direction) {
        return Math.max(0, profile.flammability());
    }

    @Override
    public int getFireSpreadSpeed(
            BlockState state,
            BlockGetter level,
            BlockPos pos,
            Direction direction) {
        return Math.max(0, profile.flammability());
    }

    @Override
    public int getLightEmission(
            BlockState state, BlockGetter level, BlockPos pos) {
        return profile.glowing() ? 15 : 0;
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new FluidBarrelBlockEntity(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(
            Level level, BlockState state, BlockEntityType<T> type) {
        return !level.isClientSide && type == ModBlockEntities.FLUID_BARREL.get()
                ? (current, pos, currentState, blockEntity) ->
                        FluidBarrelBlockEntity.serverTick(
                                current,
                                pos,
                                currentState,
                                (FluidBarrelBlockEntity) blockEntity)
                : null;
    }
}
