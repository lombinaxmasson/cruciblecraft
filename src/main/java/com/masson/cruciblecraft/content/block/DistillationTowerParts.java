package com.masson.cruciblecraft.content.block;

import com.masson.cruciblecraft.CrucibleCraft;
import com.masson.cruciblecraft.api.tool.ToolAction;
import com.masson.cruciblecraft.api.tool.ToolInteractable;
import com.masson.cruciblecraft.api.tool.ToolResult;
import com.masson.cruciblecraft.content.blockentity.ConfiguredProcessingMachineBlockEntity;
import com.masson.cruciblecraft.content.blockentity.CryoDistillationTowerBlockEntity;
import com.masson.cruciblecraft.content.blockentity.DistillationTowerBlockEntity;
import com.masson.cruciblecraft.content.blockentity.MultiblockPortBlockEntity;
import com.masson.cruciblecraft.content.blockentity.ProcessingMachineBlockEntity;
import com.masson.cruciblecraft.content.item.tool.ToolClick;
import com.masson.cruciblecraft.content.mte.MteInPlaceSpec;
import com.masson.cruciblecraft.content.multiblock.MultiblockStructureDefinition.PortType;
import com.masson.cruciblecraft.logistics.machinecover.MachineCoverBlockInteraction;
import com.masson.cruciblecraft.logistics.machinecover.MachineCoverHost;
import com.masson.cruciblecraft.registry.ModBlocks;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

/**
 * GT6 18101 heat transmitter and 18102 distillation-tower part. One 18102
 * block is both y=0 {@code ITEM_FLUID} and y=1..7 {@code FLUID_OUT}; bind
 * assigns the mode from the structure JSON.
 */
public final class DistillationTowerParts {
    public static final ResourceLocation HEAT_TRANSMITTER =
            ResourceLocation.fromNamespaceAndPath(
                    CrucibleCraft.MODID, "multiblock/heat_transmitter");
    public static final ResourceLocation TOWER_PART =
            ResourceLocation.fromNamespaceAndPath(
                    CrucibleCraft.MODID, "multiblock/distillation_tower_part");

    private DistillationTowerParts() {}

    public static boolean isHeatTransmitter(MteInPlaceSpec spec) {
        return spec != null && spec.meta() == 18101;
    }

    public static boolean isTowerPart(MteInPlaceSpec spec) {
        return spec != null && spec.meta() == 18102;
    }

    public static boolean isLivePort(MteInPlaceSpec spec) {
        return isHeatTransmitter(spec) || isTowerPart(spec);
    }

    public static boolean usesTowerSkin(MteInPlaceSpec spec) {
        return isLivePort(spec);
    }

    public static PortType defaultType(MteInPlaceSpec spec) {
        return isHeatTransmitter(spec)
                ? PortType.ENERGY_INPUT
                : PortType.ITEM_FLUID;
    }

    public static boolean accepts(MteInPlaceSpec spec, PortType type) {
        if (type == null || spec == null) {
            return false;
        }
        if (isHeatTransmitter(spec)) {
            return type == PortType.ENERGY_INPUT;
        }
        if (isTowerPart(spec)) {
            return type == PortType.ITEM_FLUID || type == PortType.FLUID_OUT;
        }
        return false;
    }

    public static Block heatTransmitter() {
        return ModBlocks.mteInPlaceBlocksById().get(HEAT_TRANSMITTER).get();
    }

    public static Block towerPart() {
        return ModBlocks.mteInPlaceBlocksById().get(TOWER_PART).get();
    }

    public static Block blockFor(PortType type) {
        return type == PortType.ENERGY_INPUT ? heatTransmitter() : towerPart();
    }

    public static ToolResult useTool(ToolAction action, UseOnContext context) {
        Level level = context.getLevel();
        BlockPos pos = context.getClickedPos();
        if (!(level.getBlockEntity(pos) instanceof MultiblockPortBlockEntity port)) {
            return ToolResult.PASS;
        }
        if (port instanceof MachineCoverHost machine) {
            ToolResult cover = MachineCoverBlockInteraction.useTool(
                    machine, action, context);
            if (cover != ToolResult.PASS) {
                return cover;
            }
        }
        if (action == ToolAction.WRENCH) {
            return rotateFacing(context);
        }
        return forwardToolToController(port, action, context);
    }

    public static InteractionResult useWithoutItem(
            Level level,
            BlockPos pos,
            Player player,
            BlockHitResult hit) {
        if (!(level.getBlockEntity(pos) instanceof MultiblockPortBlockEntity port)) {
            return InteractionResult.PASS;
        }
        if (port instanceof MachineCoverHost machine
                && MachineCoverBlockInteraction.rightClick(
                        machine, level, pos, player, hit)) {
            return InteractionResult.SUCCESS;
        }
        BlockPos controller = port.controllerPosition().orElse(null);
        if (controller == null || player == null) {
            return InteractionResult.PASS;
        }
        if (level.isClientSide) {
            return InteractionResult.SUCCESS;
        }
        if (!(player instanceof ServerPlayer server)) {
            return InteractionResult.CONSUME;
        }
        BlockEntity host = level.getBlockEntity(controller);
        if (host instanceof DistillationTowerBlockEntity tower) {
            server.openMenu(tower, data -> data.writeBlockPos(controller));
            return InteractionResult.CONSUME;
        }
        if (host instanceof CryoDistillationTowerBlockEntity cryo) {
            server.openMenu(cryo, data -> data.writeBlockPos(controller));
            return InteractionResult.CONSUME;
        }
        if (host instanceof ConfiguredProcessingMachineBlockEntity machine) {
            server.openMenu(machine, data -> data.writeBlockPos(controller));
            return InteractionResult.CONSUME;
        }
        return InteractionResult.PASS;
    }

    public static void dropCovers(Level level, BlockPos pos) {
        MachineCoverBlockInteraction.dropCovers(level, pos);
    }

    private static ToolResult rotateFacing(UseOnContext context) {
        Level level = context.getLevel();
        BlockPos pos = context.getClickedPos();
        BlockState state = level.getBlockState(pos);
        if (!state.hasProperty(MteInPlaceBlock.FACING)) {
            return ToolResult.PASS;
        }
        if (!level.isClientSide) {
            Direction current = state.getValue(MteInPlaceBlock.FACING);
            level.setBlock(
                    pos,
                    state.setValue(MteInPlaceBlock.FACING, current.getClockWise()),
                    Block.UPDATE_ALL);
            ToolClick.hurt(context);
        }
        return ToolResult.SUCCESS;
    }

    private static ToolResult forwardToolToController(
            MultiblockPortBlockEntity port,
            ToolAction action,
            UseOnContext context) {
        BlockPos controller = port.controllerPosition().orElse(null);
        if (controller == null || context.getLevel() == null) {
            return ToolResult.PASS;
        }
        Level level = context.getLevel();
        BlockEntity host = level.getBlockEntity(controller);
        if (host instanceof ProcessingMachineBlockEntity processing
                && processing.handleIoTool(
                        action, context.getClickedFace(), context.getPlayer())) {
            if (!level.isClientSide) {
                ToolClick.hurt(context);
            }
            return ToolResult.SUCCESS;
        }
        BlockState state = level.getBlockState(controller);
        if (!(state.getBlock() instanceof ToolInteractable interactable)) {
            return ToolResult.PASS;
        }
        BlockHitResult hit = new BlockHitResult(
                context.getClickLocation(),
                context.getClickedFace(),
                controller,
                false);
        UseOnContext forwarded = new UseOnContext(
                level,
                context.getPlayer(),
                context.getHand(),
                context.getItemInHand(),
                hit);
        return interactable.useTool(action, forwarded);
    }
}
