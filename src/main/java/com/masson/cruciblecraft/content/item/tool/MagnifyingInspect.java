package com.masson.cruciblecraft.content.item.tool;

import java.util.ArrayList;
import java.util.List;

import com.masson.cruciblecraft.api.tool.MagnifyingInspectable;
import com.masson.cruciblecraft.api.tool.ToolResult;
import com.masson.cruciblecraft.content.block.GtBlockObjectBlock;
import com.masson.cruciblecraft.content.block.GtHostedOreBlock;
import com.masson.cruciblecraft.content.block.GtStoneBlock;
import com.masson.cruciblecraft.content.block.GtStoneSlabBlock;
import com.masson.cruciblecraft.content.blockentity.CeramicMoldBlockEntity;
import com.masson.cruciblecraft.content.blockentity.FluidPipeBlockEntity;
import com.masson.cruciblecraft.content.blockentity.FoundryCastingBlockEntity;
import com.masson.cruciblecraft.content.blockentity.GtBushBlockEntity;
import com.masson.cruciblecraft.content.blockentity.HopperBlockEntity;
import com.masson.cruciblecraft.content.blockentity.ItemPipeBlockEntity;
import com.masson.cruciblecraft.content.blockentity.MassStorageBlockEntity;
import com.masson.cruciblecraft.content.blockentity.ProcessingMachineBlockEntity;
import com.masson.cruciblecraft.logistics.machinecover.MachineCoverHost;
import com.masson.cruciblecraft.logistics.pipe.cover.CoverItemFilters;
import com.masson.cruciblecraft.logistics.pipe.cover.PipeCover;

import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.templates.FluidTank;
import net.neoforged.neoforge.items.ItemStackHandler;

/**
 * GT6 {@code TOOL_magnifyingglass}: chat real fluid / inventory / mode state.
 * Empty reports stay {@link ToolResult#PASS}; no invented numbers.
 */
public final class MagnifyingInspect {
    private MagnifyingInspect() {}

    public static ToolResult tryUse(UseOnContext context) {
        List<Component> lines = collect(context);
        if (lines.isEmpty()) {
            return ToolResult.PASS;
        }
        if (!context.getLevel().isClientSide) {
            Player player = context.getPlayer();
            if (player != null) {
                for (Component line : lines) {
                    player.displayClientMessage(line, false);
                }
            }
            ToolClick.hurt(context);
        }
        return ToolResult.SUCCESS;
    }

    public static List<Component> collect(UseOnContext context) {
        BlockState state = context.getLevel().getBlockState(context.getClickedPos());
        Block block = state.getBlock();
        BlockEntity blockEntity = context.getLevel().getBlockEntity(
                context.getClickedPos());
        List<Component> lines = new ArrayList<>();
        if (block instanceof MagnifyingInspectable inspectable) {
            lines.addAll(inspectable.magnifyingInspect(context));
        }
        if (blockEntity instanceof MagnifyingInspectable inspectable) {
            lines.addAll(inspectable.magnifyingInspect(context));
        }
        if (lines.isEmpty() && blockEntity instanceof ProcessingMachineBlockEntity machine) {
            lines.addAll(machine.magnifyingInspect(context));
        }
        if (lines.isEmpty() && blockEntity instanceof HopperBlockEntity hopper) {
            lines.addAll(hopperContents(hopper));
        }
        if (lines.isEmpty() && blockEntity instanceof MassStorageBlockEntity storage) {
            lines.addAll(massStorage(storage));
        }
        if (lines.isEmpty() && blockEntity instanceof FluidPipeBlockEntity pipe) {
            lines.addAll(fluidPipe(pipe, context));
        }
        if (lines.isEmpty() && blockEntity instanceof ItemPipeBlockEntity pipe) {
            coverLine(pipe.coverSnapshot().get(context.getClickedFace()))
                    .ifPresent(lines::add);
        }
        if (lines.isEmpty() && blockEntity instanceof CeramicMoldBlockEntity mold) {
            contents(mold.contentsStack()).ifPresent(lines::add);
        }
        if (lines.isEmpty() && blockEntity instanceof FoundryCastingBlockEntity foundry) {
            contents(foundry.contentsStack()).ifPresent(lines::add);
        }
        if (lines.isEmpty() && blockEntity instanceof GtBushBlockEntity bush) {
            lines.addAll(bush(bush));
        }
        if (blockEntity instanceof MachineCoverHost host) {
            host.covers().get(context.getClickedFace()).ifPresent(cover ->
                    coverLine(cover).ifPresent(line -> {
                        if (!lines.contains(line)) {
                            lines.add(line);
                        }
                    }));
        }
        if (lines.isEmpty()
                && (block instanceof GtStoneBlock
                        || block instanceof GtStoneSlabBlock
                        || block instanceof GtBlockObjectBlock
                        || block instanceof GtHostedOreBlock)) {
            lines.add(Component.translatable(block.getDescriptionId()));
        }
        return List.copyOf(lines);
    }

    private static List<Component> hopperContents(HopperBlockEntity hopper) {
        List<Component> lines = new ArrayList<>();
        ItemStackHandler inventory = hopper.inventory();
        for (int slot = 0; slot < inventory.getSlots(); slot++) {
            ItemStack stack = inventory.getStackInSlot(slot);
            if (stack.isEmpty()) {
                continue;
            }
            lines.add(Component.translatable(
                    "message.cruciblecraft.inspect.item",
                    stack.getHoverName(),
                    stack.getCount()));
        }
        if (lines.isEmpty()) {
            lines.add(Component.translatable(
                    "message.cruciblecraft.inspect.no_items"));
        }
        return lines;
    }

    private static List<Component> massStorage(MassStorageBlockEntity storage) {
        List<Component> lines = new ArrayList<>();
        ItemStack filter = storage.inventory().filter();
        if (!filter.isEmpty()) {
            lines.add(Component.translatable(
                    "message.cruciblecraft.inspect.filter",
                    filter.getHoverName()));
        }
        lines.add(Component.translatable(
                "message.cruciblecraft.inspect.stored",
                storage.inventory().stored(),
                storage.inventory().capacity()));
        lines.add(storage.filterMessage());
        return lines;
    }

    private static List<Component> fluidPipe(
            FluidPipeBlockEntity pipe, UseOnContext context) {
        List<Component> lines = new ArrayList<>();
        boolean empty = true;
        for (FluidTank tank : pipe.tanks()) {
            FluidStack fluid = tank.getFluid();
            if (fluid.isEmpty()) {
                continue;
            }
            empty = false;
            lines.add(Component.translatable(
                    "message.cruciblecraft.inspect.fluid",
                    fluid.getHoverName(),
                    fluid.getAmount()));
        }
        if (empty) {
            lines.add(Component.translatable(
                    "message.cruciblecraft.inspect.no_fluids"));
        }
        coverLine(pipe.coverSnapshot().get(context.getClickedFace()))
                .ifPresent(lines::add);
        return lines;
    }

    private static List<Component> bush(GtBushBlockEntity bush) {
        List<Component> lines = new ArrayList<>();
        ItemStack berry = bush.berry();
        if (!berry.isEmpty()) {
            lines.add(Component.translatable(
                    "message.cruciblecraft.inspect.berry",
                    berry.getHoverName()));
        }
        lines.add(Component.translatable(
                "message.cruciblecraft.inspect.stage",
                bush.stage()));
        return lines;
    }

    private static java.util.Optional<Component> contents(ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            return java.util.Optional.empty();
        }
        return java.util.Optional.of(Component.translatable(
                "message.cruciblecraft.inspect.item",
                stack.getHoverName(),
                stack.getCount()));
    }

    private static java.util.Optional<Component> coverLine(PipeCover cover) {
        if (cover == null) {
            return java.util.Optional.empty();
        }
        String match = cover.matchId().orElse("");
        if (match.isEmpty()) {
            return java.util.Optional.of(Component.translatable(
                    "message.cruciblecraft.inspect.cover",
                    cover.definitionId().toString()));
        }
        boolean inverted = CoverItemFilters.inverted(cover.config());
        return java.util.Optional.of(Component.translatable(
                inverted
                        ? "message.cruciblecraft.inspect.cover_filter_inverted"
                        : "message.cruciblecraft.inspect.cover_filter",
                cover.definitionId().toString(),
                match));
    }
}
