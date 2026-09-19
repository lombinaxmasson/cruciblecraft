package com.masson.cruciblecraft.gametest;

import com.masson.cruciblecraft.CrucibleCraft;
import com.masson.cruciblecraft.content.block.HopperBlock;
import com.masson.cruciblecraft.content.blockentity.HopperBlockEntity;
import com.masson.cruciblecraft.content.item.tool.PocketMultitoolMode;
import com.masson.cruciblecraft.registry.ModBlocks;
import com.masson.cruciblecraft.registry.ModComponents;
import com.masson.cruciblecraft.registry.ModItems;

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
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * GT6 handheld world-click and harvest remainder. Lives on the opt-in
 * default grid ({@link CrucibleCraftGameTests#NAMESPACE}).
 */
@GameTestHolder(CrucibleCraftGameTests.NAMESPACE)
@PrefixGameTestTemplate(false)
public final class HandheldToolBehaviorGameTests {
    private static final String TEMPLATE = "empty";
    private static final BlockPos POS = new BlockPos(2, 2, 2);

    private HandheldToolBehaviorGameTests() {}

    @GameTest(template = TEMPLATE, timeoutTicks = 40)
    public static void wrenchRotatesVanillaFurnace(GameTestHelper helper) {
        helper.setBlock(
                POS,
                Blocks.FURNACE.defaultBlockState().setValue(
                        BlockStateProperties.HORIZONTAL_FACING, Direction.NORTH));
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        ItemStack wrench = ModItems.MATERIAL_WRENCH.get().variant("iron");
        player.setItemInHand(InteractionHand.MAIN_HAND, wrench);
        helper.assertTrue(
                useOn(helper, player, wrench, Direction.EAST).consumesAction(),
                "wrench did not rotate the furnace");
        helper.assertTrue(
                helper.getBlockState(POS).getValue(
                        BlockStateProperties.HORIZONTAL_FACING) == Direction.EAST,
                "furnace did not face the wrench grid side");
        player.discard();
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 40)
    public static void pocketMultitoolCyclesOnSneakUse(GameTestHelper helper) {
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        ItemStack pocket = ModItems.MATERIAL_POCKET_MULTITOOL.get().variant("iron");
        player.setItemInHand(InteractionHand.MAIN_HAND, pocket);
        player.setShiftKeyDown(true);
        pocket.getItem().use(helper.getLevel(), player, InteractionHand.MAIN_HAND);
        helper.assertTrue(
                PocketMultitoolMode.fromOrdinal(
                        pocket.getOrDefault(ModComponents.POCKET_MODE.get(), 0))
                        == PocketMultitoolMode.KNIFE,
                "sneak-use did not open the pocket knife");
        player.discard();
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 40)
    public static void builderWandCopiesClickedBlockFromInventory(
            GameTestHelper helper) {
        helper.setBlock(POS, Blocks.STONE);
        helper.setBlock(POS.north(), Blocks.AIR);
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        ItemStack wand = ModItems.MATERIAL_BUILDER_WAND.get().variant("iron");
        player.setItemInHand(InteractionHand.MAIN_HAND, wand);
        player.getInventory().add(new ItemStack(Items.STONE, 16));
        helper.assertTrue(
                useOn(helper, player, wand, Direction.NORTH).consumesAction(),
                "builder wand did not place from inventory");
        helper.assertTrue(
                helper.getBlockState(POS.north()).is(Blocks.STONE),
                "builder wand did not copy stone onto the clicked face");
        player.discard();
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 40)
    public static void hopperPincersExtractFirstSlot(GameTestHelper helper) {
        helper.setBlock(POS, hopperBlock("lead_hopper"));
        HopperBlockEntity hopper = helper.getBlockEntity(POS);
        hopper.inventory().setStackInSlot(0, new ItemStack(Items.IRON_INGOT, 4));
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        ItemStack pincers = ModItems.MATERIAL_PINCERS.get().variant("iron");
        player.setItemInHand(InteractionHand.MAIN_HAND, pincers);
        helper.getBlockState(POS).useItemOn(
                pincers,
                helper.getLevel(),
                player,
                InteractionHand.MAIN_HAND,
                hit(helper, Direction.UP));
        helper.assertTrue(
                hopper.inventory().getStackInSlot(0).isEmpty(),
                "pincers left the hopper slot occupied");
        helper.assertTrue(
                player.getInventory().countItem(Items.IRON_INGOT) == 4,
                "pincers did not give the extracted stack to the player");
        player.discard();
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 40)
    public static void sawPlacesSaplingFromInventory(GameTestHelper helper) {
        helper.setBlock(POS, Blocks.DIRT);
        helper.setBlock(POS.above(), Blocks.AIR);
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        ItemStack saw = ModItems.MATERIAL_SAW.get().variant("iron");
        player.setItemInHand(InteractionHand.MAIN_HAND, saw);
        player.getInventory().add(new ItemStack(Items.OAK_SAPLING, 1));
        helper.assertTrue(
                useOn(helper, player, saw, Direction.UP).consumesAction(),
                "saw did not place a sapling from inventory");
        helper.assertTrue(
                helper.getBlockState(POS.above()).is(Blocks.OAK_SAPLING),
                "saw did not plant the inventory sapling");
        player.discard();
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 40)
    public static void wrenchRotatesVanillaBarrel(GameTestHelper helper) {
        helper.setBlock(
                POS,
                Blocks.BARREL.defaultBlockState().setValue(
                        BlockStateProperties.FACING, Direction.UP));
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        ItemStack wrench = ModItems.MATERIAL_WRENCH.get().variant("iron");
        player.setItemInHand(InteractionHand.MAIN_HAND, wrench);
        helper.assertTrue(
                useOn(helper, player, wrench, Direction.SOUTH).consumesAction(),
                "wrench did not rotate the barrel");
        helper.assertTrue(
                helper.getBlockState(POS).getValue(BlockStateProperties.FACING)
                        == Direction.SOUTH,
                "barrel did not face the wrench grid side");
        player.discard();
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 40)
    public static void hammerCrushesCopperOreDrop(GameTestHelper helper) {
        helper.setBlock(POS, ModBlocks.oreBlock(
                "copper",
                com.masson.cruciblecraft.worldgen.OreHostVariantCatalog.Host.STONE)
                .get());
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        ItemStack hammer = ModItems.SMITHING_HAMMER.get().variant("iron");
        player.setItemInHand(InteractionHand.MAIN_HAND, hammer);
        helper.assertTrue(
                ((net.minecraft.server.level.ServerPlayer) player)
                        .gameMode.destroyBlock(helper.absolutePos(POS)),
                "hammer could not break copper ore");
        helper.assertTrue(
                helper.getLevel()
                        .getEntitiesOfClass(
                                net.minecraft.world.entity.item.ItemEntity.class,
                                new net.minecraft.world.phys.AABB(
                                        helper.absolutePos(POS)).inflate(2.0))
                        .stream()
                        .anyMatch(entity ->
                                com.masson.cruciblecraft.api.material.MaterialLookup
                                        .matches(
                                                entity.getItem(),
                                                "copper",
                                                com.masson.cruciblecraft.api.material
                                                        .MaterialPrefixes.CRUSHED_ORE)
                                || com.masson.cruciblecraft.api.material.MaterialLookup
                                        .matches(
                                                entity.getItem(),
                                                "copper",
                                                com.masson.cruciblecraft.api.material
                                                        .MaterialPrefixes.DUST)),
                "hammer did not convert copper ore into crushed or dust");
        player.discard();
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 40)
    public static void hammerProspectsClickedOre(GameTestHelper helper) {
        helper.setBlock(POS, ModBlocks.oreBlock(
                "copper",
                com.masson.cruciblecraft.worldgen.OreHostVariantCatalog.Host.STONE)
                .get());
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        ItemStack hammer = ModItems.SMITHING_HAMMER.get().variant("iron");
        player.setItemInHand(InteractionHand.MAIN_HAND, hammer);
        helper.assertTrue(
                useOn(helper, player, hammer, Direction.NORTH).consumesAction(),
                "hammer did not prospect the copper ore");
        player.discard();
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 40)
    public static void scissorsDisarmTripwire(GameTestHelper helper) {
        helper.setBlock(POS, Blocks.TRIPWIRE);
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        ItemStack scissors = ModItems.MATERIAL_SCISSORS.get().variant("iron");
        player.setItemInHand(InteractionHand.MAIN_HAND, scissors);
        helper.assertTrue(
                useOn(helper, player, scissors, Direction.UP).consumesAction(),
                "scissors did not cut the tripwire");
        helper.assertTrue(
                helper.getBlockState(POS).is(Blocks.AIR),
                "scissors left the tripwire in place");
        player.discard();
        helper.succeed();
    }

    private static net.minecraft.world.InteractionResult useOn(
            GameTestHelper helper,
            Player player,
            ItemStack stack,
            Direction face) {
        return stack.getItem().useOn(
                new UseOnContext(
                        helper.getLevel(),
                        player,
                        InteractionHand.MAIN_HAND,
                        stack,
                        hit(helper, face)));
    }

    private static BlockHitResult hit(GameTestHelper helper, Direction face) {
        BlockPos abs = helper.absolutePos(POS);
        return new BlockHitResult(
                Vec3.atCenterOf(abs).add(face.getStepX() * 0.5D, face.getStepY() * 0.5D, face.getStepZ() * 0.5D),
                face,
                abs,
                false);
    }

    private static HopperBlock hopperBlock(String path) {
        return ModBlocks.hopperBlocksById()
                .get(ResourceLocation.fromNamespaceAndPath(
                        CrucibleCraft.MODID, path))
                .get();
    }
}
