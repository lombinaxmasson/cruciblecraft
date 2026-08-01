package com.masson.cruciblecraft.datagen;

import com.masson.cruciblecraft.CrucibleCraft;

import net.minecraft.data.PackOutput;
import net.neoforged.neoforge.client.model.generators.ItemModelProvider;
import net.neoforged.neoforge.common.data.ExistingFileHelper;

public class ModItemModelProvider extends ItemModelProvider {
    public ModItemModelProvider(PackOutput output, ExistingFileHelper existingFileHelper) {
        super(output, CrucibleCraft.MODID, existingFileHelper);
    }

    @Override
    protected void registerModels() {
        generated("raw_ceramic_crucible", "clay_ball");
        generated("raw_ceramic_mold", "clay_ball");
        generated("raw_ingot_mold", "clay_ball");
        generated("raw_plate_mold", "clay_ball");
        generated("raw_rod_mold", "clay_ball");
        generated("raw_bolt_mold", "clay_ball");
        withExistingParent("ingot_mold", modLoc("block/ceramic_mold"));
        withExistingParent("plate_mold", modLoc("block/ceramic_mold"));
        withExistingParent("rod_mold", modLoc("block/ceramic_mold"));
        withExistingParent("bolt_mold", modLoc("block/ceramic_mold"));
        generated("coal_coke", "coal");
        generated("creosote_bucket", "water_bucket");
        generated("unknown_material", "barrier");
        withExistingParent("smithing_hammer", mcLoc("item/handheld"))
                .texture("layer0", mcLoc("item/iron_pickaxe"));
    }

    private void generated(String name, String vanillaTexture) {
        withExistingParent(name, mcLoc("item/generated"))
                .texture("layer0", mcLoc("item/" + vanillaTexture));
    }
}
