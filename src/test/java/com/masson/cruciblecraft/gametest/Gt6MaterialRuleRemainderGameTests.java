package com.masson.cruciblecraft.gametest;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.masson.cruciblecraft.api.energy.EnergyType;
import com.masson.cruciblecraft.content.blockentity.ConfiguredProcessingMachineBlockEntity;
import com.masson.cruciblecraft.content.blockentity.ProcessingMachineBlockEntity;
import com.masson.cruciblecraft.gametest.support.GameTestFailures;
import com.masson.cruciblecraft.gametest.support.GameTestRequirements;
import com.masson.cruciblecraft.machine.processing.ProcessingMachineIoFaces;
import com.masson.cruciblecraft.recipe.gt.CompactRecipeFamilyProvider;
import com.masson.cruciblecraft.recipe.gt.GTRecipeQuery;
import com.masson.cruciblecraft.recipe.gt.RecipeMap;
import com.masson.cruciblecraft.registry.ModComponents;
import com.masson.cruciblecraft.registry.ModRecipeMaps;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * Host-accepted remainder rows on the material-rule maps. Samples come from
 * {@code gt6_material_rule_remainder_samples.json}; this holder does not scan
 * the maps. Fermenter has no single-block host, and rows longer than the
 * builder's tick cap only check the live match.
 */
@GameTestHolder(Gt6MaterialRuleRemainderGameTests.NAMESPACE)
@PrefixGameTestTemplate(false)
public final class Gt6MaterialRuleRemainderGameTests {
    public static final String NAMESPACE = "cruciblecraft_wave_recipe_gt6_material_rule_remainder";
    private static final String TEMPLATE = "empty";
    private static final BlockPos MACHINE = new BlockPos(1, 2, 1);
    private static final int SAMPLE_SLACK_TICKS = 40;

    private Gt6MaterialRuleRemainderGameTests() {}

    @GameTest(template = TEMPLATE, timeoutTicks = 200)
    public static void ruleRemainderGroupsStayOnDemand(GameTestHelper helper) {
        for (MapSamples map : load()) {
            RecipeMap recipes = requireMap(helper, map.targetMap());
            String familyId = CompactRecipeFamilyProvider.familyId(
                    recipes.id(), id(map.publicationGroup()));
            int logical = 0;
            int eager = 0;
            int cache = -1;
            int matched = 0;
            for (RecipeMap.RecipeFamily family : recipes.families()) {
                if (!family.familyId().equals(familyId)
                        && !family.familyId().startsWith(familyId)) {
                    continue;
                }
                logical += family.logicalRecipeCount();
                eager += family.eagerRecipeCount();
                cache = family.cacheCeiling();
                matched++;
            }
            helper.assertTrue(
                    matched > 0
                            && logical == map.publishedRows()
                            && eager == 0
                            && cache == 16,
                    map.targetMap() + " publication is not the on-demand rule-remainder group: logical="
                            + logical + " eager=" + eager + " cache=" + cache
                            + " expected=" + map.publishedRows());
        }
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 12000)
    public static void ruleRemainderSamplesRun(GameTestHelper helper) {
        SampleRun run = new SampleRun(load());
        helper.onEachTick(() -> run.tick(helper));
        helper.succeedWhen(() -> helper.assertTrue(run.finished(), run.status()));
    }

    private static RecipeMap requireMap(GameTestHelper helper, String target) {
        ResourceLocation parsed = id(target);
        for (RecipeMap map : ModRecipeMaps.ALL) {
            if (map.id().equals(parsed)) {
                return map;
            }
        }
        GameTestFailures.fail(helper, "missing recipe map " + target);
        throw new IllegalStateException(target);
    }

    private static List<MapSamples> load() {
        InputStream stream = Gt6MaterialRuleRemainderGameTests.class.getResourceAsStream(
                "/gt6_material_rule_remainder_samples.json");
        if (stream == null) {
            throw new IllegalStateException("missing gt6_material_rule_remainder_samples.json");
        }
        try (InputStreamReader reader = new InputStreamReader(stream, StandardCharsets.UTF_8)) {
            JsonArray maps = JsonParser.parseReader(reader).getAsJsonObject().getAsJsonArray("maps");
            List<MapSamples> loaded = new ArrayList<>();
            for (JsonElement element : maps) {
                loaded.add(MapSamples.parse(element.getAsJsonObject()));
            }
            return List.copyOf(loaded);
        } catch (IOException failure) {
            throw new IllegalStateException("unreadable rule-remainder samples", failure);
        }
    }

    private static ResourceLocation id(String raw) {
        int colon = raw.indexOf(':');
        return ResourceLocation.fromNamespaceAndPath(
                raw.substring(0, colon), raw.substring(colon + 1));
    }

    private static FluidStack fluid(GameTestHelper helper, String raw, int amount) {
        Fluid fluid = BuiltInRegistries.FLUID.get(id(raw));
        helper.assertTrue(fluid != Fluids.EMPTY, "missing fluid " + raw);
        return new FluidStack(fluid, amount);
    }

    private record StackSpec(String id, int count, String material, int circuit) {
        private static StackSpec parse(JsonObject object) {
            return new StackSpec(
                    object.get("id").getAsString(),
                    object.get("count").getAsInt(),
                    object.has("material") ? object.get("material").getAsString() : "",
                    object.has("circuit") ? object.get("circuit").getAsInt() : -1);
        }

        private ItemStack stack(GameTestHelper helper) {
            Item item = BuiltInRegistries.ITEM.get(Gt6MaterialRuleRemainderGameTests.id(id));
            helper.assertTrue(item != Items.AIR, "missing item " + id);
            return withComponents(new ItemStack(item, count));
        }

        private ItemStack unchecked() {
            return withComponents(new ItemStack(
                    BuiltInRegistries.ITEM.get(Gt6MaterialRuleRemainderGameTests.id(id)), count));
        }

        private ItemStack withComponents(ItemStack stack) {
            if (!material.isEmpty()) {
                stack.set(ModComponents.PREFIX_MATERIAL.get(), material);
            }
            if (circuit >= 0) {
                stack.set(ModComponents.CIRCUIT_CONFIG.get(), circuit);
            }
            return stack;
        }
    }

    private record FluidSpec(String id, int amount) {
        private static FluidSpec parse(JsonObject object) {
            return new FluidSpec(object.get("id").getAsString(), object.get("amount").getAsInt());
        }
    }

    private record Sample(
            String role,
            int duration,
            int eut,
            boolean execute,
            List<StackSpec> items,
            List<FluidSpec> fluids,
            List<StackSpec> guaranteedOutputs) {
        private static Sample parse(JsonObject object) {
            return new Sample(
                    object.get("role").getAsString(),
                    object.get("duration").getAsInt(),
                    object.get("eut").getAsInt(),
                    object.get("execute").getAsBoolean(),
                    specs(object.getAsJsonArray("items")),
                    fluids(object.getAsJsonArray("fluids")),
                    specs(object.getAsJsonArray("guaranteed_outputs")));
        }

        private static List<StackSpec> specs(JsonArray array) {
            List<StackSpec> specs = new ArrayList<>();
            for (JsonElement element : array) {
                specs.add(StackSpec.parse(element.getAsJsonObject()));
            }
            return List.copyOf(specs);
        }

        private static List<FluidSpec> fluids(JsonArray array) {
            List<FluidSpec> specs = new ArrayList<>();
            for (JsonElement element : array) {
                specs.add(FluidSpec.parse(element.getAsJsonObject()));
            }
            return List.copyOf(specs);
        }
    }

    private record MapSamples(
            String machine,
            String block,
            String targetMap,
            String publicationGroup,
            int publishedRows,
            List<Sample> samples) {
        private static MapSamples parse(JsonObject object) {
            List<Sample> samples = new ArrayList<>();
            for (JsonElement element : object.getAsJsonArray("samples")) {
                samples.add(Sample.parse(element.getAsJsonObject()));
            }
            JsonElement block = object.get("block");
            return new MapSamples(
                    object.get("machine").getAsString(),
                    block == null || block.isJsonNull() ? "" : block.getAsString(),
                    object.get("target_map").getAsString(),
                    object.get("publication_group").getAsString(),
                    object.get("published_rows").getAsInt(),
                    List.copyOf(samples));
        }
    }

    private static final class SampleRun {
        private final List<MapSamples> maps;
        private int mapIndex;
        private int sampleIndex;
        private boolean placed;
        private int waited;
        private boolean finished;
        private String status = "rule-remainder samples";
        private ProcessingMachineBlockEntity machine;
        private int inserted;

        private SampleRun(List<MapSamples> maps) {
            this.maps = maps;
        }

        private boolean finished() {
            return finished;
        }

        private String status() {
            return status;
        }

        private void tick(GameTestHelper helper) {
            if (finished) {
                return;
            }
            if (mapIndex >= maps.size()) {
                finished = true;
                return;
            }
            MapSamples map = maps.get(mapIndex);
            Sample sample = map.samples().get(sampleIndex);
            status = map.machine() + " " + sample.role();
            if (!placed) {
                if (place(helper, map, sample)) {
                    placed = true;
                    waited = 0;
                } else {
                    advance();
                }
                return;
            }
            waited++;
            if (outputsReady(sample)) {
                advance();
                return;
            }
            if (machine.spec().energy().type() != EnergyType.TIME) {
                long packet = Math.max(sample.eut(), machine.powerDemandLong());
                if (machine instanceof ConfiguredProcessingMachineBlockEntity configured) {
                    packet = Math.min(
                            configured.variant().tierBand().inputMaximum(),
                            Math.max(packet, configured.variant().tierBand().inputNominal()));
                }
                packet = Math.max(1L, Math.min(packet, machine.spec().energy().maxPacket()));
                machine.insert(
                        machine.spec().energy().type(),
                        packet,
                        1L,
                        ProcessingMachineIoFaces.energy(machine.spec(), machine.facing()),
                        false);
            }
            int limit = Math.max(sample.duration(), machine.duration()) + SAMPLE_SLACK_TICKS;
            if (waited > limit) {
                List<ItemStack> slots = machine.spec().items().inputs().stream()
                        .map(slot -> machine.inventory().getStackInSlot(slot))
                        .toList();
                boolean slotMatch = requireMap(helper, map.targetMap())
                        .findMatch(new GTRecipeQuery(slots, List.of()))
                        .isPresent();
                GameTestFailures.fail(
                        helper,
                        status + " did not finish progress " + machine.progress()
                                + "/" + machine.duration() + " " + machine.pausedReason()
                                + " slots=" + slots + " slotMatch=" + slotMatch
                                + " all=" + java.util.stream.IntStream
                                        .range(0, machine.inventory().getSlots())
                                        .mapToObj(slot -> machine.inventory().getStackInSlot(slot))
                                        .toList());
            }
        }

        /** Returns whether the sample runs in a machine; match-only samples return false. */
        private boolean place(GameTestHelper helper, MapSamples map, Sample sample) {
            List<ItemStack> items = new ArrayList<>();
            List<ItemStack> stored = new ArrayList<>();
            for (StackSpec spec : sample.items()) {
                ItemStack stack = spec.stack(helper);
                items.add(stack);
                stored.add(stack.copy());
            }
            List<FluidStack> fluids = new ArrayList<>();
            for (FluidSpec spec : sample.fluids()) {
                fluids.add(fluid(helper, spec.id(), spec.amount()));
            }
            RecipeMap recipes = requireMap(helper, map.targetMap());
            RecipeMap.Match match = GameTestRequirements.requirePresent(
                    helper,
                    recipes.findMatch(new GTRecipeQuery(items, fluids)),
                    status + " did not match");
            for (StackSpec expected : sample.guaranteedOutputs()) {
                ItemStack target = expected.unchecked();
                helper.assertTrue(
                        match.recipe().itemOutputs().stream().anyMatch(
                                stack -> ItemStack.isSameItemSameComponents(stack, target)),
                        status + " matched " + match.id() + " without " + target);
            }
            if (!sample.execute()) {
                return false;
            }
            Block block = BuiltInRegistries.BLOCK.get(id(map.block()));
            helper.assertTrue(block != Blocks.AIR, "no block for " + map.block());
            helper.setBlock(MACHINE, Blocks.AIR);
            helper.setBlock(MACHINE, block.defaultBlockState());
            machine = GameTestRequirements.requireBlockEntity(
                    helper, MACHINE, ProcessingMachineBlockEntity.class);
            List<Integer> inputs = machine.spec().items().inputs();
            helper.assertTrue(
                    stored.size() <= inputs.size(), status + " has more inputs than slots");
            inserted = 0;
            for (int index = 0; index < stored.size(); index++) {
                int slot = inputs.get(index);
                ItemStack left = machine.inventory().insertItem(slot, stored.get(index), false);
                helper.assertTrue(left.isEmpty(), status + " rejected slot " + slot);
                if (inserted == 0 && sample.items().get(index).circuit() < 0) {
                    inserted = slot + 1;
                }
            }
            for (FluidStack stack : fluids) {
                int filled = machine.fillFluid(stack.copy(), IFluidHandler.FluidAction.EXECUTE);
                helper.assertTrue(filled == stack.getAmount(), status + " rejected fluid");
            }
            helper.assertTrue(
                    !sample.guaranteedOutputs().isEmpty() || inserted > 0,
                    status + " has neither an item output nor a consumed item input");
            return true;
        }

        private boolean outputsReady(Sample sample) {
            if (sample.guaranteedOutputs().isEmpty()) {
                int slot = inserted - 1;
                int index = machine.spec().items().inputs().indexOf(slot);
                return machine.inventory().getStackInSlot(slot).getCount()
                        < sample.items().get(index).count();
            }
            for (StackSpec expected : sample.guaranteedOutputs()) {
                int produced = 0;
                ItemStack target = expected.unchecked();
                for (int slot : machine.spec().items().outputs()) {
                    ItemStack stack = machine.inventory().getStackInSlot(slot);
                    if (ItemStack.isSameItemSameComponents(stack, target)) {
                        produced += stack.getCount();
                    }
                }
                if (produced < expected.count()) {
                    return false;
                }
            }
            return true;
        }

        private void advance() {
            placed = false;
            machine = null;
            sampleIndex++;
            if (sampleIndex >= maps.get(mapIndex).samples().size()) {
                sampleIndex = 0;
                mapIndex++;
            }
        }
    }
}
