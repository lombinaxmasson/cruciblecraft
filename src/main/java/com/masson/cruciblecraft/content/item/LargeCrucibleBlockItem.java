package com.masson.cruciblecraft.content.item;

import java.util.List;
import java.util.Optional;

import com.masson.cruciblecraft.content.block.LargeCrucibleHosts;
import com.masson.cruciblecraft.content.blockentity.LargeCrucibleBlockEntity;
import com.masson.cruciblecraft.content.mte.MteInPlaceDisplayNames;
import com.masson.cruciblecraft.machine.MachineMaterialRules;
import com.masson.cruciblecraft.machine.component.CrucibleProcessCore;
import com.masson.cruciblecraft.material.MaterialCatalog;
import com.masson.cruciblecraft.material.MaterialComponentPolicy;
import com.masson.cruciblecraft.registry.ModComponents;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

/** Shared large-crucible controller with a GT6 wall material. */
public final class LargeCrucibleBlockItem extends BlockItem
        implements MaterialComponentPolicy {
    public LargeCrucibleBlockItem(Block block, Item.Properties properties) {
        super(block, properties.component(
                ModComponents.MACHINE_MATERIAL,
                LargeCrucibleHosts.DEFAULT_MATERIAL));
    }

    public Optional<String> material(ItemStack stack) {
        String stored = stack.get(ModComponents.MACHINE_MATERIAL);
        String material = stored == null
                ? LargeCrucibleHosts.DEFAULT_MATERIAL
                : stored;
        return LargeCrucibleHosts.isAllowed(material)
                ? Optional.of(material)
                : Optional.empty();
    }

    @Override
    public String materialComponentId() {
        return MACHINE_MATERIAL_COMPONENT_ID;
    }

    @Override
    public String missingMaterialForm() {
        return MACHINE_COMPONENT_FORM;
    }

    @Override
    public boolean isPersistedMaterialAllowed(String materialId) {
        return LargeCrucibleHosts.isAllowed(materialId);
    }

    @Override
    protected boolean placeBlock(BlockPlaceContext context, BlockState state) {
        Optional<String> resolved = material(context.getItemInHand());
        if (resolved.isEmpty()) {
            if (!context.getLevel().isClientSide && context.getPlayer() != null) {
                context.getPlayer().displayClientMessage(
                        Component.translatable(
                                "message.cruciblecraft.invalid_machine_material",
                                Component.translatable("device.cruciblecraft.large_crucible"),
                                context.getItemInHand().get(
                                        ModComponents.MACHINE_MATERIAL)),
                        true);
            }
            return false;
        }
        if (!super.placeBlock(context, state)) {
            return false;
        }
        if (context.getLevel().getBlockEntity(context.getClickedPos())
                instanceof LargeCrucibleBlockEntity crucible) {
            crucible.setCasingMaterialId(resolved.orElseThrow());
        }
        return true;
    }

    @Override
    public Component getName(ItemStack stack) {
        Optional<String> resolved = material(stack);
        if (resolved.isEmpty()) {
            return super.getName(stack);
        }
        String material = resolved.orElseThrow();
        if (CatalogDisplayNames.hanLanguage()) {
            var zh = MteInPlaceDisplayNames.chineseMaterialName(material);
            if (zh.isPresent()) {
                return Component.literal("大型" + zh.get() + "坩埚");
            }
        }
        return Component.translatable(
                "block.cruciblecraft.large_crucible.named",
                Component.translatable("material.cruciblecraft." + material));
    }

    @Override
    public void appendHoverText(
            ItemStack stack,
            Item.TooltipContext context,
            List<Component> tooltip,
            TooltipFlag flag) {
        super.appendHoverText(stack, context, tooltip, flag);
        Optional<String> resolved = material(stack);
        if (resolved.isEmpty()) {
            tooltip.add(Component.translatable(
                    "tooltip.cruciblecraft.invalid_machine_material",
                    Component.translatable("device.cruciblecraft.large_crucible"),
                    stack.get(ModComponents.MACHINE_MATERIAL))
                    .withStyle(ChatFormatting.RED));
            return;
        }
        String material = resolved.orElseThrow();
        tooltip.add(Component.translatable(
                "tooltip.cruciblecraft.machine_material",
                Component.translatable("material.cruciblecraft." + material),
                MachineMaterialRules.materialTier(material)));
        tooltip.add(Component.translatable(
                "tooltip.cruciblecraft.max_temperature",
                Math.round(MachineMaterialRules.maxTemperature(
                        MaterialCatalog.require(material).thermal().meltingPoint(),
                        CrucibleProcessCore.LARGE_HEAT_RESISTANCE))));
    }
}
