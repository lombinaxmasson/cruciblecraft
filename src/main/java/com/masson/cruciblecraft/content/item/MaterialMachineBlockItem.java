package com.masson.cruciblecraft.content.item;

import java.util.List;
import java.util.Locale;
import java.util.Optional;

import com.masson.cruciblecraft.machine.MachineMaterialRules;
import com.masson.cruciblecraft.machine.MachineMaterialRules.Device;
import com.masson.cruciblecraft.material.MaterialComponentPolicy;
import com.masson.cruciblecraft.content.blockentity.AnvilBlockEntity;
import com.masson.cruciblecraft.content.blockentity.CrucibleBlockEntity;
import com.masson.cruciblecraft.registry.ModComponents;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

public final class MaterialMachineBlockItem extends BlockItem
        implements MaterialComponentPolicy {
    private final Device device;

    public MaterialMachineBlockItem(Block block, Device device, Item.Properties properties) {
        super(block, properties.component(
                ModComponents.MACHINE_MATERIAL,
                MachineMaterialRules.defaultMaterial(device)));
        this.device = device;
    }

    public Optional<String> material(ItemStack stack) {
        String stored = stack.get(ModComponents.MACHINE_MATERIAL);
        String material = stored == null
                ? MachineMaterialRules.defaultMaterial(device)
                : stored;
        return MachineMaterialRules.isAllowed(device, material)
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
        return MachineMaterialRules.isAllowed(device, materialId);
    }

    @Override
    protected boolean placeBlock(BlockPlaceContext context, BlockState state) {
        Optional<String> resolved = material(context.getItemInHand());
        if (resolved.isEmpty()) {
            if (!context.getLevel().isClientSide && context.getPlayer() != null) {
                context.getPlayer().displayClientMessage(
                        Component.translatable(
                                "message.cruciblecraft.invalid_machine_material",
                                Component.translatable(
                                        "device.cruciblecraft."
                                                + device.name().toLowerCase(Locale.ROOT)),
                                context.getItemInHand().get(
                                        ModComponents.MACHINE_MATERIAL)),
                        true);
            }
            return false;
        }
        if (!super.placeBlock(context, state)) {
            return false;
        }
        String material = resolved.orElseThrow();
        var blockEntity = context.getLevel().getBlockEntity(context.getClickedPos());
        if (blockEntity instanceof CrucibleBlockEntity crucible) {
            crucible.setCasingMaterialId(material);
        } else if (blockEntity instanceof AnvilBlockEntity anvil) {
            anvil.setMaterial(
                    material,
                    context.getItemInHand().get(ModComponents.MACHINE_DURABILITY));
        }
        return true;
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
                    Component.translatable(
                            "device.cruciblecraft."
                                    + device.name().toLowerCase(Locale.ROOT)),
                    stack.get(ModComponents.MACHINE_MATERIAL))
                    .withStyle(ChatFormatting.RED));
            return;
        }
        String material = resolved.orElseThrow();
        tooltip.add(Component.translatable(
                "tooltip.cruciblecraft.machine_material",
                Component.translatable("material.cruciblecraft." + material),
                MachineMaterialRules.materialTier(material)));
        if (device == Device.CRUCIBLE) {
            tooltip.add(Component.translatable(
                    "tooltip.cruciblecraft.max_temperature",
                    Math.round(MachineMaterialRules.crucibleMaxTemperature(material))));
        } else if (device == Device.ANVIL) {
            var durability = stack.get(ModComponents.MACHINE_DURABILITY);
            long max = durability == null
                    ? MachineMaterialRules.anvilMaxDurability(material)
                    : durability.max();
            long current = durability == null ? max : durability.current();
            tooltip.add(Component.translatable(
                    "tooltip.cruciblecraft.durability",
                    current,
                    max));
        }
    }
}
