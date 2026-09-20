package com.masson.cruciblecraft.gametest;

import com.masson.cruciblecraft.api.energy.EnergyType;
import com.masson.cruciblecraft.content.block.StorageHostBlock;
import com.masson.cruciblecraft.content.blockentity.LockerBlockEntity;
import com.masson.cruciblecraft.content.item.tool.ElectricToolCharge;
import com.masson.cruciblecraft.content.item.tool.ToolMining;
import com.masson.cruciblecraft.content.storage.StorageVariant;
import com.masson.cruciblecraft.content.storage.StorageVariantCatalog;
import com.masson.cruciblecraft.machine.ToolMaterialRules.ToolKind;
import com.masson.cruciblecraft.registry.ModBlocks;
import com.masson.cruciblecraft.registry.ModItems;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder(CrucibleCraftGameTests.NAMESPACE)
@PrefixGameTestTemplate(false)
public final class ElectricToolGameTests {
    private static final String TEMPLATE = "empty";
    private static final BlockPos POS = new BlockPos(2, 2, 2);

    private ElectricToolGameTests() {}

    @GameTest(template = TEMPLATE, timeoutTicks = 40)
    public static void electricMiningDrillSpendsEu(GameTestHelper helper) {
        helper.setBlock(POS, Blocks.STONE);
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        ItemStack drill = charged(
                ToolKind.MINING_DRILL_LV, "iron", 10_000L, 32L);
        player.setItemInHand(InteractionHand.MAIN_HAND, drill);
        helper.assertTrue(
                drill.getItem().mineBlock(
                        drill,
                        helper.getLevel(),
                        helper.getBlockState(POS),
                        helper.absolutePos(POS),
                        player),
                "mining drill did not mine");
        helper.assertTrue(
                ElectricToolCharge.charge(drill) < 10_000L,
                "mining drill did not spend EU");
        player.discard();
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 40)
    public static void electricWrenchConvertsToMonkey(GameTestHelper helper) {
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        ItemStack wrench = charged(ToolKind.WRENCH_LV, "iron", 8_000L, 32L);
        player.setItemInHand(InteractionHand.MAIN_HAND, wrench);
        player.setShiftKeyDown(true);
        wrench.getItem().use(
                helper.getLevel(), player, InteractionHand.MAIN_HAND);
        ItemStack held = player.getItemInHand(InteractionHand.MAIN_HAND);
        helper.assertTrue(
                held.is(ModItems.electricTool(ToolKind.MONKEY_WRENCH_LV).get()),
                "sneak-use did not convert the LV wrench");
        helper.assertTrue(
                ElectricToolCharge.charge(held) == 8_000L,
                "convert dropped the stored EU");
        player.discard();
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 40)
    public static void chargingLockerFillsElectricToolCapacity(
            GameTestHelper helper) {
        placeChargingLocker(helper, POS);
        LockerBlockEntity locker = helper.getBlockEntity(POS);
        ItemStack drill = charged(
                ToolKind.MINING_DRILL_MV, "iron", 50_000L, 128L);
        ElectricToolCharge.setCharge(drill, 0L);
        locker.inventory().setStackInSlot(3, drill);
        helper.assertTrue(
                locker.insert(
                        EnergyType.ELECTRIC, 128L, 40L, Direction.UP, false)
                        > 0L,
                "charging locker rejected the electric drill");
        helper.assertTrue(
                ElectricToolCharge.charge(
                        locker.inventory().getStackInSlot(3))
                        == 5_120L,
                "locker did not fill the drill from its own capacity");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 40)
    public static void jackhammerNoOresSkipsOreBlocks(GameTestHelper helper) {
        ItemStack jack = charged(
                ToolKind.JACKHAMMER_HV_NO_ORES, "iron", 20_000L, 512L);
        helper.assertTrue(
                ToolMining.mineable(
                        ToolKind.JACKHAMMER_HV_NO_ORES,
                        Blocks.STONE.defaultBlockState()),
                "no-ores jackhammer skipped stone");
        helper.assertTrue(
                !ToolMining.mineable(
                        ToolKind.JACKHAMMER_HV_NO_ORES,
                        Blocks.IRON_ORE.defaultBlockState()),
                "no-ores jackhammer mined iron ore");
        helper.assertTrue(
                jack.getItem().getDestroySpeed(
                        jack, Blocks.IRON_ORE.defaultBlockState())
                        == 1.0F,
                "no-ores jackhammer was fast on ore");
        helper.succeed();
    }

    private static ItemStack charged(
            ToolKind kind, String material, long capacity, long voltage) {
        ItemStack stack = ModItems.electricTool(kind).get().variant(material);
        ElectricToolCharge.applyEmpty(stack, capacity, voltage);
        ElectricToolCharge.setCharge(stack, capacity);
        return stack;
    }

    private static void placeChargingLocker(
            GameTestHelper helper, BlockPos pos) {
        StorageVariant variant = StorageVariantCatalog.require(
                ResourceLocation.parse("cruciblecraft:charging_locker_7500"));
        BlockState state = ModBlocks.storageBlocksById()
                .get(variant.id())
                .get()
                .defaultBlockState()
                .setValue(StorageHostBlock.FACING, Direction.NORTH);
        helper.setBlock(pos, state);
    }
}
