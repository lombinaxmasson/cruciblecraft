package com.masson.cruciblecraft.client.tooltip;

import java.util.Optional;

import com.masson.cruciblecraft.CrucibleCraft;
import com.masson.cruciblecraft.content.item.MaterialFormItem;
import com.masson.cruciblecraft.material.def.GT6MaterialMetadata;
import com.masson.cruciblecraft.material.def.MaterialDefinition;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.ItemTooltipEvent;

/** Exposes imported material facts on every generated material-form item. */
@EventBusSubscriber(modid = CrucibleCraft.MODID, value = Dist.CLIENT)
public final class MaterialMetadataTooltip {
    private MaterialMetadataTooltip() {}

    @SubscribeEvent
    public static void appendFormula(ItemTooltipEvent event) {
        if (!(event.getItemStack().getItem() instanceof MaterialFormItem form)) {
            return;
        }
        formula(form.material()).ifPresent(value ->
                event.getToolTip().add(Component.translatable(
                        "tooltip.cruciblecraft.material_formula",
                        value).withStyle(ChatFormatting.GRAY)));
    }

    static Optional<String> formula(MaterialDefinition material) {
        return material.gt6Metadata()
                .flatMap(GT6MaterialMetadata::formula)
                .map(String::trim)
                .filter(value -> !value.isEmpty());
    }
}
