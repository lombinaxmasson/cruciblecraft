package com.masson.cruciblecraft.client.tooltip;

import com.masson.cruciblecraft.CrucibleCraft;
import com.masson.cruciblecraft.content.block.CrusherBlock;
import com.masson.cruciblecraft.content.block.ProcessingMachineBlock;
import com.masson.cruciblecraft.machine.processing.ProcessingMachineIoTooltips;
import com.masson.cruciblecraft.machine.processing.ProcessingMachineSpec;
import com.masson.cruciblecraft.registry.ModProcessingMachines;

import net.minecraft.world.level.block.Block;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.ItemTooltipEvent;

/** GT6 inventory I/O faces on processing-machine items. */
@EventBusSubscriber(modid = CrucibleCraft.MODID, value = Dist.CLIENT)
public final class ProcessingMachineItemTooltip {
    private ProcessingMachineItemTooltip() {}

    @SubscribeEvent
    public static void appendIo(ItemTooltipEvent event) {
        ProcessingMachineSpec spec = specOf(Block.byItem(event.getItemStack().getItem()));
        if (spec == null) {
            return;
        }
        event.getToolTip().addAll(ProcessingMachineIoTooltips.lines(spec));
    }

    private static ProcessingMachineSpec specOf(Block block) {
        if (block instanceof ProcessingMachineBlock machine) {
            return machine.spec();
        }
        if (block instanceof CrusherBlock) {
            return ModProcessingMachines.CRUSHER;
        }
        return null;
    }
}
