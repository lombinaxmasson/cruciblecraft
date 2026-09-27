package com.masson.cruciblecraft.compat.emi;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.masson.cruciblecraft.energy.converter.EnergyConverterKindCatalog;
import com.masson.cruciblecraft.localization.LanguageNames;
import com.masson.cruciblecraft.machine.processing.MachineKindCatalog;
import com.masson.cruciblecraft.material.prefix.MaterialPrefixCatalog;

import net.minecraft.resources.ResourceLocation;

class EmiStackGroupResourceTest {
    private static final Path ROOT = Path.of("src/generated/resources");
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    @Test
    void storagePlateGroupListsUniqueHostedSlashIds() throws Exception {
        var group = EmiStackGroupPlan.group("storage_plate");
        assertFalse(group.itemIds().isEmpty());
        assertTrue(group.itemIds().contains("cruciblecraft:iron/storage_plate"));
        assertFalse(group.itemIds().contains("cruciblecraft:storage_plate"));
        Path file = ROOT.resolve(
                "assets/cruciblecraft/stack_groups/form/storage_plate.json");
        Files.createDirectories(file.getParent());
        Files.writeString(
                file,
                GSON.toJson(group.toJson()) + "\n",
                StandardCharsets.UTF_8);
        assertFileMatches(group.resourcePath(), group.toJson());
    }

    @Test
    void generatedStackGroupsMatchThePlan() throws Exception {
        if (Boolean.parseBoolean(System.getProperty("cc.writeEmiStackGroups"))
                || Boolean.parseBoolean(System.getenv("CC_WRITE_EMI_STACK_GROUPS"))) {
            writeGeneratedStackGroups();
        }
        var groups = EmiStackGroupPlan.emittedFormGroups(MaterialPrefixCatalog.values());
        assertTrue(groups.size() > 1);
        Set<String> expected = new HashSet<>();
        for (EmiStackGroupPlan.FormGroup group : groups) {
            expected.add(group.resourcePath());
            assertFileMatches(group.resourcePath(), group.toJson());
        }
        var exact = EmiStackGroupPlan.exactGroups();
        assertTrue(exact.size() > 1);
        for (EmiStackGroupPlan.ExactGroup group : exact) {
            expected.add(group.resourcePath());
            assertFileMatches(group.resourcePath(), group.toJson());
        }
        assertEquals(expected, listedStackGroupPaths());
    }

    @Test
    void representativeGroupNamesAreInGeneratedLang() throws Exception {
        JsonObject english = json("assets/cruciblecraft/lang/en_us.json");
        JsonObject chinese = json("assets/cruciblecraft/lang/zh_cn.json");
        assertEquals(
                LanguageNames.formatEnglishId("dust"),
                english.get("emi.cruciblecraft.group.dust").getAsString());
        assertEquals(
                LanguageNames.formatEnglishId("ingot"),
                english.get("emi.cruciblecraft.group.ingot").getAsString());
        assertEquals(
                LanguageNames.formatEnglishId("crushed_ore"),
                english.get("emi.cruciblecraft.group.crushed_ore").getAsString());
        assertEquals("粉", chinese.get("emi.cruciblecraft.group.dust").getAsString());
        assertEquals("锭", chinese.get("emi.cruciblecraft.group.ingot").getAsString());
        assertEquals(
                LanguageNames.formatEnglishId("tool_head_pickaxe"),
                english.get("emi.cruciblecraft.group.tool_head_pickaxe").getAsString());
        assertEquals(
                "镐头",
                chinese.get("emi.cruciblecraft.group.tool_head_pickaxe").getAsString());
        assertEquals(
                "Pickaxe",
                english.get("emi.cruciblecraft.group.tool.pickaxe").getAsString());
        assertEquals(
                "镐",
                chinese.get("emi.cruciblecraft.group.tool.pickaxe").getAsString());
        MachineKindCatalog.Kind centrifuge = MachineKindCatalog.require(
                ResourceLocation.parse("cruciblecraft:centrifuge"));
        assertEquals(
                centrifuge.langEn(),
                english.get("emi.cruciblecraft.group.machine.centrifuge")
                        .getAsString());
        assertEquals(
                centrifuge.langZh(),
                chinese.get("emi.cruciblecraft.group.machine.centrifuge")
                        .getAsString());
        EnergyConverterKindCatalog.Kind turbine = EnergyConverterKindCatalog.require(
                ResourceLocation.parse("cruciblecraft:small_gas_turbine"));
        assertEquals(
                turbine.langEn(),
                english.get("emi.cruciblecraft.group.converter.small_gas_turbine")
                        .getAsString());
        assertEquals(
                turbine.langZh(),
                chinese.get("emi.cruciblecraft.group.converter.small_gas_turbine")
                        .getAsString());
        assertEquals(
                "Glass",
                english.get("emi.cruciblecraft.group.building.glass").getAsString());
        assertEquals(
                "玻璃",
                chinese.get("emi.cruciblecraft.group.building.glass").getAsString());
        assertEquals(
                "Chest",
                english.get("emi.cruciblecraft.group.furniture.chest").getAsString());
        assertEquals(
                "箱子",
                chinese.get("emi.cruciblecraft.group.furniture.chest").getAsString());
        assertEquals(
                "Mass Storage",
                english.get("emi.cruciblecraft.group.furniture.mass_storage")
                        .getAsString());
        assertEquals(
                "量子存储器",
                chinese.get("emi.cruciblecraft.group.furniture.mass_storage")
                        .getAsString());
        assertEquals(
                "Locker",
                english.get("emi.cruciblecraft.group.furniture.locker").getAsString());
        assertEquals(
                "储物柜",
                chinese.get("emi.cruciblecraft.group.furniture.locker").getAsString());
        assertEquals(
                "Bottle Crate",
                english.get("emi.cruciblecraft.group.furniture.bottle_crate")
                        .getAsString());
        assertEquals(
                "瓶框",
                chinese.get("emi.cruciblecraft.group.furniture.bottle_crate")
                        .getAsString());
        assertEquals(
                "GT Wood",
                english.get("emi.cruciblecraft.group.building.gt_wood").getAsString());
        assertEquals(
                "GT木头",
                chinese.get("emi.cruciblecraft.group.building.gt_wood").getAsString());
        assertEquals(
                "Concrete",
                english.get("emi.cruciblecraft.group.building.concrete").getAsString());
        assertEquals(
                "混凝土",
                chinese.get("emi.cruciblecraft.group.building.concrete").getAsString());
        assertEquals(
                "Circuit",
                english.get("emi.cruciblecraft.group.component.circuit").getAsString());
        assertEquals(
                "电路",
                chinese.get("emi.cruciblecraft.group.component.circuit").getAsString());
        assertEquals(
                "Hopper",
                english.get("emi.cruciblecraft.group.hopper.hopper").getAsString());
        assertEquals(
                "料斗",
                chinese.get("emi.cruciblecraft.group.hopper.hopper").getAsString());
        assertEquals(
                "Anvil",
                english.get("emi.cruciblecraft.group.misc_tool.anvil").getAsString());
        assertEquals(
                "砧",
                chinese.get("emi.cruciblecraft.group.misc_tool.anvil").getAsString());
        assertEquals(
                "Faucet",
                english.get("emi.cruciblecraft.group.fluid_attachment.faucet")
                        .getAsString());
        assertEquals(
                "龙头",
                chinese.get("emi.cruciblecraft.group.fluid_attachment.faucet")
                        .getAsString());
        assertEquals(
                "Funnel",
                english.get("emi.cruciblecraft.group.fluid_attachment.funnel")
                        .getAsString());
        assertEquals(
                "漏斗",
                chinese.get("emi.cruciblecraft.group.fluid_attachment.funnel")
                        .getAsString());
        assertEquals(
                "Crucible",
                english.get("emi.cruciblecraft.group.foundry.crucible").getAsString());
        assertEquals(
                "坩埚",
                chinese.get("emi.cruciblecraft.group.foundry.crucible").getAsString());
        assertEquals(
                "Casting Basin",
                english.get("emi.cruciblecraft.group.foundry.basin").getAsString());
        assertEquals(
                "浇铸盆",
                chinese.get("emi.cruciblecraft.group.foundry.basin").getAsString());
        assertEquals(
                "Crucible Crossing",
                english.get("emi.cruciblecraft.group.foundry.crossing").getAsString());
        assertEquals(
                "浇铸道",
                chinese.get("emi.cruciblecraft.group.foundry.crossing").getAsString());
        assertEquals(
                "Mold",
                english.get("emi.cruciblecraft.group.mold.ceramic").getAsString());
        assertEquals(
                "模具",
                chinese.get("emi.cruciblecraft.group.mold.ceramic").getAsString());
    }

    private static void assertFileMatches(String resourcePath, JsonObject expected)
            throws Exception {
        Path file = ROOT.resolve(
                "assets/cruciblecraft/stack_groups/" + resourcePath + ".json");
        assertTrue(Files.isRegularFile(file), "missing " + file);
        JsonObject json = JsonParser.parseString(
                Files.readString(file, StandardCharsets.UTF_8))
                .getAsJsonObject();
        assertEquals(expected, json, file.toString());
    }

    private static Set<String> listedStackGroupPaths() throws Exception {
        Path root = ROOT.resolve("assets/cruciblecraft/stack_groups");
        Set<String> paths = new HashSet<>();
        try (Stream<Path> stream = Files.walk(root)) {
            stream.filter(path -> path.toString().endsWith(".json"))
                    .forEach(path -> paths.add(
                            root.relativize(path).toString()
                                    .replace('\\', '/')
                                    .replaceFirst("\\.json$", "")));
        }
        return paths;
    }

    private static void writeGeneratedStackGroups() throws Exception {
        Path root = ROOT.resolve("assets/cruciblecraft/stack_groups");
        Set<Path> expected = new HashSet<>();
        writeGroups(root, expected, EmiStackGroupPlan.emittedFormGroups(
                MaterialPrefixCatalog.values()));
        writeExact(root, expected, EmiStackGroupPlan.exactGroups());
        try (Stream<Path> stream = Files.walk(root)) {
            stream.filter(path -> path.toString().endsWith(".json"))
                    .filter(path -> !expected.contains(path.toAbsolutePath().normalize()))
                    .forEach(path -> {
                        try {
                            Files.deleteIfExists(path);
                        } catch (Exception exception) {
                            throw new IllegalStateException(path.toString(), exception);
                        }
                    });
        }
    }

    private static void writeGroups(
            Path root,
            Set<Path> expected,
            Iterable<EmiStackGroupPlan.FormGroup> groups) throws Exception {
        for (EmiStackGroupPlan.FormGroup group : groups) {
            writeJson(root, expected, group.resourcePath(), group.toJson());
        }
    }

    private static void writeExact(
            Path root,
            Set<Path> expected,
            Iterable<EmiStackGroupPlan.ExactGroup> groups) throws Exception {
        for (EmiStackGroupPlan.ExactGroup group : groups) {
            writeJson(root, expected, group.resourcePath(), group.toJson());
        }
    }

    private static void writeJson(
            Path root,
            Set<Path> expected,
            String resourcePath,
            JsonObject json) throws Exception {
        Path file = root.resolve(resourcePath + ".json");
        Files.createDirectories(file.getParent());
        Files.writeString(
                file,
                GSON.toJson(json) + "\n",
                StandardCharsets.UTF_8);
        expected.add(file.toAbsolutePath().normalize());
    }

    private static JsonObject json(String relative) throws Exception {
        return JsonParser.parseString(
                Files.readString(ROOT.resolve(relative), StandardCharsets.UTF_8))
                .getAsJsonObject();
    }
}
