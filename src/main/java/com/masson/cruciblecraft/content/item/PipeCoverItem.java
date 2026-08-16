package com.masson.cruciblecraft.content.item;

import java.util.List;
import java.util.Optional;

import com.masson.cruciblecraft.content.blockentity.FluidPipeBlockEntity;
import com.masson.cruciblecraft.content.blockentity.ItemPipeBlockEntity;
import com.masson.cruciblecraft.logistics.pipe.cover.CoverDefinition;
import com.masson.cruciblecraft.logistics.pipe.cover.CoverDefinitionCatalog;
import com.masson.cruciblecraft.logistics.pipe.cover.PipeCover;
import com.masson.cruciblecraft.logistics.pipe.cover.PipeCoverConfig;
import com.masson.cruciblecraft.logistics.pipe.cover.PipeCoverType;

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
            changed = pipe.setCover(context.getClickedFace(), cover);
        } else if (blockEntity instanceof ItemPipeBlockEntity pipe) {
            changed = pipe.setCover(context.getClickedFace(), cover);
        } else {
            return InteractionResult.PASS;
        }
        if (changed
                && context.getPlayer() != null
                && !context.getPlayer().getAbilities().instabuild) {
            context.getItemInHand().shrink(1);
        }
        return InteractionResult.sidedSuccess(
                context.getLevel().isClientSide);
    }

    private PipeCover cover(
            UseOnContext context, boolean fluidPipe) {
        CoverDefinition definition =
                CoverDefinitionCatalog.require(definitionId);
        PipeCover base = PipeCover.of(definitionId);
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
        PipeCoverConfig current = cover.config();
        return new PipeCoverConfig(
                Optional.of(matchId),
                current.rate(),
                current.pressureThreshold(),
                current.exactCount(),
                current.mode(),
                current.selector());
    }

    private boolean cycleExistingSelector(
            UseOnContext context, Object blockEntity) {
        PipeCover current;
        if (blockEntity instanceof FluidPipeBlockEntity pipe) {
            current = pipe.coverSnapshot()
                    .get(context.getClickedFace());
        } else {
            current = ((ItemPipeBlockEntity) blockEntity)
                    .coverSnapshot().get(context.getClickedFace());
        }
        if (current == null
                || !current.definitionId().equals(definitionId)
                || !CoverDefinitionCatalog.require(definitionId)
                        .configurable().contains(
                                CoverDefinition.ConfigField.SELECTOR)) {
            return false;
        }
        int next = Math.floorMod(
                CoverDefinitionCatalog.require(definitionId)
                        .resolve(current.config()).selector() + 1,
                CoverDefinition.MAX_SELECTOR + 1);
        if (context.getLevel().isClientSide) {
            return true;
        }
        if (blockEntity instanceof FluidPipeBlockEntity pipe) {
            return pipe.configureCover(
                    context.getClickedFace(),
                    CoverDefinition.ConfigField.SELECTOR,
                    next);
        }
        return ((ItemPipeBlockEntity) blockEntity).configureCover(
                context.getClickedFace(),
                CoverDefinition.ConfigField.SELECTOR,
                next);
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
    }
}
