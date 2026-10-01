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
import com.masson.cruciblecraft.api.material.MaterialLookup;
import com.masson.cruciblecraft.api.material.MaterialPrefixes;
import com.masson.cruciblecraft.content.block.GtHostedOreBlock;
import com.masson.cruciblecraft.content.block.OreStoneHost;
import com.masson.cruciblecraft.content.block.ProcessingMachineBlock;
import com.masson.cruciblecraft.content.blockentity.AnvilBlockEntity;
import com.masson.cruciblecraft.content.blockentity.ConfiguredProcessingMachineBlockEntity;
import com.masson.cruciblecraft.content.blockentity.ProcessingMachineBlockEntity;
import com.masson.cruciblecraft.gametest.support.GameTestFailures;
import com.masson.cruciblecraft.gametest.support.GameTestRequirements;
import com.masson.cruciblecraft.gametest.support.PublicationPolicyCounts;
import com.masson.cruciblecraft.machine.processing.ProcessingMachineIoFaces;
import com.masson.cruciblecraft.recipe.AnvilMode;
import com.masson.cruciblecraft.recipe.gt.CompactRecipeFamilyProvider;
import com.masson.cruciblecraft.recipe.gt.GTRecipe;
import com.masson.cruciblecraft.recipe.gt.GTRecipeQuery;
import com.masson.cruciblecraft.recipe.gt.RecipeMap;
import com.masson.cruciblecraft.registry.ModBlocks;
import com.masson.cruciblecraft.registry.ModComponents;
import com.masson.cruciblecraft.registry.ModRecipeMaps;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
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
 * Twelve prefix-regular maps. Samples come from
 * {@code gt6_prefix_regular_samples.json}; this holder does not scan the maps.
 */
@GameTestHolder(Gt6PrefixRegularBulkGameTests.NAMESPACE)
@PrefixGameTestTemplate(false)
public final class Gt6PrefixRegularBulkGameTests {
    public static final String NAMESPACE = "cruciblecraft_content";
    private static final String TEMPLATE = "empty";
    private static final BlockPos MACHINE = new BlockPos(1, 2, 1);
    private static final int SAMPLE_SLACK_TICKS = 40;

    private Gt6PrefixRegularBulkGameTests() {}

    @GameTest(template = TEMPLATE, timeoutTicks = 100)
    public static void prefixRegularGroupsStayOnDemand(GameTestHelper helper) {
        for (MapSamples map : load()) {
            RecipeMap recipes = requireMap(helper, map.targetMap());
            ResourceLocation group = id(map.publicationGroup());
            RecipeMap.RecipeFamily family = recipes.family(
                    CompactRecipeFamilyProvider.familyId(recipes.id(), group)).orElse(null);
            helper.assertTrue(
                    family != null
                            && family.logicalRecipeCount() > 0
                            && family.eagerRecipeCount() == 0
                            && family.cacheCeiling() == 16,
                    map.targetMap() + " publication is not on-demand");
        }
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 3000)
    public static void prefixRegularSamplesRun(GameTestHelper helper) {
        SampleRun run = new SampleRun(load());
        helper.onEachTick(() -> run.tick(helper));
        helper.succeedWhen(() -> helper.assertTrue(run.finished(), run.status()));
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 100)
    public static void sifterByproductTinyDustMatches(GameTestHelper helper) {
        ResourceLocation group = id("cruciblecraft:sifter/prefix-regular");
        RecipeMap recipes = requireMap(helper, "cruciblecraft:sifter");
        RecipeMap.RecipeFamily family = recipes.family(
                CompactRecipeFamilyProvider.familyId(recipes.id(), group)).orElse(null);
        int expected = PublicationPolicyCounts.relationCount(helper, group);
        helper.assertTrue(
                family != null && family.logicalRecipeCount() == expected && expected > 2377,
                "sifter publication rows != policy " + expected);
        ItemStack input = GtHostedOreBlock.item(
                ModBlocks.GT_HOSTED_ORE.get().defaultBlockState()
                        .setValue(GtHostedOreBlock.HOST, OreStoneHost.GRAVEL),
                "anti_silver");
        RecipeMap.Match match = recipes.findMatch(new GTRecipeQuery(List.of(input), List.of()))
                .orElse(null);
        helper.assertTrue(match != null, "anti-silver gravel ore did not match a sifter row");
        ItemStack washed = MaterialLookup.stack(
                "anti_silver", MaterialPrefixes.WASHED_CRUSHED_ORE);
        ItemStack tiny = MaterialLookup.stack("anti_silver", MaterialPrefixes.TINY_DUST);
        GTRecipe recipe = match.recipe();
        int washedCount = 0;
        int tinyCount = 0;
        for (int index = 0; index < recipe.itemOutputs().size(); index++) {
            ItemStack stack = recipe.itemOutputs().get(index);
            int chance = recipe.outputChances().get(index);
            if (ItemStack.isSameItemSameComponents(stack, washed)
                    && chance == GTRecipe.GUARANTEED_CHANCE) {
                washedCount += stack.getCount();
            }
            if (ItemStack.isSameItemSameComponents(stack, tiny)) {
                tinyCount += stack.getCount();
            }
        }
        helper.assertTrue(
                washedCount == 2 && tinyCount == 3,
                "anti-silver sifter outputs washed=" + washedCount + " tiny=" + tinyCount);
        helper.succeed();
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
        InputStream stream = Gt6PrefixRegularBulkGameTests.class.getResourceAsStream(
                "/gt6_prefix_regular_samples.json");
        if (stream == null) {
            throw new IllegalStateException("missing gt6_prefix_regular_samples.json");
        }
        try (InputStreamReader reader = new InputStreamReader(stream, StandardCharsets.UTF_8)) {
            JsonArray maps = JsonParser.parseReader(reader).getAsJsonObject().getAsJsonArray("maps");
            List<MapSamples> loaded = new ArrayList<>();
            for (JsonElement element : maps) {
                loaded.add(MapSamples.parse(element.getAsJsonObject()));
            }
            return List.copyOf(loaded);
        } catch (IOException failure) {
            throw new IllegalStateException("unreadable prefix-regular samples", failure);
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

    private static Block machineBlock(String machine) {
        return switch (machine) {
            case "cutter" -> ModBlocks.CUTTER.get();
            case "lathe" -> ModBlocks.LATHE.get();
            case "rollingmill" -> ModBlocks.ROLLINGMILL.get();
            case "press" -> ModBlocks.PRESS.get();
            case "crusher" -> ModBlocks.BRONZE_CRUSHER.get();
            case "mortar" -> ModBlocks.MORTAR.get();
            case "rollbender" -> ModBlocks.ROLLBENDER.get();
            case "wiremill" -> ModBlocks.WIREMILL.get();
            case "sifter" -> ModBlocks.SIFTER.get();
            case "welder" -> ModBlocks.tieredProcessingBlocksById()
                    .get(ResourceLocation.fromNamespaceAndPath(
                            "cruciblecraft", "laser_welder"))
                    .get();
            case "anvil_bend_big", "anvil_bend_small" ->
                    ModBlocks.mteInPlaceBlocksById().get(id("cruciblecraft:steel/anvil")).get();
            default -> null;
        };
    }

    private record StackSpec(String id, int count, String material) {
        private static StackSpec parse(JsonObject object) {
            String material = object.has("material")
                    ? object.get("material").getAsString()
                    : "";
            return new StackSpec(
                    object.get("id").getAsString(),
                    object.get("count").getAsInt(),
                    material);
        }

        private ItemStack stack(GameTestHelper helper) {
            Item item = BuiltInRegistries.ITEM.get(Gt6PrefixRegularBulkGameTests.id(id));
            helper.assertTrue(item != Items.AIR, "missing item " + id);
            return withMaterial(new ItemStack(item, count));
        }

        private ItemStack unchecked() {
            return withMaterial(new ItemStack(
                    BuiltInRegistries.ITEM.get(Gt6PrefixRegularBulkGameTests.id(id)), count));
        }

        private ItemStack withMaterial(ItemStack stack) {
            if (!material.isEmpty()) {
                stack.set(ModComponents.PREFIX_MATERIAL.get(), material);
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
            int specialValue,
            List<StackSpec> items,
            List<FluidSpec> fluids,
            List<StackSpec> guaranteedOutputs) {
        private static Sample parse(JsonObject object) {
            return new Sample(
                    object.get("role").getAsString(),
                    object.get("duration").getAsInt(),
                    object.get("eut").getAsInt(),
                    object.get("special_value").getAsInt(),
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
            String machine, String targetMap, String publicationGroup, List<Sample> samples) {
        private static MapSamples parse(JsonObject object) {
            List<Sample> samples = new ArrayList<>();
            for (JsonElement element : object.getAsJsonArray("samples")) {
                samples.add(Sample.parse(element.getAsJsonObject()));
            }
            return new MapSamples(
                    object.get("machine").getAsString(),
                    object.get("target_map").getAsString(),
                    object.get("publication_group").getAsString(),
                    List.copyOf(samples));
        }

        private boolean anvil() {
            return machine.startsWith("anvil_");
        }

        private AnvilMode anvilMode() {
            return "anvil_bend_small".equals(machine)
                    ? AnvilMode.BEND_SMALL
                    : AnvilMode.BEND_BIG;
        }
    }

    private static final class SampleRun {
        private final List<MapSamples> maps;
        private int mapIndex;
        private int sampleIndex;
        private boolean placed;
        private int waited;
        private boolean finished;
        private String status = "prefix regular samples";
        private ProcessingMachineBlockEntity machine;
        private AnvilBlockEntity anvil;
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
                place(helper, map, sample);
                placed = true;
                waited = 0;
                return;
            }
            waited++;
            if (map.anvil()) {
                tickAnvil(helper, map, sample);
            } else if (outputsReady(sample)) {
                advance();
            } else {
                long packet = Math.max(sample.eut(), machine.powerDemandLong());
                if (machine instanceof ConfiguredProcessingMachineBlockEntity configured) {
                    packet = Math.min(
                            configured.variant().tierBand().inputMaximum(),
                            Math.max(packet, configured.variant().tierBand().inputNominal()));
                }
                packet = Math.max(1L, Math.min(packet, machine.spec().energy().maxPacket()));
                var energySide = "welder".equals(map.machine())
                        ? Direction.UP
                        : ProcessingMachineIoFaces.energy(machine.spec(), machine.facing());
                machine.insert(
                        machine.spec().energy().type(),
                        packet,
                        1L,
                        energySide,
                        false);
                int limit = Math.max(sample.duration(), machine.duration()) + SAMPLE_SLACK_TICKS;
                if (waited > limit) {
                    GameTestFailures.fail(
                            helper,
                            status + " did not finish progress " + machine.progress()
                                    + "/" + machine.duration());
                }
            }
        }

        private void place(GameTestHelper helper, MapSamples map, Sample sample) {
            Block block = machineBlock(map.machine());
            helper.assertTrue(block != null, "no block for " + map.machine());
            helper.setBlock(MACHINE, Blocks.AIR);
            var state = block.defaultBlockState();
            if ("welder".equals(map.machine())
                    && state.hasProperty(ProcessingMachineBlock.FACING)) {
                state = state.setValue(ProcessingMachineBlock.FACING, Direction.NORTH);
            }
            helper.setBlock(MACHINE, state);
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
            GameTestRequirements.requirePresent(
                    helper,
                    recipes.findMatch(new GTRecipeQuery(items, fluids)),
                    status + " did not match");
            if (map.anvil()) {
                anvil = GameTestRequirements.requireBlockEntity(
                        helper, MACHINE, AnvilBlockEntity.class);
                machine = null;
                for (int slot = 0; slot < stored.size(); slot++) {
                    helper.assertTrue(
                            anvil.insertOrMerge(slot, stored.get(slot)) > 0,
                            status + " rejected input");
                }
                return;
            }
            anvil = null;
            machine = GameTestRequirements.requireBlockEntity(
                    helper, MACHINE, ProcessingMachineBlockEntity.class);
            inserted = stored.isEmpty() ? 0 : stored.getFirst().getCount();
            for (int slot = 0; slot < stored.size(); slot++) {
                ItemStack left = machine.inventory().insertItem(slot, stored.get(slot), false);
                helper.assertTrue(left.isEmpty(), status + " rejected slot " + slot);
            }
            for (FluidStack stack : fluids) {
                int filled = machine.fillFluid(stack.copy(), IFluidHandler.FluidAction.EXECUTE);
                helper.assertTrue(filled == stack.getAmount(), status + " rejected fluid");
            }
        }

        private void tickAnvil(GameTestHelper helper, MapSamples map, Sample sample) {
            if (outputsReady(sample)) {
                advance();
                return;
            }
            if (waited > 8) {
                GameTestFailures.fail(helper, status + " did not finish");
                return;
            }
            AnvilBlockEntity.StrikeResult result = GameTestRequirements.requirePresent(
                    helper,
                    anvil.strike(map.anvilMode(), Integer.MAX_VALUE),
                    status + " strike missed");
            if (result.completed() && !outputsReady(sample)) {
                GameTestFailures.fail(helper, status + " produced the wrong item");
            }
        }

        private boolean outputsReady(Sample sample) {
            if (anvil != null) {
                for (StackSpec expected : sample.guaranteedOutputs()) {
                    if (!anvilHas(expected)) {
                        return false;
                    }
                }
                return !sample.guaranteedOutputs().isEmpty();
            }
            if (sample.guaranteedOutputs().isEmpty()) {
                return machine.inventory().getStackInSlot(0).getCount() < inserted;
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

        private boolean anvilHas(StackSpec expected) {
            ItemStack target = expected.unchecked();
            for (int slot = 0; slot < 2; slot++) {
                ItemStack stack = anvil.workpiece(slot);
                if (ItemStack.isSameItemSameComponents(stack, target)
                        && stack.getCount() >= expected.count()) {
                    return true;
                }
            }
            return false;
        }

        private void advance() {
            placed = false;
            machine = null;
            anvil = null;
            sampleIndex++;
            if (sampleIndex >= maps.get(mapIndex).samples().size()) {
                sampleIndex = 0;
                mapIndex++;
            }
        }
    }
}
