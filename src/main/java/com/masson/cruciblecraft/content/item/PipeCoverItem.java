package com.masson.cruciblecraft.content.item;

import java.util.List;

import com.masson.cruciblecraft.content.blockentity.CableBlockEntity;
import com.masson.cruciblecraft.content.blockentity.FluidPipeBlockEntity;
import com.masson.cruciblecraft.content.blockentity.ItemPipeBlockEntity;
import com.masson.cruciblecraft.content.blockentity.RedstoneWireBlockEntity;
import com.masson.cruciblecraft.content.redstonewire.RedstoneWireCovers;
import com.masson.cruciblecraft.energy.cable.CableCovers;
import com.masson.cruciblecraft.logistics.displaycpu.DisplayCpuKinds;
import com.masson.cruciblecraft.logistics.machinecover.MachineCoverBehaviors;
import com.masson.cruciblecraft.logistics.machinecover.MachineCoverHost;
import com.masson.cruciblecraft.logistics.machinecover.MachineCoverKinds;
import com.masson.cruciblecraft.logistics.pipe.cover.CoverDefinition;
import com.masson.cruciblecraft.logistics.pipe.cover.CoverDefinitionCatalog;
import com.masson.cruciblecraft.logistics.pipe.cover.CoverComponentTiers;
import com.masson.cruciblecraft.logistics.pipe.cover.CoverPlacement;
import com.masson.cruciblecraft.logistics.pipe.cover.CoverSounds;
import com.masson.cruciblecraft.logistics.pipe.cover.PipeCover;
import com.masson.cruciblecraft.logistics.pipe.cover.PipeCoverConfig;
import com.masson.cruciblecraft.logistics.pipe.cover.PipeCoverType;

import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.ChatFormatting;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.neoforged.neoforge.fluids.FluidUtil;

/** Installs one data-defined pipe cover and exposes its bounded parameters. */
public final class PipeCoverItem extends Item {
    private final ResourceLocation definitionId;

    public PipeCoverItem(PipeCoverType type, Properties properties) {
        this(type.definitionId(), properties);
    }

    public PipeCoverItem(String definitionId, Properties properties) {
        this(ResourceLocation.parse(definitionId), properties);
    }

    public PipeCoverItem(
            ResourceLocation definitionId, Properties properties) {
        super(properties);
        this.definitionId = CoverDefinitionCatalog.require(
                definitionId).id();
    }

    @Deprecated(forRemoval = false)
    public PipeCoverType type() {
        return PipeCoverType.fromDefinition(definitionId)
                .orElseThrow(() -> new IllegalStateException(
                        "Definition has no legacy cover type " + definitionId));
    }

    public ResourceLocation definitionId() {
        return definitionId;
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        var blockEntity = context.getLevel().getBlockEntity(
                context.getClickedPos());
        Direction side = CoverPlacement.placeSide(
                blockEntity, CoverPlacement.hit(context));
        if (blockEntity instanceof MachineCoverHost machine) {
            PipeCover cover = PipeCover.of(definitionId);
            if (!MachineCoverBehaviors.canPlace(machine, side, cover)) {
                return InteractionResult.FAIL;
            }
            boolean changed = machine.setCover(side, cover);
            if (changed) {
                CoverSounds.placed(
                        context.getLevel(),
                        context.getClickedPos(),
                        cover);
            }
            if (changed
                    && context.getPlayer() != null
                    && !context.getPlayer().getAbilities().instabuild) {
                context.getItemInHand().shrink(1);
            }
            return InteractionResult.sidedSuccess(
                    context.getLevel().isClientSide);
        }
        if (blockEntity instanceof CableBlockEntity) {
            if (!CableCovers.tryInstall(
                    context.getLevel(),
                    context.getClickedPos(),
                    CoverPlacement.hit(context),
                    context.getItemInHand(),
                    context.getPlayer())) {
                return InteractionResult.FAIL;
            }
            return InteractionResult.sidedSuccess(
                    context.getLevel().isClientSide);
        }
        if (blockEntity instanceof RedstoneWireBlockEntity) {
            if (!RedstoneWireCovers.tryInstall(
                    context.getLevel(),
                    context.getClickedPos(),
                    CoverPlacement.hit(context),
                    context.getItemInHand(),
                    context.getPlayer())) {
                return InteractionResult.FAIL;
            }
            return InteractionResult.sidedSuccess(
                    context.getLevel().isClientSide);
        }
        if (MachineCoverKinds.requiresMachineHost(definitionId)) {
            return InteractionResult.FAIL;
        }
        CoverDefinition.Medium medium =
                blockEntity instanceof FluidPipeBlockEntity
                        ? CoverDefinition.Medium.FLUID
                        : CoverDefinition.Medium.ITEM;
        if (!(blockEntity instanceof FluidPipeBlockEntity)
                && !(blockEntity instanceof ItemPipeBlockEntity)) {
            return InteractionResult.PASS;
        }
        if (!CoverDefinitionCatalog.require(definitionId)
                .medium().supports(medium)) {
            return InteractionResult.FAIL;
        }
        if (context.getPlayer() != null
                && context.getPlayer().isShiftKeyDown()
                && cycleExistingSelector(context, blockEntity)) {
            return InteractionResult.sidedSuccess(
                    context.getLevel().isClientSide);
        }
        PipeCover cover = cover(
                context, blockEntity instanceof FluidPipeBlockEntity);
        boolean changed;
        if (blockEntity instanceof FluidPipeBlockEntity pipe) {
            changed = pipe.setCover(side, cover);
        } else if (blockEntity instanceof ItemPipeBlockEntity pipe) {
            changed = pipe.setCover(side, cover);
        } else {
            return InteractionResult.PASS;
        }
        if (changed
                && context.getPlayer() != null
                && !context.getPlayer().getAbilities().instabuild) {
            context.getItemInHand().shrink(1);
        }
        if (changed) {
            CoverSounds.placed(
                    context.getLevel(),
                    context.getClickedPos(),
                    cover);
        }
        return InteractionResult.sidedSuccess(
                context.getLevel().isClientSide);
    }

    private PipeCover cover(
            UseOnContext context, boolean fluidPipe) {
        CoverDefinition definition =
                CoverDefinitionCatalog.require(definitionId);
        PipeCover base = PipeCover.of(definitionId);
        if (fluidPipe
                && CoverComponentTiers.findByDefinition(definitionId)
                        .map(entry -> entry.family() == CoverComponentTiers.Family.PUMP)
                        .orElse(false)) {
            base = base.withDisplay(1, base.config().redstone());
        }
        if (!definition.configurable().contains(
                    CoverDefinition.ConfigField.MATCH_ID)
                || context.getPlayer() == null
                || context.getPlayer().getOffhandItem().isEmpty()) {
            return base;
        }
        if (fluidPipe) {
            var handler = FluidUtil.getFluidHandler(
                    context.getPlayer().getOffhandItem());
            if (handler.isPresent() && handler.orElseThrow().getTanks() > 0) {
                var fluid = handler.orElseThrow().getFluidInTank(0);
                if (!fluid.isEmpty()) {
                    return base.withConfig(withMatch(
                            base,
                            BuiltInRegistries.FLUID.getKey(
                                    fluid.getFluid()).toString()));
                }
            }
        }
        String itemId = BuiltInRegistries.ITEM.getKey(
                context.getPlayer().getOffhandItem().getItem()).toString();
        return base.withConfig(withMatch(base, itemId));
    }

    private static PipeCoverConfig withMatch(
            PipeCover cover, String matchId) {
        return cover.config().withMatchId(matchId);
    }

    private boolean cycleExistingSelector(
            UseOnContext context, Object blockEntity) {
        Direction side = CoverPlacement.interactSide(
                context.getLevel().getBlockEntity(context.getClickedPos()),
                CoverPlacement.hit(context));
        PipeCover current;
        if (blockEntity instanceof FluidPipeBlockEntity pipe) {
            current = pipe.coverSnapshot().get(side);
        } else {
            current = ((ItemPipeBlockEntity) blockEntity)
                    .coverSnapshot().get(side);
        }
        if (current == null
                || !current.definitionId().equals(definitionId)) {
            return false;
        }
        CoverDefinition definition =
                CoverDefinitionCatalog.require(definitionId);
        if ("retriever_item".equals(definition.behaviorId().getPath())) {
            if (context.getLevel().isClientSide) {
                return true;
            }
            if (blockEntity instanceof FluidPipeBlockEntity pipe) {
                return pipe.toggleCoverInvert(side);
            }
            return ((ItemPipeBlockEntity) blockEntity).toggleCoverInvert(side);
        }
        CoverDefinition.ConfigField field;
        int max;
        int currentValue;
        if (definition.configurable().contains(
                CoverDefinition.ConfigField.NETWORK_ID)) {
            field = CoverDefinition.ConfigField.NETWORK_ID;
            max = CoverDefinition.MAX_NETWORK_ID;
            currentValue = current.config().networkId().orElse(0);
        } else if (definition.configurable().contains(
                CoverDefinition.ConfigField.SELECTOR)) {
            field = CoverDefinition.ConfigField.SELECTOR;
            max = CoverDefinition.MAX_SELECTOR;
            currentValue = definition.resolve(current.config()).selector();
        } else {
            return false;
        }
        int next = Math.floorMod(currentValue + 1, max + 1);
        if (context.getLevel().isClientSide) {
            return true;
        }
        if (blockEntity instanceof FluidPipeBlockEntity pipe) {
            return pipe.configureCover(side, field, next);
        }
        return ((ItemPipeBlockEntity) blockEntity).configureCover(
                side, field, next);
    }

    @Override
    public void appendHoverText(
            ItemStack stack,
            TooltipContext context,
            List<Component> tooltip,
            TooltipFlag flag) {
        super.appendHoverText(stack, context, tooltip, flag);
        CoverDefinition definition =
                CoverDefinitionCatalog.require(definitionId);
        if (DisplayCpuKinds.isDisplay(definition.id())) {
            tooltip.add(Component.translatable(
                            "tooltip.cruciblecraft.cover.display_cpu")
                    .withStyle(ChatFormatting.GRAY));
            return;
        }
        CoverDefinition.Values values = definition.values();
        tooltip.add(Component.translatable(
                        "tooltip.cruciblecraft.cover.behavior",
                        definition.behaviorId().getPath())
                .withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.translatable(
                        "tooltip.cruciblecraft.cover.parameters",
                        values.rate(),
                        values.exactCount(),
                        values.mode().name().toLowerCase(
                                java.util.Locale.ROOT),
                        values.pressureThreshold(),
                        values.selector())
                .withStyle(ChatFormatting.GRAY));
        if (values.interval() > 1) {
            tooltip.add(Component.translatable(
                            "tooltip.cruciblecraft.cover.interval",
                            values.interval())
                    .withStyle(ChatFormatting.GRAY));
        }
        if ("retriever_item".equals(definition.behaviorId().getPath())) {
            tooltip.add(Component.translatable(
                            "tooltip.cruciblecraft.cover.retriever")
                    .withStyle(ChatFormatting.DARK_GRAY));
        }
    }
}
