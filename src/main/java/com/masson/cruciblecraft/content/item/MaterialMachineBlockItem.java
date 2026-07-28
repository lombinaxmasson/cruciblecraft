package com.masson.cruciblecraft.content.item;

import java.util.List;

import com.masson.cruciblecraft.machine.MachineMaterialRules;
import com.masson.cruciblecraft.machine.MachineMaterialRules.Device;
import com.masson.cruciblecraft.content.blockentity.AnvilBlockEntity;
import com.masson.cruciblecraft.content.blockentity.CrucibleBlockEntity;
import com.masson.cruciblecraft.registry.ModComponents;

import net.minecraft.network.chat.Component;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

public final class MaterialMachineBlockItem extends BlockItem {
    private final Device device;

    public MaterialMachineBlockItem(Block block, Device device, Item.Properties properties) {
        super(block, properties.component(
                ModComponents.MACHINE_MATERIAL,
                MachineMaterialRules.defaultMaterial(device)));
        this.device = device;
    }

    public String material(ItemStack stack) {
        return MachineMaterialRules.sanitize(
                device,
                stack.getOrDefault(
                        ModComponents.MACHINE_MATERIAL,
                        MachineMaterialRules.defaultMaterial(device)));
    }

    @Override
    protected boolean placeBlock(BlockPlaceContext context, BlockState state) {
        if (!super.placeBlock(context, state)) {
            return false;
        }
        String material = material(context.getItemInHand());
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
        String material = material(stack);
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
