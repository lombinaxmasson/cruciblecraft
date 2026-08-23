package com.masson.cruciblecraft.datagen;

import java.util.Locale;
import java.util.Optional;

import com.masson.cruciblecraft.CrucibleCraft;
import com.masson.cruciblecraft.content.item.ExtruderShapeCatalog;
import com.masson.cruciblecraft.content.item.ToolPatternCatalog;
import com.masson.cruciblecraft.logistics.hopper.HopperVariantCatalog;
import com.masson.cruciblecraft.material.MaterialCatalog;
import com.masson.cruciblecraft.material.MaterialZhNames;
import com.masson.cruciblecraft.material.prefix.MaterialPrefixCatalog;
import com.masson.cruciblecraft.registry.ModBlocks;
import com.masson.cruciblecraft.registry.ModFluids;
import com.masson.cruciblecraft.registry.ModItems;
import com.masson.cruciblecraft.registry.ModMachineVariants;
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
            addItem(ModItems.PROGRAMMED_CIRCUIT, "编程电路");
            add("tooltip.cruciblecraft.circuit_config", "配置：%s");
            add("screen.cruciblecraft.processing.status.unsupported_version",
                    "存档版本不受支持（版本 %s）");
            add("screen.cruciblecraft.processing.status.material_quarantined",
                    "机器材料已隔离");
            add("screen.cruciblecraft.processing.status.unknown", "未知状态");
            add("screen.cruciblecraft.processing.status.idle", "空闲");
            add("screen.cruciblecraft.processing.status.running", "运行中");
            add("screen.cruciblecraft.processing.status.invalid_recipe", "配方无效");
            add("screen.cruciblecraft.processing.status.recipe_power_exceeded",
                    "配方功率超过机器输入窗口");
            add("screen.cruciblecraft.processing.status.overcharged",
                    "输入包过压");
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
            add("tooltip.cruciblecraft.pipe.connect",
                    "扳手：九宫格切换连接");
            add("tooltip.cruciblecraft.cable.connect",
                    "剪线钳：九宫格切换连接");
            add("jade.cruciblecraft.fluid_pipe",
                    "%s：%s/%s mB，近 20 tick 传输 %s mB，失效 %s");
            add("jade.cruciblecraft.item_pipe",
                    "%s：实际送达 %s，堵塞 %s，Cover %s");
            add("death.attack.electricity", "%s 被电死了");
            addItem(ModItems.PORTABLE_FLUID_TANK, "便携流体罐");
            addItem(ModItems.FLUID_CELL, "通用流体单元");
            addItem(ModItems.GAS_CELL, "通用气体单元");
            addItem(
                    ModItems.BRONZE_DOUBLE_MACHINE_CASING,
                    "青铜双层机器外壳");
            addItem(
                    ModItems.STEEL_DOUBLE_MACHINE_CASING,
                    "钢双层机器外壳");
            addItem(
                    ModItems.TITANIUM_DOUBLE_MACHINE_CASING,
                    "钛双层机器外壳");
            addItem(
                    ModItems.STEEL_GALVANIZED_MACHINE_CASING,
                    "镀锌钢机器外壳");
            addItem(
                    ModItems.ALUMINIUM_MACHINE_CASING,
                    "铝机器外壳");
            addItem(
                    ModItems.STAINLESS_STEEL_MACHINE_CASING,
                    "不锈钢机器外壳");
            addItem(
                    ModItems.CHROMIUM_MACHINE_CASING,
                    "铬制机器外壳");
            addItem(
                    ModItems.TITANIUM_MACHINE_CASING,
                    "钛制机器外壳");
            addItem(
                    ModItems.TUNGSTENSTEEL_DOUBLE_MACHINE_CASING,
                    "钨钢双层机器外壳");
            addItem(
                    ModItems.INVAR_DOUBLE_MACHINE_CASING,
                    "殷钢双层机器外壳");
            addItem(
                    ModItems.TUNGSTEN_CARBIDE_DOUBLE_MACHINE_CASING,
                    "碳化钨双层机器外壳");
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
            addItem(ModItems.CONVEYOR_COVER, "传送带盖板");
            addItem(ModItems.RETRIEVER_ITEM_COVER, "物品检索器盖板");
            addItem(ModItems.ROBOT_ARM_COVER, "机械臂盖板");
            addItem(ModItems.PRESSURE_VALVE_COVER, "压力阀盖板");
            addItem(ModItems.SELECTOR_MANUAL_COVER, "手动选择器盖板");
            add("tooltip.cruciblecraft.portable_fluid_tank.empty",
                    "空（容量 %s mB）");
            add("tooltip.cruciblecraft.portable_fluid_tank.contents",
                    "%s：%s/%s mB");
            addBlock(ModBlocks.FIREBOX, "固体燃料燃烧室");
            addBlock(ModBlocks.CRUCIBLE, "坩埚");
            addBlock(ModBlocks.ANVIL, "锻造砧");
            addBlock(ModBlocks.BRONZE_BOILER, "青铜锅炉");
            addBlock(ModBlocks.BRONZE_STEAM_ENGINE, "青铜蒸汽机");
            addBlock(ModBlocks.BRONZE_DYNAMO, "青铜发电机");
            addBlock(ModBlocks.MULTIBLOCK_CASING, "通用多方块外壳");
            addBlock(ModBlocks.MULTIBLOCK_ITEM_FLUID_PORT, "多方块物品流体端口");
            addBlock(ModBlocks.MULTIBLOCK_ENERGY_INPUT_PORT, "多方块能源输入端口");
            addBlock(ModBlocks.LARGE_CENTRIFUGE, "大型离心机");
            addBlock(ModBlocks.DISTILLATION_TOWER, "蒸馏塔");
            addBlock(ModBlocks.LARGE_BOILER, "大型锅炉");
            addBlock(ModBlocks.TANK_3X3X3, "3x3x3 储罐");
            addBlock(ModBlocks.LARGE_CRUCIBLE, "大型坩埚");
            addBlock(ModBlocks.CENTRIFUGE, "离心机");
            addBlock(ModBlocks.SIFTER, "筛选机");
            addBlock(ModBlocks.EXTRUDER, "挤压机");
            addBlock(ModBlocks.ELECTROLYZER, "电解机");
            addBlock(ModBlocks.ALUMINIUM_ELECTROLYZER, "铝制电解机");
            addBlock(
                    ModBlocks.STAINLESS_STEEL_ELECTROLYZER,
                    "不锈钢电解机");
            addBlock(ModBlocks.STEEL_CENTRIFUGE, "钢制离心机");
            addBlock(ModBlocks.TITANIUM_CENTRIFUGE, "钛制离心机");
            addBlock(ModBlocks.STEEL_SIFTER, "钢制筛选机");
            addBlock(ModBlocks.TITANIUM_SIFTER, "钛制筛选机");
            addBlock(ModBlocks.MIXER, "混合机");
            addBlock(ModBlocks.AUTOCLAVE, "高压釜");
            addBlock(ModBlocks.COMPRESSOR, "压缩机");
            addBlock(ModBlocks.GENERIFIER, "通化机");
            addBlock(ModBlocks.FLUID_DEPOSIT_EXTRACTOR, "流体矿床抽取机");
            addBlock(ModBlocks.FUEL_ENGINE, "燃油发动机");
            addBlock(ModBlocks.BURNING_GAS_GENERATOR, "燃气发电机");
            addBlock(ModBlocks.ELECTRIC_MOTOR, "电动机");
            addBlock(ModBlocks.ROTATIONAL_AXLE, "旋转传动轴");
            addBlock(ModBlocks.ROTATIONAL_GEARBOX, "旋转齿轮箱");
            ModMachineVariants.T16_SELECTED.forEach(variant ->
                    add(
                            "block." + CrucibleCraft.MODID + "."
                                    + variant.id().getPath(),
                            t16MachineName(variant, true)));
            ModMachineVariants.T17_SELECTED.forEach(variant ->
                    add(
                            "block." + CrucibleCraft.MODID + "."
                                    + variant.id().getPath(),
                            t17MachineName(variant, true)));
            addRemainingCatalogMachineNames(true);
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
                    java.util.Map.entry("compressor", "压缩机"),
                    java.util.Map.entry("roaster", "焙烧炉"),
                    java.util.Map.entry("coagulator", "凝固机"))
                    .forEach((id, name) ->
                            add("emi.category.cruciblecraft." + id, name));
            java.util.Map.ofEntries(
                    java.util.Map.entry("generifier", "通化机"),
                    java.util.Map.entry("fuels_engine", "燃油发电"),
                    java.util.Map.entry("fuels_gas", "燃气发电"))
                    .forEach((id, name) ->
                            add("emi.category.cruciblecraft." + id, name));
            add("emi.cruciblecraft.processing.preserved", "保留，不消耗");
            add("emi.cruciblecraft.anvil.hits", "%s · %s 次击打");
            add("emi.cruciblecraft.anvil.hits_with_chance", "%s · %s 次击打 · %s%%");
            add("pack.cruciblecraft.generated_materials", "CrucibleCraft 生成材质包");
            add("emi.cruciblecraft.processing.wear", "工具耐久消耗：%s");
            add("emi.cruciblecraft.processing.chance", "产出概率：%s%%");
            add("emi.cruciblecraft.processing.duration", "耗时：%s tick（%s 秒）");
            add("emi.cruciblecraft.processing.power.kinetic", "功率：%s KU/t");
            add("emi.cruciblecraft.processing.power.electric", "功率：%s EU/t");
            add("emi.cruciblecraft.processing.power.heat", "热功率：%s HU/t");
            add("emi.cruciblecraft.processing.power.time", "工时：%s TU/t");
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
            add("item.cruciblecraft.material_wire_cutter", "%s剪线钳");
            add("jade.cruciblecraft.pipe_covers", "Cover 参数：%s");
            add("tooltip.cruciblecraft.cover.behavior", "行为：%s");
            add("tooltip.cruciblecraft.cover.parameters",
                    "速率 %s，精确数 %s，模式 %s，压力 %s mB，选择 %s");
            add("screen.cruciblecraft.coke_oven.creosote", "杂酚油：%s / %s mB");
            add("screen.cruciblecraft.coke_oven.invalid_structure", "结构无效");
            add("screen.cruciblecraft.coke_oven.no_heat", "无热量");
            add("container.cruciblecraft.bronze_crusher", "青铜破碎机");
            add("container.cruciblecraft.coke_oven", "焦炉");
            add("disconnect.cruciblecraft.material_handshake_missing",
                    "客户端无法执行 CrucibleCraft 的材质兼容性检查。"
                            + "请安装与服务端相同版本的 CrucibleCraft。");
            add("disconnect.cruciblecraft.material_handshake_too_large",
                    "服务端的 CrucibleCraft 材质配置有 %s 项，"
                            + "超出了 %s 项的支持上限。");
            add("disconnect.cruciblecraft.material_mismatch",
                    "CrucibleCraft 材质定义与服务端不一致：%s。"
                            + "请安装与服务端相同的附加组件和材质定义。");
            add("itemGroup.cruciblecraft.cables", "Crucible Craft：电缆");
            add("itemGroup.cruciblecraft.dusts", "Crucible Craft：粉");
            add("itemGroup.cruciblecraft.mechanical_parts",
                    "Crucible Craft：机械部件");
            add("itemGroup.cruciblecraft.metals_gems",
                    "Crucible Craft：金属与宝石");
            add("itemGroup.cruciblecraft.misc",
                    "Crucible Craft：杂项材料");
            add("itemGroup.cruciblecraft.ore_processing",
                    "Crucible Craft：矿石处理");
            add("itemGroup.cruciblecraft.ores", "Crucible Craft：矿石");
            add("itemGroup.cruciblecraft.parts", "Crucible Craft：部件");
            add("itemGroup.cruciblecraft.pipes", "Crucible Craft：管道");
            add("itemGroup.cruciblecraft.plates", "Crucible Craft：板");
            add("itemGroup.cruciblecraft.tools", "Crucible Craft：工具");
            add("itemGroup.cruciblecraft.fluid_cells",
                    "Crucible Craft：流体单元");
            add("item.cruciblecraft.fluid_cell.filled", "%s 流体单元");
            add("item.cruciblecraft.gas_cell.filled", "%s 气体单元");
            add("itemGroup.cruciblecraft.wires", "Crucible Craft：线材");
            add("cruciblecraft.configuration.section.cruciblecraft.client.toml",
                    "客户端");
            add("cruciblecraft.configuration.section.cruciblecraft.client.toml.title",
                    "客户端");
            add("cruciblecraft.configuration.temperatureUnit", "温度单位");
            add("cruciblecraft.configuration.title", "CrucibleCraft 设置");
            add("screen.cruciblecraft.processing.tank", "%s / %s mB");
            add("jade.cruciblecraft.anvil_durability", "耐久：%s / %s");
            add("jade.cruciblecraft.anvil_progress", "锤击：%s");
            add("jade.cruciblecraft.anvil_slot", "槽位 %s：%s× %s");
            add("jade.cruciblecraft.anvil_workpiece", "工件：%s");
            add("jade.cruciblecraft.boiler",
                    "水：%s/%s mB，蒸汽：%s/%s mB，热量：%s/80 HU");
            add("jade.cruciblecraft.casing",
                    "外壳：%s（等级 %s，工艺等级 %s）");
            add("jade.cruciblecraft.coke_oven.creosote", "杂酚油：%s / %s mB");
            add("jade.cruciblecraft.coke_oven.invalid", "无效");
            add("jade.cruciblecraft.coke_oven.progress",
                    "进度：%s / %s tick");
            add("jade.cruciblecraft.coke_oven.structure", "结构：%s");
            add("jade.cruciblecraft.coke_oven.valid", "有效");
            add("jade.cruciblecraft.contents", "%s（%s/%s 单位）");
            add("jade.cruciblecraft.crusher",
                    "功率：%s KU/t，进度：%s/%s（%s）");
            add("jade.cruciblecraft.firebox_heat",
                    "热量：%s HU（剩余 %s 秒）");
            add("jade.cruciblecraft.firebox_output", "输出：%s HU/t");
            add("jade.cruciblecraft.machine_material", "材质：%s（等级 %s）");
            add("jade.cruciblecraft.max_temperature", "最高温度：%s %s");
            add("jade.cruciblecraft.mold_contents", "内容物：%s× %s");
            add("jade.cruciblecraft.mold_cooling", "冷却中");
            add("jade.cruciblecraft.mold_empty", "空");
            add("jade.cruciblecraft.mold_shape", "模具：%s");
            add("jade.cruciblecraft.mold_solid", "已凝固");
            add("jade.cruciblecraft.mold_state", "状态：%s，%s °C");
            add("jade.cruciblecraft.steam_engine",
                    "蒸汽：%s/%s mB，KU：%s/%s（%s 冲程）");
            add("jade.cruciblecraft.temperature", "温度：%s %s");
            add("tooltip.cruciblecraft.durability", "耐久：%s / %s");
            add("tooltip.cruciblecraft.machine_material",
                    "外壳：%s（材质等级 %s）");
            add("tooltip.cruciblecraft.max_temperature", "最高温度：%s °C");
            add("tooltip.cruciblecraft.temperature", "温度：%s %s");
            add("tooltip.cruciblecraft.tool_material",
                    "头部：%s（材质等级 %s）");
            add("tooltip.cruciblecraft.unknown_material", "缺失材质：%s（%s）");
            add("message.cruciblecraft.air_injection_continued",
                    "气流持续时间已延长");
            add("message.cruciblecraft.air_injection_started",
                    "气流已开始；脱碳正在进行");
            add("message.cruciblecraft.air_injection_too_cold",
                    "吹气前铁水必须已熔化");
            add("message.cruciblecraft.anvil_completed", "已锻造 %s");
            add("message.cruciblecraft.anvil_inserted", "工件已放置到砧上");
            add("message.cruciblecraft.anvil_no_recipe",
                    "请先在砧上放置有效工件");
            add("message.cruciblecraft.anvil_progress", "锤击中：%s/%s");
            add("message.cruciblecraft.anvil_rejected",
                    "该物品没有匹配的砧配方");
            add("message.cruciblecraft.bellows_active", "风箱已经在动");
            add("message.cruciblecraft.bellows_started", "风箱鼓风开始");
            add("message.cruciblecraft.coke_oven_cannot_ignite",
                    "结构必须完整并从下方受热");
            add("message.cruciblecraft.coke_oven_ignited", "焦炉已点燃");
            add("message.cruciblecraft.crucible_full", "坩埚已满");
            add("message.cruciblecraft.crucible_status", "温度：%s °C | %s");
            add("message.cruciblecraft.crucible_tier_too_low",
                    "该坩埚外壳无法处理该材质等级");
            add("message.cruciblecraft.firebox_fuel_rejected",
                    "火箱现在无法接受这种燃料");
            add("message.cruciblecraft.firebox_fueled",
                    "火箱已加燃料：%s HU（剩余 %s 秒）");
            add("message.cruciblecraft.inexact_material_amount",
                    "该数量无法分解为整数材质单位");
            add("message.cruciblecraft.invalid_material",
                    "该材质无法加入坩埚");
            add("message.cruciblecraft.invalid_steel_charge",
                    "炼钢需要三份铁配一份碳");
            add("message.cruciblecraft.material_cast", "铸造了一个 %s 锭");
            add("message.cruciblecraft.material_inserted",
                    "材质已加入坩埚");
            add("message.cruciblecraft.materials_changed",
                    "CrucibleCraft 的材质定义自上次打开世界以来发生了变化。"
                            + "缺失的材质被保留，使用它们的机器已暂停。");
            add("message.cruciblecraft.mold_cooling", "模具冷却中：%s °C");
            add("message.cruciblecraft.mold_filled", "熔融材料已浇入模具");
            add("message.cruciblecraft.mold_no_molten_material",
                    "相邻的坩埚没有合适的熔融材料");
            add("message.cruciblecraft.not_ready",
                    "成分不可浇铸或低于熔化温度");
            add("emi.category.cruciblecraft.anvil", "砧加工");
            add("emi.category.cruciblecraft.coke_oven", "焦炉");
            add("emi.category.cruciblecraft.crucible", "坩埚合金");
            add("emi.category.cruciblecraft.crusher", "破碎机");
            add("emi.category.cruciblecraft.mold_casting", "陶瓷模具铸造");
            addHopperTranslations(true);
            // v1 关键路径材料名域：表内材料按表生成，表外显式 post_1_0
            // （不写 zh 键，回退 en_us，禁止英文冒充）。
            MaterialCatalog.startupValues().forEach(material ->
                    MaterialZhNames.material(material.id()).ifPresent(name ->
                            add(material.translationKey(), name)));
            for (var form : MaterialPrefixCatalog.values()) {
                MaterialZhNames.prefix(form.serializedName()).ifPresent(name ->
                        add(
                                "item.cruciblecraft.material_form."
                                        + form.serializedName(),
                                "%s " + name));
            }
            ModBlocks.oreBlockPaths().keySet().forEach(key ->
                    MaterialZhNames.material(key.materialId())
                            .ifPresent(name -> addBlock(
                                    ModBlocks.oreBlock(
                                            key.materialId(), key.host()),
                                    (key.host() == Host.DEEPSLATE
                                            ? "深板岩" : "")
                                            + name + "矿石")));
            ModBlocks.pipeBlocks().forEach(holder -> {
                var pipe = holder.get().pipe();
                MaterialZhNames.material(pipe.materialId()).ifPresent(mat ->
                        MaterialZhNames.pipe(pipe.form().serializedName())
                                .ifPresent(name -> add(
                                        "block." + CrucibleCraft.MODID
                                                + "." + pipe.registryName(),
                                        mat + name)));
            });
            ModBlocks.electricalConductorBlocks().forEach(holder -> {
                var conductor = holder.get().conductor();
                MaterialZhNames.material(conductor.materialId())
                        .ifPresent(mat ->
                                MaterialZhNames.conductor(
                                                conductor.form()
                                                        .serializedName())
                                        .ifPresent(name -> add(
                                                "block." + CrucibleCraft.MODID
                                                        + "."
                                                        + conductor
                                                                .registryName(),
                                                mat + name)));
            });
            ModFluids.moltenFluids().forEach(entry ->
                    MaterialZhNames.material(entry.materialId())
                            .ifPresent(name -> add(
                                    "fluid_type.cruciblecraft.molten_"
                                            + entry.materialId(),
                                    "熔融" + name)));
            ModFluids.chemicalFluids().forEach(entry -> {
                String id = entry.id();
                boolean molten = id.startsWith("molten_");
                MaterialZhNames.material(id)
                        .or(() -> molten
                                ? MaterialZhNames.material(
                                        id.substring("molten_".length()))
                                : Optional.empty())
                        .ifPresent(name -> add(
                                "fluid_type.cruciblecraft." + id,
                                (molten ? "熔融" : "") + name));
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
        add("itemGroup.cruciblecraft.wires", "Crucible Craft: Stranded Wires");
        add("itemGroup.cruciblecraft.cables", "Crucible Craft: Conductors");
        add("itemGroup.cruciblecraft.pipes", "Crucible Craft: Pipes");
        add("itemGroup.cruciblecraft.tools", "Crucible Craft: Tools");
        add("itemGroup.cruciblecraft.fluid_cells", "Crucible Craft: Fluid Cells");
        add("item.cruciblecraft.fluid_cell.filled", "%s Fluid Cell");
        add("item.cruciblecraft.gas_cell.filled", "%s Gas Cell");
        add("itemGroup.cruciblecraft.misc", "Crucible Craft: Miscellaneous Materials");
        addBlock(ModBlocks.FIREBRICK, "Firebrick");
        addBlock(ModBlocks.FIREBOX, "Solid Fuel Firebox");
        addBlock(ModBlocks.CRUCIBLE, "Crucible");
        addBlock(ModBlocks.ANVIL, "Smithing Anvil");
        addBlock(ModBlocks.COKE_OVEN, "Coke Oven Controller");
        addBlock(ModBlocks.MULTIBLOCK_CASING, "Multiblock Casing");
        addBlock(
                ModBlocks.MULTIBLOCK_ITEM_FLUID_PORT,
                "Multiblock Item/Fluid Port");
        addBlock(
                ModBlocks.MULTIBLOCK_ENERGY_INPUT_PORT,
                "Multiblock Energy Input Port");
        addBlock(ModBlocks.LARGE_CENTRIFUGE, "Large Centrifuge");
        addBlock(ModBlocks.DISTILLATION_TOWER, "Distillation Tower");
        addBlock(ModBlocks.LARGE_BOILER, "Large Boiler");
        addBlock(ModBlocks.TANK_3X3X3, "3x3x3 Tank");
        addBlock(ModBlocks.LARGE_CRUCIBLE, "Large Crucible");
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
        addItem(ModItems.PROGRAMMED_CIRCUIT, "Programmed Circuit");
        add("tooltip.cruciblecraft.circuit_config", "Configuration: %s");
        addBlock(ModBlocks.BELLOWS, "Bellows");
        addItem(ModItems.CREOSOTE_BUCKET, "Creosote Bucket");
        add("fluid_type.cruciblecraft.creosote", "Creosote");
        addItem(ModItems.STEAM_BUCKET, "Steam Bucket");
        add("fluid_type.cruciblecraft.steam", "Steam");
        addItem(ModItems.PORTABLE_FLUID_TANK, "Portable Fluid Tank");
        addItem(ModItems.FLUID_CELL, "Universal Fluid Cell");
        addItem(ModItems.GAS_CELL, "Universal Gas Cell");
        addItem(
                ModItems.BRONZE_DOUBLE_MACHINE_CASING,
                "Bronze Double Machine Casing");
        addItem(
                ModItems.STEEL_DOUBLE_MACHINE_CASING,
                "Steel Double Machine Casing");
        addItem(
                ModItems.TITANIUM_DOUBLE_MACHINE_CASING,
                "Titanium Double Machine Casing");
        addItem(
                ModItems.STEEL_GALVANIZED_MACHINE_CASING,
                "Galvanized Steel Machine Casing");
        addItem(
                ModItems.ALUMINIUM_MACHINE_CASING,
                "Aluminium Machine Casing");
        addItem(
                ModItems.STAINLESS_STEEL_MACHINE_CASING,
                "Stainless Steel Machine Casing");
        addItem(
                ModItems.CHROMIUM_MACHINE_CASING,
                "Chromium Machine Casing");
        addItem(
                ModItems.TITANIUM_MACHINE_CASING,
                "Titanium Machine Casing");
        addItem(
                ModItems.TUNGSTENSTEEL_DOUBLE_MACHINE_CASING,
                "Tungstensteel Double Machine Casing");
        addItem(
                ModItems.INVAR_DOUBLE_MACHINE_CASING,
                "Invar Double Machine Casing");
        addItem(
                ModItems.TUNGSTEN_CARBIDE_DOUBLE_MACHINE_CASING,
                "Tungsten Carbide Double Machine Casing");
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
        addBlock(ModBlocks.ELECTRIC_MOTOR, "Electric Motor");
        addBlock(ModBlocks.ROTATIONAL_AXLE, "Rotational Axle");
        addBlock(ModBlocks.ROTATIONAL_GEARBOX, "Rotational Gearbox");
        addBlock(ModBlocks.BRONZE_CRUSHER, "Bronze Crusher");
        addBlock(ModBlocks.SLUICE, "Sluice");
        addBlock(ModBlocks.BATH, "Ore Washing Bath");
        addBlock(ModBlocks.CENTRIFUGE, "Centrifuge");
        addBlock(ModBlocks.STEEL_CENTRIFUGE, "Steel Centrifuge");
        addBlock(
                ModBlocks.TITANIUM_CENTRIFUGE,
                "Titanium Centrifuge");
        addBlock(ModBlocks.SIFTER, "Sifter");
        addBlock(ModBlocks.STEEL_SIFTER, "Steel Sifter");
        addBlock(ModBlocks.TITANIUM_SIFTER, "Titanium Sifter");
        addBlock(ModBlocks.MORTAR, "Powered Mortar");
        addBlock(ModBlocks.EXTRUDER, "Extruder");
        addBlock(ModBlocks.CUTTER, "Cutter");
        addBlock(ModBlocks.ROLLBENDER, "Roll Bender");
        addBlock(ModBlocks.BENDER, "Bender");
        addBlock(ModBlocks.ASSEMBLER, "Assembler");
        addBlock(ModBlocks.WELDER, "Welder");
        ModMachineVariants.T16_SELECTED.forEach(variant ->
                add(
                        "block." + CrucibleCraft.MODID + "."
                                + variant.id().getPath(),
                        t16MachineName(variant, false)));
        ModMachineVariants.T17_SELECTED.forEach(variant ->
                add(
                        "block." + CrucibleCraft.MODID + "."
                                + variant.id().getPath(),
                        t17MachineName(variant, false)));
        addRemainingCatalogMachineNames(false);
        addBlock(ModBlocks.ELECTROLYZER, "Electrolyzer");
        addBlock(
                ModBlocks.ALUMINIUM_ELECTROLYZER,
                "Aluminium Electrolyzer");
        addBlock(
                ModBlocks.STAINLESS_STEEL_ELECTROLYZER,
                "Stainless Steel Electrolyzer");
        addBlock(ModBlocks.MIXER, "Mixer");
        addBlock(ModBlocks.AUTOCLAVE, "Autoclave");
        addBlock(ModBlocks.COMPRESSOR, "Compressor");
        addBlock(ModBlocks.GENERIFIER, "Generifier");
        addBlock(ModBlocks.FLUID_DEPOSIT_EXTRACTOR, "Fluid Deposit Extractor");
        addBlock(ModBlocks.FUEL_ENGINE, "Fuel Engine");
        addBlock(ModBlocks.BURNING_GAS_GENERATOR, "Burning Gas Generator");
        add("screen.cruciblecraft.processing.status.idle", "Idle");
        add("screen.cruciblecraft.processing.status.running", "Running");
        add("screen.cruciblecraft.processing.status.invalid_recipe", "Invalid recipe");
        add("screen.cruciblecraft.processing.status.recipe_power_exceeded",
                "Recipe power exceeds machine input window");
        add("screen.cruciblecraft.processing.status.overcharged",
                "Input packet overcharged");
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
        add("tooltip.cruciblecraft.pipe.connect",
                "Wrench: 3x3 grid connection toggle");
        add("tooltip.cruciblecraft.cable.connect",
                "Wire Cutter: 3x3 grid connection toggle");
        add("jade.cruciblecraft.fluid_pipe",
                "%s: %s/%s mB, last 20 ticks %s mB, failure %s");
        add("jade.cruciblecraft.item_pipe",
                "%s: actual delivery %s, clogged %s, covers %s");
        add("jade.cruciblecraft.pipe_covers", "Cover parameters: %s");
        add("tooltip.cruciblecraft.cover.behavior", "Behavior: %s");
        add("tooltip.cruciblecraft.cover.parameters",
                "Rate %s, exact %s, mode %s, pressure %s mB, selector %s");
        add("death.attack.electricity",
                "%s was electrocuted");
        addItem(ModItems.PIPE_FILTER_COVER, "Pipe Filter Cover");
        addItem(ModItems.PIPE_VALVE_COVER, "Pipe One-Way Valve Cover");
        addItem(ModItems.PIPE_PUMP_COVER, "Pipe Output Pump Cover");
        addItem(ModItems.CONVEYOR_COVER, "Conveyor Cover");
        addItem(ModItems.RETRIEVER_ITEM_COVER, "Item Retriever Cover");
        addItem(ModItems.ROBOT_ARM_COVER, "Robot Arm Cover");
        addItem(ModItems.PRESSURE_VALVE_COVER, "Pressure Valve Cover");
        addItem(ModItems.SELECTOR_MANUAL_COVER, "Manual Selector Cover");
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
        add("item.cruciblecraft.material_wire_cutter", "%s Wire Cutter");
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
        add("emi.category.cruciblecraft.fuels_engine", "Fuel Engine");
        add("emi.category.cruciblecraft.fuels_gas", "Burning Gas Generator");
        add("emi.cruciblecraft.processing.preserved", "Preserved, not consumed");
        add("emi.cruciblecraft.anvil.hits", "%s · %s hits");
        add("emi.cruciblecraft.anvil.hits_with_chance", "%s · %s hits · %s%%");
        add("pack.cruciblecraft.generated_materials", "CrucibleCraft Generated Materials");
        add("emi.cruciblecraft.processing.wear", "Tool damage: %s");
        add("emi.cruciblecraft.processing.chance", "Output chance: %s%%");
        add("emi.cruciblecraft.processing.duration", "Duration: %s ticks (%s s)");
        add("emi.cruciblecraft.processing.power.kinetic", "Power: %s KU/t");
        add("emi.cruciblecraft.processing.power.electric", "Power: %s EU/t");
        add("emi.cruciblecraft.processing.power.heat", "Heat: %s HU/t");
        add("emi.cruciblecraft.processing.power.time", "Work time: %s TU/t");

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
        addHopperTranslations(false);
    }

    private void addHopperTranslations(boolean chinese) {
        HopperVariantCatalog.variants().forEach(variant -> {
            String key = "block." + CrucibleCraft.MODID + "."
                    + variant.id().getPath();
            if (chinese) {
                MaterialZhNames.material(variant.materialPath()).ifPresent(zh ->
                        add(key, zh + (variant.kind().isQueue()
                                ? "制队列料斗" : "制料斗")));
            } else {
                add(key, title(variant.materialPath())
                        + (variant.kind().isQueue()
                                ? " Queue Hopper" : " Hopper"));
            }
        });
        if (chinese) {
            addBlock(ModBlocks.STEEL_DUST_FUNNEL, "钢制粉末漏斗");
            add("container.cruciblecraft.hopper", "料斗");
            add("container.cruciblecraft.queue_hopper", "队列料斗");
            add("tooltip.cruciblecraft.hopper.slots", "槽位：%s");
            add("tooltip.cruciblecraft.hopper.fifo", "先进先出");
            add("message.cruciblecraft.hopper.queue_slot_size", "队列槽上限：%s");
            add("message.cruciblecraft.hopper.mode_stack", "精确整组输出");
            add("message.cruciblecraft.hopper.mode_any", "任意数量输出");
            add("message.cruciblecraft.hopper.mode_exact", "精确输出 %s 个");
            add("message.cruciblecraft.hopper.mode_divisible", "整除输出 %s 个");
            add("message.cruciblecraft.hopper.queue_no_exact", "队列料斗没有精确模式");
            add("message.cruciblecraft.dust_funnel.mode", "粉末漏斗输出：%s");
        } else {
            addBlock(ModBlocks.STEEL_DUST_FUNNEL, "Steel Dust Funnel");
            add("container.cruciblecraft.hopper", "Hopper");
            add("container.cruciblecraft.queue_hopper", "Queue Hopper");
            add("tooltip.cruciblecraft.hopper.slots", "Slots: %s");
            add("tooltip.cruciblecraft.hopper.fifo", "First in, first out");
            add("message.cruciblecraft.hopper.queue_slot_size", "Queue slot size: %s");
            add("message.cruciblecraft.hopper.mode_stack", "Exact full-stack output");
            add("message.cruciblecraft.hopper.mode_any", "Any-count output");
            add("message.cruciblecraft.hopper.mode_exact", "Exact output of %s");
            add("message.cruciblecraft.hopper.mode_divisible", "Divisible output of %s");
            add("message.cruciblecraft.hopper.queue_no_exact", "Queue hoppers have no exact mode");
            add("message.cruciblecraft.dust_funnel.mode", "Dust funnel output: %s");
        }
    }

    private static String title(String value) {
        String spaced = value.replace('_', ' ');
        return spaced.substring(0, 1).toUpperCase(Locale.ROOT) + spaced.substring(1);
    }

    private static String t16MachineName(
            com.masson.cruciblecraft.machine.processing.MachineVariant variant,
            boolean chinese) {
        String material = variant.tierBand().materialId()
                .substring(
                        variant.tierBand().materialId().indexOf(':') + 1);
        String kind = variant.kind().id().getPath();
        if (chinese) {
            String tierName = switch (material) {
                case "bronze" -> "青铜";
                case "steel" -> "钢制";
                case "titanium" -> "钛制";
                default -> throw new IllegalArgumentException(
                        "Unsupported T16 material " + material);
            };
            String machineName = switch (kind) {
                case "lathe" -> "车床";
                case "rollingmill" -> "轧机";
                case "wiremill" -> "线材轧机";
                case "shredder" -> "粉碎机";
                case "press" -> "压机";
                default -> throw new IllegalArgumentException(
                        "Unsupported T16 machine kind " + kind);
            };
            return tierName + machineName;
        }
        String machineName = switch (kind) {
            case "lathe" -> "Lathe";
            case "rollingmill" -> "Rolling Mill";
            case "wiremill" -> "Wire Mill";
            case "shredder" -> "Shredder";
            case "press" -> "Press";
            default -> throw new IllegalArgumentException(
                    "Unsupported T16 machine kind " + kind);
        };
        return title(material) + " " + machineName;
    }

    private static String t17MachineName(
            com.masson.cruciblecraft.machine.processing.MachineVariant variant,
            boolean chinese) {
        String material = variant.tierBand().materialId()
                .substring(
                        variant.tierBand().materialId().indexOf(':') + 1);
        String kind = variant.kind().id().getPath();
        if (chinese) {
            String tierName = switch (material) {
                case "steel" -> "钢制";
                case "invar" -> "殷钢";
                case "titanium" -> "钛制";
                default -> throw new IllegalArgumentException(
                        "Unsupported T17 material " + material);
            };
            String machineName = switch (kind) {
                case "distillery" -> "蒸馏机";
                case "drying" -> "干燥机";
                case "smelter" -> "熔炼炉";
                default -> throw new IllegalArgumentException(
                        "Unsupported T17 machine kind " + kind);
            };
            return tierName + machineName;
        }
        String machineName = switch (kind) {
            case "distillery" -> "Distillery";
            case "drying" -> "Drying Machine";
            case "smelter" -> "Smelter";
            default -> throw new IllegalArgumentException(
                    "Unsupported T17 machine kind " + kind);
        };
        return title(material) + " " + machineName;
    }

    private void addRemainingCatalogMachineNames(boolean chinese) {
        java.util.Set<String> alreadyNamed = new java.util.HashSet<>();
        alreadyNamed.add("centrifuge");
        alreadyNamed.add("steel_centrifuge");
        alreadyNamed.add("titanium_centrifuge");
        alreadyNamed.add("sifter");
        alreadyNamed.add("steel_sifter");
        alreadyNamed.add("titanium_sifter");
        alreadyNamed.add("electrolyzer");
        alreadyNamed.add("aluminium_electrolyzer");
        alreadyNamed.add("stainless_steel_electrolyzer");
        alreadyNamed.add("mixer");
        alreadyNamed.add("autoclave");
        alreadyNamed.add("compressor");
        alreadyNamed.add("generifier");
        alreadyNamed.add("extruder");
        if (!chinese) {
            alreadyNamed.add("sluice");
            alreadyNamed.add("bath");
            alreadyNamed.add("mortar");
            alreadyNamed.add("cutter");
            alreadyNamed.add("rollbender");
            alreadyNamed.add("bender");
            alreadyNamed.add("assembler");
            alreadyNamed.add("welder");
            alreadyNamed.add("shredder");
            alreadyNamed.add("press");
            alreadyNamed.add("lathe");
            alreadyNamed.add("rollingmill");
            alreadyNamed.add("wiremill");
            alreadyNamed.add("distillery");
            alreadyNamed.add("drying");
            alreadyNamed.add("smelter");
        }
        ModMachineVariants.T16_SELECTED.forEach(variant ->
                alreadyNamed.add(variant.id().getPath()));
        ModMachineVariants.T17_SELECTED.forEach(variant ->
                alreadyNamed.add(variant.id().getPath()));
        ModMachineVariants.ALL.forEach(variant -> {
            String path = variant.id().getPath();
            if (alreadyNamed.contains(path)
                    || ModMachineVariants.isOpening(variant.id())) {
                return;
            }
            add(
                    "block." + CrucibleCraft.MODID + "." + path,
                    catalogMachineName(variant, chinese));
        });
    }

    private static String catalogMachineName(
            com.masson.cruciblecraft.machine.processing.MachineVariant variant,
            boolean chinese) {
        String material = variant.tierBand().materialId()
                .substring(
                        variant.tierBand().materialId().indexOf(':') + 1);
        String kind = variant.kind().id().getPath();
        if ("bronze_crusher".equals(kind)) {
            kind = "crusher";
        }
        if (chinese) {
            return materialZh(material) + kindZh(kind);
        }
        return title(material) + " " + kindEn(kind);
    }

    private static String materialZh(String material) {
        return switch (material) {
            case "bronze" -> "青铜";
            case "steel" -> "钢制";
            case "titanium" -> "钛制";
            case "aluminium" -> "铝制";
            case "stainless_steel" -> "不锈钢";
            case "invar" -> "殷钢";
            case "tungstensteel" -> "钨钢";
            case "tungsten_carbide" -> "碳化钨";
            case "chromium" -> "铬制";
            case "steel_galvanized" -> "镀锌钢";
            default -> title(material);
        };
    }

    private static String kindZh(String kind) {
        return switch (kind) {
            case "centrifuge" -> "离心机";
            case "sifter" -> "筛选机";
            case "electrolyzer" -> "电解机";
            case "lathe" -> "车床";
            case "rollingmill" -> "轧机";
            case "wiremill" -> "线材轧机";
            case "shredder" -> "粉碎机";
            case "press" -> "压机";
            case "distillery" -> "蒸馏机";
            case "drying" -> "干燥机";
            case "smelter" -> "熔炼炉";
            case "assembler" -> "装配机";
            case "autoclave" -> "高压釜";
            case "bath" -> "洗矿浴池";
            case "bender" -> "折弯机";
            case "crusher" -> "破碎机";
            case "coagulator" -> "凝固机";
            case "compressor" -> "压缩机";
            case "cutter" -> "切割机";
            case "extruder" -> "挤压机";
            case "generifier" -> "通化机";
            case "mixer" -> "混合机";
            case "mortar" -> "动力研钵";
            case "roaster" -> "焙烧炉";
            case "rollbender" -> "卷板机";
            case "sluice" -> "溜槽";
            case "welder" -> "焊机";
            default -> title(kind);
        };
    }

    private static String kindEn(String kind) {
        return switch (kind) {
            case "rollingmill" -> "Rolling Mill";
            case "wiremill" -> "Wire Mill";
            case "drying" -> "Drying Machine";
            case "rollbender" -> "Roll Bender";
            default -> title(kind);
        };
    }
}
