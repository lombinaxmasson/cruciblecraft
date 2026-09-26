package com.masson.cruciblecraft.gametest;

import com.masson.cruciblecraft.CrucibleCraft;
import com.masson.cruciblecraft.api.material.MaterialLookup;
import com.masson.cruciblecraft.api.material.MaterialPrefixes;
import com.masson.cruciblecraft.content.block.GtBushBlock;
import com.masson.cruciblecraft.content.block.HopperBlock;
import com.masson.cruciblecraft.content.block.ProcessingMachineBlock;
import com.masson.cruciblecraft.content.blockentity.GtBushBlockEntity;
import com.masson.cruciblecraft.content.blockentity.MixingBowlBlockEntity;
import com.masson.cruciblecraft.content.item.tool.ElectricToolCharge;
import com.masson.cruciblecraft.machine.ToolMaterialRules.ToolKind;
import com.masson.cruciblecraft.recipe.gt.GTRecipe;
import com.masson.cruciblecraft.recipe.gt.RecipeMap;
import com.masson.cruciblecraft.registry.ModBlocks;
import com.masson.cruciblecraft.registry.ModItems;
import com.masson.cruciblecraft.registry.ModRecipeMaps;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * Isolated GT6 magnifying-glass, mixing-bowl, hand-drill, grafter, and
 * electric placer remainder. Run with
 * {@code -PgameTestGrid=content}.
 */
@GameTestHolder(Gt6ToolWorldBehaviorGameTests.NAMESPACE)
@PrefixGameTestTemplate(false)
public final class Gt6ToolWorldBehaviorGameTests {
    public static final String NAMESPACE = "cruciblecraft_content";
    private static final String TEMPLATE = "empty";
    private static final BlockPos POS = new BlockPos(2, 2, 2);

    private Gt6ToolWorldBehaviorGameTests() {}

    @GameTest(template = TEMPLATE, timeoutTicks = 40)
    public static void magnifyingGlassInspectsHopper(GameTestHelper helper) {
        helper.setBlock(POS, hopperBlock("lead_hopper"));
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        ItemStack glass = ModItems.MATERIAL_MAGNIFYING_GLASS.get().variant("iron");
        player.setItemInHand(InteractionHand.MAIN_HAND, glass);
        helper.assertTrue(
                useOn(helper, player, glass, Direction.UP).consumesAction(),
                "magnifying glass did not inspect the hopper");
        player.discard();
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 40)
    public static void magnifyingGlassInspectsProcessingMachine(GameTestHelper helper) {
        helper.setBlock(
                POS,
                ModBlocks.MIXER.get()
                        .defaultBlockState()
                        .setValue(ProcessingMachineBlock.FACING, Direction.NORTH));
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        ItemStack glass = ModItems.MATERIAL_MAGNIFYING_GLASS.get().variant("iron");
        player.setItemInHand(InteractionHand.MAIN_HAND, glass);
        helper.assertTrue(
                useOn(helper, player, glass, Direction.NORTH).consumesAction(),
                "magnifying glass did not inspect the mixer");
        player.discard();
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 40)
    public static void handDrillReinforcesConcrete(GameTestHelper helper) {
        var concrete = ModBlocks.bathRemainderBlockObjectBlocksById().get(
                ResourceLocation.fromNamespaceAndPath(
                        CrucibleCraft.MODID, "concrete/concrete"));
        var reinforced = ModBlocks.bathRemainderBlockObjectBlocksById().get(
                ResourceLocation.fromNamespaceAndPath(
                        CrucibleCraft.MODID, "concrete_reinforced/reinforced_concrete"));
        helper.assertTrue(concrete != null && reinforced != null, "concrete identities missing");
        helper.setBlock(POS, concrete.get());
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        ItemStack drill = ModItems.MATERIAL_HAND_DRILL.get().variant("iron");
        player.setItemInHand(InteractionHand.MAIN_HAND, drill);
        player.getInventory().add(MaterialLookup.stack("iron", MaterialPrefixes.ROD));
        helper.assertTrue(
                useOn(helper, player, drill, Direction.UP).consumesAction(),
                "hand drill did not reinforce concrete");
        helper.assertTrue(
                helper.getBlockState(POS).is(reinforced.get()),
                "hand drill left unreinforced concrete");
        helper.assertTrue(
                player.getInventory().countItem(
                        MaterialLookup.item("iron", MaterialPrefixes.ROD).orElseThrow())
                        == 0,
                "hand drill did not consume the iron rod");
        player.discard();
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 40)
    public static void mixingBowlMixesOnTopWithHandMixer(GameTestHelper helper) {
        helper.setBlock(POS, ModBlocks.MIXING_BOWL.get());
        MixingBowlBlockEntity bowl = helper.getBlockEntity(POS);
        GTRecipe recipe = mixerRecipe();
        loadBowl(bowl, recipe);
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        ItemStack mixer = chargedMixer();
        player.setItemInHand(InteractionHand.MAIN_HAND, mixer);
        helper.assertTrue(
                !useOn(helper, player, mixer, Direction.NORTH).consumesAction(),
                "mixer accepted a non-top face");
        helper.assertTrue(
                useOn(helper, player, mixer, Direction.UP).consumesAction(),
                "mixer did not mix on the top face");
        helper.assertTrue(
                bowlHasOutput(bowl),
                "mixing bowl produced no output");
        helper.assertTrue(
                ElectricToolCharge.charge(mixer) < ElectricToolCharge.capacity(mixer),
                "hand mixer did not spend EU");
        player.discard();
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 40)
    public static void magnifyingGlassInspectsMixingBowl(GameTestHelper helper) {
        helper.setBlock(POS, ModBlocks.MIXING_BOWL.get());
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        ItemStack glass = ModItems.MATERIAL_MAGNIFYING_GLASS.get().variant("iron");
        player.setItemInHand(InteractionHand.MAIN_HAND, glass);
        helper.assertTrue(
                useOn(helper, player, glass, Direction.UP).consumesAction(),
                "magnifying glass did not inspect the mixing bowl");
        player.discard();
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 40)
    public static void grafterHarvestsRipeBush(GameTestHelper helper) {
        helper.setBlock(POS, Blocks.DIRT);
        helper.setBlock(
                POS.above(),
                ModBlocks.GT_BUSH.get()
                        .defaultBlockState()
                        .setValue(GtBushBlock.FACING, Direction.DOWN)
                        .setValue(GtBushBlock.STAGE, GtBushBlock.MAX_STAGE));
        GtBushBlockEntity bush = helper.getBlockEntity(POS.above());
        bush.setBerry(new ItemStack(Items.SWEET_BERRIES));
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        ItemStack cutter = ModItems.MATERIAL_BRANCH_CUTTER.get().variant("iron");
        player.setItemInHand(InteractionHand.MAIN_HAND, cutter);
        helper.assertTrue(
                useOn(helper, POS.above(), player, cutter, Direction.UP).consumesAction(),
                "grafter did not harvest the ripe bush");
        helper.assertTrue(
                helper.getBlockState(POS.above()).getValue(GtBushBlock.STAGE) == 0,
                "grafter left the bush ripe");
        player.discard();
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 40)
    public static void miningDrillPlacesTorchFromInventory(GameTestHelper helper) {
        helper.setBlock(POS, Blocks.STONE);
        helper.setBlock(POS.north(), Blocks.AIR);
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        ItemStack drill = charged(
                ToolKind.MINING_DRILL_LV, "iron", 10_000L, 32L);
        player.setItemInHand(InteractionHand.MAIN_HAND, drill);
        player.getInventory().add(new ItemStack(Items.TORCH, 1));
        helper.assertTrue(
                useOn(helper, player, drill, Direction.NORTH).consumesAction(),
                "mining drill did not place a torch");
        helper.assertTrue(
                helper.getBlockState(POS.north()).is(Blocks.TORCH)
                        || helper.getBlockState(POS.north()).is(Blocks.WALL_TORCH),
                "mining drill did not plant the inventory torch");
        player.discard();
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 40)
    public static void miningDrillPlugsAdjacentWater(GameTestHelper helper) {
        helper.setBlock(POS, Blocks.STONE);
        helper.setBlock(POS.north(), Blocks.WATER);
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        ItemStack drill = charged(
                ToolKind.MINING_DRILL_LV, "iron", 10_000L, 32L);
        player.setItemInHand(InteractionHand.MAIN_HAND, drill);
        player.getInventory().add(new ItemStack(Items.COBBLESTONE, 1));
        helper.assertTrue(
                useOn(helper, player, drill, Direction.NORTH).consumesAction(),
                "mining drill did not plug the leak");
        helper.assertTrue(
                helper.getBlockState(POS.north()).is(Blocks.COBBLESTONE),
                "mining drill did not place cobblestone into water");
        player.discard();
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 40)
    public static void chainsawPlacesSaplingFromInventory(GameTestHelper helper) {
        helper.setBlock(POS, Blocks.DIRT);
        helper.setBlock(POS.above(), Blocks.AIR);
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        ItemStack saw = charged(
                ToolKind.CHAINSAW_LV, "iron", 10_000L, 32L);
        player.setItemInHand(InteractionHand.MAIN_HAND, saw);
        player.getInventory().add(new ItemStack(Items.OAK_SAPLING, 1));
        helper.assertTrue(
                useOn(helper, player, saw, Direction.UP).consumesAction(),
                "chainsaw did not place a sapling");
        helper.assertTrue(
                helper.getBlockState(POS.above()).is(Blocks.OAK_SAPLING),
                "chainsaw did not plant the inventory sapling");
        player.discard();
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 40)
    public static void chainsawPlacesWorkbenchFromInventory(GameTestHelper helper) {
        helper.setBlock(POS, Blocks.STONE);
        helper.setBlock(POS.north(), Blocks.AIR);
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        ItemStack saw = charged(
                ToolKind.CHAINSAW_LV, "iron", 10_000L, 32L);
        player.setItemInHand(InteractionHand.MAIN_HAND, saw);
        player.getInventory().add(new ItemStack(Items.CRAFTING_TABLE, 1));
        helper.assertTrue(
                useOn(helper, player, saw, Direction.NORTH).consumesAction(),
                "chainsaw did not place a workbench");
        helper.assertTrue(
                helper.getBlockState(POS.north()).is(Blocks.CRAFTING_TABLE),
                "chainsaw did not plant the inventory workbench");
        player.discard();
        helper.succeed();
    }

    private static net.minecraft.world.InteractionResult useOn(
            GameTestHelper helper,
            Player player,
            ItemStack stack,
            Direction face) {
        return useOn(helper, POS, player, stack, face);
    }

    private static net.minecraft.world.InteractionResult useOn(
            GameTestHelper helper,
            BlockPos pos,
            Player player,
            ItemStack stack,
            Direction face) {
        return stack.getItem().useOn(
                new UseOnContext(
                        helper.getLevel(),
                        player,
                        InteractionHand.MAIN_HAND,
                        stack,
                        hit(helper, pos, face)));
    }

    private static BlockHitResult hit(
            GameTestHelper helper, BlockPos pos, Direction face) {
        BlockPos abs = helper.absolutePos(pos);
        return new BlockHitResult(
                Vec3.atCenterOf(abs).add(
                        face.getStepX() * 0.5D,
                        face.getStepY() * 0.5D,
                        face.getStepZ() * 0.5D),
                face,
                abs,
                false);
    }

    private static ItemStack chargedMixer() {
        return charged(ToolKind.MIXER_LV, "iron", 10_000L, 32L);
    }

    private static ItemStack charged(
            ToolKind kind, String material, long capacity, long voltage) {
        ItemStack stack = ModItems.electricTool(kind).get().variant(material);
        ElectricToolCharge.applyEmpty(stack, capacity, voltage);
        ElectricToolCharge.setCharge(stack, capacity);
        return stack;
    }

    private static GTRecipe mixerRecipe() {
        return ModRecipeMaps.MIXER.entry(
                        ResourceLocation.fromNamespaceAndPath(
                                CrucibleCraft.MODID,
                                "chemical/mixer/gunpowder/carbon"))
                .map(RecipeMap.Entry::recipe)
                .orElseGet(() -> ModRecipeMaps.MIXER.entries().stream()
                        .map(RecipeMap.Entry::recipe)
                        .filter(recipe -> !recipe.itemInputs().isEmpty()
                                && recipe.itemInputs().stream().allMatch(
                                        ingredient -> ingredient.getItems().length > 0))
                        .findFirst()
                        .orElseThrow(() -> new IllegalStateException(
                                "No mixer recipe with live item inputs")));
    }

    private static void loadBowl(MixingBowlBlockEntity bowl, GTRecipe recipe) {
        for (int index = 0; index < recipe.itemInputs().size(); index++) {
            ItemStack[] options = recipe.itemInputs().get(index).getItems();
            if (options.length == 0) {
                continue;
            }
            ItemStack sample = options[0].copy();
            sample.setCount(Math.max(1, recipe.itemInputCounts().get(index)));
            bowl.items().setStackInSlot(
                    MixingBowlBlockEntity.INPUT_SLOTS.get(index), sample);
        }
        for (int index = 0; index < recipe.fluidInputs().size(); index++) {
            bowl.tanks().get(MixingBowlBlockEntity.INPUT_TANKS.get(index))
                    .setFluid(recipe.fluidInputs().get(index).copy());
        }
    }

    private static boolean bowlHasOutput(MixingBowlBlockEntity bowl) {
        if (!bowl.items().getStackInSlot(MixingBowlBlockEntity.OUTPUT_SLOTS.getFirst())
                .isEmpty()) {
            return true;
        }
        return MixingBowlBlockEntity.OUTPUT_TANKS.stream().anyMatch(
                tank -> !bowl.tanks().get(tank).getFluid().isEmpty());
    }

    private static HopperBlock hopperBlock(String path) {
        return ModBlocks.hopperBlocksById()
                .get(ResourceLocation.fromNamespaceAndPath(
                        CrucibleCraft.MODID, path))
                .get();
    }
}
