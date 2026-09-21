package com.masson.cruciblecraft.gametest;

import java.util.List;

import com.masson.cruciblecraft.CrucibleCraft;
import com.masson.cruciblecraft.api.material.MaterialLookup;
import com.masson.cruciblecraft.api.material.MaterialPrefixes;
import com.masson.cruciblecraft.content.block.RockBlock;
import com.masson.cruciblecraft.material.prefix.MaterialPrefixCatalog;
import com.masson.cruciblecraft.recipe.gt.GTRecipeQuery;
import com.masson.cruciblecraft.registry.ModBlocks;
import com.masson.cruciblecraft.registry.ModRecipeMaps;
import com.masson.cruciblecraft.worldgen.PebbleBlocks;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.SingleRecipeInput;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * GT6 {@code OP.rockGt} pickup, 32757 physics, names, 2×2 cobble packing,
 * crushing, and furnace metal yields.
 * Lives on the default GameTest namespace so a bare {@code runGameTestServer}
 * exercises it.
 */
@GameTestHolder(CrucibleCraft.MODID)
@PrefixGameTestTemplate(false)
public final class RockPebbleGameTests {
    private static final String TEMPLATE = "empty";
    private static final BlockPos POS = new BlockPos(2, 2, 2);

    private RockPebbleGameTests() {}

    @GameTest(template = TEMPLATE, timeoutTicks = 40)
    public static void fourStoneRocksCraftCobblestone(GameTestHelper helper) {
        ItemStack rock = MaterialLookup.stack(
                "stone", MaterialPrefixCatalog.require("rock"));
        ItemStack assembled = craft(helper, List.of(rock, rock, rock, rock));
        helper.assertTrue(
                assembled.is(Items.COBBLESTONE) && assembled.getCount() == 1,
                "4 stone rocks did not pack to cobblestone: " + assembled);
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 40)
    public static void fourGraniteBlackRocksCraftLayerCobble(GameTestHelper helper) {
        ItemStack rock = MaterialLookup.stack(
                "granite_black", MaterialPrefixCatalog.require("rock"));
        ItemStack assembled = craft(helper, List.of(rock, rock, rock, rock));
        helper.assertTrue(
                assembled.is(ModBlocks.layerStone("granite_black/cobble").get().asItem())
                        && assembled.getCount() == 1,
                "4 granite_black rocks did not pack to granite_black/cobble: "
                        + assembled);
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 40)
    public static void uniqueRockUsesGt6PlacerPhysicsAndName(GameTestHelper helper) {
        RockBlock block = ModBlocks.rockBlock("stone").get();
        helper.setBlock(POS, Blocks.STONE);
        helper.setBlock(POS.above(), block);
        helper.assertTrue(
                helper.getBlockState(POS.above()).getBlock() instanceof RockBlock,
                "stone/rock did not place");
        helper.assertTrue(
                helper.getBlockState(POS.above())
                        .getCollisionShape(
                                helper.getLevel(),
                                helper.absolutePos(POS.above()))
                        .isEmpty(),
                "pebble collision must be empty");
        helper.assertTrue(
                Math.abs(
                                helper.getBlockState(POS.above()).getDestroySpeed(
                                        helper.getLevel(),
                                        helper.absolutePos(POS.above()))
                                        - 0.25F)
                        < 0.001F,
                "pebble hardness must be 0.25");
        helper.assertTrue(
                PebbleBlocks.canSurvive(
                        helper.getLevel(), helper.absolutePos(POS.above())),
                "pebble must survive on stone");
        ItemStack named = new ItemStack(block);
        helper.assertTrue(
                named.getHoverName().getContents() instanceof TranslatableContents contents
                        && "item.cruciblecraft.rock.stone".equals(contents.getKey()),
                "stone rock name must use GT6 Rock special: " + named.getHoverName());
        helper.assertTrue(
                MaterialLookup.stack(
                                "granite_black",
                                MaterialPrefixCatalog.require("rock"))
                        .getHoverName()
                        .getContents()
                        instanceof TranslatableContents form
                        && "item.cruciblecraft.material_form.rock".equals(form.getKey()),
                "granite_black rock must use %s Rock / %s石子");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 40)
    public static void emptyHandPicksUpUniqueRock(GameTestHelper helper) {
        RockBlock block = ModBlocks.rockBlock("stone").get();
        helper.setBlock(POS, Blocks.STONE);
        helper.setBlock(POS.above(), block);
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        BlockHitResult hit = new BlockHitResult(
                Vec3.atCenterOf(helper.absolutePos(POS.above())),
                Direction.UP,
                helper.absolutePos(POS.above()),
                false);
        helper.getBlockState(POS.above()).useWithoutItem(
                helper.getLevel(), player, hit);
        helper.assertTrue(
                helper.getBlockState(POS.above()).isAir(),
                "empty-hand click must remove the pebble");
        helper.assertTrue(
                player.getInventory().contains(new ItemStack(block)),
                "empty-hand click must give stone/rock");
        player.discard();
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 40)
    public static void uniqueRockPlacesOnlyWhenSneaking(GameTestHelper helper) {
        helper.setBlock(POS, Blocks.STONE);
        helper.setBlock(POS.above(), Blocks.AIR);
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        ItemStack stack = new ItemStack(ModBlocks.rockBlock("stone").get());
        player.setItemInHand(InteractionHand.MAIN_HAND, stack);
        BlockPos floor = helper.absolutePos(POS);
        BlockHitResult hit = new BlockHitResult(
                Vec3.atCenterOf(floor).add(0.0, 0.5, 0.0),
                Direction.UP,
                floor,
                false);
        UseOnContext standing = new UseOnContext(
                helper.getLevel(),
                player,
                InteractionHand.MAIN_HAND,
                stack,
                hit);
        helper.assertTrue(
                stack.getItem().useOn(standing) == InteractionResult.PASS,
                "standing right-click must not place a unique rock");
        helper.assertTrue(
                helper.getBlockState(POS.above()).isAir(),
                "stone surface stays empty without sneak");
        player.setShiftKeyDown(true);
        UseOnContext sneaking = new UseOnContext(
                helper.getLevel(),
                player,
                InteractionHand.MAIN_HAND,
                stack,
                hit);
        helper.assertTrue(
                stack.getItem().useOn(sneaking).consumesAction(),
                "shift+right-click must place unique rock");
        helper.assertTrue(
                helper.getBlockState(POS.above()).getBlock() instanceof RockBlock,
                "sneak place puts stone/rock");
        player.discard();
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 40)
    public static void unsupportedPebbleDropsWhenFloorIsRemoved(GameTestHelper helper) {
        RockBlock block = ModBlocks.rockBlock("stone").get();
        helper.setBlock(POS, Blocks.STONE);
        helper.setBlock(POS.above(), block);
        helper.setBlock(POS, Blocks.AIR);
        PebbleBlocks.dropIfUnsupported(
                helper.getBlockState(POS.above()),
                helper.getLevel(),
                helper.absolutePos(POS.above()),
                new ItemStack(block));
        helper.assertTrue(
                helper.getBlockState(POS.above()).isAir(),
                "removing the floor must drop the pebble");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 40)
    public static void chalcopyriteRockCrushesToNineSmallDust(GameTestHelper helper) {
        ItemStack rock = MaterialLookup.stack(
                "chalcopyrite", MaterialPrefixCatalog.require("rock"));
        var match = ModRecipeMaps.CRUSHER.findMatch(GTRecipeQuery.items(rock))
                .orElse(null);
        helper.assertTrue(match != null, "crusher has no chalcopyrite rock recipe");
        ItemStack output = match.recipe().itemOutputs().getFirst();
        ItemStack expected = MaterialLookup.stack(
                "chalcopyrite", MaterialPrefixes.SMALL_DUST, 9);
        helper.assertTrue(
                ItemStack.isSameItemSameComponents(output, expected)
                        && output.getCount() == 9,
                "chalcopyrite rock must crush to 9 small dust: " + output);
        helper.assertTrue(
                match.recipe().duration() == 36 && match.recipe().eut() == 16,
                "chalcopyrite rock crusher duration/EU must follow GT6 getCosts");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 40)
    public static void chalcopyriteRockSmeltsToTwoCopperChunks(GameTestHelper helper) {
        ItemStack rock = MaterialLookup.stack(
                "chalcopyrite", MaterialPrefixCatalog.require("rock"));
        var match = helper.getLevel()
                .getRecipeManager()
                .getRecipeFor(
                        RecipeType.SMELTING,
                        new SingleRecipeInput(rock),
                        helper.getLevel())
                .orElse(null);
        helper.assertTrue(match != null, "furnace has no chalcopyrite rock recipe");
        ItemStack output = match.value().getResultItem(
                helper.getLevel().registryAccess());
        ItemStack expected = MaterialLookup.stack(
                "copper", MaterialPrefixes.CHUNK, 2);
        helper.assertTrue(
                ItemStack.isSameItemSameComponents(output, expected)
                        && output.getCount() == 2,
                "chalcopyrite rock must smelt to 2 copper chunks: " + output);
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 40)
    public static void netherrackRockSmeltsToNetherBrickRock(GameTestHelper helper) {
        ItemStack rock = MaterialLookup.stack(
                "netherrack", MaterialPrefixCatalog.require("rock"));
        var match = helper.getLevel()
                .getRecipeManager()
                .getRecipeFor(
                        RecipeType.SMELTING,
                        new SingleRecipeInput(rock),
                        helper.getLevel())
                .orElse(null);
        helper.assertTrue(match != null, "furnace has no netherrack rock recipe");
        ItemStack output = match.value().getResultItem(
                helper.getLevel().registryAccess());
        ItemStack expected = MaterialLookup.stack(
                "nether_brick", MaterialPrefixCatalog.require("rock"));
        helper.assertTrue(
                ItemStack.isSameItemSameComponents(output, expected)
                        && output.getCount() == 1,
                "netherrack rock must smelt to nether-brick rock: " + output);
        helper.succeed();
    }

    private static ItemStack craft(GameTestHelper helper, List<ItemStack> slots) {
        CraftingInput input = CraftingInput.of(2, 2, List.copyOf(slots));
        var match = helper.getLevel()
                .getRecipeManager()
                .getRecipeFor(RecipeType.CRAFTING, input, helper.getLevel())
                .orElse(null);
        helper.assertTrue(match != null, "no 2x2 rock cobble crafting match");
        return match.value().assemble(
                input, helper.getLevel().registryAccess());
    }
}
