package com.masson.cruciblecraft.datagen;

import java.util.Locale;
import java.util.Map;
import java.util.Optional;

import com.masson.cruciblecraft.CrucibleCraft;
import com.masson.cruciblecraft.content.item.BathMteFluidCatalog;
import com.masson.cruciblecraft.content.item.BathMteIdentityCatalog;
import com.masson.cruciblecraft.content.item.SmelterMteIdentityCatalog;
import com.masson.cruciblecraft.content.item.ExtruderShapeCatalog;
import com.masson.cruciblecraft.content.item.GtWoodCatalog;
import com.masson.cruciblecraft.content.item.GtBlockObjectCatalog;
import com.masson.cruciblecraft.content.item.GtStoneCatalog;
import com.masson.cruciblecraft.content.item.ToolPatternCatalog;
import com.masson.cruciblecraft.content.item.TechnologicalPartCatalog;
import com.masson.cruciblecraft.content.mold.CeramicMoldCatalog;
import com.masson.cruciblecraft.content.mte.MteInPlaceCatalog;
import com.masson.cruciblecraft.content.mte.MteInPlaceKind;
import com.masson.cruciblecraft.content.mte.MteInPlaceDisplayNames;
import com.masson.cruciblecraft.content.mte.MteInPlaceMaterials;
import com.masson.cruciblecraft.content.mte.MteInPlaceSpec;
import com.masson.cruciblecraft.content.storage.StorageVariantCatalog;
import com.masson.cruciblecraft.logistics.hopper.HopperVariantCatalog;
import com.masson.cruciblecraft.content.sensor.SensorKind;
import com.masson.cruciblecraft.content.redstonewire.RedstoneWireKind;
import com.masson.cruciblecraft.logistics.machinecover.MachineCoverKinds;
import com.masson.cruciblecraft.logistics.pipe.cover.CoverComponentTiers;
import com.masson.cruciblecraft.material.MaterialCatalog;
import com.masson.cruciblecraft.material.MaterialZhNames;
import com.masson.cruciblecraft.material.prefix.MaterialPrefixCatalog;
import com.masson.cruciblecraft.localization.LanguageNames;
import com.masson.cruciblecraft.energy.battery.EnergyBatteryCatalog;
import com.masson.cruciblecraft.energy.converter.EnergyConverterKindCatalog;
import com.masson.cruciblecraft.energy.converter.EnergyConverterTierCatalog;
import com.masson.cruciblecraft.energy.transformer.EnergyTransformerCatalog;
import com.masson.cruciblecraft.energy.heatexchanger.HeatExchangerCatalog;
import com.masson.cruciblecraft.energy.cooler.CoolerCatalog;
import com.masson.cruciblecraft.energy.flux.FluxCatalog;
import com.masson.cruciblecraft.machine.ToolMaterialRules.ToolKind;
import com.masson.cruciblecraft.machine.processing.MachineKindCatalog;
import com.masson.cruciblecraft.compat.emi.EmiStackGroupPlan;
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
                    LanguageNames.chineseOrEmpty(shape.chineseName(), shape.englishName())
                            .ifPresent(name -> addItem(
                                    ModItems.extruderShape(shape.id()), name)));
            GtWoodCatalog.DEFINITIONS.forEach(wood ->
                    LanguageNames.chineseOrEmpty(wood.chineseName(), wood.englishName())
                            .ifPresent(name -> addBlock(
                                    ModBlocks.gtWood(wood.id()), name)));
            GtStoneCatalog.variants().forEach(stone -> {
                var holder = ModBlocks.gtStoneBlocksById().get(stone.id());
                if (holder != null && !stone.chineseName().isBlank()) {
                    addBlock(holder, stone.chineseName());
                }
            });
            com.masson.cruciblecraft.worldgen.StoneLayerStones.registeredCubes().forEach(cube -> {
                if (ModBlocks.hasLayerStone(cube.registryPath())) {
                    addBlock(
                            ModBlocks.layerStone(cube.registryPath()),
                            cube.chinese());
                }
            });
            GtBlockObjectCatalog.variants().forEach(block -> {
                var holder = ModBlocks.gtBlockObjectBlocksById().get(block.id());
                LanguageNames.playerChinese(block.chineseName(), block.registryPath())
                        .or(() -> LanguageNames.chineseOrEmpty(
                                block.chineseName(), block.englishName()))
                        .ifPresent(name -> addBlock(holder, name));
            });
            com.masson.cruciblecraft.content.item.BathRemainderBlockObjectCatalog.variants().forEach(block -> {
                var holder = ModBlocks.bathRemainderBlockObjectBlocksById().get(block.id());
                LanguageNames.playerChinese(block.chineseName(), block.registryPath())
                        .or(() -> LanguageNames.chineseOrEmpty(
                                block.chineseName(), block.englishName()))
                        .ifPresent(name -> addBlock(holder, name));
            });
            com.masson.cruciblecraft.content.item.GtBuildingBlockCatalog.variants().forEach(block -> {
                var holder = ModBlocks.gtBuildingBlockObjectBlocksById().get(block.id());
                LanguageNames.playerChinese(block.chineseName(), block.registryPath())
                        .or(() -> LanguageNames.chineseOrEmpty(
                                block.chineseName(), block.englishName()))
                        .ifPresent(name -> addBlock(holder, name));
            });
            add("tooltip.cruciblecraft.fireproof", "防火");
            ToolPatternCatalog.DEFINITIONS.forEach(pattern ->
                    LanguageNames.chineseOrEmpty(
                                    pattern.chineseName(), pattern.englishName())
                            .ifPresent(name -> addItem(
                                    ModItems.toolPattern(pattern.id()), name)));
            addItem(ModItems.PROGRAMMED_CIRCUIT, "编程电路");
            add("tooltip.cruciblecraft.circuit_config", "配置：%s");
            add("tooltip.cruciblecraft.reactor_rod.empty", "空反应棒");
            add("tooltip.cruciblecraft.reactor_rod.absorber", "中子吸收");
            add("tooltip.cruciblecraft.reactor_rod.reflector", "中子反射");
            add("tooltip.cruciblecraft.reactor_rod.moderator", "中子慢化");
            add("tooltip.cruciblecraft.reactor_rod.nuclear", "裂变燃料");
            add("tooltip.cruciblecraft.reactor_rod.depleted", "乏燃料");
            add("tooltip.cruciblecraft.reactor_rod.breeder", "增殖棒");
            add("tooltip.cruciblecraft.reactor_rod.product", "富集产物");
            add("tooltip.cruciblecraft.reactor_rod.durability", "耐久：%s");
            add("tooltip.cruciblecraft.reactor_rod.neutrons", "中子 邻 %s / 自 %s / 上限 %s / 除数 %s");
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
            addJadeObservation();
            addJadePluginConfig();
            addEmptyToolHeadNames();
            add("tooltip.cruciblecraft.electrical.specification",
                    "GT6 规格：%s");
            add("tooltip.cruciblecraft.electrical.rating",
                    "额定：%s V，%s A，损耗 %s EU/方块");
            add("tooltip.cruciblecraft.electrical.insulated", "绝缘电缆");
            add("tooltip.cruciblecraft.electrical.bare", "裸线");
            add("tooltip.cruciblecraft.lu_fiber_cable",
                    "GT6 LU 光纤：无损传输");
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
            add("jade.cruciblecraft.fluid_spring.fluid", "流体：%s");
            add("jade.cruciblecraft.fluid_spring.amount", "泉量：%s mB");
            add("jade.cruciblecraft.item_pipe",
                    "%s：实际送达 %s，堵塞 %s，盖板 %s");
            add("death.attack.electricity", "%s 被电死了");
            add("death.attack.crusher", "%s 被碾成了肉酱");
            add("death.attack.shredder", "%s 被粉碎成了碎片");
            addItem(ModItems.PORTABLE_FLUID_TANK, "便携流体罐");
            addItem(ModItems.FLUID_CELL, "通用流体单元");
            addItem(ModItems.GAS_CELL, "通用气体单元");
            addBatteryCellNames(true);
            add("tooltip.cruciblecraft.fluid_cell.empty",
                    "空流体单元（容量 %s mB）");
            add("tooltip.cruciblecraft.fluid_cell.contents",
                    "%s：%s/%s mB");
            add("tooltip.cruciblecraft.gas_cell.empty",
                    "空气体单元（容量 %s mB）");
            add("tooltip.cruciblecraft.gas_cell.contents",
                    "%s：%s/%s mB");
            add("tooltip.cruciblecraft.fluid_attachment.liquid",
                    "GT6 液体附件：右键容器交互");
            add("tooltip.cruciblecraft.fluid_attachment.gas",
                    "GT6 气体附件：右键容器交互");
            add("tooltip.cruciblecraft.fluid_attachment.faucet",
                    "GT6 浇铸口：点击浇铸坩埚或向下方容器排液");
            add("tooltip.cruciblecraft.fluid_attachment.acid_proof",
                    "耐酸");
            add("tooltip.cruciblecraft.fluid_attachment.magic_proof",
                    "耐魔法流体");
            addItem(ModItems.PIPE_FILTER_COVER, "物品过滤覆盖板");
            addItem(ModItems.PIPE_VALVE_COVER, "管道封闭覆盖板");
            addItem(ModItems.PIPE_PUMP_COVER, "管道输出泵盖板");
            addItem(ModItems.CONVEYOR_COVER, "传送带盖板");
            addItem(ModItems.RETRIEVER_ITEM_COVER, "物品抽取覆盖板");
            addItem(ModItems.ROBOT_ARM_COVER, "机械臂");
            addItem(ModItems.PRESSURE_VALVE_COVER, "释压安全阀");
            addItem(ModItems.SELECTOR_MANUAL_COVER, "手动选择面板");
            addItem(ModItems.LOGISTICS_ITEM_STORAGE_COVER, "过滤物流存储总线(物品)");
            addItem(ModItems.LOGISTICS_ITEM_IMPORT_COVER, "过滤物流输入总线(物品)");
            addItem(ModItems.LOGISTICS_ITEM_EXPORT_COVER, "过滤物流输出总线(物品)");
            addItem(ModItems.LOGISTICS_FLUID_STORAGE_COVER, "过滤物流存储总线(流体)");
            addItem(ModItems.LOGISTICS_FLUID_IMPORT_COVER, "过滤物流输入总线(流体)");
            addItem(ModItems.LOGISTICS_FLUID_EXPORT_COVER, "过滤物流输出总线(流体)");
            addItem(ModItems.LOGISTICS_GENERIC_STORAGE_COVER, "通用物流存储总线");
            addItem(ModItems.LOGISTICS_GENERIC_IMPORT_COVER, "通用物流输入总线");
            addItem(ModItems.LOGISTICS_GENERIC_EXPORT_COVER, "通用物流输出总线");
            addItem(ModItems.LOGISTICS_GENERIC_DUMP_COVER, "物流回收总线(物品)");
            addItem(ModItems.LOGISTICS_DISPLAY_CPU_LOGIC_COVER, "物流监视器(逻辑处理器)");
            addItem(ModItems.LOGISTICS_DISPLAY_CPU_CONTROL_COVER, "物流监视器(控制处理器)");
            addItem(ModItems.LOGISTICS_DISPLAY_CPU_STORAGE_COVER, "物流监视器(存储处理器)");
            addItem(ModItems.LOGISTICS_DISPLAY_CPU_CONVERSION_COVER, "物流监视器(转换处理器)");
            CoverComponentTiers.entries().forEach(entry ->
                    addItem(
                            ModItems.compactElectricCover(entry.itemPath()),
                            entry.family().chineseName(entry.tier())));
            MachineCoverKinds.ITEMS.forEach(entry ->
                    addItem(
                            ModItems.machineCover(entry.itemPath()),
                            entry.chinese()));
            addBlock(ModBlocks.LOGISTICS_CORE, "物流核心");
            addBlock(ModBlocks.GALVANIZED_STEEL_WALL, "镀锌钢墙");
            addBlock(ModBlocks.VENTILATION_UNIT, "通风单元");
            addBlock(ModBlocks.VERSATILE_PROCESSOR_UNIT, "通用处理器单元");
            addBlock(ModBlocks.LOGIC_PROCESSOR_UNIT, "逻辑处理器单元");
            addBlock(ModBlocks.CONTROL_PROCESSOR_UNIT, "控制处理器单元");
            addBlock(ModBlocks.STORAGE_PROCESSOR_UNIT, "仓储处理器单元");
            addBlock(ModBlocks.CONVERSION_PROCESSOR_UNIT, "转换处理器单元");
            add("tooltip.cruciblecraft.portable_fluid_tank.empty",
                    "空（容量 %s mB）");
            add("tooltip.cruciblecraft.portable_fluid_tank.contents",
                    "%s：%s/%s mB");
            addBlock(ModBlocks.ANVIL, "锻造砧");
            addBlock(ModBlocks.CERAMIC_MOLD, "陶瓷模具");
            addItem(ModItems.RAW_CERAMIC_CRUCIBLE, "黏土坩埚");
            addItem(ModItems.RAW_CERAMIC_BOWL, "黏土碗");
            addItem(ModItems.RAW_CERAMIC_FAUCET, "黏土浇铸口");
            addItem(ModItems.RAW_CERAMIC_TAP, "黏土龙头");
            addItem(ModItems.RAW_CERAMIC_FUNNEL, "黏土漏斗");
            addItem(ModItems.RAW_CERAMIC_MOLD, "黏土模具");
            CeramicMoldCatalog.SHAPED.forEach(variant -> {
                addItem(ModItems.rawShapedMold(variant.id()), variant.chineseRaw());
                addItem(ModItems.firedShapedMold(variant.id()), variant.chineseFired());
            });
            addBlock(ModBlocks.GAS_CLOUD, "气云");
            addBlock(ModBlocks.SUBSURFACE_FLUID_DEPOSIT, "地下流体矿床");
            addBlock(ModBlocks.LU_FIBER_CABLE, "LU 光纤线缆");
            addBlock(ModBlocks.LASER_ENGRAVER, "激光雕刻机");
            addBlock(ModBlocks.AUTOMATIC_HAMMER, "青铜自动锤");
            addBlock(ModBlocks.STEEL_AUTOMATIC_HAMMER, "钢自动锤");
            addBlock(ModBlocks.TITANIUM_AUTOMATIC_HAMMER, "钛自动锤");
            addBlock(ModBlocks.TUNGSTENSTEEL_AUTOMATIC_HAMMER, "钨钢自动锤");
            addBlock(ModBlocks.BOOMSTICK, "爆竹");
            addBlock(ModBlocks.DYNAMITE, "炸药");
            addBlock(ModBlocks.STRONG_DYNAMITE, "强力炸药");
            addBlock(ModBlocks.FUSION_REACTOR, "聚变反应堆");
            add("tooltip.cruciblecraft.fusion.structure", "结构：");
            add("tooltip.cruciblecraft.fusion.1", "组装说明见界面中的手册。");
            add("tooltip.cruciblecraft.fusion.2",
                    "144 个铱线圈，576 块普通钨钢墙，50 个通风单元。");
            add("tooltip.cruciblecraft.fusion.3",
                    "36 块普通不锈钢墙，53 块镀锌钢墙。");
            add("tooltip.cruciblecraft.fusion.4",
                    "3 个通用、12 个逻辑、12 个控制四核处理单元。");
            add("tooltip.cruciblecraft.fusion.5", "电能从电力接口输出");
            add("tooltip.cruciblecraft.fusion.6", "激光从「玻璃」环输入");
            add("tooltip.cruciblecraft.fusion.7", "物品和流体走普通墙壁");
            addItem(ModItems.REMOTE_ACTIVATOR, "远程激活器");
            add("tooltip.cruciblecraft.remote_activator", "潜行点击炸药绑定，普通使用触发");
            add("tooltip.cruciblecraft.remote_activator.count", "已绑定目标：%s");
            add("message.cruciblecraft.remote.full", "每个维度最多绑定 64 个目标");
            add("message.cruciblecraft.remote.invalid", "这里不是可远程激活的目标");
            add("message.cruciblecraft.remote.added", "已添加远程目标");
            add("message.cruciblecraft.remote.removed", "已移除远程目标");
            addBlock(ModBlocks.LARGE_HEAT_EXCHANGER, "大型热交换器");
            addBlock(ModBlocks.BEDROCK_DRILL, "基岩采矿钻机控制器");
            addBlock(ModBlocks.BEDROCK_DRILL_HEAD, "基岩采矿钻头");
            addBlock(ModBlocks.GT_BUSH, "浆果灌木");
            addBlock(ModBlocks.GT_SURFACE_ROCK, "地表石子");
            addBlock(ModBlocks.GT_STICK, "散落木棍");
            add("tooltip.cruciblecraft.surface_rock.material", "材质：%s");
            add("tooltip.cruciblecraft.small_ore.material", "矿物：%s");
            add("tooltip.cruciblecraft.rock.indicates", "表明存在 %s");
            add("item.cruciblecraft.rock.stone", "石子");
            add("item.cruciblecraft.rock.netherrack", "下界石子");
            add("item.cruciblecraft.rock.endstone", "末地石子");
            add("item.cruciblecraft.rock.meteorite", "陨石");
            addBlock(ModBlocks.GT_BEDROCK_ORE, "基岩矿");
            addBlock(ModBlocks.GT_SMALL_BEDROCK_ORE, "小型基岩矿");
            addBlock(ModBlocks.GT_SMALL_ORE, "小型矿石");
            addBedrockOreRemainderNames();
            addBlock(ModBlocks.REACTOR_CORE_1X1, "反应堆芯 1×1");
            add("item.cruciblecraft.reactor_core_1x1", "反应堆芯 1×1");
            addBlock(ModBlocks.REACTOR_CORE_2X2, "反应堆芯 2×2");
            add("item.cruciblecraft.reactor_core_2x2", "反应堆芯 2×2");
            addBlock(ModBlocks.TUNGSTENSTEEL_WALL, "钨钢墙");
            addBlock(ModBlocks.STAINLESS_STEEL_WALL, "不锈钢墙");
            addReactorRodNames();
            addTechnologicalPartNames();
            addGtTreeNames();
            addBlock(ModBlocks.MULTIBLOCK_CASING, "通用多方块外壳");
            addBlock(ModBlocks.MULTIBLOCK_ITEM_FLUID_PORT, "多方块物品流体端口");
            addBlock(ModBlocks.MULTIBLOCK_ENERGY_INPUT_PORT, "多方块能源输入端口");
            addBlock(ModBlocks.MULTIBLOCK_FLUID_OUT_PORT, "多方块流体输出端口");
            addBlock(ModBlocks.LARGE_CENTRIFUGE, "大型离心机");
            addBlock(ModBlocks.LARGE_MIXER, "大型搅拌器");
            add("tooltip.cruciblecraft.large_mixer.structure",
                    "结构：3×3×2 不锈钢墙");
            add("tooltip.cruciblecraft.large_mixer.controller",
                    "主机在侧底中心，朝外");
            add("tooltip.cruciblecraft.large_mixer.io",
                    "上半层输入；下半层与主机朝下输出");
            addBlock(ModBlocks.LARGE_ELECTROLYZER, "大型电解机");
            add("tooltip.cruciblecraft.large_electrolyzer.structure",
                    "结构：3×3×2 电解机零件");
            add("tooltip.cruciblecraft.large_electrolyzer.controller",
                    "主机在侧底中心，朝外");
            add("tooltip.cruciblecraft.large_electrolyzer.io",
                    "下半层输入，上半层与主机输出");
            addBlock(ModBlocks.LARGE_OVEN, "大型电炉");
            add("tooltip.cruciblecraft.large_oven.structure",
                    "结构：底层与顶层 3×3 殷钢墙");
            add("tooltip.cruciblecraft.large_oven.coils",
                    "中层空心 8 个大型镍铬或碳化硅线圈，禁止混装");
            add("tooltip.cruciblecraft.large_oven.controller",
                    "主机在侧底中心，朝外");
            add("tooltip.cruciblecraft.large_oven.io",
                    "殷钢墙物品/流体/EU；主机朝下自动输出；线圈不接受输入输出");
            addBlock(ModBlocks.LARGE_CRUSHER, "大型破碎机");
            add("tooltip.cruciblecraft.large_crusher.structure",
                    "结构：5×5×3 钨钢墙盆");
            add("tooltip.cruciblecraft.large_crusher.wheels",
                    "3×3×2 填充破碎轮");
            add("tooltip.cruciblecraft.large_crusher.controller",
                    "主机在侧底中心，朝外");
            add("tooltip.cruciblecraft.large_crusher.io",
                    "仅破碎轮输入，底层输出");
            addBlock(ModBlocks.LARGE_SHREDDER, "大型粉碎机");
            add("tooltip.cruciblecraft.large_shredder.structure",
                    "结构：5×5×3 钨钢墙盆");
            add("tooltip.cruciblecraft.large_shredder.blades",
                    "3×3×2 填充粉碎刀片");
            add("tooltip.cruciblecraft.large_shredder.controller",
                    "主机在侧底中心，朝外");
            add("tooltip.cruciblecraft.large_shredder.io",
                    "仅粉碎刀片输入，底层输出");
            addBlock(ModBlocks.LARGE_SLUICE, "大型溜槽");
            add("tooltip.cruciblecraft.large_sluice.structure",
                    "结构：3×7×3 两层钛墙与顶层溜槽部件");
            add("tooltip.cruciblecraft.large_sluice.parts",
                    "远侧顶层溜槽部件输入；运行时部件随主机方向切换");
            add("tooltip.cruciblecraft.large_sluice.controller",
                    "主机在近侧底层中心，朝外");
            add("tooltip.cruciblecraft.large_sluice.io",
                    "远侧输入物品/水；近侧底部输出物品/溜槽汁；两侧相邻 RU 输入");
            addBlock(ModBlocks.LARGE_SQUEEZER, "大型挤压机");
            add("tooltip.cruciblecraft.large_squeezer.structure",
                    "结构：5×5×3，65 个钢墙，中层 3×3 空气");
            add("tooltip.cruciblecraft.large_squeezer.controller",
                    "主机在侧面底层中心，朝外");
            add("tooltip.cruciblecraft.large_squeezer.io",
                    "顶层输入，底层输出，两侧相邻 RU 输入");
            addBlock(ModBlocks.LARGE_BATH, "大型浸洗器");
            add("tooltip.cruciblecraft.large_bath.structure",
                    "结构：5×5×2 不锈钢墙");
            add("tooltip.cruciblecraft.large_bath.controller",
                    "主机在侧底中心，朝外");
            add("tooltip.cruciblecraft.large_bath.io",
                    "任意方块输入输出");
            addBlock(ModBlocks.LARGE_COAGULATOR, "大型凝固机");
            add("tooltip.cruciblecraft.large_coagulator.structure",
                    "结构：5×5×2 不锈钢墙");
            add("tooltip.cruciblecraft.large_coagulator.controller",
                    "主机在侧底中心，朝外");
            add("tooltip.cruciblecraft.large_coagulator.io",
                    "任意方块输入输出");
            addBlock(ModBlocks.LARGE_AUTOCLAVE, "大型高压釜");
            add("tooltip.cruciblecraft.large_autoclave.structure",
                    "结构：3×3×3 空心致密不锈钢墙");
            add("tooltip.cruciblecraft.large_autoclave.controller",
                    "主机在侧底中心，朝外");
            add("tooltip.cruciblecraft.large_autoclave.io",
                    "任意方块输入输出");
            addBlock(ModBlocks.IMPLOSION_COMPRESSOR, "聚爆压缩机");
            add("tooltip.cruciblecraft.implosion_compressor.structure",
                    "结构：3×3×3 空心致密钨钢墙");
            add("tooltip.cruciblecraft.implosion_compressor.controller",
                    "主机在侧底中心，朝外");
            add("tooltip.cruciblecraft.implosion_compressor.io",
                    "墙口接受物品/流体/TU；主机底面自动输出");
            addBlock(ModBlocks.LARGE_FERMENTER, "大型发酵器");
            add("tooltip.cruciblecraft.large_fermenter.structure",
                    "底层 5×5 热传导器，上两层 5×5×2 不锈钢墙");
            add("tooltip.cruciblecraft.large_fermenter.controller",
                    "主机在侧面中心，朝外");
            add("tooltip.cruciblecraft.large_fermenter.io",
                    "墙输入；远侧上后出物品、下后出流体");
            addBlock(ModBlocks.DISTILLATION_TOWER, "蒸馏塔");
            addBlock(ModBlocks.CRYO_DISTILLATION_TOWER, "低温蒸馏塔");
            addBlock(ModBlocks.LARGE_BOILER, "大型锅炉");
            addBlock(ModBlocks.TANK_3X3X3, "3x3x3 储罐");
            addBlock(ModBlocks.LARGE_CRUCIBLE, "大型坩埚");
            add("block.cruciblecraft.large_crucible.named", "大型%s坩埚");
            addBlock(ModBlocks.BRONZE_CRUSHER, "青铜破碎机");
            addCatalogMachineNames();
            addBlock(ModBlocks.FLUID_DEPOSIT_EXTRACTOR, "流体矿床抽取机");
            addBlock(ModBlocks.ROTATIONAL_AXLE, "旋转传动轴");
            addBlock(ModBlocks.ROTATIONAL_GEARBOX, "旋转齿轮箱");
            MachineKindCatalog.kinds().forEach(kind -> {
                if ("bronze_crusher".equals(kind.id().getPath())) {
                    return;
                }
                add(
                        "emi.category.cruciblecraft." + kind.id().getPath(),
                        kind.langZh());
            });
            add("emi.category.cruciblecraft.fuels_engine", "燃油发电");
            add("emi.category.cruciblecraft.fuels_gas_turbine", "微型燃气涡轮");
            add("emi.category.cruciblecraft.fuels_gas", "燃气燃烧室");
            add("emi.category.cruciblecraft.fuels_fluidbed", "流化床燃烧室");
            add("emi.category.cruciblecraft.fuels_hot", "热交换燃料");
            add("emi.category.cruciblecraft.fusion", "聚变反应堆");
            add("emi.category.cruciblecraft.multiblock_blueprint", "多方块结构蓝图");
            add("emi.category.cruciblecraft.distillation_tower", "蒸馏塔");
            add("emi.category.cruciblecraft.cryo_distillation_tower", "低温蒸馏塔");
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
            add("emi.cruciblecraft.processing.costs", "消耗：%s %s");
            add("emi.cruciblecraft.processing.gain", "获得：%s %s");
            add("emi.cruciblecraft.processing.usage", "功率：%s %s");
            add("emi.cruciblecraft.processing.output", "产出：%s %s");
            add("emi.cruciblecraft.processing.time_ticks", "时间：%s tick");
            add("emi.cruciblecraft.processing.time_secs", "时间：%s 秒");
            add("emi.cruciblecraft.processing.time_mins", "时间：%s 分钟");
            add("emi.cruciblecraft.processing.special", "%s");
            add("emi.cruciblecraft.fusion.start", "启动：%s LU");
            add("emi.cruciblecraft.multiblock.blueprint", "结构：%s");
            add("emi.cruciblecraft.multiblock.materials", "材料清单");
            add("emi.cruciblecraft.multiblock.layer", "层 Y=%s");
            add("device.cruciblecraft.anvil", "砧");
            add("device.cruciblecraft.crucible", "坩埚");
            add("device.cruciblecraft.large_crucible", "大型坩埚");
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
            add("item.cruciblecraft.material_monkey_wrench", "%s活动扳手");
            add("item.cruciblecraft.material_wire_cutter", "%s剪线钳");
            add("item.cruciblecraft.material_knife", "%s刀");
            add("item.cruciblecraft.material_club", "%s棒");
            add("item.cruciblecraft.material_spade", "%s锹");
            add("item.cruciblecraft.material_double_axe", "%s双刃斧");
            add("item.cruciblecraft.material_sense", "%s镰刀");
            add("item.cruciblecraft.material_plow", "%s犁");
            add("item.cruciblecraft.material_construction_pick", "%s建筑镐");
            add("item.cruciblecraft.material_gem_pick", "%s宝石镐");
            add("item.cruciblecraft.material_builder_wand", "%s建筑杖");
            add("item.cruciblecraft.material_universal_spade", "%s万能锹");
            add("item.cruciblecraft.material_crowbar", "%s撬棍");
            add("item.cruciblecraft.material_plunger", "%s皮搋子");
            add("item.cruciblecraft.material_scoop", "%s捕虫网");
            add("item.cruciblecraft.material_butchery_knife", "%s屠宰刀");
            add("item.cruciblecraft.material_branch_cutter", "%s修枝剪");
            add("item.cruciblecraft.material_scissors", "%s剪刀");
            add("item.cruciblecraft.material_pincers", "%s钳子");
            add("item.cruciblecraft.material_soft_hammer", "%s软锤");
            add("item.cruciblecraft.material_bending_cylinder", "%s折弯筒");
            add("item.cruciblecraft.material_bending_cylinder_small", "%s小型折弯筒");
            add("item.cruciblecraft.material_hand_drill", "%s手钻");
            add("item.cruciblecraft.material_magnifying_glass", "%s放大镜");
            add("item.cruciblecraft.material_rolling_pin", "%s擀面杖");
            add("item.cruciblecraft.material_flint_and_tinder", "%s火绒");
            add("item.cruciblecraft.material_pocket_multitool", "%s口袋多功能工具");
            for (com.masson.cruciblecraft.content.item.tool.ElectricToolCatalog spec :
                    com.masson.cruciblecraft.content.item.tool.ElectricToolCatalog.values()) {
                add(spec.nameKey(), "%s" + spec.langZh());
            }
            add("tooltip.cruciblecraft.electric_tool.charge",
                    "EU：%s / %s  %sV");
            add("tooltip.cruciblecraft.electric_tool.sneak_switch",
                    "潜行右键切换模式");
            add("tooltip.cruciblecraft.pocket_multitool.mode.closed", "收起");
            add("tooltip.cruciblecraft.pocket_multitool.mode.knife", "刀");
            add("tooltip.cruciblecraft.pocket_multitool.mode.saw", "锯");
            add("tooltip.cruciblecraft.pocket_multitool.mode.file", "锉");
            add("tooltip.cruciblecraft.pocket_multitool.mode.screwdriver", "螺丝刀");
            add("tooltip.cruciblecraft.pocket_multitool.mode.wire_cutter", "剪线钳");
            add("tooltip.cruciblecraft.pocket_multitool.mode.scissors", "剪刀");
            add("tooltip.cruciblecraft.pocket_multitool.mode.chisel", "凿");
            add("tooltip.cruciblecraft.plunger.fluid", "从储罐抽出 1000 mB 流体");
            add("tooltip.cruciblecraft.plunger.item", "清理管道中卡住的物品");
            add("tooltip.cruciblecraft.crowbar", "撬下盖板，并整桶搬走仓储桶");
            add("tooltip.cruciblecraft.soft_hammer", "轻敲以切换红石灯与动力铁轨");
            add("message.cruciblecraft.prospect.ore", "%s！");
            add("message.cruciblecraft.prospect.lava", "这块石头后面有岩浆");
            add("message.cruciblecraft.prospect.fluid", "这块石头后面有流体");
            add("message.cruciblecraft.prospect.air", "这块石头后面有空洞");
            add("message.cruciblecraft.prospect.material_change", "这块石头后面的材料变了");
            add("message.cruciblecraft.prospect.traces", "发现了%s的痕迹");
            add("message.cruciblecraft.prospect.none", "没有发现矿石痕迹");
            add("jade.cruciblecraft.pipe_covers", "盖板参数：%s");
            add("tooltip.cruciblecraft.cover.behavior", "行为：%s");
            add("tooltip.cruciblecraft.cover.parameters",
                    "速率 %s，精确数 %s，模式 %s，压力 %s mB，选择 %s");
            add("tooltip.cruciblecraft.cover.interval",
                    "每 %s 游戏刻传输一组");
            add("tooltip.cruciblecraft.cover.retriever",
                    "从管网请求物品到盖板前方的容器。潜行右键切换反相过滤。");
            add("tooltip.cruciblecraft.cover.display_cpu",
                    "发出红石并显示物流核心状态。");
            add("screen.cruciblecraft.coke_oven.fluid", "产出流体：%s / %s mB");
            add("screen.cruciblecraft.coke_oven.invalid_structure", "结构无效");
            add("screen.cruciblecraft.coke_oven.needs_ignition", "需要点火");
            add("tooltip.cruciblecraft.coke_oven.structure", "3×3×3 耐火砖空心，中心为空气");
            add("tooltip.cruciblecraft.coke_oven.controller", "主方块在侧面正中、朝外");
            add("tooltip.cruciblecraft.coke_oven.ignite", "需要打火石或点火工具");
            add("tooltip.cruciblecraft.coke_oven.fluid_drain", "杂酚油自动滴入正下方 3×3 的储罐");
            add("tooltip.cruciblecraft.coke_oven.io", "耐火砖顶面漏斗进料、底面抽出；除顶面外可抽流体");
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
            add("itemGroup.cruciblecraft.machines", "Crucible Craft：机器");
            add("itemGroup.cruciblecraft.energy", "Crucible Craft：能量");
            add("itemGroup.cruciblecraft.covers", "Crucible Craft：盖板");
            add("itemGroup.cruciblecraft.storage", "Crucible Craft：仓储");
            add("itemGroup.cruciblecraft.building", "Crucible Craft：建筑");
            add("itemGroup.cruciblecraft.nature", "Crucible Craft：自然");
            add("itemGroup.cruciblecraft.components", "Crucible Craft：科技部件");
            add("itemGroup.cruciblecraft.cables", "Crucible Craft：导线与电缆");
            add("itemGroup.cruciblecraft.dusts", "Crucible Craft：粉");
            add("itemGroup.cruciblecraft.mechanical_parts",
                    "Crucible Craft：机械零件");
            add("itemGroup.cruciblecraft.metals_gems",
                    "Crucible Craft：金属");
            add("itemGroup.cruciblecraft.misc",
                    "Crucible Craft：杂项");
            add("itemGroup.cruciblecraft.ore_processing",
                    "Crucible Craft：矿石处理");
            add("itemGroup.cruciblecraft.ores", "Crucible Craft：矿物方块");
            add("itemGroup.cruciblecraft.raw_ores", "Crucible Craft：粗矿");
            add("itemGroup.cruciblecraft.parts", "Crucible Craft：材料零件");
            add("itemGroup.cruciblecraft.pipes", "Crucible Craft：管道");
            add("itemGroup.cruciblecraft.plates", "Crucible Craft：板");
            add("itemGroup.cruciblecraft.tools", "Crucible Craft：工具");
            add("itemGroup.cruciblecraft.tool_heads", "Crucible Craft：工具部件");
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
            add("jade.cruciblecraft.coke_oven.fluid", "产出流体：%s / %s mB");
            add("jade.cruciblecraft.coke_oven.invalid", "无效");
            add("jade.cruciblecraft.coke_oven.progress",
                    "进度：%s / %s tick");
            add("jade.cruciblecraft.coke_oven.structure", "结构：%s");
            add("jade.cruciblecraft.coke_oven.valid", "有效");
            add("jade.cruciblecraft.contents", "%s");
            add("jade.cruciblecraft.material_amount", "%s %s");
            add("jade.cruciblecraft.crusher",
                    "功率：%s KU/t，进度：%s/%s（%s）");
            add("jade.cruciblecraft.machine_material", "材质：%s（等级 %s）");
            add("jade.cruciblecraft.max_temperature", "最高温度：%s %s");
            add("jade.cruciblecraft.mold_contents", "内容物：%s× %s");
            add("jade.cruciblecraft.mold_cooling", "冷却中");
            add("jade.cruciblecraft.mold_empty", "空");
            add("jade.cruciblecraft.mold_shape", "模具：%s");
            add("jade.cruciblecraft.mold_units", "耗料：%s 单位");
            add("tooltip.cruciblecraft.mold_unshaped", "凿出形状后才能浇注");
            add("tooltip.cruciblecraft.mold_recipe", "浇注 %s（%s 单位）");
            add("item.cruciblecraft.coin", "%s 硬币");
            add("jade.cruciblecraft.mold_solid", "已凝固");
            add("jade.cruciblecraft.mold_state", "状态：%s，%s °C");
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
            add("message.cruciblecraft.coke_oven_cannot_ignite",
                    "结构必须完整");
            add("message.cruciblecraft.coke_oven_ignited", "焦炉已点燃");
            add("message.cruciblecraft.crucible_full", "坩埚已满");
            add("message.cruciblecraft.crucible_status", "温度：%s °C | %s");
            add("message.cruciblecraft.crucible_tier_too_low",
                    "该坩埚外壳无法处理该材质等级");
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
            add("message.cruciblecraft.mold_auto_input_on", "坩埚自动输入：开");
            add("message.cruciblecraft.mold_auto_input_off", "坩埚自动输入：关");
            add("message.cruciblecraft.mold_auto_input_redstone", "坩埚自动输入：红石");
            add("message.cruciblecraft.mold_auto_input_no_redstone",
                    "坩埚自动输入：不用红石");
            add("message.cruciblecraft.mold_auto_input_cleared",
                    "坩埚自动输入：关且不用红石");
            add("message.cruciblecraft.steam_engine.stopped", "蒸汽机已停机");
            add("message.cruciblecraft.steam_engine.running", "蒸汽机已启动");
            add("message.cruciblecraft.rotation_engine.stopped", "旋转引擎已停机");
            add("message.cruciblecraft.rotation_engine.running", "旋转引擎已启动");
            add("message.cruciblecraft.gas_turbine.stopped", "燃气轮机已停机");
            add("message.cruciblecraft.gas_turbine.running", "燃气轮机已启动");
            add("message.cruciblecraft.steam_turbine.stopped", "蒸汽轮机已停机");
            add("message.cruciblecraft.steam_turbine.running", "蒸汽轮机已启动");
            add("message.cruciblecraft.steam_turbine.clockwise", "顺时针");
            add("message.cruciblecraft.steam_turbine.counterclockwise", "逆时针");
            add("message.cruciblecraft.thermometer_kelvin", "温度：%s K");
            add("message.cruciblecraft.thermometer_kelvin_too_hot",
                    "温度：%s K（太烫，现在拿不起来！）");
            add("message.cruciblecraft.not_ready",
                    "成分不可浇铸或低于熔化温度");
            add("emi.category.cruciblecraft.anvil", "砧加工");
            add("emi.category.cruciblecraft.coke_oven", "焦炉");
            add("emi.category.cruciblecraft.crucible", "坩埚合金");
            add("emi.category.cruciblecraft.crusher", "破碎机");
            add("emi.category.cruciblecraft.mold_casting", "陶瓷模具铸造");
            addMachineIoTranslations();
            addHopperTranslations(true);
            addSensorTranslations();
            addRedstoneWireTranslations();
            addStorageTranslations(true);
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
                                LanguageNames.chineseFormTemplate(name)));
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
                                .ifPresent(name -> addBlock(holder, mat + name)));
            });
            ModBlocks.electricalConductorBlocks().forEach(holder -> {
                var conductor = holder.get().conductor();
                MaterialZhNames.material(conductor.materialId())
                        .ifPresent(mat ->
                                MaterialZhNames.conductor(
                                                conductor.form()
                                                        .serializedName())
                                        .ifPresent(name -> addBlock(
                                                holder, mat + name)));
            });
            ModFluids.moltenFluids().forEach(entry ->
                    MaterialZhNames.material(entry.materialId())
                            .ifPresent(name -> add(
                                    "fluid_type.cruciblecraft.molten_"
                                            + entry.materialId(),
                                    "熔融" + name)));
            ModFluids.chemicalFluids().forEach(entry -> {
                String id = entry.id();
                if ("ice".equals(id)) {
                    add("fluid_type.cruciblecraft.ice", "近冰点水");
                    return;
                }
                if ("petrotheum".equals(id)) {
                    add("fluid_type.cruciblecraft.petrotheum", "构造岩石油质");
                    return;
                }
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
            ModFluids.hotFluids().forEach(entry -> {
                add("fluid_type.cruciblecraft." + entry.id(), entry.chinese());
                add("fluid.cruciblecraft." + entry.id(), entry.chinese());
            });
            ModFluids.namedFluids().forEach(entry -> {
                add("fluid_type.cruciblecraft." + entry.id(), entry.chinese());
                add("fluid.cruciblecraft." + entry.id(), entry.chinese());
            });
            add("fluid_type.cruciblecraft.sluice_juice", "溜槽汁");
            add("fluid.cruciblecraft.sluice_juice", "溜槽汁");
            addEmiStackGroupNames();
            return;
        }
        add("itemGroup.cruciblecraft", "Crucible Craft");
        add("itemGroup.cruciblecraft.machines", "Crucible Craft: Machines");
        add("itemGroup.cruciblecraft.energy", "Crucible Craft: Energy");
        add("itemGroup.cruciblecraft.covers", "Crucible Craft: Covers");
        add("itemGroup.cruciblecraft.storage", "Crucible Craft: Storage");
        add("itemGroup.cruciblecraft.building", "Crucible Craft: Building");
        add("itemGroup.cruciblecraft.nature", "Crucible Craft: Nature");
        add("itemGroup.cruciblecraft.components", "Crucible Craft: Components");
        add("itemGroup.cruciblecraft.ores", "Crucible Craft: Ore Blocks");
        add("itemGroup.cruciblecraft.raw_ores", "Crucible Craft: Raw Ores");
        add("itemGroup.cruciblecraft.ore_processing", "Crucible Craft: Ore Processing");
        add("itemGroup.cruciblecraft.dusts", "Crucible Craft: Dusts");
        add("itemGroup.cruciblecraft.metals_gems", "Crucible Craft: Metals");
        add("itemGroup.cruciblecraft.plates", "Crucible Craft: Plates");
        add("itemGroup.cruciblecraft.parts", "Crucible Craft: Material Parts");
        add("itemGroup.cruciblecraft.mechanical_parts", "Crucible Craft: Mechanical Parts");
        add("itemGroup.cruciblecraft.wires", "Crucible Craft: Stranded Wires");
        add("itemGroup.cruciblecraft.cables", "Crucible Craft: Conductors");
        add("itemGroup.cruciblecraft.pipes", "Crucible Craft: Pipes");
        add("itemGroup.cruciblecraft.tools", "Crucible Craft: Tools");
        add("itemGroup.cruciblecraft.tool_heads", "Crucible Craft: Tool Heads");
        add("itemGroup.cruciblecraft.fluid_cells", "Crucible Craft: Fluid Cells");
        add("item.cruciblecraft.fluid_cell.filled", "%s Fluid Cell");
        add("item.cruciblecraft.gas_cell.filled", "%s Gas Cell");
        add("itemGroup.cruciblecraft.misc", "Crucible Craft: Miscellaneous");
        addBlock(ModBlocks.FIREBRICK, "Firebrick");
        addBlock(ModBlocks.ANVIL, "Smithing Anvil");
        addBlock(ModBlocks.CERAMIC_MOLD, "Ceramic Mold");
        addBlock(ModBlocks.GAS_CLOUD, "Gas Cloud");
        addBlock(ModBlocks.SUBSURFACE_FLUID_DEPOSIT, "Subsurface Fluid Deposit");
        addBlock(ModBlocks.LU_FIBER_CABLE, "LU Fiber Cable");
        addBlock(ModBlocks.LASER_ENGRAVER, "Laser Engraver");
        addBlock(ModBlocks.AUTOMATIC_HAMMER, "Bronze Automatic Hammer");
        addBlock(ModBlocks.STEEL_AUTOMATIC_HAMMER, "Steel Automatic Hammer");
        addBlock(ModBlocks.TITANIUM_AUTOMATIC_HAMMER, "Titanium Automatic Hammer");
        addBlock(ModBlocks.TUNGSTENSTEEL_AUTOMATIC_HAMMER, "Tungstensteel Automatic Hammer");
        addBlock(ModBlocks.BOOMSTICK, "Boomstick");
        addBlock(ModBlocks.DYNAMITE, "Dynamite");
        addBlock(ModBlocks.STRONG_DYNAMITE, "Strong Dynamite");
        addBlock(ModBlocks.FUSION_REACTOR, "Fusion Reactor");
        add("tooltip.cruciblecraft.fusion.structure", "Structure:");
        add("tooltip.cruciblecraft.fusion.1",
                "For Assembly Instructions read the Manual in the GUI.");
        add("tooltip.cruciblecraft.fusion.2",
                "144 Iridium Coils, 576 Regular Tungstensteel Walls, 50 Ventilation Units.");
        add("tooltip.cruciblecraft.fusion.3",
                "36 Regular Stainless Steel Walls, 53 Galvanized Steel Walls.");
        add("tooltip.cruciblecraft.fusion.4",
                "3 Versatile, 12 Logic and 12 Control Quadcore Processing Units.");
        add("tooltip.cruciblecraft.fusion.5", "Energy Output at the Electric Interfaces");
        add("tooltip.cruciblecraft.fusion.6", "Laser Input at the 'Glass' Ring");
        add("tooltip.cruciblecraft.fusion.7",
                "Items and Fluids are handeled at the normal Walls");
        addBlock(ModBlocks.LARGE_HEAT_EXCHANGER, "Large Heat Exchanger");
        addBlock(ModBlocks.BEDROCK_DRILL, "Bedrock Mining Drill Controller");
        addBlock(ModBlocks.BEDROCK_DRILL_HEAD, "Bedrock Mining Drill Head");
        addBlock(ModBlocks.GT_BUSH, "Berry Bush");
        addBlock(ModBlocks.GT_SURFACE_ROCK, "Surface Rock");
        addBlock(ModBlocks.GT_STICK, "Scattered Stick");
        add("tooltip.cruciblecraft.surface_rock.material", "Material: %s");
        add("tooltip.cruciblecraft.small_ore.material", "Material: %s");
        add("tooltip.cruciblecraft.rock.indicates", "Indicates occurrence of %s");
        add("item.cruciblecraft.rock.stone", "Rock");
        add("item.cruciblecraft.rock.netherrack", "Nether Rock");
        add("item.cruciblecraft.rock.endstone", "End Rock");
        add("item.cruciblecraft.rock.meteorite", "Meteorite");
        addBlock(ModBlocks.GT_BEDROCK_ORE, "Bedrock Ore");
        addBlock(ModBlocks.GT_SMALL_BEDROCK_ORE, "Small Bedrock Ore");
        addBlock(ModBlocks.GT_SMALL_ORE, "Small Ore");
        addBedrockOreRemainderNames();
        addBlock(ModBlocks.REACTOR_CORE_1X1, "Reactor Core 1x1");
        add("item.cruciblecraft.reactor_core_1x1", "Reactor Core 1x1");
        addBlock(ModBlocks.REACTOR_CORE_2X2, "Reactor Core 2x2");
        add("item.cruciblecraft.reactor_core_2x2", "Reactor Core 2x2");
        addBlock(ModBlocks.TUNGSTENSTEEL_WALL, "Tungstensteel Wall");
        addBlock(ModBlocks.STAINLESS_STEEL_WALL, "Stainless Steel Wall");
        addReactorRodNames();
        addTechnologicalPartNames();
        addGtTreeNames();
        addBlock(ModBlocks.COKE_OVEN, "Coke Oven Controller");
        addBlock(ModBlocks.MULTIBLOCK_CASING, "Multiblock Casing");
        addBlock(
                ModBlocks.MULTIBLOCK_ITEM_FLUID_PORT,
                "Multiblock Item/Fluid Port");
        addBlock(
                ModBlocks.MULTIBLOCK_ENERGY_INPUT_PORT,
                "Multiblock Energy Input Port");
        addBlock(
                ModBlocks.MULTIBLOCK_FLUID_OUT_PORT,
                "Multiblock Fluid Output Port");
        addBlock(ModBlocks.LARGE_CENTRIFUGE, "Large Centrifuge");
        addBlock(ModBlocks.LARGE_MIXER, "Large Batch Mixer");
        add("tooltip.cruciblecraft.large_mixer.structure",
                "3x3x2 of Stainless Steel Walls");
        add("tooltip.cruciblecraft.large_mixer.controller",
                "Main Block centered on Side-Bottom and facing outwards");
        add("tooltip.cruciblecraft.large_mixer.io",
                "Top Half accepts Input, Bottom Half and Main Block emit Output");
        addBlock(ModBlocks.LARGE_ELECTROLYZER, "Large Electrolyzer");
        add("tooltip.cruciblecraft.large_electrolyzer.structure",
                "3x3x2 of Electrolyzer Parts");
        add("tooltip.cruciblecraft.large_electrolyzer.controller",
                "Main Block centered on Side-Bottom and facing outwards");
        add("tooltip.cruciblecraft.large_electrolyzer.io",
                "Bottom Half accepts Input, Top Half and Main Block emit Output");
        addBlock(ModBlocks.LARGE_OVEN, "Large Electric Oven");
        add("tooltip.cruciblecraft.large_oven.structure",
                "3x3 of Invar Walls");
        add("tooltip.cruciblecraft.large_oven.coils",
                "3x3 Hollow of 8 Large Nichrome or Carborundum Coils (No Mixing)");
        add("tooltip.cruciblecraft.large_oven.controller",
                "Main Block centered on Side-Bottom and facing outwards");
        add("tooltip.cruciblecraft.large_oven.io",
                "Invar walls take items, fluids and EU. The main block auto-outputs downward. Coils accept neither input nor output.");
        addBlock(ModBlocks.LARGE_CRUSHER, "Large Crusher");
        add("tooltip.cruciblecraft.large_crusher.structure",
                "5x5x3 'Basin' of 56 Tungstensteel Walls");
        add("tooltip.cruciblecraft.large_crusher.wheels",
                "3x3x2 Filling with Crusher Wheels");
        add("tooltip.cruciblecraft.large_crusher.controller",
                "Main Block centered on Side-Bottom and facing outwards");
        add("tooltip.cruciblecraft.large_crusher.io",
                "Input only at Crusher Wheels, Output at Bottom Layer");
        addBlock(ModBlocks.LARGE_SHREDDER, "Large Shredder");
        add("tooltip.cruciblecraft.large_shredder.structure",
                "5x5x3 'Basin' of 56 Tungstensteel Walls");
        add("tooltip.cruciblecraft.large_shredder.blades",
                "3x3x2 Filling with Shredder Blades");
        add("tooltip.cruciblecraft.large_shredder.controller",
                "Main Block centered on Side-Bottom and facing outwards");
        add("tooltip.cruciblecraft.large_shredder.io",
                "Input only at Shredder Blades, Output at Bottom Layer");
        addBlock(ModBlocks.LARGE_SLUICE, "Large Sluice");
        add("tooltip.cruciblecraft.large_sluice.structure",
                "3x7x3: two Titanium Wall layers with a top Sluice Part layer");
        add("tooltip.cruciblecraft.large_sluice.parts",
                "Input only at the far-side top Sluice Parts; designs rotate while active");
        add("tooltip.cruciblecraft.large_sluice.controller",
                "Main Block centered on the close-side bottom and facing outwards");
        add("tooltip.cruciblecraft.large_sluice.io",
                "Far-side item/water input; close-side bottom item/Sluice Juice output; two adjacent RU inputs");
        addBlock(ModBlocks.LARGE_SQUEEZER, "Large Squeezer");
        add("tooltip.cruciblecraft.large_squeezer.structure",
                "5x5x3 hollow: 65 Steel Walls with a 3x3 air core");
        add("tooltip.cruciblecraft.large_squeezer.controller",
                "Main Block centered on the side-bottom and facing outwards");
        add("tooltip.cruciblecraft.large_squeezer.io",
                "Top-layer item/fluid input; bottom-layer output; two adjacent RU inputs");
        addBlock(ModBlocks.LARGE_BATH, "Large Bathing Vat");
        add("tooltip.cruciblecraft.large_bath.structure",
                "5x5x2 of Stainless Steel Walls");
        add("tooltip.cruciblecraft.large_bath.controller",
                "Main Block centered on Side-Bottom and facing outwards");
        add("tooltip.cruciblecraft.large_bath.io",
                "Input and Output at any Blocks");
        addBlock(ModBlocks.LARGE_COAGULATOR, "Large Coagulator Array");
        add("tooltip.cruciblecraft.large_coagulator.structure",
                "5x5x2 of Stainless Steel Walls");
        add("tooltip.cruciblecraft.large_coagulator.controller",
                "Main Block centered on Side-Bottom and facing outwards");
        add("tooltip.cruciblecraft.large_coagulator.io",
                "Input and Output at any Blocks");
        addBlock(ModBlocks.LARGE_AUTOCLAVE, "Large Autoclave");
        add("tooltip.cruciblecraft.large_autoclave.structure",
                "3x3x3 Hollow of Dense Stainless Steel Walls");
        add("tooltip.cruciblecraft.large_autoclave.controller",
                "Main Block centered on Side-Bottom and facing outwards");
        add("tooltip.cruciblecraft.large_autoclave.io",
                "Input and Output at any Blocks");
        addBlock(ModBlocks.IMPLOSION_COMPRESSOR, "Implosion Compressor");
        add("tooltip.cruciblecraft.implosion_compressor.structure",
                "3x3x3 Hollow of Dense Tungstensteel Walls");
        add("tooltip.cruciblecraft.implosion_compressor.controller",
                "Main Block centered on Side-Bottom and facing outwards");
        add("tooltip.cruciblecraft.implosion_compressor.io",
                "Walls accept items, fluids and TU; output is automatic below");
        addBlock(ModBlocks.LARGE_FERMENTER, "Large Fermenter");
        add("tooltip.cruciblecraft.large_fermenter.structure",
                "5x5 Heat Transmitters as bottom layer, 5x5x2 Stainless Steel Walls");
        add("tooltip.cruciblecraft.large_fermenter.controller",
                "Main Block centered on Side-Middle and facing outwards");
        add("tooltip.cruciblecraft.large_fermenter.io",
                "Walls accept Input, Top Back emits Items, Bottom Back emits Fluids");
        addBlock(ModBlocks.DISTILLATION_TOWER, "Distillation Tower");
        addBlock(ModBlocks.CRYO_DISTILLATION_TOWER, "Cryo Distillation Tower");
        addBlock(ModBlocks.LARGE_BOILER, "Large Boiler");
        addBlock(ModBlocks.TANK_3X3X3, "3x3x3 Tank");
        addBlock(ModBlocks.LARGE_CRUCIBLE, "Large Crucible");
        add("block.cruciblecraft.large_crucible.named", "Large %s Crucible");
        addItem(ModItems.RAW_CERAMIC_CRUCIBLE, "Clay Crucible");
        addItem(ModItems.RAW_CERAMIC_BOWL, "Clay Bowl");
        addItem(ModItems.RAW_CERAMIC_FAUCET, "Clay Faucet");
        addItem(ModItems.RAW_CERAMIC_TAP, "Clay Tap");
        addItem(ModItems.RAW_CERAMIC_FUNNEL, "Clay Funnel");
        addItem(ModItems.RAW_CERAMIC_MOLD, "Clay Mold");
        CeramicMoldCatalog.SHAPED.forEach(variant -> {
            addItem(ModItems.rawShapedMold(variant.id()), variant.englishRaw());
            addItem(ModItems.firedShapedMold(variant.id()), variant.englishFired());
        });
        addItem(ModItems.MATCH, "Match");
        addItem(ModItems.REMOTE_ACTIVATOR, "Remote Activator");
        add("tooltip.cruciblecraft.remote_activator",
                "Sneak-click a target to bind it; use normally to activate");
        add("tooltip.cruciblecraft.remote_activator.count",
                "Bound targets: %s");
        add("message.cruciblecraft.remote.full",
                "A dimension can hold at most 64 targets");
        add("message.cruciblecraft.remote.invalid",
                "This is not a remotely activatable target");
        add("message.cruciblecraft.remote.added", "Remote target added");
        add("message.cruciblecraft.remote.removed", "Remote target removed");
        addItem(ModItems.PROGRAMMED_CIRCUIT, "Programmed Circuit");
        add("tooltip.cruciblecraft.circuit_config", "Configuration: %s");
        add("tooltip.cruciblecraft.reactor_rod.empty", "Empty reactor rod");
        add("tooltip.cruciblecraft.reactor_rod.absorber", "Neutron absorber");
        add("tooltip.cruciblecraft.reactor_rod.reflector", "Neutron reflector");
        add("tooltip.cruciblecraft.reactor_rod.moderator", "Neutron moderator");
        add("tooltip.cruciblecraft.reactor_rod.nuclear", "Fission fuel");
        add("tooltip.cruciblecraft.reactor_rod.depleted", "Depleted fuel");
        add("tooltip.cruciblecraft.reactor_rod.breeder", "Breeder rod");
        add("tooltip.cruciblecraft.reactor_rod.product", "Enrichment product");
        add("tooltip.cruciblecraft.reactor_rod.durability", "Durability: %s");
        add("tooltip.cruciblecraft.reactor_rod.neutrons",
                "Neutrons other %s / self %s / max %s / div %s");
        GtWoodCatalog.DEFINITIONS.forEach(wood ->
                addBlock(ModBlocks.gtWood(wood.id()), wood.englishName()));
        GtStoneCatalog.variants().forEach(stone -> {
            var holder = ModBlocks.gtStoneBlocksById().get(stone.id());
            if (holder != null) {
                addBlock(holder, stone.englishName());
            }
        });
        com.masson.cruciblecraft.worldgen.StoneLayerStones.registeredCubes().forEach(cube -> {
            if (ModBlocks.hasLayerStone(cube.registryPath())) {
                addBlock(
                        ModBlocks.layerStone(cube.registryPath()),
                        cube.english());
            }
        });
        GtBlockObjectCatalog.variants().forEach(block ->
                addBlock(
                        ModBlocks.gtBlockObjectBlocksById().get(block.id()),
                        LanguageNames.playerEnglish(
                                block.englishName(), block.registryPath())));
        com.masson.cruciblecraft.content.item.BathRemainderBlockObjectCatalog.variants().forEach(block ->
                addBlock(
                        ModBlocks.bathRemainderBlockObjectBlocksById().get(block.id()),
                        LanguageNames.playerEnglish(
                                block.englishName(), block.registryPath())));
        com.masson.cruciblecraft.content.item.GtBuildingBlockCatalog.variants().forEach(block ->
                addBlock(
                        ModBlocks.gtBuildingBlockObjectBlocksById().get(block.id()),
                        LanguageNames.playerEnglish(
                                block.englishName(), block.registryPath())));
        add("tooltip.cruciblecraft.fireproof", "Fireproof");
        addItem(ModItems.CREOSOTE_BUCKET, "Creosote Bucket");
        add("fluid_type.cruciblecraft.creosote", "Creosote");
        addItem(ModItems.STEAM_BUCKET, "Steam Bucket");
        add("fluid_type.cruciblecraft.steam", "Steam");
        add("fluid_type.cruciblecraft.sluice_juice", "Sluice Juice");
        add("fluid.cruciblecraft.sluice_juice", "Sluice Juice");
        addItem(ModItems.PORTABLE_FLUID_TANK, "Portable Fluid Tank");
        addItem(ModItems.FLUID_CELL, "Universal Fluid Cell");
        addItem(ModItems.GAS_CELL, "Universal Gas Cell");
        addBatteryCellNames(false);
        add("tooltip.cruciblecraft.fluid_cell.empty",
                "Empty fluid cell (capacity: %s mB)");
        add("tooltip.cruciblecraft.fluid_cell.contents",
                "%s: %s/%s mB");
        add("tooltip.cruciblecraft.gas_cell.empty",
                "Empty gas cell (capacity: %s mB)");
        add("tooltip.cruciblecraft.gas_cell.contents",
                "%s: %s/%s mB");
        add("tooltip.cruciblecraft.fluid_attachment.liquid",
                "GT6 liquid attachment: right-click with a fluid container");
        add("tooltip.cruciblecraft.fluid_attachment.gas",
                "GT6 gas attachment: right-click with a fluid container");
        add("tooltip.cruciblecraft.fluid_attachment.faucet",
                "GT6 crucible faucet: click to cast from a crucible or drain downward");
        add("tooltip.cruciblecraft.fluid_attachment.acid_proof",
                "Acid proof");
        add("tooltip.cruciblecraft.fluid_attachment.magic_proof",
                "Magic-fluid proof");
        add("tooltip.cruciblecraft.portable_fluid_tank.empty",
                "Empty (capacity: %s mB)");
        add("tooltip.cruciblecraft.portable_fluid_tank.contents",
                "%s: %s/%s mB");
        addBlock(ModBlocks.ROTATIONAL_AXLE, "Rotational Axle");
        addBlock(ModBlocks.ROTATIONAL_GEARBOX, "Rotational Gearbox");
        addBlock(ModBlocks.BRONZE_CRUSHER, "Bronze Crusher");
        addCatalogMachineNames();
        addBlock(ModBlocks.FLUID_DEPOSIT_EXTRACTOR, "Fluid Deposit Extractor");
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
        add("jade.cruciblecraft.crusher", "Power: %s KU/t, Progress: %s/%s (%s)");
        add("jade.cruciblecraft.processing_machine",
                "Power: %s/t, Progress: %s/%s (%s)");
        add("jade.cruciblecraft.processing_tank",
                "Tank %s: %s, %s/%s mB");
        add("jade.cruciblecraft.cable",
                "%s / %s: %s V, %s A, loss %s EU/block, load %s A, burn %s/16");
        addJadeObservation();
        addJadePluginConfig();
        addEmptyToolHeadNames();
        add("tooltip.cruciblecraft.electrical.specification",
                "GT6 specification: %s");
        add("tooltip.cruciblecraft.electrical.rating",
                "Rating: %s V, %s A, loss %s EU/block");
        add("tooltip.cruciblecraft.electrical.insulated",
                "Insulated cable");
        add("tooltip.cruciblecraft.electrical.bare", "Bare wire");
        add("tooltip.cruciblecraft.lu_fiber_cable",
                "GT6 LU fiber: lossless transport");
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
        add("jade.cruciblecraft.fluid_spring.fluid", "Fluid: %s");
        add("jade.cruciblecraft.fluid_spring.amount", "Spring amount: %s mB");
        add("jade.cruciblecraft.item_pipe",
                "%s: actual delivery %s, clogged %s, covers %s");
        add("jade.cruciblecraft.pipe_covers", "Cover parameters: %s");
        add("tooltip.cruciblecraft.cover.behavior", "Behavior: %s");
        add("tooltip.cruciblecraft.cover.parameters",
                "Rate %s, exact %s, mode %s, pressure %s mB, selector %s");
        add("tooltip.cruciblecraft.cover.interval",
                "Transfers a stack every %s ticks");
        add("tooltip.cruciblecraft.cover.retriever",
                "Requests items from the pipe network into the inventory in front. Shift-use to invert the filter.");
        add("tooltip.cruciblecraft.cover.display_cpu",
                "Emits Redstone and Displays Status of Logistics Core.");
        add("death.attack.electricity",
                "%s was electrocuted");
        add("death.attack.crusher",
                "%s was crushed to a pulp");
        add("death.attack.shredder",
                "%s was shredded to pieces");
        addItem(ModItems.PIPE_FILTER_COVER, "Item Filter");
        addItem(ModItems.PIPE_VALVE_COVER, "Shutter Cover");
        addItem(ModItems.PIPE_PUMP_COVER, "Pipe Output Pump Cover");
        addItem(ModItems.CONVEYOR_COVER, "Conveyor Cover");
        addItem(ModItems.RETRIEVER_ITEM_COVER, "Item Retriever Cover");
        addItem(ModItems.ROBOT_ARM_COVER, "Robot Arm Cover");
        addItem(ModItems.PRESSURE_VALVE_COVER, "Pressure Valve");
        addItem(ModItems.SELECTOR_MANUAL_COVER, "Manual Selector");
        addItem(ModItems.LOGISTICS_ITEM_STORAGE_COVER, "Filtered Logistics Storage Bus (Item)");
        addItem(ModItems.LOGISTICS_ITEM_IMPORT_COVER, "Filtered Logistics Import Bus (Item)");
        addItem(ModItems.LOGISTICS_ITEM_EXPORT_COVER, "Filtered Logistics Export Bus (Item)");
        addItem(ModItems.LOGISTICS_FLUID_STORAGE_COVER, "Filtered Logistics Storage Bus (Fluid)");
        addItem(ModItems.LOGISTICS_FLUID_IMPORT_COVER, "Filtered Logistics Import Bus (Fluid)");
        addItem(ModItems.LOGISTICS_FLUID_EXPORT_COVER, "Filtered Logistics Export Bus (Fluid)");
        addItem(ModItems.LOGISTICS_GENERIC_STORAGE_COVER, "Generic Logistics Storage Bus");
        addItem(ModItems.LOGISTICS_GENERIC_IMPORT_COVER, "Generic Logistics Import Bus");
        addItem(ModItems.LOGISTICS_GENERIC_EXPORT_COVER, "Generic Logistics Export Bus");
        addItem(ModItems.LOGISTICS_GENERIC_DUMP_COVER, "Logistics Dump Bus (Item)");
        addItem(ModItems.LOGISTICS_DISPLAY_CPU_LOGIC_COVER,
                "Logistics Display (CPU Logic)");
        addItem(ModItems.LOGISTICS_DISPLAY_CPU_CONTROL_COVER,
                "Logistics Display (CPU Control)");
        addItem(ModItems.LOGISTICS_DISPLAY_CPU_STORAGE_COVER,
                "Logistics Display (CPU Storage)");
        addItem(ModItems.LOGISTICS_DISPLAY_CPU_CONVERSION_COVER,
                "Logistics Display (CPU Conversion)");
        CoverComponentTiers.entries().forEach(entry ->
                addItem(
                        ModItems.compactElectricCover(entry.itemPath()),
                        entry.family().englishName(entry.tier())));
        MachineCoverKinds.ITEMS.forEach(entry ->
                addItem(
                        ModItems.machineCover(entry.itemPath()),
                        entry.english()));
        addBlock(ModBlocks.LOGISTICS_CORE, "Logistics Core");
        addBlock(ModBlocks.GALVANIZED_STEEL_WALL, "Galvanized Steel Wall");
        addBlock(ModBlocks.VENTILATION_UNIT, "Ventilation Unit");
        addBlock(ModBlocks.VERSATILE_PROCESSOR_UNIT, "Versatile Processor Unit");
        addBlock(ModBlocks.LOGIC_PROCESSOR_UNIT, "Logic Processor Unit");
        addBlock(ModBlocks.CONTROL_PROCESSOR_UNIT, "Control Processor Unit");
        addBlock(ModBlocks.STORAGE_PROCESSOR_UNIT, "Storage Processor Unit");
        addBlock(ModBlocks.CONVERSION_PROCESSOR_UNIT, "Conversion Processor Unit");
        add("item.cruciblecraft.smithing_hammer", "%s Smithing Hammer");
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
        add("item.cruciblecraft.material_monkey_wrench", "%s Monkey Wrench");
        add("item.cruciblecraft.material_wire_cutter", "%s Wire Cutter");
        add("item.cruciblecraft.material_knife", "%s Knife");
        add("item.cruciblecraft.material_club", "%s Club");
        add("item.cruciblecraft.material_spade", "%s Spade");
        add("item.cruciblecraft.material_double_axe", "%s Double Axe");
        add("item.cruciblecraft.material_sense", "%s Scythe");
        add("item.cruciblecraft.material_plow", "%s Plow");
        add("item.cruciblecraft.material_construction_pick", "%s Construction Pick");
        add("item.cruciblecraft.material_gem_pick", "%s Gem Pick");
        add("item.cruciblecraft.material_builder_wand", "%s Builder Wand");
        add("item.cruciblecraft.material_universal_spade", "%s Universal Spade");
        add("item.cruciblecraft.material_crowbar", "%s Crowbar");
        add("item.cruciblecraft.material_plunger", "%s Plunger");
        add("item.cruciblecraft.material_scoop", "%s Scoop");
        add("item.cruciblecraft.material_butchery_knife", "%s Butchery Knife");
        add("item.cruciblecraft.material_branch_cutter", "%s Branch Cutter");
        add("item.cruciblecraft.material_scissors", "%s Scissors");
        add("item.cruciblecraft.material_pincers", "%s Pincers");
        add("item.cruciblecraft.material_soft_hammer", "%s Soft Hammer");
        add("item.cruciblecraft.material_bending_cylinder", "%s Bending Cylinder");
        add("item.cruciblecraft.material_bending_cylinder_small",
                "%s Small Bending Cylinder");
        add("item.cruciblecraft.material_hand_drill", "%s Hand Drill");
        add("item.cruciblecraft.material_magnifying_glass", "%s Magnifying Glass");
        add("item.cruciblecraft.material_rolling_pin", "%s Rolling Pin");
        add("item.cruciblecraft.material_flint_and_tinder", "%s Flint and Tinder");
        add("item.cruciblecraft.material_pocket_multitool", "%s Pocket Multitool");
        for (com.masson.cruciblecraft.content.item.tool.ElectricToolCatalog spec :
                com.masson.cruciblecraft.content.item.tool.ElectricToolCatalog.values()) {
            add(spec.nameKey(), "%s " + spec.langEn());
        }
        add("tooltip.cruciblecraft.electric_tool.charge",
                "EU: %s / %s  %sV");
        add("tooltip.cruciblecraft.electric_tool.sneak_switch",
                "Sneak-use to switch mode");
        add("tooltip.cruciblecraft.pocket_multitool.mode.closed", "Closed");
        add("tooltip.cruciblecraft.pocket_multitool.mode.knife", "Knife");
        add("tooltip.cruciblecraft.pocket_multitool.mode.saw", "Saw");
        add("tooltip.cruciblecraft.pocket_multitool.mode.file", "File");
        add("tooltip.cruciblecraft.pocket_multitool.mode.screwdriver", "Screwdriver");
        add("tooltip.cruciblecraft.pocket_multitool.mode.wire_cutter", "Wire Cutter");
        add("tooltip.cruciblecraft.pocket_multitool.mode.scissors", "Scissors");
        add("tooltip.cruciblecraft.pocket_multitool.mode.chisel", "Chisel");
        add("tooltip.cruciblecraft.plunger.fluid",
                "Clears 1000 mB of fluid from tanks");
        add("tooltip.cruciblecraft.plunger.item", "Clears items from pipes");
        add("tooltip.cruciblecraft.crowbar",
                "Pries off covers and picks up storage barrels");
        add("tooltip.cruciblecraft.soft_hammer",
                "Toggles redstone lamps and powered rails");
        add("message.cruciblecraft.prospect.ore", "%s!");
        add("message.cruciblecraft.prospect.lava", "There is Lava behind this Rock");
        add("message.cruciblecraft.prospect.fluid", "There is a Fluid behind this Rock");
        add("message.cruciblecraft.prospect.air",
                "There is an Air Pocket behind this Rock");
        add("message.cruciblecraft.prospect.material_change",
                "Material is changing behind this Rock");
        add("message.cruciblecraft.prospect.traces", "Found traces of %s");
        add("message.cruciblecraft.prospect.none", "No traces of Ore found");
        addItem(ModItems.UNKNOWN_MATERIAL, "Unknown Material");
        ExtruderShapeCatalog.DEFINITIONS.forEach(shape ->
                addItem(ModItems.extruderShape(shape.id()), shape.englishName()));
        ToolPatternCatalog.DEFINITIONS.forEach(pattern ->
                addItem(ModItems.toolPattern(pattern.id()), pattern.englishName()));
        ModBlocks.oreBlockPaths().keySet().forEach(key -> addBlock(
                ModBlocks.oreBlock(key.materialId(), key.host()),
                (key.host() == Host.DEEPSLATE ? "Deepslate " : "")
                        + LanguageNames.formatEnglishId(key.materialId()) + " Ore"));
        ModBlocks.electricalConductorBlocks().forEach(holder -> {
            var conductor = holder.get().conductor();
            addBlock(
                    holder,
                    LanguageNames.composeEnglish(
                            conductor.materialId(),
                            conductor.form().serializedName()));
        });
        ModBlocks.pipeBlocks().forEach(holder -> {
            var pipe = holder.get().pipe();
            addBlock(
                    holder,
                    LanguageNames.composeEnglish(
                            pipe.materialId(),
                            pipe.form().serializedName()));
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
                    LanguageNames.englishFormTemplate(
                            LanguageNames.formatEnglishId(form.serializedName())));
        }
        addEmiStackGroupNames();
        MaterialCatalog.startupValues().forEach(material ->
                add(material.translationKey(), title(material.id())));
        ModFluids.moltenFluids().forEach(entry ->
                add(
                        "fluid_type.cruciblecraft.molten_" + entry.materialId(),
                        "Molten " + title(entry.materialId())));
        ModFluids.chemicalFluids().forEach(entry ->
                add(
                        "fluid_type.cruciblecraft." + entry.id(),
                        switch (entry.id()) {
                            case "ice" -> "Near Frozen Water";
                            case "petrotheum" -> "Tectonic Petrotheum";
                            default -> title(entry.id());
                        }));
        ModFluids.hotFluids().forEach(entry -> {
            add("fluid_type.cruciblecraft." + entry.id(), entry.english());
            add("fluid.cruciblecraft." + entry.id(), entry.english());
        });
        ModFluids.namedFluids().forEach(entry -> {
            add("fluid_type.cruciblecraft." + entry.id(), entry.english());
            add("fluid.cruciblecraft." + entry.id(), entry.english());
        });
        if (!MaterialCatalog.contains("stone")) {
            add("material.cruciblecraft.stone", "Stone");
        }
        ModProcessingMachines.CONFIGURED_MACHINES.forEach(spec ->
                add(
                        "emi.category.cruciblecraft." + spec.id().getPath(),
                        title(spec.id().getPath())));
        add("emi.category.cruciblecraft.fuels_engine", "Fuel Engine");
        add("emi.category.cruciblecraft.fuels_gas_turbine", "Small Gas Turbine");
        add("emi.category.cruciblecraft.fuels_gas", "Gas Burning Box");
        add("emi.category.cruciblecraft.fuels_fluidbed", "Fluid-Bed Burning Box");
        add("emi.category.cruciblecraft.fuels_hot", "Heat Exchanger Fuel");
        add("emi.category.cruciblecraft.fusion", "Fusion Reactor");
        add("emi.category.cruciblecraft.multiblock_blueprint",
                "Multiblock Structure Blueprint");
        add("emi.category.cruciblecraft.distillation_tower", "Distillation Tower");
        add("emi.category.cruciblecraft.cryo_distillation_tower",
                "Cryo Distillation Tower");
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
        add("emi.cruciblecraft.processing.costs", "Costs: %s %s");
        add("emi.cruciblecraft.processing.gain", "Gain: %s %s");
        add("emi.cruciblecraft.processing.usage", "Usage: %s %s");
        add("emi.cruciblecraft.processing.output", "Output: %s %s");
        add("emi.cruciblecraft.processing.time_ticks", "Time: %s ticks");
        add("emi.cruciblecraft.processing.time_secs", "Time: %s secs");
        add("emi.cruciblecraft.processing.time_mins", "Time: %s mins");
        add("emi.cruciblecraft.processing.special", "%s");
        add("emi.cruciblecraft.fusion.start", "Start: %s LU");
        add("emi.cruciblecraft.multiblock.blueprint", "Structure: %s");
        add("emi.cruciblecraft.multiblock.materials", "Materials");
        add("emi.cruciblecraft.multiblock.layer", "Layer Y=%s");

        add("message.cruciblecraft.air_injection_started", "Airflow started; decarburization is underway");
        add("message.cruciblecraft.air_injection_continued", "Airflow duration extended");
        add("message.cruciblecraft.air_injection_too_cold", "The iron charge must be molten before blowing air");
        add("message.cruciblecraft.invalid_steel_charge", "Steelmaking requires exactly three parts iron to one part carbon");
        add("message.cruciblecraft.coke_oven_ignited", "Coke oven ignited");
        add("message.cruciblecraft.coke_oven_cannot_ignite", "The structure must be complete");
        add("container.cruciblecraft.coke_oven", "Coke Oven");
        add("screen.cruciblecraft.coke_oven.invalid_structure", "Invalid structure");
        add("screen.cruciblecraft.coke_oven.needs_ignition", "Needs ignition");
        add("screen.cruciblecraft.coke_oven.fluid", "Output fluid: %s / %s mB");
        add("tooltip.cruciblecraft.coke_oven.structure", "3x3x3 hollow of Fire Bricks filled with Air");
        add("tooltip.cruciblecraft.coke_oven.controller", "Main Block centered on Side and facing outwards");
        add("tooltip.cruciblecraft.coke_oven.ignite", "Requires Flint and Steel or an igniter tool");
        add("tooltip.cruciblecraft.coke_oven.fluid_drain", "Creosote auto-outputs into tanks under the oven");
        add("tooltip.cruciblecraft.coke_oven.io", "Hoppers insert on firebrick tops and extract from bottoms; fluids except the top face");
        add("jade.cruciblecraft.coke_oven.structure", "Structure: %s");
        add("jade.cruciblecraft.coke_oven.valid", "Valid");
        add("jade.cruciblecraft.coke_oven.invalid", "Invalid");
        add("jade.cruciblecraft.coke_oven.progress", "Progress: %s / %s ticks");
        add("jade.cruciblecraft.coke_oven.fluid", "Output fluid: %s / %s mB");
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
        add("message.cruciblecraft.mold_auto_input_on", "Crucible Auto-Input: ON");
        add("message.cruciblecraft.mold_auto_input_off", "Crucible Auto-Input: OFF");
        add("message.cruciblecraft.mold_auto_input_redstone", "Crucible Auto-Input: REDSTONE");
        add("message.cruciblecraft.mold_auto_input_no_redstone", "Crucible Auto-Input: NO REDSTONE");
        add("message.cruciblecraft.mold_auto_input_cleared", "Crucible Auto-Input: OFF & NO REDSTONE");
        add("message.cruciblecraft.steam_engine.stopped", "Steam engine stopped");
        add("message.cruciblecraft.steam_engine.running", "Steam engine running");
        add("message.cruciblecraft.rotation_engine.stopped", "Rotation engine stopped");
        add("message.cruciblecraft.rotation_engine.running", "Rotation engine running");
        add("message.cruciblecraft.gas_turbine.stopped", "Gas turbine stopped");
        add("message.cruciblecraft.gas_turbine.running", "Gas turbine running");
        add("message.cruciblecraft.steam_turbine.stopped", "Steam turbine stopped");
        add("message.cruciblecraft.steam_turbine.running", "Steam turbine running");
        add("message.cruciblecraft.steam_turbine.clockwise", "Clockwise");
        add("message.cruciblecraft.steam_turbine.counterclockwise", "Counterclockwise");
        add("message.cruciblecraft.thermometer_kelvin", "Temperature: %s K");
        add(
                "message.cruciblecraft.thermometer_kelvin_too_hot",
                "Temperature: %s K (too hot to pick it up right now!)");
        add(
                "message.cruciblecraft.crucible_status",
                "Temperature: %s °C | %s");
        add("tooltip.cruciblecraft.temperature", "Temperature: %s %s");
        add("jade.cruciblecraft.temperature", "Temperature: %s %s");
        add("jade.cruciblecraft.casing", "Casing: %s (tier %s, process tier %s)");
        add("jade.cruciblecraft.machine_material", "Material: %s (tier %s)");
        add("jade.cruciblecraft.material_quarantined",
                "%s material quarantined: %s");
        add("jade.cruciblecraft.max_temperature", "Maximum temperature: %s %s");
        add("jade.cruciblecraft.contents", "%s");
        add("jade.cruciblecraft.material_amount", "%s %s");
        add("jade.cruciblecraft.anvil_workpiece", "Workpiece: %s");
        add("jade.cruciblecraft.anvil_slot", "Slot %s: %sx %s");
        add("jade.cruciblecraft.anvil_durability", "Durability: %s / %s");
        add("jade.cruciblecraft.anvil_progress", "Hammer strikes: %s");
        add("jade.cruciblecraft.mold_shape", "Mold: %s");
        add("jade.cruciblecraft.mold_units", "Requires %s units");
        add("tooltip.cruciblecraft.mold_unshaped", "Chisel a shape before filling");
        add("tooltip.cruciblecraft.mold_recipe", "Casts %s (%s units)");
        add("item.cruciblecraft.coin", "%s Coin");
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
        add("device.cruciblecraft.large_crucible", "large crucible");
        add("device.cruciblecraft.hammer", "hammer");

        add("cruciblecraft.configuration.title", "Crucible Craft");
        add("cruciblecraft.configuration.section.cruciblecraft.client.toml", "Client");
        add("cruciblecraft.configuration.section.cruciblecraft.client.toml.title", "Client");
        add("cruciblecraft.configuration.temperatureUnit", "Temperature Unit");
        addMachineIoTranslations();
        addHopperTranslations(false);
        addSensorTranslations();
        addRedstoneWireTranslations();
        addStorageTranslations(false);
    }

    private void addStorageTranslations(boolean chinese) {
        StorageVariantCatalog.variants().forEach(variant -> {
            String key = LanguageNames.translationKey("block", variant.path());
            if (!chinese) {
                add(key, LanguageNames.playerEnglish(variant.english(), variant.path()));
                return;
            }
            LanguageNames.playerChinese(variant.chinese(), variant.path())
                    .or(() -> LanguageNames.chineseOrEmpty(
                            variant.chinese(), variant.english()))
                    .ifPresent(name -> add(key, name));
        });
        MteInPlaceCatalog.specs().forEach(this::addMteInPlaceStorageName);
        if (chinese) {
            add("container.cruciblecraft.bookshelf", "书架");
            add("container.cruciblecraft.bottle_crate", "瓶箱");
            add("container.cruciblecraft.drawer", "分区抽屉");
        } else {
            add("container.cruciblecraft.bookshelf", "Bookshelf");
            add("container.cruciblecraft.bottle_crate", "Bottle Crate");
            add("container.cruciblecraft.drawer", "Compartment Drawer");
        }
    }

    private void addMteInPlaceStorageName(MteInPlaceSpec spec) {
        String blockKey = LanguageNames.translationKey("block", spec.registryPath());
        String itemKey = LanguageNames.translationKey("item", spec.registryPath());
        if (!chinese) {
            String english = MteInPlaceDisplayNames.english(
                    spec.englishName(), spec.registryPath());
            add(blockKey, english);
            add(itemKey, english);
            return;
        }
        String zh = mteInPlaceChinese(spec);
        if (zh == null) {
            return;
        }
        add(blockKey, zh);
        add(itemKey, zh);
    }

    private static String mteInPlaceChinese(MteInPlaceSpec spec) {
        if (LanguageNames.hasCjk(spec.chineseName())) {
            return spec.chineseName();
        }
        String composed = MteInPlaceDisplayNames.chinese(spec.registryPath()).orElse(null);
        if (composed != null) {
            return composed;
        }
        if (spec.kind() == MteInPlaceKind.WOOD_PANEL) {
            return "木制覆盖板 #" + spec.meta();
        }
        String kindZh = switch (spec.kind()) {
            case CHEST -> "箱子";
            case SAFE -> spec.registryPath().contains("key_locked")
                    ? "钥匙保险箱"
                    : "机械保险箱";
            case CRAFTING_TABLE -> "工作台";
            case SCAFFOLD -> "脚手架";
            case BARREL -> "物品桶";
            case BOOKSHELF -> "书架";
            case BOTTLE_CRATE -> "瓶箱";
            case DRAWER -> "分区抽屉";
            case LOCKER -> "储物柜";
            case MASS_STORAGE -> "大容量仓储";
            case LARGE_BOILER -> "大型锅炉主气压表";
            default -> null;
        };
        if (kindZh == null) {
            return null;
        }
        return MteInPlaceMaterials.chineseMaterial(spec.registryPath())
                .map(material -> material + kindZh)
                .orElse(null);
    }

    private void addMachineIoTranslations() {
        add(
                "tooltip.cruciblecraft.fluid_bed.molten_calcite",
                chinese
                        ? "燃料：粉尘 + 熔融方解石（方解石粉进冶炼炉，用流体单元灌入）"
                        : "Fuel: dust + molten calcite (smelt calcite dust, fill a fluid cell)");
        add(
                "tooltip.cruciblecraft.burning_box.ignite",
                chinese
                        ? "正面打火石点火；燃烧中不能装卸燃料"
                        : "Ignite the front with flint and steel; fuel cannot be swapped while burning");
        add(
                "tooltip.cruciblecraft.burning_box.gas_only",
                chinese
                        ? "只烧气体燃料（甲烷、天然气等）"
                        : "Burns gases only (methane, natural gas, ...)");
        add(
                "tooltip.cruciblecraft.burning_box.liquid_only",
                chinese
                        ? "只烧液体燃料（柴油、煤油、汽油等）"
                        : "Burns liquids only (diesel, kerosene, petrol, ...)");
        add(
                "tooltip.cruciblecraft.machine.items_in",
                chinese ? "物品输入" : "Items IN");
        add(
                "tooltip.cruciblecraft.machine.items_out",
                chinese ? "物品输出" : "Items OUT");
        add(
                "tooltip.cruciblecraft.machine.fluids_in",
                chinese ? "流体输入" : "Fluids IN");
        add(
                "tooltip.cruciblecraft.machine.fluids_out",
                chinese ? "流体输出" : "Fluids OUT");
        add(
                "tooltip.cruciblecraft.machine.energy_in",
                chinese ? "能量输入" : "Energy IN");
        add("tooltip.cruciblecraft.machine.auto", chinese ? "(自动)" : "(auto)");
        add(
                "tooltip.cruciblecraft.machine.auto_otherwise_any",
                chinese ? "(自动，否则任意面)" : "(auto, otherwise any)");
        add(
                "tooltip.cruciblecraft.machine.no_auto",
                chinese ? "(无自动)" : "(no auto)");
        add(
                "tooltip.cruciblecraft.machine.face.any",
                chinese ? "任意面" : "Any Side");
        add(
                "tooltip.cruciblecraft.machine.face.bottom",
                chinese ? "底面" : "Bottom");
        add("tooltip.cruciblecraft.machine.face.top", chinese ? "顶面" : "Top");
        add("tooltip.cruciblecraft.machine.face.left", chinese ? "左面" : "Left");
        add(
                "tooltip.cruciblecraft.machine.face.front",
                chinese ? "正面" : "Front");
        add(
                "tooltip.cruciblecraft.machine.face.right",
                chinese ? "右面" : "Right");
        add("tooltip.cruciblecraft.machine.face.back", chinese ? "背面" : "Back");
        add(
                "tooltip.cruciblecraft.machine.screwdriver",
                chinese ? "用螺丝刀切换模式" : "Use Screwdriver to toggle Modes");
        add(
                "tooltip.cruciblecraft.machine.monkey_wrench.auto_in",
                chinese
                        ? "用活动扳手切换自动输入"
                        : "Use Monkey Wrench to toggle automatic Inputs");
        add(
                "tooltip.cruciblecraft.machine.monkey_wrench.auto_out",
                chinese
                        ? "用活动扳手切换自动输出"
                        : "Use Monkey Wrench to toggle automatic Outputs");
        add(
                "message.cruciblecraft.machine.mode.output_empty",
                chinese
                        ? "仅在输出完全空时生产"
                        : "Only produce when Output is completely empty");
        add(
                "message.cruciblecraft.machine.mode.output_space",
                chinese
                        ? "只要有空位就生产"
                        : "Produce whenever there is space");
        add(
                "message.cruciblecraft.machine.mode.input_empty",
                chinese
                        ? "仅向空的输入槽接受输入"
                        : "Only accept Input on empty Input Slots");
        add(
                "message.cruciblecraft.machine.mode.input_all",
                chinese
                        ? "向所有输入槽接受输入"
                        : "Accept Input on all Input Slots");
        add(
                "message.cruciblecraft.machine.auto.item_in.disabled",
                chinese ? "自动物品输入：关" : "Auto Item Input Disabled");
        add(
                "message.cruciblecraft.machine.auto.item_in.enabled",
                chinese ? "自动物品输入：开" : "Auto Item Input Enabled");
        add(
                "message.cruciblecraft.machine.auto.item_out.disabled",
                chinese ? "自动物品输出：关" : "Auto Item Output Disabled");
        add(
                "message.cruciblecraft.machine.auto.item_out.enabled",
                chinese ? "自动物品输出：开" : "Auto Item Output Enabled");
        add(
                "message.cruciblecraft.machine.auto.fluid_in.disabled",
                chinese ? "自动流体输入：关" : "Auto Fluid Input Disabled");
        add(
                "message.cruciblecraft.machine.auto.fluid_in.enabled",
                chinese ? "自动流体输入：开" : "Auto Fluid Input Enabled");
        add(
                "message.cruciblecraft.machine.auto.fluid_out.disabled",
                chinese ? "自动流体输出：关" : "Auto Fluid Output Disabled");
        add(
                "message.cruciblecraft.machine.auto.fluid_out.enabled",
                chinese ? "自动流体输出：开" : "Auto Fluid Output Enabled");
        add(
                "message.cruciblecraft.inspect.no_fluids",
                chinese ? "没有流体" : "Contains no Fluids");
        add(
                "message.cruciblecraft.inspect.no_items",
                chinese ? "没有物品" : "Contains no Items");
        add(
                "message.cruciblecraft.inspect.fluid",
                chinese ? "流体：%s %s mB" : "Fluid: %s %s mB");
        add(
                "message.cruciblecraft.inspect.input_fluid",
                chinese ? "输入：%s %s mB" : "Input: %s %s mB");
        add(
                "message.cruciblecraft.inspect.output_fluid",
                chinese ? "输出：%s %s mB" : "Output: %s %s mB");
        add(
                "message.cruciblecraft.inspect.item",
                chinese ? "物品：%s × %s" : "Item: %s x %s");
        add(
                "message.cruciblecraft.inspect.stored",
                chinese ? "库存：%s / %s" : "Stored: %s / %s");
        add(
                "message.cruciblecraft.inspect.filter",
                chinese ? "过滤：%s" : "Filter: %s");
        add(
                "message.cruciblecraft.inspect.cover",
                chinese ? "盖板：%s" : "Cover: %s");
        add(
                "message.cruciblecraft.inspect.cover_filter",
                chinese ? "盖板：%s 匹配 %s" : "Cover: %s matching %s");
        add(
                "message.cruciblecraft.inspect.cover_filter_inverted",
                chinese ? "盖板：%s 排除 %s" : "Cover: %s excluding %s");
        add(
                "message.cruciblecraft.inspect.berry",
                chinese ? "浆果：%s" : "Berry: %s");
        add(
                "message.cruciblecraft.inspect.stage",
                chinese ? "生长阶段：%s" : "Stage: %s");
    }

    private void addHopperTranslations(boolean chinese) {
        HopperVariantCatalog.variants().forEach(variant -> {
            String key = LanguageNames.translationKey(
                    "block", variant.id().getPath());
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
            addBlock(ModBlocks.MIXING_BOWL, "陶瓷搅拌碗");
            add("container.cruciblecraft.hopper", "料斗");
            add("container.cruciblecraft.queue_hopper", "队列料斗");
            add("tooltip.cruciblecraft.hopper.slots", "槽位：%s");
            add("tooltip.cruciblecraft.hopper.fifo", "先进先出");
            add("message.cruciblecraft.hopper.queue_slot_size", "槽位上限：%s");
            add("message.cruciblecraft.hopper.mode_stack", "每次最多输出一组");
            add("message.cruciblecraft.hopper.mode_any", "每次最多输出 64 个");
            add("message.cruciblecraft.hopper.mode_exact", "精确输出数量：%s");
            add("message.cruciblecraft.hopper.mode_divisible", "整除输出数量：%s");
            add("message.cruciblecraft.hopper.queue_no_exact", "队列料斗没有精确模式");
            add("message.cruciblecraft.dust_funnel.mode", "按 %s 尺寸输出");
            add("message.cruciblecraft.mass_storage.auto_output_on", "会向下填充库存");
            add("message.cruciblecraft.mass_storage.auto_output_off", "不会向下填充库存");
            add("message.cruciblecraft.mass_storage.filter_reset", "空时重置过滤");
            add("message.cruciblecraft.mass_storage.filter_stay", "空时保留过滤");
            add("message.cruciblecraft.mass_storage.overflow_on", "会向下排出溢出");
            add("message.cruciblecraft.mass_storage.overflow_off", "不会向下排出溢出");
        } else {
            addBlock(ModBlocks.STEEL_DUST_FUNNEL, "Steel Dust Funnel");
            addBlock(ModBlocks.MIXING_BOWL, "Ceramic Mixing Bowl");
            add("container.cruciblecraft.hopper", "Hopper");
            add("container.cruciblecraft.queue_hopper", "Queue Hopper");
            add("tooltip.cruciblecraft.hopper.slots", "Slots: %s");
            add("tooltip.cruciblecraft.hopper.fifo", "First in, first out");
            add("message.cruciblecraft.hopper.queue_slot_size", "Max Stacksize: %s");
            add("message.cruciblecraft.hopper.mode_stack", "Emits up to 1 Stack");
            add("message.cruciblecraft.hopper.mode_any", "Emits up to 64 Items");
            add("message.cruciblecraft.hopper.mode_exact", "Emits exact Stacksize of: %s");
            add("message.cruciblecraft.hopper.mode_divisible", "Emits divisible Stacksize of: %s");
            add("message.cruciblecraft.hopper.queue_no_exact", "Queue hoppers have no exact mode");
            add("message.cruciblecraft.dust_funnel.mode", "Outputs in the Size of %s");
            add("message.cruciblecraft.mass_storage.auto_output_on", "Will fill inventories below");
            add("message.cruciblecraft.mass_storage.auto_output_off", "Won't fill inventories below");
            add("message.cruciblecraft.mass_storage.filter_reset", "Filter resets when empty");
            add("message.cruciblecraft.mass_storage.filter_stay", "Filter stays when empty");
            add("message.cruciblecraft.mass_storage.overflow_on", "Will emit overflow to inventories below");
            add("message.cruciblecraft.mass_storage.overflow_off", "Won't emit overflow");
        }
    }

    private void addSensorTranslations() {
        for (SensorKind kind : SensorKind.all()) {
            String name = chinese ? kind.langZh() : kind.langEn();
            add(LanguageNames.translationKey("block", kind.path()), name);
            add(LanguageNames.translationKey("item", kind.path()), name);
        }
        add(
                "message.cruciblecraft.sensor.mode.display",
                chinese ? "显示" : "Display");
        add(
                "message.cruciblecraft.sensor.mode.percent",
                chinese ? "百分比" : "Percent");
        add(
                "message.cruciblecraft.sensor.mode.greater",
                chinese ? "大于" : "Greater");
        add(
                "message.cruciblecraft.sensor.mode.equal",
                chinese ? "等于" : "Equal");
        add(
                "message.cruciblecraft.sensor.mode.smaller",
                chinese ? "小于" : "Smaller");
        add(
                "message.cruciblecraft.sensor.mode.scale",
                chinese ? "比例" : "Scale");
        add(
                "message.cruciblecraft.sensor.mode.full",
                chinese ? "已满" : "Full");
        add(
                "message.cruciblecraft.sensor.mode.not_full",
                chinese ? "未满" : "Not full");
    }

    private void addRedstoneWireTranslations() {
        for (RedstoneWireKind kind : RedstoneWireKind.catalog()) {
            String name = chinese ? kind.langZh() : kind.langEn();
            add(LanguageNames.translationKey("block", kind.path()), name);
            add(LanguageNames.translationKey("item", kind.path()), name);
        }
        add(
                "tooltip.cruciblecraft.redstone_wire.range",
                chinese ? "范围：%s" : "Range: %s");
        add(
                "tooltip.cruciblecraft.redstone_wire.bandwidth",
                chinese ? "带宽：%s" : "Bandwidth: %s");
    }

    private void addJadePluginConfig() {
        add("config.jade.plugin_cruciblecraft",
                chinese ? "坩埚工艺" : "CrucibleCraft");
        add("config.jade.plugin_cruciblecraft.anvil",
                chinese ? "锻造砧" : "Anvil");
        add("config.jade.plugin_cruciblecraft.bronze_boiler",
                chinese ? "青铜锅炉" : "Bronze Boiler");
        add("config.jade.plugin_cruciblecraft.bronze_crusher",
                chinese ? "青铜破碎机" : "Bronze Crusher");
        add("config.jade.plugin_cruciblecraft.bronze_steam_engine",
                chinese ? "青铜蒸汽机" : "Bronze Steam Engine");
        add("config.jade.plugin_cruciblecraft.cable",
                chinese ? "电缆" : "Cable");
        add("config.jade.plugin_cruciblecraft.ceramic_mold",
                chinese ? "陶瓷模具" : "Ceramic Mold");
        add("config.jade.plugin_cruciblecraft.coke_oven",
                chinese ? "焦炉" : "Coke Oven");
        add("config.jade.plugin_cruciblecraft.crucible",
                chinese ? "坩埚" : "Crucible");
        add("config.jade.plugin_cruciblecraft.fluid_pipe",
                chinese ? "流体管道" : "Fluid Pipe");
        add("config.jade.plugin_cruciblecraft.item_pipe",
                chinese ? "物品管道" : "Item Pipe");
        add("config.jade.plugin_cruciblecraft.processing_machine",
                chinese ? "加工机" : "Processing Machine");
        add("config.jade.plugin_cruciblecraft.transformer",
                chinese ? "变压器" : "Transformer");
        add("config.jade.plugin_cruciblecraft.reactor_core",
                chinese ? "裂变堆芯" : "Reactor Core");
        add("config.jade.plugin_cruciblecraft.battery",
                chinese ? "电池" : "Battery");
        add("config.jade.plugin_cruciblecraft.converter_dynamo",
                chinese ? "转换机" : "Converter");
        add("config.jade.plugin_cruciblecraft.surface_rock",
                chinese ? "地表石子" : "Surface Rock");
        add("config.jade.plugin_cruciblecraft.rock_block",
                chinese ? "石子" : "Rock");
        add("config.jade.plugin_cruciblecraft.small_ore",
                chinese ? "小矿石" : "Small Ore");
        add("config.jade.plugin_cruciblecraft.fluid_spring",
                chinese ? "流体泉" : "Fluid Spring");
        add("config.jade.plugin_cruciblecraft.steam_engine_source",
                chinese ? "蒸汽机源" : "Steam Engine Source");
        add("config.jade.plugin_cruciblecraft.large_boiler",
                chinese ? "大型锅炉" : "Large Boiler");
    }

    private void addJadeObservation() {
        add("jade.cruciblecraft.unavailable",
                chinese ? "不可用" : "unavailable");
        add("jade.cruciblecraft.source.state",
                chinese ? "状态：%s" : "State: %s");
        add("jade.cruciblecraft.source.state.active",
                chinese ? "运行中" : "active");
        add("jade.cruciblecraft.source.state.passive",
                chinese ? "被动运行" : "passive");
        add("jade.cruciblecraft.source.state.ready",
                chinese ? "待机" : "ready");
        add("jade.cruciblecraft.source.state.stopped",
                chinese ? "已停止" : "stopped");
        add("jade.cruciblecraft.source.state.stopped_force",
                chinese ? "强制停止" : "forced stop");
        add("jade.cruciblecraft.source.state.power_saving",
                chinese ? "节能" : "power saving");
        add("jade.cruciblecraft.source.state.unavailable",
                chinese ? "不可用" : "unavailable");
        add("jade.cruciblecraft.source.energy_io_range",
                chinese
                        ? "输入：%s %s/A；输出：%s %s/A"
                        : "Input: %s %s/A; Output: %s %s/A");
        add("jade.cruciblecraft.source.energy_io_recommended",
                chinese
                        ? "推荐输入：%s %s/A；推荐输出：%s %s/A"
                        : "Recommended input: %s %s/A; recommended output: %s %s/A");
        add("jade.cruciblecraft.source.energy_input_range",
                chinese ? "能量输入：%s %s/A" : "Energy input: %s %s/A");
        add("jade.cruciblecraft.source.energy_amount",
                chinese ? "%s：%s %s" : "%s: %s %s");
        add("jade.cruciblecraft.source.energy_contained",
                chinese ? "能量储存" : "Energy Contained");
        add("jade.cruciblecraft.source.energy_output",
                chinese ? "能量输出：%s %s/t" : "Energy output: %s %s/t");
        add("jade.cruciblecraft.source.tank",
                chinese ? "罐 %s：%s/%s %s %s" : "Tank %s: %s/%s %s %s");
        add("jade.cruciblecraft.source.fluid_output",
                chinese ? "流体输出 %s：%s %s %s"
                        : "Fluid output %s: %s %s %s");
        add("jade.cruciblecraft.source.efficiency",
                chinese ? "效率：%s%s" : "Efficiency: %s%s");
        add("jade.cruciblecraft.source.contents",
                chinese ? "内容物：%s %s" : "Contents: %s %s");
        add("jade.cruciblecraft.source.temperature",
                chinese ? "温度：%s%s / %s%s"
                        : "Temperature: %s%s / %s%s");
        add("jade.cruciblecraft.source.weight",
                chinese ? "重量：%s %s" : "Weight: %s %s");
        add("jade.cruciblecraft.source.producing",
                chinese ? "产出：%s" : "Producing: %s");
        add("jade.cruciblecraft.source.rod",
                chinese ? "%s %s %s，剩余 %s %s%s"
                        : "%s %s %s, remaining %s %s%s");
        add("jade.cruciblecraft.temperature_k",
                chinese ? "温度：%s K" : "Temperature: %s K");
        add("jade.cruciblecraft.buffered_heat",
                chinese ? "缓冲热量：%s HU" : "Buffered heat: %s HU");
        add("jade.cruciblecraft.meltdown_at",
                chinese ? "熔毁点：%s K" : "Meltdown at: %s K");
        add("jade.cruciblecraft.fill_level",
                chinese ? "填充：%s" : "Fill level: %s");
        add("jade.cruciblecraft.render_state",
                chinese ? "状态：%s" : "Render state: %s");
        add("jade.cruciblecraft.render_state.empty",
                chinese ? "空" : "empty");
        add("jade.cruciblecraft.render_state.solid",
                chinese ? "固态" : "solid");
        add("jade.cruciblecraft.render_state.molten",
                chinese ? "熔融" : "molten");
        add("jade.cruciblecraft.render_state.active",
                chinese ? "工作中" : "active");
        add("jade.cruciblecraft.cache_slot",
                chinese ? "缓存槽：%s" : "Cache slot: %s");
        add("jade.cruciblecraft.transformer.profile",
                chinese ? "档位：%s → %s" : "Profile: %s → %s");
        add("jade.cruciblecraft.transformer.mode",
                chinese ? "模式：%s" : "Mode: %s");
        add("jade.cruciblecraft.transformer.mode.step_down",
                chinese ? "降压（正面高压输入）" : "step-down (front HV in)");
        add("jade.cruciblecraft.transformer.mode.step_up",
                chinese ? "升压（正面高压输出）" : "step-up (front HV out)");
        add("jade.cruciblecraft.transformer.buffer",
                chinese ? "缓冲：%s / %s EU" : "Buffer: %s / %s EU");
        add("jade.cruciblecraft.transformer.activity",
                chinese ? "活动：%s" : "Activity: %s");
        add("jade.cruciblecraft.transformer.activity.active",
                chinese ? "工作" : "active");
        add("jade.cruciblecraft.transformer.activity.idle",
                chinese ? "空闲" : "idle");
        add("jade.cruciblecraft.transformer.side",
                chinese ? "%s：%s" : "%s: %s");
        add("jade.cruciblecraft.transformer.input",
                chinese ? "输入 %s EU" : "input %s EU");
        add("jade.cruciblecraft.transformer.output",
                chinese ? "输出 %s EU ×%s" : "output %s EU ×%s");
        add("jade.cruciblecraft.side.down", chinese ? "下" : "down");
        add("jade.cruciblecraft.side.up", chinese ? "上" : "up");
        add("jade.cruciblecraft.side.north", chinese ? "北" : "north");
        add("jade.cruciblecraft.side.south", chinese ? "南" : "south");
        add("jade.cruciblecraft.side.west", chinese ? "西" : "west");
        add("jade.cruciblecraft.side.east", chinese ? "东" : "east");
        add("jade.cruciblecraft.reactor.heat",
                chinese ? "热量：%s HU" : "Heat: %s HU");
        add("jade.cruciblecraft.reactor.last_heat",
                chinese ? "上次热量：%s HU" : "Last heat: %s HU");
        add("jade.cruciblecraft.reactor.neutrons",
                chinese ? "中子：%s n" : "Neutrons: %s n");
        add("jade.cruciblecraft.reactor.coolant",
                chinese ? "冷却剂：%s %s mB" : "Coolant: %s %s mB");
        add("jade.cruciblecraft.reactor.output",
                chinese ? "输出：%s %s mB" : "Output: %s %s mB");
        add("jade.cruciblecraft.reactor.running",
                chinese ? "运行：%s" : "Running: %s");
        add("jade.cruciblecraft.reactor.running.on",
                chinese ? "是" : "yes");
        add("jade.cruciblecraft.reactor.running.off",
                chinese ? "否" : "no");
        add("jade.cruciblecraft.reactor.safety",
                chinese ? "安全：%s" : "Safety: %s");
        add("jade.cruciblecraft.reactor.safety.ok",
                chinese ? "正常" : "ok");
        add("jade.cruciblecraft.reactor.safety.rods_destroyed_no_coolant",
                chinese ? "缺冷却剂已毁棒" : "rods destroyed (no coolant)");
        add("jade.cruciblecraft.reactor.safety.output_full_stalled",
                chinese ? "输出满、转换停滞" : "output full, conversion stalled");
        add("jade.cruciblecraft.battery.charge",
                chinese ? "%s：%s / %s" : "%s: %s / %s");
        add("jade.cruciblecraft.battery.packet",
                chinese ? "包：%s–%s，输入 %s" : "Packet: %s–%s, input %s");
        add("jade.cruciblecraft.converter.accepts",
                chinese ? "输入：%s" : "Accepts: %s");
        add("jade.cruciblecraft.converter.emits",
                chinese ? "输出：%s" : "Emits: %s");
        add("jade.cruciblecraft.converter.packet",
                chinese ? "包：入 %s / 出 %s" : "Packet: in %s / out %s");
        add("jade.cruciblecraft.converter.window",
                chinese ? "窗口：%s / %s / %s" : "Window: %s / %s / %s");
        add("jade.cruciblecraft.converter.activity",
                chinese ? "活动：%s" : "Activity: %s");
        add("jade.cruciblecraft.converter.buffer",
                chinese ? "缓冲：%s / %s" : "Buffer: %s / %s");
        add("jade.cruciblecraft.converter.tank",
                chinese ? "%s：%s/%s mB" : "%s: %s/%s mB");
        add("jade.cruciblecraft.converter.tank_empty",
                chinese ? "空" : "Empty");
    }

    private void addEmptyToolHeadNames() {
        addItem(ModItems.EMPTY_TOOL_HEADS.get("empty/tool_head_chainsaw"),
                chinese ? "空 链锯头" : "Empty Chainsaw Head");
        addItem(ModItems.EMPTY_TOOL_HEADS.get("empty/tool_head_drill"),
                chinese ? "空 钻头" : "Empty Drill Head");
        addItem(ModItems.EMPTY_TOOL_HEADS.get("empty/tool_head_pickaxe_gem"),
                chinese ? "空 宝石镐头" : "Empty Gem Pickaxe Head");
        addItem(ModItems.EMPTY_TOOL_HEADS.get("empty/tool_head_wrench"),
                chinese ? "空 扳手头" : "Empty Wrench Head");
    }

    private static String formEnglish(String serializedName) {
        return LanguageNames.formatEnglishId(serializedName);
    }

    private void addEmiStackGroupNames() {
        for (var form : MaterialPrefixCatalog.values()) {
            String key = "emi.cruciblecraft.group." + form.serializedName();
            if (chinese) {
                MaterialZhNames.prefix(form.serializedName())
                        .or(() -> MaterialZhNames.pipe(form.serializedName()))
                        .or(() -> MaterialZhNames.conductor(form.serializedName()))
                        .ifPresent(name -> add(key, name));
            } else {
                add(key, LanguageNames.formatEnglishId(form.serializedName()));
            }
        }
        for (ToolKind kind : ToolKind.values()) {
            add(
                    "emi.cruciblecraft.group.tool." + kind.serializedName(),
                    chinese ? toolGroupZh(kind) : toolGroupEn(kind));
        }
        for (EmiStackGroupPlan.ExactGroup group : EmiStackGroupPlan.machineGroups()) {
            add(
                    group.nameKey(),
                    machineGroupName(group.resourcePath().substring("machine/".length())));
        }
        for (EmiStackGroupPlan.ExactGroup group :
                EmiStackGroupPlan.converterGroups()) {
            add(
                    group.nameKey(),
                    converterGroupName(
                            group.resourcePath().substring("converter/".length())));
        }
        for (EmiStackGroupPlan.ExactGroup group : EmiStackGroupPlan.catalogGroups()) {
            add(group.nameKey(), catalogGroupName(group.resourcePath()));
        }
    }

    private String catalogGroupName(String resourcePath) {
        return switch (resourcePath) {
            case "building/glass" -> chinese ? "玻璃" : "Glass";
            case "building/glass_slab" -> chinese ? "玻璃台阶" : "Glass Slab";
            case "building/glow_glass" -> chinese ? "荧光玻璃" : "Glow Glass";
            case "building/glow_glass_slab" ->
                    chinese ? "荧光玻璃台阶" : "Glow Glass Slab";
            case "building/planks" -> chinese ? "木板" : "Planks";
            case "building/slab" -> chinese ? "台阶" : "Slab";
            case "building/log" -> chinese ? "原木" : "Log";
            case "building/bars" -> chinese ? "栏杆" : "Bars";
            case "building/rail" -> chinese ? "铁轨" : "Rail";
            case "building/spike" -> chinese ? "尖刺" : "Spike";
            case "building/bale" -> chinese ? "草捆" : "Bale";
            case "building/cfoam" -> chinese ? "建筑泡沫" : "C-Foam";
            case "building/cfoam_fresh" -> chinese ? "新鲜建筑泡沫" : "Fresh C-Foam";
            case "building/diggable" -> chinese ? "可挖掘方块" : "Diggable";
            case "building/sands" -> chinese ? "沙子" : "Sands";
            case "building/stone" -> chinese ? "石头" : "Stone";
            case "furniture/bookshelf" -> chinese ? "书架" : "Bookshelf";
            case "furniture/drawer" -> chinese ? "分区抽屉" : "Compartment Drawer";
            case "furniture/safe" -> chinese ? "保险箱" : "Safe";
            case "furniture/chest" -> chinese ? "箱子" : "Chest";
            case "hopper/hopper" -> chinese ? "料斗" : "Hopper";
            case "hopper/queue_hopper" -> chinese ? "队列料斗" : "Queue Hopper";
            case "misc_tool/anvil" -> chinese ? "砧" : "Anvil";
            case "misc_tool/mortar" -> chinese ? "研钵" : "Mortar";
            case "fluid_attachment/faucet" -> chinese ? "龙头" : "Faucet";
            case "fluid_attachment/tap" -> chinese ? "水龙头" : "Tap";
            case "fluid_attachment/funnel" -> chinese ? "漏斗" : "Funnel";
            case "fluid_attachment/nozzle" -> chinese ? "喷嘴" : "Nozzle";
            case "fluid_attachment/cap_nozzle" ->
                    chinese ? "有盖喷嘴" : "Cap Nozzle";
            case "foundry/crucible" -> chinese ? "坩埚" : "Crucible";
            case "foundry/basin" -> chinese ? "浇铸盆" : "Casting Basin";
            case "foundry/crossing" -> chinese ? "浇铸道" : "Crucible Crossing";
            case "foundry/mold" -> chinese ? "铸造模具" : "Foundry Mold";
            case "mold/ceramic" -> chinese ? "模具" : "Mold";
            case "energy/large_gas_turbine" ->
                    chinese ? "大型燃气轮机" : "Large Gas Turbine";
            default -> throw new IllegalStateException(
                    "Missing EMI catalog group name for " + resourcePath);
        };
    }

    private String machineGroupName(String kindPath) {
        MachineKindCatalog.Kind kind = MachineKindCatalog.require(
                net.minecraft.resources.ResourceLocation.fromNamespaceAndPath(
                        CrucibleCraft.MODID, kindPath));
        return chinese ? kind.langZh() : kind.langEn();
    }

    private String converterGroupName(String kindPath) {
        EnergyConverterKindCatalog.Kind kind = EnergyConverterKindCatalog.require(
                net.minecraft.resources.ResourceLocation.fromNamespaceAndPath(
                        CrucibleCraft.MODID, kindPath));
        return chinese ? kind.langZh() : kind.langEn();
    }

    private static String toolGroupZh(ToolKind kind) {
        return switch (kind) {
            case PICKAXE -> "镐";
            case SHOVEL -> "铲";
            case AXE -> "斧";
            case HOE -> "锄";
            case SWORD -> "剑";
            case SMITHING_HAMMER -> "锻造锤";
            case FILE -> "锉刀";
            case CHISEL -> "凿子";
            case SAW -> "锯";
            case SCREWDRIVER -> "螺丝刀";
            case WRENCH -> "扳手";
            case MONKEY_WRENCH -> "活动扳手";
            case WIRE_CUTTER -> "剪线钳";
            case KNIFE -> "刀";
            case CLUB -> "棒";
            case SPADE -> "锹";
            case DOUBLE_AXE -> "双刃斧";
            case SENSE -> "镰刀";
            case PLOW -> "犁";
            case CONSTRUCTION_PICK -> "建筑镐";
            case GEM_PICK -> "宝石镐";
            case BUILDER_WAND -> "建筑杖";
            case UNIVERSAL_SPADE -> "万能锹";
            case CROWBAR -> "撬棍";
            case PLUNGER -> "皮搋子";
            case SCOOP -> "捕虫网";
            case BUTCHERY_KNIFE -> "屠宰刀";
            case BRANCH_CUTTER -> "修枝剪";
            case SCISSORS -> "剪刀";
            case PINCERS -> "钳子";
            case SOFT_HAMMER -> "软锤";
            case BENDING_CYLINDER -> "折弯筒";
            case BENDING_CYLINDER_SMALL -> "小型折弯筒";
            case HAND_DRILL -> "手钻";
            case ROLLING_PIN -> "擀面杖";
            case FLINT_AND_TINDER -> "火绒";
            case POCKET_MULTITOOL -> "口袋多功能工具";
            case MAGNIFYING_GLASS -> "放大镜";
            default -> com.masson.cruciblecraft.content.item.tool.ElectricToolCatalog
                    .of(kind)
                    .map(com.masson.cruciblecraft.content.item.tool.ElectricToolCatalog::langZh)
                    .orElseThrow();
        };
    }

    private static String toolGroupEn(ToolKind kind) {
        return switch (kind) {
            case PICKAXE -> "Pickaxe";
            case SHOVEL -> "Shovel";
            case AXE -> "Axe";
            case HOE -> "Hoe";
            case SWORD -> "Sword";
            case SMITHING_HAMMER -> "Smithing Hammer";
            case FILE -> "File";
            case CHISEL -> "Chisel";
            case SAW -> "Saw";
            case SCREWDRIVER -> "Screwdriver";
            case WRENCH -> "Wrench";
            case MONKEY_WRENCH -> "Monkey Wrench";
            case WIRE_CUTTER -> "Wire Cutter";
            case KNIFE -> "Knife";
            case CLUB -> "Club";
            case SPADE -> "Spade";
            case DOUBLE_AXE -> "Double Axe";
            case SENSE -> "Scythe";
            case PLOW -> "Plow";
            case CONSTRUCTION_PICK -> "Construction Pick";
            case GEM_PICK -> "Gem Pick";
            case BUILDER_WAND -> "Builder Wand";
            case UNIVERSAL_SPADE -> "Universal Spade";
            case CROWBAR -> "Crowbar";
            case PLUNGER -> "Plunger";
            case SCOOP -> "Scoop";
            case BUTCHERY_KNIFE -> "Butchery Knife";
            case BRANCH_CUTTER -> "Branch Cutter";
            case SCISSORS -> "Scissors";
            case PINCERS -> "Pincers";
            case SOFT_HAMMER -> "Soft Hammer";
            case BENDING_CYLINDER -> "Bending Cylinder";
            case BENDING_CYLINDER_SMALL -> "Small Bending Cylinder";
            case HAND_DRILL -> "Hand Drill";
            case ROLLING_PIN -> "Rolling Pin";
            case FLINT_AND_TINDER -> "Flint and Tinder";
            case POCKET_MULTITOOL -> "Pocket Multitool";
            case MAGNIFYING_GLASS -> "Magnifying Glass";
            default -> com.masson.cruciblecraft.content.item.tool.ElectricToolCatalog
                    .of(kind)
                    .map(com.masson.cruciblecraft.content.item.tool.ElectricToolCatalog::langEn)
                    .orElseThrow();
        };
    }

    private void addReactorRodNames() {
        for (com.masson.cruciblecraft.nuclear.ReactorRodCatalog.Entry entry :
                com.masson.cruciblecraft.nuclear.ReactorRodCatalog.entries()) {
            addItem(ModItems.reactorRod(entry.id().getPath()), rodName(entry));
        }
    }

    private void addTechnologicalPartNames() {
        TechnologicalPartCatalog.parts().forEach(part -> {
            if (CoverComponentTiers.findByItemPath(part.registryPath()).isPresent()) {
                return;
            }
            if (!chinese) {
                addItem(
                        ModItems.technologicalPart(part.registryPath()),
                        part.englishName());
                return;
            }
            LanguageNames.chineseOrEmpty(part.chineseName(), part.englishName())
                    .ifPresent(name -> addItem(
                            ModItems.technologicalPart(part.registryPath()),
                            name));
        });
    }

    private String rodName(
            com.masson.cruciblecraft.nuclear.ReactorRodCatalog.Entry entry) {
        if (!chinese) {
            return title(entry.id().getPath());
        }
        String material = MaterialZhNames.material(entry.material())
                .orElseGet(() -> rodMaterialZh(entry.material()));
        return switch (entry.kind()) {
            case EMPTY -> "空反应棒";
            case ABSORBER -> "中子吸收棒";
            case REFLECTOR -> "中子反射棒";
            case MODERATOR -> "中子慢化棒";
            case NUCLEAR -> material + "燃料棒";
            case DEPLETED -> material + "乏燃料棒";
            case BREEDER -> material + "增殖棒";
            case PRODUCT -> material + "富集棒";
        };
    }

    private static String rodMaterialZh(String material) {
        return switch (material) {
            case "zirconium" -> "锆";
            case "cd_in_ag_alloy" -> "镉铟银合金";
            case "beryllium" -> "铍";
            case "graphite" -> "石墨";
            case "thorium" -> "钍";
            case "cyanite" -> "青晶石";
            case "uranium" -> "铀-238";
            case "uranium235" -> "铀-235";
            case "uranium233" -> "铀-233";
            case "yellorium" -> "黄铀";
            case "plutonium" -> "钚-244";
            case "plutonium241" -> "钚-241";
            case "plutonium243" -> "钚-243";
            case "plutonium239" -> "钚-239";
            case "blutonium" -> "蓝钚";
            case "americium" -> "镅-245";
            case "americium241" -> "镅-241";
            case "ludicrite" -> "狂金";
            case "cobalt60" -> "钴-60";
            case "naquadah_enriched" -> "富集硅岩";
            case "naquadria" -> "超能硅岩";
            case "naquadah" -> "硅岩";
            case "lithium" -> "锂";
            case "tritium" -> "氚";
            default -> material;
        };
    }

    private static String title(String value) {
        return LanguageNames.formatEnglishId(value);
    }

    private void addCatalogMachineNames() {
        ModMachineVariants.ALL.forEach(variant -> {
            String path = variant.id().getPath();
            String englishRaw = catalogMachineName(variant, false);
            String english = LanguageNames.playerEnglish(englishRaw, path);
            if (!chinese) {
                add(LanguageNames.translationKey("block", path), english);
                add(LanguageNames.translationKey("item", path), english);
                return;
            }
            if (!english.equals(englishRaw)) {
                return;
            }
            String rawZh = catalogMachineName(variant, true);
            LanguageNames.playerChinese(rawZh, path)
                    .or(() -> LanguageNames.chineseOrEmpty(rawZh, englishRaw))
                    .ifPresent(name -> {
                        add(LanguageNames.translationKey("block", path), name);
                        add(LanguageNames.translationKey("item", path), name);
                    });
        });
        addConverterCatalogNames();
        addBatteryCatalogNames();
        addTransformerCatalogNames();
        addHeatExchangerCatalogNames();
        addLargeGasTurbineTooltips();
        addCoolerCatalogNames();
        addFluxConverterCatalogNames();
        addQuantumEnergizerCatalogNames();
        addLongDistanceCatalogNames();
        BathMteIdentityCatalog.newItems().forEach(identity -> {
            if (identity.decorativePanel()) {
                addIdentityBlockName(
                        identity.registryPath(),
                        identity.englishName(),
                        identity.chineseName());
            } else {
                addIdentityItemName(
                        identity.registryPath(),
                        identity.englishName(),
                        identity.chineseName());
            }
        });
        SmelterMteIdentityCatalog.newItems().forEach(identity ->
                addIdentityItemName(
                        identity.registryPath(),
                        identity.englishName(),
                        identity.chineseName()));
        com.masson.cruciblecraft.content.item.BathIdentityCatalog.identities().forEach(identity ->
                addIdentityItemName(
                        identity.registryPath(),
                        identity.englishName(),
                        identity.chineseName()));
        com.masson.cruciblecraft.content.item.SemanticObjectCatalog.identities().forEach(identity ->
                addIdentityItemName(
                        identity.registryPath(),
                        identity.englishName(),
                        identity.chineseName()));
        com.masson.cruciblecraft.content.item.SlicerOperandCatalog.operands().forEach(operand ->
                addIdentityItemName(
                        operand.registryPath(),
                        operand.englishName(),
                        operand.chineseName()));
        com.masson.cruciblecraft.content.item.PressureWasherOperandCatalog
                .operands().forEach(operand ->
                        addIdentityItemName(
                                operand.registryPath(),
                                operand.englishName(),
                                operand.chineseName()));
        BathMteFluidCatalog.fluids().forEach(fluid ->
                addFluidEnglishOnly(fluid.id().getPath(), fluid.englishName()));
        com.masson.cruciblecraft.content.item.BathRemainderFluidCatalog.fluids().forEach(fluid ->
                addFluidEnglishOnly(fluid.id().getPath(), fluid.englishName()));
        com.masson.cruciblecraft.worldgen.tree.TreeHoleFluidCatalog.fluids().forEach(fluid ->
                addFluidEnglishOnly(fluid.id().getPath(), fluid.englishName()));
        com.masson.cruciblecraft.content.item.SemanticFluidCatalog.fluids().forEach(fluid ->
                addFluidEnglishOnly(fluid.id().getPath(), fluid.englishName()));
    }

    private void addIdentityItemName(
            String registryPath, String english, String chineseName) {
        addIdentityName("item", registryPath, english, chineseName);
    }

    private void addIdentityBlockName(
            String registryPath, String english, String chineseName) {
        addIdentityName("block", registryPath, english, chineseName);
    }

    private void addIdentityName(
            String kind, String registryPath, String english, String chineseName) {
        String key = LanguageNames.translationKey(kind, registryPath);
        if (!chinese) {
            add(key, LanguageNames.playerEnglish(english, registryPath));
            return;
        }
        LanguageNames.playerChinese(chineseName, registryPath)
                .or(() -> LanguageNames.chineseOrEmpty(chineseName, english))
                .or(() -> LanguageNames.composeMaterialFormZh(registryPath))
                .ifPresent(name -> add(key, name));
    }

    private void addFluidEnglishOnly(String registryPath, String english) {
        if (chinese) {
            return;
        }
        add(LanguageNames.translationKey("fluid", registryPath), english);
        add(LanguageNames.translationKey("fluid_type", registryPath), english);
    }

    private String catalogMachineName(
            com.masson.cruciblecraft.machine.processing.MachineVariant variant) {
        return catalogMachineName(variant, chinese);
    }

    private String catalogMachineName(
            com.masson.cruciblecraft.machine.processing.MachineVariant variant,
            boolean chineseLocale) {
        MachineKindCatalog.Kind kind = MachineKindCatalog.require(
                variant.kind().id());
        String kindName = chineseLocale ? kind.langZh() : kind.langEn();
        boolean bareOpening = variant.id().getPath().equals(kind.id().getPath())
                && !"kinetic".equals(kind.displayGroup())
                && !"heat".equals(kind.displayGroup());
        if (bareOpening) {
            return kindName;
        }
        MachineKindCatalog.MaterialLang material = MachineKindCatalog.materialLang(
                variant.tierBand().materialId());
        if (chineseLocale) {
            return material.zh() + kindName;
        }
        return material.en() + " " + kindName;
    }

    private void addConverterCatalogNames() {
        EnergyConverterTierCatalog.entries().forEach(entry -> {
            String path = entry.id().getPath();
            String name = converterDisplayName(entry);
            add(LanguageNames.translationKey("block", path), name);
            add(LanguageNames.translationKey("item", path), name);
        });
    }

    private String converterDisplayName(
            EnergyConverterTierCatalog.Entry entry) {
        EnergyConverterKindCatalog.Kind kind =
                EnergyConverterKindCatalog.require(entry.kindId());
        EnergyConverterKindCatalog.MaterialLang material =
                EnergyConverterKindCatalog.materialLang(entry.material());
        if (chinese) {
            return prefixedConverterName(kind.langZh(), material.zh(), true);
        }
        return prefixedConverterName(kind.langEn(), material.en(), false);
    }

    private static String prefixedConverterName(
            String kindName, String materialName, boolean chineseLocale) {
        if (kindAlreadyIncludesMaterial(kindName, materialName, chineseLocale)) {
            return kindName;
        }
        if (chineseLocale) {
            return materialName + kindName;
        }
        return materialName + " " + kindName;
    }

    private static boolean kindAlreadyIncludesMaterial(
            String kindName, String materialName, boolean chineseLocale) {
        if (!kindName.startsWith(materialName)) {
            return false;
        }
        if (kindName.length() == materialName.length()) {
            return true;
        }
        char next = kindName.charAt(materialName.length());
        return chineseLocale || next == ' ';
    }

    private void addBatteryCatalogNames() {
        add(
                "tooltip.cruciblecraft.battery.charge",
                chinese
                        ? "%s / %s %s — 包大小: %s"
                        : "%s / %s %s - Size: %s");
        add(
                "tooltip.cruciblecraft.battery.sneak_place",
                chinese ? "潜行才能放置" : "Sneak to place");
        EnergyBatteryCatalog.profiles().forEach(profile -> {
            String path = profile.id().getPath();
            String name = batteryDisplayName(profile);
            add(LanguageNames.translationKey("block", path), name);
            add(LanguageNames.translationKey("item", path), name);
        });
    }

    private void addTransformerCatalogNames() {
        add(
                "message.cruciblecraft.transformer.normal",
                chinese ? "正向" : "Normal");
        add(
                "message.cruciblecraft.transformer.reversed",
                chinese ? "反向" : "Reversed");
        add(
                "tooltip.cruciblecraft.transformer.front_in",
                chinese ? "正面为高压输入" : "Front is HV in");
        add(
                "tooltip.cruciblecraft.transformer.wrench",
                chinese ? "活动扳手切换升压/降压"
                        : "Monkey wrench reverses step-up/step-down");
        EnergyTransformerCatalog.profiles().forEach(profile -> {
            String path = profile.id().getPath();
            String name = transformerDisplayName(profile);
            add(LanguageNames.translationKey("block", path), name);
            add(LanguageNames.translationKey("item", path), name);
        });
    }

    private void addLargeGasTurbineTooltips() {
        add(
                "tooltip.cruciblecraft.large_gas_turbine.structure_header",
                chinese ? "结构：" : "Structure:");
        add(
                "tooltip.cruciblecraft.large_gas_turbine.structure",
                chinese ? "3×3×4，共 35 块%s" : "3x3x4 of 35 %s");
        add(
                "tooltip.cruciblecraft.large_gas_turbine.controller",
                chinese ? "主机在朝外的 3×3 正中"
                        : "Main centered on the 3x3 facing outwards");
        add(
                "tooltip.cruciblecraft.large_gas_turbine.input",
                chinese ? "只能从正面 3×3 输入"
                        : "Input only possible at frontal 3x3");
        add(
                "tooltip.cruciblecraft.large_gas_turbine.exhaust",
                chinese ? "必须抽走排气！" : "Exhaust Gas has to be removed!");
        add(
                "tooltip.cruciblecraft.large_gas_turbine.energy_out",
                chinese ? "输出：%s RU/t（%s 至 %s）"
                        : "Outputs: %s RU/t (%s to %s)");
    }

    private void addHeatExchangerCatalogNames() {
        add(
                "tooltip.cruciblecraft.heat_exchanger.hu_rate",
                chinese ? "输出 %s HU/t" : "Outputs %s HU/t");
        add(
                "tooltip.cruciblecraft.heat_exchanger.efficiency",
                chinese ? "效率 %s%%" : "Efficiency %s%%");
        HeatExchangerCatalog.profiles().forEach(profile -> {
            String path = profile.id().getPath();
            String name = chinese ? profile.langZh() : profile.langEn();
            add(LanguageNames.translationKey("block", path), name);
            add(LanguageNames.translationKey("item", path), name);
        });
    }

    private void addCoolerCatalogNames() {
        add(
                "tooltip.cruciblecraft.cooler.electric",
                chinese ? "输入 %s EU/t → 输出 %s CU/t 与 HU/t" : "Inputs %s EU/t → %s CU/t and HU/t");
        add(
                "tooltip.cruciblecraft.cooler.flux",
                chinese ? "输入 %s FE/t → 输出 %s CU/t 与 HU/t" : "Inputs %s FE/t → %s CU/t and HU/t");
        add(
                "tooltip.cruciblecraft.cooler.sides",
                chinese ? "前面 CU，后面 HU；侧面输入" : "CU front, HU back; input on the sides");
        CoolerCatalog.profiles().forEach(profile -> {
            String path = profile.id().getPath();
            String name = chinese ? profile.langZh() : profile.langEn();
            add(LanguageNames.translationKey("block", path), name);
            add(LanguageNames.translationKey("item", path), name);
        });
    }

    private void addFluxConverterCatalogNames() {
        add(
                "tooltip.cruciblecraft.flux.heater",
                chinese ? "输入 %s FE/t → 输出 %s HU/t" : "Inputs %s FE/t → %s HU/t");
        add(
                "tooltip.cruciblecraft.flux.engine",
                chinese ? "输入 %s FE/t → 输出 %s KU/t" : "Inputs %s FE/t → %s KU/t");
        add(
                "tooltip.cruciblecraft.flux.motor",
                chinese ? "输入 %s FE/t → 输出 %s RU/t" : "Inputs %s FE/t → %s RU/t");
        add(
                "tooltip.cruciblecraft.flux.magnet",
                chinese ? "输入 %s FE/t → 输出 %s MU/t" : "Inputs %s FE/t → %s MU/t");
        add(
                "tooltip.cruciblecraft.flux.laser",
                chinese ? "输入 %s FE/t → 输出 %s LU/t" : "Inputs %s FE/t → %s LU/t");
        add(
                "tooltip.cruciblecraft.flux.dynamo",
                chinese ? "输入 %s RU/t → 输出 %s FE/t" : "Inputs %s RU/t → %s FE/t");
        add(
                "tooltip.cruciblecraft.flux.sides.heater",
                chinese ? "前面 HU；其余面输入 FE" : "HU front; FE on the other faces");
        add(
                "tooltip.cruciblecraft.flux.sides.engine",
                chinese ? "后面 FE，前面 KU" : "FE back, KU front");
        add(
                "tooltip.cruciblecraft.flux.sides.motor",
                chinese ? "前面 RU；其余面输入 FE" : "RU front; FE on the other faces");
        add(
                "tooltip.cruciblecraft.flux.sides.magnet",
                chinese ? "前后 MU；侧面输入 FE" : "MU front and back; FE on the sides");
        add(
                "tooltip.cruciblecraft.flux.sides.laser",
                chinese ? "前面 LU；其余面输入 FE" : "LU front; FE on the other faces");
        add(
                "tooltip.cruciblecraft.flux.sides.dynamo",
                chinese ? "后面 RU，前面 FE" : "RU back, FE front");
        FluxCatalog.profiles().forEach(profile -> {
            String path = profile.id().getPath();
            String name = chinese ? profile.langZh() : profile.langEn();
            add(LanguageNames.translationKey("block", path), name);
            add(LanguageNames.translationKey("item", path), name);
        });
    }

    private void addQuantumEnergizerCatalogNames() {
        com.masson.cruciblecraft.energy.quantum.QuantumEnergizerCatalog.profiles()
                .forEach(profile -> {
                    String path = profile.id().getPath();
                    String name = chinese ? profile.langZh() : profile.langEn();
                    add(LanguageNames.translationKey("block", path), name);
                    add(LanguageNames.translationKey("item", path), name);
                });
    }

    private void addLongDistanceCatalogNames() {
        com.masson.cruciblecraft.energy.longdistance.LongDistanceTransformerCatalog
                .endpoints()
                .forEach(profile -> {
                    String path = profile.id().getPath();
                    String name = chinese ? profile.langZh() : profile.langEn();
                    add(LanguageNames.translationKey("block", path), name);
                    add(LanguageNames.translationKey("item", path), name);
                });
        com.masson.cruciblecraft.energy.longdistance.LongDistanceTransformerCatalog
                .wires()
                .forEach(profile -> {
                    String path = profile.id().getPath();
                    String name = chinese ? profile.langZh() : profile.langEn();
                    add(LanguageNames.translationKey("block", path), name);
                    add(LanguageNames.translationKey("item", path), name);
                });
    }

    private String transformerDisplayName(
            com.masson.cruciblecraft.energy.transformer.EnergyTransformerProfile profile) {
        String low = chinese
                ? transformerVoltageZh(profile.lowVoltage())
                : profile.lowVoltage().toUpperCase(Locale.ROOT);
        String high = chinese
                ? transformerVoltageZh(profile.highVoltage())
                : profile.highVoltage().toUpperCase(Locale.ROOT);
        if (chinese) {
            return profile.langZh() + "（" + low + "-" + high + "）";
        }
        return profile.langEn() + " (" + low + "-" + high + ")";
    }

    private static String transformerVoltageZh(String voltage) {
        return switch (voltage) {
            case "ulv" -> "超低压";
            case "lv" -> "低压";
            case "mv" -> "中压";
            case "hv" -> "高压";
            case "ev" -> "超高压";
            case "iv" -> "极高压";
            case "luv" -> "LuV";
            case "zpm" -> "ZPM";
            case "uv" -> "终极";
            case "puv1" -> "PUV1";
            default -> voltage.toUpperCase(Locale.ROOT);
        };
    }

    private void addBatteryCellNames(boolean chinese) {
        Map<String, String> names = chinese
                ? Map.of(
                        "lead_acid_cell_empty", "铅酸电池空芯",
                        "lead_acid_cell_filled", "铅酸电池灌液芯",
                        "alkaline_cell_empty", "碱性电池空芯",
                        "alkaline_cell_filled", "碱性电池灌液芯",
                        "nickel_cadmium_cell_empty", "镍镉电池空芯",
                        "nickel_cadmium_cell_filled", "镍镉电池灌液芯",
                        "lithium_cobalt_cell_empty", "锂钴电池空芯",
                        "lithium_cobalt_cell_filled", "锂钴电池灌液芯",
                        "lithium_manganese_cell_empty", "锂锰电池空芯",
                        "lithium_manganese_cell_filled", "锂锰电池灌液芯")
                : Map.of(
                        "lead_acid_cell_empty", "Lead-Acid Cell (Empty)",
                        "lead_acid_cell_filled", "Lead-Acid Cell (Filled)",
                        "alkaline_cell_empty", "Alkaline Button Cell (Empty)",
                        "alkaline_cell_filled", "Alkaline Button Cell (Filled)",
                        "nickel_cadmium_cell_empty", "Nickel-Cadmium Cell (Empty)",
                        "nickel_cadmium_cell_filled", "Nickel-Cadmium Cell (Filled)",
                        "lithium_cobalt_cell_empty", "Lithium-Cobalt Cell (Empty)",
                        "lithium_cobalt_cell_filled", "Lithium-Cobalt Cell (Filled)",
                        "lithium_manganese_cell_empty", "Lithium-Manganese Cell (Empty)",
                        "lithium_manganese_cell_filled", "Lithium-Manganese Cell (Filled)");
        names.forEach((path, name) ->
                addItem(ModItems.batteryCell(path), name));
        add(
                "tooltip.cruciblecraft.battery_cell.empty",
                chinese
                        ? "空芯：右键对应流体源灌入 %s mB（%s）"
                        : "Empty cell: right-click its fluid source to fill %s mB (%s)");
        add(
                "tooltip.cruciblecraft.battery_cell.filled",
                chinese
                        ? "已灌入固定流体：%s mB（%s）"
                        : "Filled with fixed chemistry: %s mB (%s)");
    }

    private String batteryDisplayName(
            com.masson.cruciblecraft.energy.battery.EnergyBatteryProfile profile) {
        String voltage = chinese
                ? batteryVoltageZh(profile.voltage())
                : profile.voltage().toUpperCase(Locale.ROOT);
        if (chinese) {
            return profile.langZh() + "（" + voltage + "）";
        }
        return profile.langEn() + " (" + voltage + ")";
    }

    private static String batteryVoltageZh(String voltage) {
        return switch (voltage) {
            case "ulv" -> "超低压";
            case "lv" -> "低压";
            case "mv" -> "中压";
            case "hv" -> "高压";
            case "ev" -> "超高压";
            case "iv" -> "极高压";
            default -> voltage.toUpperCase(Locale.ROOT);
        };
    }

    private void addGtTreeNames() {
        for (com.masson.cruciblecraft.worldgen.tree.prep.GtTreeSpecies species :
                com.masson.cruciblecraft.worldgen.tree.prep.GtTreeSpecies.ALL) {
            if (chinese) {
                addBlock(ModBlocks.treeSapling(species), species.chineseName() + "树苗");
                addBlock(ModBlocks.treeLog(species), species.chineseName() + "原木");
                addBlock(ModBlocks.treeBeam(species), species.chineseName() + "木梁");
                addBlock(ModBlocks.treeLeaves(species), species.chineseName() + "树叶");
                if (species.hasHole()) {
                    addBlock(ModBlocks.treeHole(species), species.chineseHoleName());
                }
            } else {
                addBlock(ModBlocks.treeSapling(species), species.englishName() + " Sapling");
                addBlock(ModBlocks.treeLog(species), species.englishName() + " Log");
                addBlock(ModBlocks.treeBeam(species), species.englishName() + " Beam");
                addBlock(ModBlocks.treeLeaves(species), species.englishName() + " Leaves");
                if (species.hasHole()) {
                    addBlock(ModBlocks.treeHole(species), species.englishHoleName());
                }
            }
        }
        addItem(ModItems.RUBBER_RESIN, chinese ? "橡胶树脂" : "Rubber Resin");
        addItem(ModItems.WOOD_PELLET, chinese ? "木粒" : "Wood Pellet");
        if (chinese) {
            add(LanguageNames.translationKey("fluid", "rubber_tree_sap"), "橡胶树液");
            add(LanguageNames.translationKey("fluid_type", "rubber_tree_sap"), "橡胶树液");
        }
    }

    private void addBedrockOreRemainderNames() {
        addBlock(
                ModBlocks.GT_HOSTED_ORE,
                chinese ? "宿主矿石" : "Hosted Ore");
        addBlock(
                ModBlocks.GT_BROKEN_ORE,
                chinese ? "破碎矿石" : "Broken Ore");
        addBlock(
                ModBlocks.GT_INDICATOR_FLOWER,
                chinese ? "指示花" : "Ore Indicator Flower");
        add(
                "block.cruciblecraft.gt_hosted_ore.named",
                chinese ? "%s矿石" : "%s Ore");
        add(
                "block.cruciblecraft.gt_small_ore.named",
                chinese ? "%s小型矿石" : "Small %s Ore");
        add(
                "block.cruciblecraft.gt_broken_ore.named",
                chinese ? "破碎%s矿石" : "Broken %s Ore");
        for (com.masson.cruciblecraft.worldgen.IndicatorFlower flower :
                com.masson.cruciblecraft.worldgen.IndicatorFlower.values()) {
            add(
                    "block.cruciblecraft.gt_indicator_flower."
                            + flower.getSerializedName(),
                    chinese ? flower.chineseName() : flower.englishName());
            add(
                    "tooltip.cruciblecraft.gt_indicator_flower."
                            + flower.getSerializedName(),
                    chinese ? flower.tooltipChinese() : flower.tooltipEnglish());
        }
        addFluidSpringNames();
    }

    private void addFluidSpringNames() {
        addBlock(
                ModBlocks.GT_FLUID_SPRING,
                chinese ? "流体泉" : "Fluid Spring");
        add(
                "block.cruciblecraft.gt_fluid_spring.oil_extra_heavy",
                chinese ? "超重油泉" : "Extra Heavy Oil Spring");
        add(
                "block.cruciblecraft.gt_fluid_spring.oil_heavy",
                chinese ? "重油泉" : "Heavy Oil Spring");
        add(
                "block.cruciblecraft.gt_fluid_spring.oil_medium",
                chinese ? "中油泉" : "Medium Oil Spring");
        add(
                "block.cruciblecraft.gt_fluid_spring.oil_light",
                chinese ? "轻油泉" : "Light Oil Spring");
        add(
                "block.cruciblecraft.gt_fluid_spring.natural_gas",
                chinese ? "天然气泉" : "Natural Gas Spring");
        add(
                "block.cruciblecraft.gt_fluid_spring.water_geothermal",
                chinese ? "地热水泉" : "Geothermal Water Spring");
        add(
                "block.cruciblecraft.gt_fluid_spring.lava",
                chinese ? "熔岩泉" : "Lava Spring");
        add(
                "tooltip.cruciblecraft.fluid_spring.contents",
                chinese ? "流体：%s（%s mB）" : "Fluid: %s (%s mB)");
        addBlock(
                ModBlocks.GT_INDICATOR_GRASS,
                chinese ? "指示草" : "Indicator Grass");
        addItem(
                ModItems.OIL_EXTRA_HEAVY_BUCKET,
                chinese ? "超重油桶" : "Extra Heavy Oil Bucket");
        addItem(
                ModItems.OIL_HEAVY_BUCKET,
                chinese ? "重油桶" : "Heavy Oil Bucket");
        addItem(
                ModItems.OIL_MEDIUM_BUCKET,
                chinese ? "中油桶" : "Medium Oil Bucket");
        addItem(
                ModItems.OIL_LIGHT_BUCKET,
                chinese ? "轻油桶" : "Light Oil Bucket");
        addItem(
                ModItems.NATURAL_GAS_BUCKET,
                chinese ? "天然气桶" : "Natural Gas Bucket");
        addItem(
                ModItems.WATER_GEOTHERMAL_BUCKET,
                chinese ? "地热水桶" : "Geothermal Water Bucket");
        add(
                "fluid_type.cruciblecraft.oil_extra_heavy",
                chinese ? "超重油" : "Extra Heavy Oil");
        add(
                "fluid_type.cruciblecraft.oil_heavy",
                chinese ? "重油" : "Heavy Oil");
        add(
                "fluid_type.cruciblecraft.oil_medium",
                chinese ? "中油" : "Medium Oil");
        add(
                "fluid_type.cruciblecraft.oil_light",
                chinese ? "轻油" : "Light Oil");
        add(
                "block.cruciblecraft.oil_extra_heavy",
                chinese ? "超重油" : "Extra Heavy Oil");
        add(
                "block.cruciblecraft.oil_heavy",
                chinese ? "重油" : "Heavy Oil");
        add(
                "block.cruciblecraft.oil_medium",
                chinese ? "中油" : "Medium Oil");
        add(
                "block.cruciblecraft.oil_light",
                chinese ? "轻油" : "Light Oil");
        add(
                "block.cruciblecraft.natural_gas",
                chinese ? "天然气" : "Natural Gas");
        add(
                "block.cruciblecraft.water_geothermal",
                chinese ? "地热水" : "Geothermal Water");
        for (com.masson.cruciblecraft.worldgen.IndicatorGrass grass :
                com.masson.cruciblecraft.worldgen.IndicatorGrass.values()) {
            add(
                    "block.cruciblecraft.gt_indicator_grass."
                            + grass.getSerializedName(),
                    chinese ? grass.chineseName() : grass.englishName());
            add(
                    "tooltip.cruciblecraft.gt_indicator_grass."
                            + grass.getSerializedName(),
                    chinese ? grass.tooltipChinese() : grass.tooltipEnglish());
        }
    }
}
