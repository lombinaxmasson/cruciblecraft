package com.masson.cruciblecraft.content.item;

import java.util.List;
import java.util.Optional;

import com.masson.cruciblecraft.api.tool.ToolAction;
import com.masson.cruciblecraft.api.tool.ToolActionSource;
import com.masson.cruciblecraft.content.item.tool.ProvidedToolActions;
import com.masson.cruciblecraft.content.item.tool.ToolClick;
import com.masson.cruciblecraft.content.item.tool.ToolMining;
import com.masson.cruciblecraft.machine.ToolMaterialRules;
import com.masson.cruciblecraft.machine.ToolMaterialRules.ToolKind;
import com.masson.cruciblecraft.material.MaterialCatalog;
import com.masson.cruciblecraft.material.MaterialComponentPolicy;
import com.masson.cruciblecraft.registry.ModComponents;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Shearable;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

/** One registered item whose stack components select a tool material. */
public abstract class MaterialToolItem extends Item
        implements MaterialComponentPolicy, ToolActionSource {
    private static final int DAMAGEABILITY_SENTINEL_MAX_DAMAGE = 1;

    private final ToolKind kind;
    private final String nameKey;

    protected MaterialToolItem(
            Properties properties,
            ToolKind kind,
            String nameKey) {
        super(properties.stacksTo(1)
                // ItemStack.isDamageableItem() still requires these vanilla
                // marker components. The real maximum is derived by
                // getMaxDamage(stack), so this sentinel is not material data.
                .durability(DAMAGEABILITY_SENTINEL_MAX_DAMAGE)
                .component(ModComponents.TOOL_MATERIAL, "iron"));
        this.kind = kind;
        this.nameKey = nameKey;
    }

    public final ItemStack variant(String materialId) {
        ToolMaterialRules.requireStats(kind, materialId);
        ItemStack stack = new ItemStack(this);
        stack.set(ModComponents.TOOL_MATERIAL, materialId);
        return stack;
    }

    public final Optional<String> material(ItemStack stack) {
        String materialId = stack.getOrDefault(
                ModComponents.TOOL_MATERIAL, "iron");
        return ToolMaterialRules.isAllowed(kind, materialId)
                ? Optional.of(materialId)
                : Optional.empty();
    }

    public final ToolKind kind() {
        return kind;
    }

    @Override
    public boolean provides(ItemStack stack, ToolAction action) {
        return ProvidedToolActions.of(kind).contains(action);
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        if (!canApplyDurabilityDamage(context.getItemInHand())) {
            return InteractionResult.PASS;
        }
        return ToolClick.useOn(context);
    }

    @Override
    public InteractionResult interactLivingEntity(
            ItemStack stack,
            Player player,
            LivingEntity entity,
            InteractionHand hand) {
        if (!canApplyDurabilityDamage(stack)
                || !provides(stack, ToolAction.SHEARS)) {
            return InteractionResult.PASS;
        }
        if (!(entity instanceof Shearable shearable)
                || !shearable.readyForShearing()) {
            return InteractionResult.PASS;
        }
        if (!player.level().isClientSide) {
            shearable.shear(SoundSource.PLAYERS);
            ToolClick.hurt(stack, player, hand);
        }
        return InteractionResult.sidedSuccess(player.level().isClientSide);
    }

    @Override
    public int getMaxDamage(ItemStack stack) {
        return material(stack)
                .map(materialId -> ToolMaterialRules.durability(
                        kind, materialId))
                .orElse(DAMAGEABILITY_SENTINEL_MAX_DAMAGE);
    }

    public final boolean canApplyDurabilityDamage(ItemStack stack) {
        return material(stack).isPresent();
    }

    @Override
    public float getDestroySpeed(ItemStack stack, BlockState state) {
        return material(stack)
                .map(materialId -> ToolMining.destroySpeed(
                        kind, materialId, state))
                .orElse(1.0F);
    }

    @Override
    public boolean isCorrectToolForDrops(ItemStack stack, BlockState state) {
        return material(stack).isPresent()
                && ToolMining.correctTool(kind, state);
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
        if (!level.isClientSide
                && state.getDestroySpeed(level, pos) != 0.0F
                && (isCorrectToolForDrops(stack, state)
                        || getDestroySpeed(stack, state) > 1.0F)) {
            stack.hurtAndBreak(1, miningEntity, EquipmentSlot.MAINHAND);
        }
        return true;
    }

    @Override
    public String materialComponentId() {
        return TOOL_MATERIAL_COMPONENT_ID;
    }

    @Override
    public String missingMaterialForm() {
        return TOOL_COMPONENT_FORM;
    }

    @Override
    public boolean isPersistedMaterialAllowed(String materialId) {
        return ToolMaterialRules.isAllowed(kind, materialId);
    }

    @Override
    public Component getName(ItemStack stack) {
        Optional<String> materialId = material(stack);
        if (materialId.isEmpty()) {
            return super.getName(stack);
        }
        return Component.translatable(
                nameKey,
                Component.translatable(
                        MaterialCatalog.require(materialId.orElseThrow())
                                .translationKey()));
    }

    @Override
    public void appendHoverText(
            ItemStack stack,
            TooltipContext context,
            List<Component> tooltip,
            TooltipFlag flag) {
        Optional<String> materialId = material(stack);
        if (materialId.isEmpty()) {
            tooltip.add(Component.translatable(
                    "tooltip.cruciblecraft.invalid_tool_material",
                    stack.get(ModComponents.TOOL_MATERIAL))
                    .withStyle(ChatFormatting.RED));
            return;
        }
        String id = materialId.orElseThrow();
        tooltip.add(Component.translatable(
                "tooltip.cruciblecraft.tool_material",
                Component.translatable(MaterialCatalog.require(id).translationKey()),
                MaterialCatalog.require(id).tier())
                .withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.translatable(
                "tooltip.cruciblecraft.durability",
                Math.max(0, stack.getMaxDamage() - stack.getDamageValue()),
                stack.getMaxDamage())
                .withStyle(ChatFormatting.GRAY));
    }
}
