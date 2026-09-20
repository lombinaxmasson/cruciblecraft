package com.masson.cruciblecraft.compat.emi;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.Test;

import com.masson.cruciblecraft.api.material.MaterialPrefix;
import com.masson.cruciblecraft.machine.ToolMaterialRules.ToolKind;
import com.masson.cruciblecraft.machine.processing.MachineKindCatalog;
import com.masson.cruciblecraft.machine.processing.MachineTierCatalog;
import com.masson.cruciblecraft.material.prefix.MaterialPrefixCatalog;

class EmiStackGroupPlanTest {
    @Test
    void oneGroupPerPrefixAndRegexesStayFormExact() {
        List<EmiStackGroupPlan.FormGroup> groups =
                EmiStackGroupPlan.groups(MaterialPrefixCatalog.values());
        assertEquals(MaterialPrefixCatalog.values().size(), groups.size());
        assertTrue(groups.size() > 1, "prefix catalog must not be empty");

        Set<String> ids = new HashSet<>();
        Set<String> paths = new HashSet<>();
        for (EmiStackGroupPlan.FormGroup group : groups) {
            assertTrue(ids.add(group.id()), "duplicate group id " + group.id());
            assertTrue(paths.add(group.serializedPath()));
            assertEquals(EmiStackGroupPlan.TYPE, group.toJson().get("type").getAsString());
            assertEquals(
                    EmiStackGroupPlan.PRIORITY,
                    group.toJson().get("priority").getAsInt());
            assertTrue(group.toJson().has("contents"), group.id());
            assertFalse(group.toJson().has("regexes"), group.id());
        }

        for (MaterialPrefix left : MaterialPrefixCatalog.values()) {
            EmiStackGroupPlan.FormGroup group =
                    EmiStackGroupPlan.group(left.serializedName());
            String shared = "cruciblecraft:" + left.serializedName();
            String unique = "cruciblecraft:copper/" + left.serializedName();
            assertTrue(EmiStackGroupPlan.matches(group.sharedItemRegex(), shared),
                    group.sharedItemRegex() + " missed " + shared);
            assertTrue(EmiStackGroupPlan.matches(group.uniqueItemRegex(), unique),
                    group.uniqueItemRegex() + " missed " + unique);
            assertFalse(EmiStackGroupPlan.matches(group.sharedItemRegex(), unique));
            assertFalse(EmiStackGroupPlan.matches(group.uniqueItemRegex(), shared));
            for (MaterialPrefix right : MaterialPrefixCatalog.values()) {
                if (left.equals(right)) {
                    continue;
                }
                String otherShared = "cruciblecraft:" + right.serializedName();
                String otherUnique = "cruciblecraft:iron/" + right.serializedName();
                assertFalse(
                        EmiStackGroupPlan.matches(group.sharedItemRegex(), otherShared),
                        left.serializedName() + " stole shared " + right.serializedName());
                assertFalse(
                        EmiStackGroupPlan.matches(group.uniqueItemRegex(), otherUnique),
                        left.serializedName() + " stole unique " + right.serializedName());
            }
        }
    }

    @Test
    void dustDoesNotCollapseSmallTinyOrStorageDust() {
        EmiStackGroupPlan.FormGroup dust = EmiStackGroupPlan.group("dust");
        assertTrue(dust.itemIds().contains("cruciblecraft:iron/dust"));
        assertFalse(dust.itemIds().contains("cruciblecraft:iron/small_dust"));
        assertFalse(dust.itemIds().contains("cruciblecraft:dust"));
        EmiStackGroupPlan.FormGroup crushed =
                EmiStackGroupPlan.group("crushed_ore");
        assertTrue(crushed.itemIds().contains("cruciblecraft:crushed_ore"));
        assertFalse(crushed.itemIds().contains("cruciblecraft:copper/crushed_ore"));
        assertTrue(EmiStackGroupPlan.matches(
                dust.uniqueItemRegex(), "cruciblecraft:iron/dust"));
        assertFalse(EmiStackGroupPlan.matches(
                dust.uniqueItemRegex(), "cruciblecraft:iron/small_dust"));
        assertFalse(EmiStackGroupPlan.matches(
                dust.uniqueItemRegex(), "cruciblecraft:iron/tiny_dust"));
        assertFalse(EmiStackGroupPlan.matches(
                dust.uniqueItemRegex(), "cruciblecraft:iron/storage_dust"));
        assertFalse(EmiStackGroupPlan.matches(
                dust.uniqueItemRegex(), "cruciblecraft:iron/purified_dust"));
        assertFalse(EmiStackGroupPlan.matches(
                dust.sharedItemRegex(), "cruciblecraft:small_dust"));
        assertTrue(EmiStackGroupPlan.matches(
                EmiStackGroupPlan.group("crushed_ore").sharedItemRegex(),
                "cruciblecraft:crushed_ore"));
        assertTrue(EmiStackGroupPlan.matches(
                EmiStackGroupPlan.group("tool_head_pickaxe").uniqueItemRegex(),
                "cruciblecraft:empty/tool_head_pickaxe"));
        assertFalse(EmiStackGroupPlan.matches(
                EmiStackGroupPlan.group("wire").uniqueItemRegex(),
                "cruciblecraft:copper/double_wire"));
        EmiStackGroupPlan.FormGroup storagePlate =
                EmiStackGroupPlan.group("storage_plate");
        assertTrue(storagePlate.itemIds().contains("cruciblecraft:iron/storage_plate"));
        assertFalse(storagePlate.itemIds().contains("cruciblecraft:storage_plate"));
    }

    @Test
    void toolsFoldTheSharedItemWithoutStealingHeadsOrOtherKinds() {
        List<EmiStackGroupPlan.ExactGroup> tools = EmiStackGroupPlan.toolGroups();
        assertEquals(ToolKind.values().length, tools.size());
        EmiStackGroupPlan.ExactGroup pickaxe = toolGroup("pickaxe");
        assertEquals(List.of("cruciblecraft:material_pickaxe"), pickaxe.itemIds());
        assertEquals("remi:group", pickaxe.toJson().get("type").getAsString());
        assertEquals(
                "item:cruciblecraft:material_pickaxe",
                pickaxe.toJson().getAsJsonArray("contents").get(0).getAsString());
        assertTrue(EmiStackGroupPlan.matchesAny(pickaxe, "cruciblecraft:material_pickaxe"));
        assertFalse(EmiStackGroupPlan.matchesAny(
                pickaxe, "cruciblecraft:material_shovel"));
        assertFalse(EmiStackGroupPlan.matchesAny(
                pickaxe, "cruciblecraft:tool_head_pickaxe"));
        assertFalse(EmiStackGroupPlan.matchesAny(
                pickaxe, "cruciblecraft:empty/tool_head_pickaxe"));
        assertFalse(EmiStackGroupPlan.matchesAny(
                pickaxe, "cruciblecraft:material_construction_pick"));
        assertEquals(
                List.of("cruciblecraft:smithing_hammer"),
                toolGroup("smithing_hammer").itemIds());
    }

    @Test
    void machinesFoldExactKindVariantsAndSkipSingletons() {
        Map<String, EmiStackGroupPlan.ExactGroup> byPath = new HashMap<>();
        for (EmiStackGroupPlan.ExactGroup group : EmiStackGroupPlan.machineGroups()) {
            assertTrue(
                    byPath.put(group.resourcePath(), group) == null,
                    "duplicate machine group " + group.id());
            assertTrue(group.itemIds().size() >= 2, group.id());
        }
        for (MachineKindCatalog.Kind kind : MachineKindCatalog.kinds()) {
            boolean expected =
                    MachineTierCatalog.itemIdsOf(kind.id()).size() >= 2;
            assertEquals(
                    expected,
                    byPath.containsKey("machine/" + kind.id().getPath()),
                    kind.id().toString());
        }
        EmiStackGroupPlan.ExactGroup centrifuge = byPath.get("machine/centrifuge");
        assertNotNull(centrifuge);
        assertTrue(centrifuge.itemIds().contains("cruciblecraft:centrifuge"));
        assertTrue(centrifuge.itemIds().contains("cruciblecraft:steel_centrifuge"));
        assertTrue(centrifuge.itemIds().contains("cruciblecraft:titanium_centrifuge"));
        assertFalse(centrifuge.itemIds().contains("cruciblecraft:large_centrifuge"));
        assertTrue(EmiStackGroupPlan.matchesAny(
                centrifuge, "cruciblecraft:steel_centrifuge"));
        assertFalse(EmiStackGroupPlan.matchesAny(
                centrifuge, "cruciblecraft:large_centrifuge"));
        assertFalse(EmiStackGroupPlan.matchesAny(
                centrifuge, "cruciblecraft:sifter"));
        EmiStackGroupPlan.ExactGroup crusher = byPath.get("machine/bronze_crusher");
        assertNotNull(crusher);
        assertTrue(crusher.itemIds().contains("cruciblecraft:bronze_crusher"));
        assertTrue(crusher.itemIds().contains("cruciblecraft:steel_crusher"));
        assertFalse(EmiStackGroupPlan.matchesAny(
                crusher, "cruciblecraft:bronze_crusher_unused"));
    }

    @Test
    void convertersFoldExactKindVariants() {
        Map<String, EmiStackGroupPlan.ExactGroup> byPath = new HashMap<>();
        for (EmiStackGroupPlan.ExactGroup group : EmiStackGroupPlan.converterGroups()) {
            assertTrue(
                    byPath.put(group.resourcePath(), group) == null,
                    "duplicate converter group " + group.id());
            assertTrue(group.itemIds().size() >= 2, group.id());
        }
        EmiStackGroupPlan.ExactGroup turbine =
                byPath.get("converter/small_gas_turbine");
        assertNotNull(turbine);
        assertTrue(turbine.itemIds().contains(
                "cruciblecraft:bronze_small_gas_turbine"));
        assertTrue(turbine.itemIds().contains(
                "cruciblecraft:chromium_small_gas_turbine"));
        assertTrue(EmiStackGroupPlan.matchesAny(
                turbine, "cruciblecraft:bronze_small_gas_turbine"));
        assertFalse(EmiStackGroupPlan.matchesAny(
                turbine, "cruciblecraft:large_gas_turbine"));
    }

    @Test
    void formToolAndMachineGroupsDoNotStealEachOther() {
        List<EmiStackGroupPlan.FormGroup> forms =
                EmiStackGroupPlan.groups(MaterialPrefixCatalog.values());
        List<EmiStackGroupPlan.ExactGroup> exact = EmiStackGroupPlan.exactGroups();
        Set<String> ids = new HashSet<>();
        for (EmiStackGroupPlan.FormGroup group : forms) {
            assertTrue(ids.add(group.id()), group.id());
        }
        for (EmiStackGroupPlan.ExactGroup group : exact) {
            assertTrue(ids.add(group.id()), group.id());
            for (String itemId : group.itemIds()) {
                for (EmiStackGroupPlan.FormGroup form : forms) {
                    assertFalse(
                            EmiStackGroupPlan.matches(form.sharedItemRegex(), itemId)
                                    || EmiStackGroupPlan.matches(
                                            form.uniqueItemRegex(), itemId),
                            form.id() + " stole " + itemId);
                }
            }
        }
        EmiStackGroupPlan.ExactGroup pickaxe = toolGroup("pickaxe");
        String pickaxeId = pickaxe.itemIds().get(0);
        for (EmiStackGroupPlan.ExactGroup other : EmiStackGroupPlan.toolGroups()) {
            if (other.id().equals(pickaxe.id())) {
                continue;
            }
            assertFalse(
                    EmiStackGroupPlan.matchesAny(other, pickaxeId),
                    other.id() + " stole pickaxe");
        }
    }

    @Test
    void buildingFurnitureHopperCrucibleAndMoldGroupsStayKindExact() {
        Map<String, EmiStackGroupPlan.ExactGroup> byPath = catalogByPath();
        EmiStackGroupPlan.ExactGroup glass = byPath.get("building/glass");
        assertNotNull(glass);
        assertTrue(EmiStackGroupPlan.matchesAny(glass, "cruciblecraft:glass/black"));
        assertFalse(EmiStackGroupPlan.matchesAny(glass, "cruciblecraft:glass/ingot"));
        assertFalse(EmiStackGroupPlan.matchesAny(
                glass, "cruciblecraft:glass/black/slab_down"));
        assertFalse(EmiStackGroupPlan.matchesAny(glass, "cruciblecraft:glow_glass/black"));
        EmiStackGroupPlan.ExactGroup glassSlab = byPath.get("building/glass_slab");
        assertNotNull(glassSlab);
        assertTrue(EmiStackGroupPlan.matchesAny(
                glassSlab, "cruciblecraft:glass/black/slab_down"));
        assertFalse(EmiStackGroupPlan.matchesAny(glassSlab, "cruciblecraft:glass/black"));
        assertFalse(EmiStackGroupPlan.matchesAny(
                glassSlab, "cruciblecraft:glow_glass/black/slab_down"));
        EmiStackGroupPlan.ExactGroup planks = byPath.get("building/planks");
        assertNotNull(planks);
        assertTrue(EmiStackGroupPlan.matchesAny(
                planks, "cruciblecraft:gt_wood/rubberwood_planks"));
        assertFalse(EmiStackGroupPlan.matchesAny(planks, "cruciblecraft:gt_wood/crate"));
        EmiStackGroupPlan.ExactGroup slab = byPath.get("building/slab");
        assertNotNull(slab);
        assertTrue(EmiStackGroupPlan.matchesAny(
                slab, "cruciblecraft:asphalt/white/slab_up"));
        assertFalse(EmiStackGroupPlan.matchesAny(
                slab, "cruciblecraft:glass/black/slab_down"));
        EmiStackGroupPlan.ExactGroup bookshelf = byPath.get("furniture/bookshelf");
        assertNotNull(bookshelf);
        assertTrue(EmiStackGroupPlan.matchesAny(
                bookshelf, "cruciblecraft:bookshelf_7100"));
        assertTrue(EmiStackGroupPlan.matchesAny(
                bookshelf, "cruciblecraft:furniture/bookshelf_aluminium"));
        EmiStackGroupPlan.ExactGroup chest = byPath.get("furniture/chest");
        assertNotNull(chest);
        assertTrue(EmiStackGroupPlan.matchesAny(chest, "cruciblecraft:lead/chest"));
        assertFalse(EmiStackGroupPlan.matchesAny(
                chest, "cruciblecraft:safe/mechanical_lead_safe"));
        EmiStackGroupPlan.ExactGroup hopper = byPath.get("hopper/hopper");
        assertNotNull(hopper);
        assertTrue(EmiStackGroupPlan.matchesAny(hopper, "cruciblecraft:bronze_hopper"));
        assertFalse(EmiStackGroupPlan.matchesAny(
                hopper, "cruciblecraft:bronze_queue_hopper"));
        EmiStackGroupPlan.ExactGroup crucible = byPath.get("foundry/crucible");
        assertNotNull(crucible);
        assertTrue(EmiStackGroupPlan.matchesAny(
                crucible, "cruciblecraft:foundry/smelting_crucible_steel"));
        assertFalse(EmiStackGroupPlan.matchesAny(
                crucible, "cruciblecraft:foundry/crucible_crossing_octine"));
        assertFalse(EmiStackGroupPlan.matchesAny(
                crucible, "cruciblecraft:foundry/mold_steel"));
        EmiStackGroupPlan.ExactGroup mold = byPath.get("mold/ceramic");
        assertNotNull(mold);
        assertTrue(EmiStackGroupPlan.matchesAny(mold, "cruciblecraft:ingot_mold"));
        assertTrue(EmiStackGroupPlan.matchesAny(mold, "cruciblecraft:raw_ingot_mold"));
        assertFalse(EmiStackGroupPlan.matchesAny(
                mold, "cruciblecraft:foundry/mold_steel"));
        assertTrue(byPath.keySet().containsAll(Set.of(
                "building/glass",
                "building/glass_slab",
                "building/glow_glass",
                "building/glow_glass_slab",
                "building/planks",
                "building/slab",
                "building/log",
                "building/bars",
                "building/rail",
                "building/spike",
                "building/bale",
                "building/cfoam",
                "building/cfoam_fresh",
                "building/diggable",
                "building/sands",
                "building/stone",
                "furniture/bookshelf",
                "furniture/drawer",
                "furniture/safe",
                "furniture/chest",
                "hopper/hopper",
                "hopper/queue_hopper",
                "foundry/crucible",
                "foundry/mold",
                "mold/ceramic")));
        Set<String> seen = new HashSet<>();
        for (EmiStackGroupPlan.ExactGroup group : EmiStackGroupPlan.catalogGroups()) {
            assertTrue(seen.add(group.id()), group.id());
            assertTrue(group.itemIds().size() >= 2, group.id());
        }
        for (EmiStackGroupPlan.ExactGroup left : EmiStackGroupPlan.catalogGroups()) {
            for (String itemId : left.itemIds()) {
                for (EmiStackGroupPlan.ExactGroup right :
                        EmiStackGroupPlan.catalogGroups()) {
                    if (left.id().equals(right.id())) {
                        continue;
                    }
                    assertFalse(
                            EmiStackGroupPlan.matchesAny(right, itemId),
                            right.id() + " stole " + itemId);
                }
            }
        }
    }

    private static Map<String, EmiStackGroupPlan.ExactGroup> catalogByPath() {
        Map<String, EmiStackGroupPlan.ExactGroup> byPath = new HashMap<>();
        for (EmiStackGroupPlan.ExactGroup group : EmiStackGroupPlan.catalogGroups()) {
            assertTrue(
                    byPath.put(group.resourcePath(), group) == null,
                    "duplicate catalog group " + group.id());
        }
        return byPath;
    }

    private static EmiStackGroupPlan.ExactGroup toolGroup(String serializedName) {
        return EmiStackGroupPlan.toolGroups().stream()
                .filter(group -> group.resourcePath().equals("tool/" + serializedName))
                .findFirst()
                .orElseThrow();
    }
}
