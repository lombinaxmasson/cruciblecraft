package com.masson.cruciblecraft.datagen;

import java.util.Locale;

import com.masson.cruciblecraft.CrucibleCraft;
import com.masson.cruciblecraft.content.item.ExtruderShapeCatalog;
import com.masson.cruciblecraft.content.item.ToolPatternCatalog;
import com.masson.cruciblecraft.material.MaterialCatalog;
import com.masson.cruciblecraft.material.prefix.MaterialPrefixCatalog;
import com.masson.cruciblecraft.registry.ModBlocks;
import com.masson.cruciblecraft.registry.ModFluids;
import com.masson.cruciblecraft.registry.ModItems;
import com.masson.cruciblecraft.registry.ModProcessingMachines;
import com.masson.cruciblecraft.worldgen.OreHostVariantCatalog.Host;

import net.minecraft.data.PackOutput;
import net.neoforged.neoforge.common.data.LanguageProvider;

public class ModLanguageProvider extends LanguageProvider {
    private final boolean chinese;

    public ModLanguageProvider(PackOutput output) {
        this(output, "en_us");
    }

    public ModLanguageProvider(PackOutput output, String locale) {
        super(output, CrucibleCraft.MODID, locale);
        this.chinese = locale.equals("zh_cn");
    }

    @Override
    protected void addTranslations() {
        if (chinese) {
            ExtruderShapeCatalog.DEFINITIONS.forEach(shape ->
                    addItem(ModItems.extruderShape(shape.id()), shape.chineseName()));
            ToolPatternCatalog.DEFINITIONS.forEach(pattern ->
                    addItem(ModItems.toolPattern(pattern.id()), pattern.chineseName()));
            addItem(ModItems.FLINT_KNIFE, "燧石刀");
            add("screen.cruciblecraft.processing.status.unsupported_version",
                    "存档版本不受支持（版本 %s）");
            add("screen.cruciblecraft.processing.status.material_quarantined",
                    "机器材料已隔离");
            add("screen.cruciblecraft.processing.status.unknown", "未知状态");
            add("screen.cruciblecraft.processing.status.idle", "空闲");
            add("screen.cruciblecraft.processing.status.running", "运行中");
            add("screen.cruciblecraft.processing.status.invalid_recipe", "配方无效");
            add("screen.cruciblecraft.processing.status.output_blocked", "输出受阻");
            add("screen.cruciblecraft.processing.status.underpowered", "供能不足");
            add("screen.cruciblecraft.processing.status.inventory_layout_quarantined",
                    "库存布局不兼容（存档槽位：%s）");
            add("screen.cruciblecraft.processing.tank_empty", "空罐（容量 %s mB）");
            add("screen.cruciblecraft.processing.tank_named", "%s：%s/%s mB");
            add("message.cruciblecraft.anvil_material_quarantined",
                    "砧的材料 %s 已被隔离");
            add("message.cruciblecraft.crucible_casing_quarantined",
                    "坩埚外壳材料 %s 已被隔离");
            add("message.cruciblecraft.invalid_hammer_material",
                    "锤头材料 %s 无效");
            add("message.cruciblecraft.invalid_machine_material",
                    "%s材料 %s 无效");
            add("tooltip.cruciblecraft.invalid_tool_material",
                    "无效工具材料：%s");
            add("tooltip.cruciblecraft.invalid_machine_material",
                    "无效%s材料：%s");
            add("tooltip.cruciblecraft.material_formula", "化学式：%s");
            add("jade.cruciblecraft.material_quarantined",
                    "%s材料已隔离：%s");
            add("jade.cruciblecraft.processing_machine",
                    "功率：%s/t，进度：%s/%s（%s）");
            add("jade.cruciblecraft.processing_tank",
                    "罐 %s：%s，%s/%s mB");
            add("jade.cruciblecraft.cable",
                    "%s / %s：%s V，%s A，损耗 %s EU/方块，负载 %s A，烧毁 %s/16");
            add("tooltip.cruciblecraft.electrical.specification",
                    "GT6 规格：%s");
            add("tooltip.cruciblecraft.electrical.rating",
                    "额定：%s V，%s A，损耗 %s EU/方块");
            add("tooltip.cruciblecraft.electrical.insulated", "绝缘电缆");
            add("tooltip.cruciblecraft.electrical.bare", "裸线");
            add("tooltip.cruciblecraft.pipe.specification",
                    "管道规格：%s");
            add("tooltip.cruciblecraft.pipe.fluid_rating",
                    "容量：%s mB，最高温度：%s K");
            add("tooltip.cruciblecraft.pipe.item_rating",
                    "吞吐：%s 组/秒，路径成本：%s");
            add("jade.cruciblecraft.fluid_pipe",
                    "%s：%s/%s mB，近 20 tick 传输 %s mB，失效 %s");
            add("jade.cruciblecraft.item_pipe",
                    "%s：实际送达 %s，堵塞 %s，Cover %s");
            add("death.attack.electricity", "%s 被电死了");
            addItem(ModItems.PORTABLE_FLUID_TANK, "便携流体罐");
            addItem(ModItems.FLUID_CELL, "通用流体单元");
            addItem(ModItems.GAS_CELL, "通用气体单元");
            add("tooltip.cruciblecraft.fluid_cell.empty",
                    "空流体单元（容量 %s mB）");
            add("tooltip.cruciblecraft.fluid_cell.contents",
                    "%s：%s/%s mB");
            add("tooltip.cruciblecraft.gas_cell.empty",
                    "空气体单元（容量 %s mB）");
            add("tooltip.cruciblecraft.gas_cell.contents",
                    "%s：%s/%s mB");
            addItem(ModItems.PIPE_FILTER_COVER, "管道过滤器 Cover");
            addItem(ModItems.PIPE_VALVE_COVER, "管道单向阀 Cover");
            addItem(ModItems.PIPE_PUMP_COVER, "管道输出泵 Cover");
            add("tooltip.cruciblecraft.portable_fluid_tank.empty",
                    "空（容量 %s mB）");
            add("tooltip.cruciblecraft.portable_fluid_tank.contents",
                    "%s：%s/%s mB");
            addBlock(ModBlocks.ELECTROLYZER, "电解机");
            addBlock(ModBlocks.MIXER, "混合机");
            addBlock(ModBlocks.DISTILLERY, "蒸馏机");
            addBlock(ModBlocks.AUTOCLAVE, "高压釜");
            addBlock(ModBlocks.DRYING, "干燥机");
            addBlock(ModBlocks.COMPRESSOR, "压缩机");
            java.util.Map.ofEntries(
                    java.util.Map.entry("sluice", "溜槽"),
                    java.util.Map.entry("bath", "洗矿浴池"),
                    java.util.Map.entry("centrifuge", "离心机"),
                    java.util.Map.entry("shredder", "粉碎机"),
                    java.util.Map.entry("sifter", "筛选机"),
                    java.util.Map.entry("smelter", "熔炼炉"),
                    java.util.Map.entry("mortar", "动力研钵"),
                    java.util.Map.entry("extruder", "挤压机"),
                    java.util.Map.entry("cutter", "切割机"),
                    java.util.Map.entry("lathe", "车床"),
                    java.util.Map.entry("rollingmill", "轧机"),
                    java.util.Map.entry("rollbender", "卷板机"),
                    java.util.Map.entry("wiremill", "线材轧机"),
                    java.util.Map.entry("bender", "折弯机"),
                    java.util.Map.entry("assembler", "装配机"),
                    java.util.Map.entry("welder", "焊机"),
                    java.util.Map.entry("press", "压机"),
                    java.util.Map.entry("electrolyzer", "电解机"),
                    java.util.Map.entry("mixer", "混合机"),
                    java.util.Map.entry("distillery", "蒸馏机"),
                    java.util.Map.entry("autoclave", "高压釜"),
                    java.util.Map.entry("drying", "干燥机"),
                    java.util.Map.entry("compressor", "压缩机"))
                    .forEach((id, name) ->
                            add("emi.category.cruciblecraft." + id, name));
            add("emi.cruciblecraft.processing.preserved", "保留，不消耗");
            add("emi.cruciblecraft.processing.wear", "工具耐久消耗：%s");
            add("emi.cruciblecraft.processing.chance", "产出概率：%s%%");
            add("emi.cruciblecraft.processing.duration", "耗时：%s tick（%s 秒）");
            add("emi.cruciblecraft.processing.power.kinetic", "功率：%s KU/t");
            add("emi.cruciblecraft.processing.power.electric", "功率：%s EU/t");
            add("emi.cruciblecraft.processing.power.heat", "热功率：%s HU/t");
            java.util.Map.ofEntries(
                    java.util.Map.entry("molten_alumina", "熔融氧化铝"),
                    java.util.Map.entry("bromine", "溴"),
                    java.util.Map.entry("carbon_dioxide", "二氧化碳"),
                    java.util.Map.entry("chlorine", "氯气"),
                    java.util.Map.entry("fluorine", "氟气"),
                    java.util.Map.entry("glue", "胶水"),
                    java.util.Map.entry("helium", "氦气"),
                    java.util.Map.entry("hydrochloric_acid", "盐酸"),
                    java.util.Map.entry("hydrogen", "氢气"),
                    java.util.Map.entry("hydrogen_fluoride", "氟化氢"),
                    java.util.Map.entry("latex", "乳胶"),
                    java.util.Map.entry("nitrogen", "氮气"),
                    java.util.Map.entry("oxygen", "氧气"),
                    java.util.Map.entry("sulfur_trioxide", "三氧化硫"),
                    java.util.Map.entry("water_distilled", "蒸馏水"))
                    .forEach((id, name) ->
                            add("fluid_type.cruciblecraft." + id, name));
            add("device.cruciblecraft.anvil", "砧");
            add("device.cruciblecraft.crucible", "坩埚");
            add("device.cruciblecraft.hammer", "锤");
            add("item.cruciblecraft.material_pickaxe", "%s镐");
            add("item.cruciblecraft.material_shovel", "%s铲");
            add("item.cruciblecraft.material_axe", "%s斧");
            add("item.cruciblecraft.material_hoe", "%s锄");
            add("item.cruciblecraft.material_sword", "%s剑");
            add("item.cruciblecraft.smithing_hammer", "%s锻造锤");
            add("item.cruciblecraft.material_file", "%s锉刀");
            add("item.cruciblecraft.material_chisel", "%s凿子");
            add("item.cruciblecraft.material_saw", "%s锯");
            add("item.cruciblecraft.material_screwdriver", "%s螺丝刀");
            add("item.cruciblecraft.material_wrench", "%s扳手");
            ModBlocks.electricalConductorBlocks().forEach(holder -> {
                var conductor = holder.get().conductor();
                add(
                        "block." + CrucibleCraft.MODID + "."
                                + conductor.registryName(),
                        title(conductor.materialId()) + " "
                                + (conductor.bareWire() ? "裸线" : "电缆"));
            });
            return;
        }
        add("itemGroup.cruciblecraft", "Crucible Craft");
        add("itemGroup.cruciblecraft.ores", "Crucible Craft: Ores");
        add("itemGroup.cruciblecraft.ore_processing", "Crucible Craft: Ore Processing");
        add("itemGroup.cruciblecraft.dusts", "Crucible Craft: Dusts");
        add("itemGroup.cruciblecraft.metals_gems", "Crucible Craft: Metals & Gems");
        add("itemGroup.cruciblecraft.plates", "Crucible Craft: Plates");
        add("itemGroup.cruciblecraft.parts", "Crucible Craft: Parts");
        add("itemGroup.cruciblecraft.mechanical_parts", "Crucible Craft: Mechanical Parts");
        add("itemGroup.cruciblecraft.wires", "Crucible Craft: Wires");
        add("itemGroup.cruciblecraft.cables", "Crucible Craft: Cables");
        add("itemGroup.cruciblecraft.misc", "Crucible Craft: Miscellaneous Materials");
        addBlock(ModBlocks.FIREBRICK, "Firebrick");
        addBlock(ModBlocks.FIREBOX, "Solid Fuel Firebox");
        addBlock(ModBlocks.CRUCIBLE, "Crucible");
        addBlock(ModBlocks.ANVIL, "Smithing Anvil");
        addBlock(ModBlocks.COKE_OVEN, "Coke Oven Controller");
        addItem(ModItems.RAW_CERAMIC_CRUCIBLE, "Unfired Ceramic Crucible");
        addItem(ModItems.RAW_CERAMIC_MOLD, "Unshaped Unfired Ceramic Mold");
        addItem(ModItems.RAW_INGOT_MOLD, "Unfired Ingot Mold");
        addItem(ModItems.RAW_PLATE_MOLD, "Unfired Plate Mold");
        addItem(ModItems.RAW_ROD_MOLD, "Unfired Rod Mold");
        addItem(ModItems.RAW_BOLT_MOLD, "Unfired Bolt Mold");
        addItem(ModItems.INGOT_MOLD, "Ingot Mold");
        addItem(ModItems.PLATE_MOLD, "Plate Mold");
        addItem(ModItems.ROD_MOLD, "Rod Mold");
        addItem(ModItems.BOLT_MOLD, "Bolt Mold");
        addItem(ModItems.COAL_COKE, "Coal Coke");
        addItem(ModItems.MATCH, "Match");
        addBlock(ModBlocks.BELLOWS, "Bellows");
        addItem(ModItems.CREOSOTE_BUCKET, "Creosote Bucket");
        add("fluid_type.cruciblecraft.creosote", "Creosote");
        addItem(ModItems.STEAM_BUCKET, "Steam Bucket");
        add("fluid_type.cruciblecraft.steam", "Steam");
        addItem(ModItems.PORTABLE_FLUID_TANK, "Portable Fluid Tank");
        addItem(ModItems.FLUID_CELL, "Universal Fluid Cell");
        addItem(ModItems.GAS_CELL, "Universal Gas Cell");
        add("tooltip.cruciblecraft.fluid_cell.empty",
                "Empty fluid cell (capacity: %s mB)");
        add("tooltip.cruciblecraft.fluid_cell.contents",
                "%s: %s/%s mB");
        add("tooltip.cruciblecraft.gas_cell.empty",
                "Empty gas cell (capacity: %s mB)");
        add("tooltip.cruciblecraft.gas_cell.contents",
                "%s: %s/%s mB");
        add("tooltip.cruciblecraft.portable_fluid_tank.empty",
                "Empty (capacity: %s mB)");
        add("tooltip.cruciblecraft.portable_fluid_tank.contents",
                "%s: %s/%s mB");
        addBlock(ModBlocks.BRONZE_BOILER, "Bronze Boiler");
        addBlock(ModBlocks.BRONZE_STEAM_ENGINE, "Bronze Steam Engine");
        addBlock(ModBlocks.BRONZE_DYNAMO, "Bronze Dynamo");
        addBlock(ModBlocks.BRONZE_CRUSHER, "Bronze Crusher");
        addBlock(ModBlocks.SLUICE, "Sluice");
        addBlock(ModBlocks.BATH, "Ore Washing Bath");
        addBlock(ModBlocks.CENTRIFUGE, "Centrifuge");
        addBlock(ModBlocks.SHREDDER, "Shredder");
        addBlock(ModBlocks.SIFTER, "Sifter");
        addBlock(ModBlocks.SMELTER, "Smelter");
        addBlock(ModBlocks.MORTAR, "Powered Mortar");
        addBlock(ModBlocks.EXTRUDER, "Extruder");
        addBlock(ModBlocks.CUTTER, "Cutter");
        addBlock(ModBlocks.LATHE, "Lathe");
        addBlock(ModBlocks.ROLLINGMILL, "Rolling Mill");
        addBlock(ModBlocks.ROLLBENDER, "Roll Bender");
        addBlock(ModBlocks.WIREMILL, "Wire Mill");
        addBlock(ModBlocks.BENDER, "Bender");
        addBlock(ModBlocks.ASSEMBLER, "Assembler");
        addBlock(ModBlocks.WELDER, "Welder");
        addBlock(ModBlocks.PRESS, "Press");
        addBlock(ModBlocks.ELECTROLYZER, "Electrolyzer");
        addBlock(ModBlocks.MIXER, "Mixer");
        addBlock(ModBlocks.DISTILLERY, "Distillery");
        addBlock(ModBlocks.AUTOCLAVE, "Autoclave");
        addBlock(ModBlocks.DRYING, "Drying Machine");
        addBlock(ModBlocks.COMPRESSOR, "Compressor");
        add("screen.cruciblecraft.processing.status.idle", "Idle");
        add("screen.cruciblecraft.processing.status.running", "Running");
        add("screen.cruciblecraft.processing.status.invalid_recipe", "Invalid recipe");
        add("screen.cruciblecraft.processing.status.output_blocked", "Output blocked");
        add("screen.cruciblecraft.processing.status.underpowered", "Underpowered");
        add("screen.cruciblecraft.processing.status.unsupported_version",
                "Unsupported saved version (%s)");
        add("screen.cruciblecraft.processing.status.inventory_layout_quarantined",
                "Inventory layout quarantined (saved slots: %s)");
        add("screen.cruciblecraft.processing.status.material_quarantined",
                "Machine material quarantined");
        add("screen.cruciblecraft.processing.status.unknown", "Unknown status");
        add("screen.cruciblecraft.processing.tank", "%s/%s mB");
        add("screen.cruciblecraft.processing.tank_empty", "Empty (capacity: %s mB)");
        add("screen.cruciblecraft.processing.tank_named", "%s: %s/%s mB");
        add("container.cruciblecraft.bronze_crusher", "Bronze Crusher");
        add("emi.category.cruciblecraft.crusher", "Crusher");
        add("jade.cruciblecraft.boiler", "Water: %s/%s mB, Steam: %s/%s mB, Heat: %s/80 HU");
        add("jade.cruciblecraft.steam_engine", "Steam: %s/%s mB, KU: %s/%s (%s stroke)");
        add("jade.cruciblecraft.crusher", "Power: %s KU/t, Progress: %s/%s (%s)");
        add("jade.cruciblecraft.processing_machine",
                "Power: %s/t, Progress: %s/%s (%s)");
        add("jade.cruciblecraft.processing_tank",
                "Tank %s: %s, %s/%s mB");
        add("jade.cruciblecraft.cable",
                "%s / %s: %s V, %s A, loss %s EU/block, load %s A, burn %s/16");
        add("tooltip.cruciblecraft.electrical.specification",
                "GT6 specification: %s");
        add("tooltip.cruciblecraft.electrical.rating",
                "Rating: %s V, %s A, loss %s EU/block");
        add("tooltip.cruciblecraft.electrical.insulated",
                "Insulated cable");
        add("tooltip.cruciblecraft.electrical.bare", "Bare wire");
        add("tooltip.cruciblecraft.pipe.specification",
                "Pipe specification: %s");
        add("tooltip.cruciblecraft.pipe.fluid_rating",
                "Capacity: %s mB, maximum temperature: %s K");
        add("tooltip.cruciblecraft.pipe.item_rating",
                "Throughput: %s stacks/s, route cost: %s");
        add("jade.cruciblecraft.fluid_pipe",
                "%s: %s/%s mB, last 20 ticks %s mB, failure %s");
        add("jade.cruciblecraft.item_pipe",
                "%s: actual delivery %s, clogged %s, covers %s");
        add("death.attack.electricity",
                "%s was electrocuted");
        addItem(ModItems.PIPE_FILTER_COVER, "Pipe Filter Cover");
        addItem(ModItems.PIPE_VALVE_COVER, "Pipe One-Way Valve Cover");
        addItem(ModItems.PIPE_PUMP_COVER, "Pipe Output Pump Cover");
        add("item.cruciblecraft.smithing_hammer", "%s Smithing Hammer");
        addItem(ModItems.FLINT_KNIFE, "Flint Knife");
        add("item.cruciblecraft.material_pickaxe", "%s Pickaxe");
        add("item.cruciblecraft.material_shovel", "%s Shovel");
        add("item.cruciblecraft.material_axe", "%s Axe");
        add("item.cruciblecraft.material_hoe", "%s Hoe");
        add("item.cruciblecraft.material_sword", "%s Sword");
        add("item.cruciblecraft.material_file", "%s File");
        add("item.cruciblecraft.material_chisel", "%s Chisel");
        add("item.cruciblecraft.material_saw", "%s Saw");
        add("item.cruciblecraft.material_screwdriver", "%s Screwdriver");
        add("item.cruciblecraft.material_wrench", "%s Wrench");
        addItem(ModItems.UNKNOWN_MATERIAL, "Unknown Material");
        ExtruderShapeCatalog.DEFINITIONS.forEach(shape ->
                addItem(ModItems.extruderShape(shape.id()), shape.englishName()));
        ToolPatternCatalog.DEFINITIONS.forEach(pattern ->
                addItem(ModItems.toolPattern(pattern.id()), pattern.englishName()));
        ModBlocks.oreBlockPaths().keySet().forEach(key -> addBlock(
                ModBlocks.oreBlock(key.materialId(), key.host()),
                (key.host() == Host.DEEPSLATE ? "Deepslate " : "")
                        + title(key.materialId()) + " Ore"));
        ModBlocks.electricalConductorBlocks().forEach(holder -> {
            var conductor = holder.get().conductor();
            add(
                    "block." + CrucibleCraft.MODID + "."
                            + conductor.registryName(),
                    title(conductor.materialId()) + " "
                            + title(conductor.form().serializedName()));
        });
        ModBlocks.pipeBlocks().forEach(holder -> {
            var pipe = holder.get().pipe();
            add(
                    "block." + CrucibleCraft.MODID + "."
                            + pipe.registryName(),
                    title(pipe.materialId()) + " "
                            + title(pipe.form().serializedName()));
        });
        add("tooltip.cruciblecraft.unknown_material", "Missing material: %s (%s)");
        add("tooltip.cruciblecraft.machine_material", "Casing: %s (material tier %s)");
        add("tooltip.cruciblecraft.tool_material", "Head: %s (material tier %s)");
        add("tooltip.cruciblecraft.invalid_tool_material", "Invalid tool material: %s");
        add("tooltip.cruciblecraft.invalid_machine_material", "Invalid %s material: %s");
        add("tooltip.cruciblecraft.material_formula", "Formula: %s");
        add("tooltip.cruciblecraft.durability", "Durability: %s / %s");
        add("tooltip.cruciblecraft.max_temperature", "Maximum temperature: %s °C");
        add("message.cruciblecraft.materials_changed", "CrucibleCraft's material definitions changed since this world was last opened. Missing materials are preserved but machines using them are paused.");
        add(
                "disconnect.cruciblecraft.material_mismatch",
                "CrucibleCraft material definitions do not match the server: %s. Install the same addons and material definitions as the server.");
        add(
                "disconnect.cruciblecraft.material_handshake_missing",
                "The client cannot perform CrucibleCraft's material compatibility check. Install the same CrucibleCraft version as the server.");
        add(
                "disconnect.cruciblecraft.material_handshake_too_large",
                "The server's CrucibleCraft material configuration has %s entries, exceeding the supported limit of %s.");

        for (var form : MaterialPrefixCatalog.values()) {
            add(
                    "item.cruciblecraft.material_form." + form.serializedName(),
                    "%s " + title(form.serializedName()));
        }
        MaterialCatalog.startupValues().forEach(material ->
                add(material.translationKey(), title(material.id())));
        ModFluids.moltenFluids().forEach(entry ->
                add(
                        "fluid_type.cruciblecraft.molten_" + entry.materialId(),
                        "Molten " + title(entry.materialId())));
        ModFluids.chemicalFluids().forEach(entry ->
                add(
                        "fluid_type.cruciblecraft." + entry.id(),
                        title(entry.id())));
        if (!MaterialCatalog.contains("stone")) {
            add("material.cruciblecraft.stone", "Stone");
        }
        ModProcessingMachines.CONFIGURED_MACHINES.forEach(spec ->
                add(
                        "emi.category.cruciblecraft." + spec.id().getPath(),
                        title(spec.id().getPath())));
        add("emi.cruciblecraft.processing.preserved", "Preserved, not consumed");
        add("emi.cruciblecraft.processing.wear", "Tool damage: %s");
        add("emi.cruciblecraft.processing.chance", "Output chance: %s%%");
        add("emi.cruciblecraft.processing.duration", "Duration: %s ticks (%s s)");
        add("emi.cruciblecraft.processing.power.kinetic", "Power: %s KU/t");
        add("emi.cruciblecraft.processing.power.electric", "Power: %s EU/t");
        add("emi.cruciblecraft.processing.power.heat", "Heat: %s HU/t");

        add("message.cruciblecraft.firebox_fueled", "Firebox fueled: %s HU stored (%s seconds remaining)");
        add("message.cruciblecraft.firebox_fuel_rejected", "The firebox cannot accept this fuel right now");
        add("message.cruciblecraft.air_injection_started", "Airflow started; decarburization is underway");
        add("message.cruciblecraft.air_injection_continued", "Airflow duration extended");
        add("message.cruciblecraft.air_injection_too_cold", "The iron charge must be molten before blowing air");
        add("message.cruciblecraft.invalid_steel_charge", "Steelmaking requires exactly three parts iron to one part carbon");
        add("message.cruciblecraft.bellows_started", "Bellows stroke started");
        add("message.cruciblecraft.bellows_active", "The bellows are already moving");
        add("message.cruciblecraft.coke_oven_ignited", "Coke oven ignited");
        add("message.cruciblecraft.coke_oven_cannot_ignite", "The structure must be complete and heated from below");
        add("container.cruciblecraft.coke_oven", "Coke Oven");
        add("screen.cruciblecraft.coke_oven.invalid_structure", "Invalid structure");
        add("screen.cruciblecraft.coke_oven.no_heat", "No heat");
        add("screen.cruciblecraft.coke_oven.creosote", "Creosote: %s / %s mB");
        add("jade.cruciblecraft.coke_oven.structure", "Structure: %s");
        add("jade.cruciblecraft.coke_oven.valid", "Valid");
        add("jade.cruciblecraft.coke_oven.invalid", "Invalid");
        add("jade.cruciblecraft.coke_oven.progress", "Progress: %s / %s ticks");
        add("jade.cruciblecraft.coke_oven.creosote", "Creosote: %s / %s mB");
        add("emi.category.cruciblecraft.coke_oven", "Coke Oven");
        add("message.cruciblecraft.material_inserted", "Material added to crucible");
        add("message.cruciblecraft.crucible_full", "Crucible is full");
        add("message.cruciblecraft.crucible_tier_too_low",
                "This crucible casing cannot process that material tier");
        add("message.cruciblecraft.inexact_material_amount",
                "That amount cannot be decomposed into whole material units");
        add("message.cruciblecraft.invalid_material",
                "This material cannot be added to the crucible");
        add("message.cruciblecraft.crucible_casing_quarantined",
                "This crucible's casing material is quarantined: %s");
        add("message.cruciblecraft.invalid_machine_material",
                "Cannot place this block: invalid %s material %s");
        add("message.cruciblecraft.material_cast", "Cast one %s ingot");
        add("message.cruciblecraft.not_ready", "Composition is not castable or is below melting temperature");
        add("message.cruciblecraft.mold_filled", "Molten material poured into the mold");
        add("message.cruciblecraft.mold_cooling", "Mold is cooling: %s °C");
        add("message.cruciblecraft.mold_no_molten_material", "No adjacent crucible has suitable molten material");
        add(
                "message.cruciblecraft.crucible_status",
                "Temperature: %s °C | %s");
        add("tooltip.cruciblecraft.temperature", "Temperature: %s %s");
        add("jade.cruciblecraft.temperature", "Temperature: %s %s");
        add("jade.cruciblecraft.firebox_heat", "Heat: %s HU (%s seconds remaining)");
        add("jade.cruciblecraft.firebox_output", "Output: %s HU/t");
        add("jade.cruciblecraft.casing", "Casing: %s (tier %s, process tier %s)");
        add("jade.cruciblecraft.machine_material", "Material: %s (tier %s)");
        add("jade.cruciblecraft.material_quarantined",
                "%s material quarantined: %s");
        add("jade.cruciblecraft.max_temperature", "Maximum temperature: %s %s");
        add("jade.cruciblecraft.contents", "%s (%s/%s u)");
        add("jade.cruciblecraft.anvil_workpiece", "Workpiece: %s");
        add("jade.cruciblecraft.anvil_slot", "Slot %s: %sx %s");
        add("jade.cruciblecraft.anvil_durability", "Durability: %s / %s");
        add("jade.cruciblecraft.anvil_progress", "Hammer strikes: %s");
        add("jade.cruciblecraft.mold_shape", "Mold: %s");
        add("jade.cruciblecraft.mold_contents", "Contents: %sx %s");
        add("jade.cruciblecraft.mold_state", "State: %s at %s °C");
        add("jade.cruciblecraft.mold_empty", "Empty");
        add("jade.cruciblecraft.mold_solid", "Solidified");
        add("jade.cruciblecraft.mold_cooling", "Cooling");
        add("message.cruciblecraft.anvil_inserted", "Workpiece placed on the anvil");
        add("message.cruciblecraft.anvil_rejected", "This item has no matching anvil recipe");
        add("message.cruciblecraft.anvil_no_recipe", "Place a valid workpiece on the anvil first");
        add("message.cruciblecraft.anvil_material_quarantined",
                "This anvil's material is quarantined: %s");
        add("message.cruciblecraft.invalid_hammer_material",
                "This hammer has an invalid material: %s");
        add("message.cruciblecraft.anvil_progress", "Hammering: %s/%s");
        add("message.cruciblecraft.anvil_completed", "Forged %s");
        add("emi.category.cruciblecraft.crucible", "Crucible Alloying");
        add("emi.category.cruciblecraft.anvil", "Anvil Working");
        add("emi.category.cruciblecraft.mold_casting", "Ceramic Mold Casting");
        add("device.cruciblecraft.anvil", "anvil");
        add("device.cruciblecraft.crucible", "crucible");
        add("device.cruciblecraft.hammer", "hammer");

        add("cruciblecraft.configuration.title", "Crucible Craft");
        add("cruciblecraft.configuration.section.cruciblecraft.client.toml", "Client");
        add("cruciblecraft.configuration.section.cruciblecraft.client.toml.title", "Client");
        add("cruciblecraft.configuration.temperatureUnit", "Temperature Unit");
    }

    private static String title(String value) {
        String spaced = value.replace('_', ' ');
        return spaced.substring(0, 1).toUpperCase(Locale.ROOT) + spaced.substring(1);
    }
}
