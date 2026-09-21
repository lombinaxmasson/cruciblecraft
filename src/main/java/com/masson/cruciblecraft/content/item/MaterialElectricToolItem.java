package com.masson.cruciblecraft.content.item;

import java.util.List;
import java.util.Optional;

import com.masson.cruciblecraft.content.item.tool.ElectricToolCatalog;
import com.masson.cruciblecraft.content.item.tool.ElectricToolCharge;
import com.masson.cruciblecraft.content.item.tool.InventoryBlockPlacer;
import com.masson.cruciblecraft.content.item.tool.DynamitePlacement;
import com.masson.cruciblecraft.content.item.tool.ToolMining;
import com.masson.cruciblecraft.machine.ToolMaterialRules;
import com.masson.cruciblecraft.machine.ToolMaterialRules.ToolKind;
import com.masson.cruciblecraft.registry.ModComponents;
import com.masson.cruciblecraft.registry.ModItems;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Tier;
import net.minecraft.world.item.Tiers;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.common.ItemAbilities;
import net.neoforged.neoforge.common.ItemAbility;

/** One GT6 electric {@code addTool} identity. Energy is optional stack data. */
public final class MaterialElectricToolItem extends MaterialToolItem {
    public MaterialElectricToolItem(Properties properties, ToolKind kind) {
        super(
                properties,
                kind,
                ElectricToolCatalog.of(kind).orElseThrow().nameKey());
    }

    public ElectricToolCatalog spec() {
        return ElectricToolCatalog.of(kind()).orElseThrow();
    }

    @Override
    public int getMaxDamage(ItemStack stack) {
        return material(stack)
                .map(materialId -> {
                    int base = ToolMaterialRules.durability(kind(), materialId);
                    float mul = spec().durabilityMultiplier();
                    return Math.max(1, Math.round(base * mul));
                })
                .orElse(1);
    }

    @Override
    public boolean canApplyDurabilityDamage(ItemStack stack) {
        return super.canApplyDurabilityDamage(stack)
                && ElectricToolCharge.hasCharge(stack);
    }

    @Override
    public float getDestroySpeed(ItemStack stack, BlockState state) {
        Optional<String> materialId = material(stack);
        if (materialId.isEmpty() || !ElectricToolCharge.hasCharge(stack)) {
            return 1.0F;
        }
        if (!ToolMining.mineable(kind(), state)
                || !harvestable(materialId.orElseThrow(), state)) {
            return 1.0F;
        }
        float raw = ToolMining.destroySpeed(kind(), materialId.orElseThrow(), state);
        return raw * spec().speedMultiplier();
    }

    @Override
    public boolean isCorrectToolForDrops(ItemStack stack, BlockState state) {
        return material(stack)
                .filter(id -> ElectricToolCharge.hasCharge(stack))
                .filter(id -> ToolMining.correctTool(kind(), state))
                .filter(id -> harvestable(id, state))
                .isPresent();
    }

    @Override
    public boolean mineBlock(
            ItemStack stack,
            Level level,
            BlockState state,
            BlockPos pos,
            LivingEntity miningEntity) {
        if (!canApplyDurabilityDamage(stack)) {
            return false;
        }
        if (!level.isClientSide && state.getDestroySpeed(level, pos) != 0.0F) {
            float hardness = state.getDestroySpeed(level, pos);
            int amount = Math.max(1, Math.round(hardness * spec().damagePerBlock()));
            ElectricToolCharge.spend(
                    stack, amount, miningEntity, EquipmentSlot.MAINHAND);
        }
        return true;
    }

    @Override
    public boolean hurtEnemy(
            ItemStack stack, LivingEntity target, LivingEntity attacker) {
        return canApplyDurabilityDamage(stack);
    }

    @Override
    public void postHurtEnemy(
            ItemStack stack, LivingEntity target, LivingEntity attacker) {
        if (canApplyDurabilityDamage(stack)) {
            ElectricToolCharge.spend(
                    stack,
                    spec().damagePerEntity(),
                    attacker,
                    EquipmentSlot.MAINHAND);
        }
    }

    @Override
    public boolean canPerformAction(ItemStack stack, ItemAbility ability) {
        if (!canApplyDurabilityDamage(stack)) {
            return false;
        }
        return switch (spec().family()) {
            case MINING_DRILL -> ItemAbilities.DEFAULT_PICKAXE_ACTIONS.contains(ability)
                    || ItemAbilities.DEFAULT_SHOVEL_ACTIONS.contains(ability);
            case CHAINSAW -> ItemAbilities.DEFAULT_AXE_ACTIONS.contains(ability);
            default -> false;
        };
    }

    @Override
    public InteractionResultHolder<ItemStack> use(
            Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (player.isShiftKeyDown()
                && !spec().checkSwitchTarget()
                && convert(stack, player, hand, level.isClientSide)) {
            return InteractionResultHolder.sidedSuccess(
                    player.getItemInHand(hand), level.isClientSide);
        }
        return super.use(level, player, hand);
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Player player = context.getPlayer();
        if (player != null && player.isShiftKeyDown()) {
            boolean blocked = spec().checkSwitchTarget()
                    && context.getLevel().getBlockEntity(context.getClickedPos())
                            instanceof BlockEntity;
            if (!blocked
                    && convert(
                            context.getItemInHand(),
                            player,
                            context.getHand(),
                            context.getLevel().isClientSide)) {
                return InteractionResult.sidedSuccess(
                        context.getLevel().isClientSide);
            }
        }
        ItemStack stack = context.getItemInHand();
        if (spec().family() == ElectricToolCatalog.Family.HAND_DRILL
                && canApplyDurabilityDamage(stack)) {
            InteractionResult dynamite = DynamitePlacement.use(context)
                    .toInteractionResult(context.getLevel().isClientSide);
            if (dynamite.consumesAction()) {
                return dynamite;
            }
        }
        InteractionResult converted = super.useOn(context);
        if (converted.consumesAction()) {
            return converted;
        }
        if (!canApplyDurabilityDamage(stack)) {
            return converted;
        }
        return switch (spec().family()) {
            case MINING_DRILL, JACKHAMMER, JACKHAMMER_NO_ORES -> {
                InteractionResult plug = InventoryBlockPlacer.plugLeak(context);
                if (plug.consumesAction()) {
                    yield plug;
                }
                yield InventoryBlockPlacer.placeTorch(context);
            }
            case CHAINSAW -> InventoryBlockPlacer.placeSaplingOrWorkbench(context);
            default -> converted;
        };
    }

    @Override
    public boolean isBarVisible(ItemStack stack) {
        return ElectricToolCharge.isElectric(stack);
    }

    @Override
    public int getBarWidth(ItemStack stack) {
        long cap = ElectricToolCharge.capacity(stack);
        if (cap <= 0L) {
            return 0;
        }
        return Math.round(13.0F * ElectricToolCharge.charge(stack) / (float) cap);
    }

    @Override
    public int getBarColor(ItemStack stack) {
        return spec().voltage().handleColor();
    }

    @Override
    public void appendHoverText(
            ItemStack stack,
            TooltipContext context,
            List<Component> tooltip,
            TooltipFlag flag) {
        super.appendHoverText(stack, context, tooltip, flag);
        if (ElectricToolCharge.isElectric(stack)) {
            tooltip.add(Component.translatable(
                            "tooltip.cruciblecraft.electric_tool.charge",
                            ElectricToolCharge.charge(stack),
                            ElectricToolCharge.capacity(stack),
                            ElectricToolCharge.voltage(stack))
                    .withStyle(ChatFormatting.YELLOW));
        }
        spec().switchPartner().ifPresent(unused -> tooltip.add(Component.translatable(
                        "tooltip.cruciblecraft.electric_tool.sneak_switch")
                .withStyle(ChatFormatting.GRAY)));
    }

    private boolean harvestable(String materialId, BlockState state) {
        int quality = spec().baseQuality()
                + ToolMaterialRules.requireStats(kind(), materialId).quality();
        Tier tier = switch (Math.max(0, Math.min(4, quality))) {
            case 0 -> Tiers.WOOD;
            case 1 -> Tiers.STONE;
            case 2 -> Tiers.IRON;
            case 3 -> Tiers.DIAMOND;
            default -> Tiers.NETHERITE;
        };
        return !state.is(tier.getIncorrectBlocksForDrops());
    }

    private static boolean convert(
            ItemStack stack,
            Player player,
            InteractionHand hand,
            boolean client) {
        if (!(stack.getItem() instanceof MaterialElectricToolItem current)) {
            return false;
        }
        Optional<ElectricToolCatalog> partner = current.spec().switchPartner();
        if (partner.isEmpty()) {
            return false;
        }
        if (client) {
            return true;
        }
        ItemStack next = new ItemStack(
                ModItems.electricTool(partner.orElseThrow().kind()).get());
        next.set(
                ModComponents.TOOL_MATERIAL.get(),
                stack.get(ModComponents.TOOL_MATERIAL.get()));
        next.setDamageValue(stack.getDamageValue());
        if (ElectricToolCharge.isElectric(stack)
                || ElectricToolCharge.capacity(stack) > 0L
                || stack.has(ModComponents.ELECTRIC_CHARGE.get())) {
            ElectricToolCharge.applyEmpty(
                    next,
                    ElectricToolCharge.capacity(stack),
                    ElectricToolCharge.voltage(stack));
            ElectricToolCharge.setCharge(next, ElectricToolCharge.charge(stack));
        }
        player.setItemInHand(hand, next);
        return true;
    }
}
