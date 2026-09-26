package com.masson.cruciblecraft.gametest;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import com.masson.cruciblecraft.content.block.MteInPlaceBlock;
import com.masson.cruciblecraft.content.block.StorageHostBlock;
import com.masson.cruciblecraft.content.blockentity.BookshelfBlockEntity;
import com.masson.cruciblecraft.content.blockentity.BottleCrateBlockEntity;
import com.masson.cruciblecraft.content.blockentity.MassStorageBlockEntity;
import com.masson.cruciblecraft.content.blockentity.MteInPlaceBlockEntity;
import com.masson.cruciblecraft.content.storage.StorageBookDisplay;
import com.masson.cruciblecraft.content.storage.StorageBottleDisplay;
import com.masson.cruciblecraft.content.storage.StorageCountFormat;
import com.masson.cruciblecraft.content.storage.StorageVariant;
import com.masson.cruciblecraft.content.storage.StorageVariantCatalog;
import com.masson.cruciblecraft.registry.ModBlocks;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * Isolated GT6 storage art. Run with
 * {@code -PgameTestGrid=content}.
 */
@GameTestHolder(StorageArtGameTests.NAMESPACE)
@PrefixGameTestTemplate(false)
public final class StorageArtGameTests {
    public static final String NAMESPACE =
            "cruciblecraft_content";
    private static final String TEMPLATE = "empty";

    private StorageArtGameTests() {}

    @GameTest(template = TEMPLATE, timeoutTicks = 40)
    public static void storageArtManifestResolvesLocalGt6(GameTestHelper helper) {
        String manifest = resource(
                "/assets/cruciblecraft/gt6_storage_art_manifest.json");
        helper.assertTrue(
                manifest.contains("machines/massstorage/barrel/colored/side.png")
                        && manifest.contains("machines/massstorage/barrel/overlay/front.png")
                        && manifest.contains("iconsets/planks_wood.png")
                        && !manifest.contains("minecraft:block/barrel")
                        && !manifest.contains("minecraft:block/iron_block"),
                "storage art manifest drifted from local GT6 iconsets");
        helper.assertTrue(
                classpathExists(
                        "/assets/cruciblecraft/textures/block/gt6_import/"
                                + "storage/mass_storage_barrel/colored_side.png")
                        && classpathExists(
                                "/assets/cruciblecraft/textures/block/gt6_import/"
                                        + "storage/mass_storage_barrel/overlay_front.png")
                        && classpathExists(
                                "/assets/cruciblecraft/textures/block/gt6_import/"
                                        + "storage/planks_wood.png"),
                "GT6 storage iconset was not imported");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 40)
    public static void storageArtBarrelNotVanillaOak(GameTestHelper helper) {
        String model = resource(
                "/assets/cruciblecraft/models/block/storage_mass_barrel.json");
        String state = resource(
                "/assets/cruciblecraft/blockstates/item_barrel_6999.json");
        helper.assertTrue(
                model.contains("gt6_import/storage/mass_storage_barrel")
                        && model.contains("tintindex")
                        && !model.contains("minecraft:block/barrel")
                        && !model.contains("minecraft:block/oak_planks")
                        && state.contains("storage_mass_barrel")
                        && !state.contains("minecraft:block/barrel"),
                "T44 item barrel still uses the vanilla oak barrel");
        BlockPos pos = new BlockPos(2, 2, 2);
        place(helper, pos, "cruciblecraft:item_barrel_6999");
        MassStorageBlockEntity barrel =
                (MassStorageBlockEntity) helper.getBlockEntity(pos);
        helper.assertTrue(
                barrel.inventory().insertAll(
                        new ItemStack(Items.IRON_INGOT, 32), false).isEmpty(),
                "item barrel rejected iron");
        CompoundTag tag = barrel.getUpdateTag(helper.getLevel().registryAccess());
        helper.assertTrue(
                tag.getInt("stored") == 32
                        && tag.contains("filter")
                        && "32".equals(
                                StorageCountFormat.face(
                                        32,
                                        barrel.inventory().capacity(),
                                        "mass_storage_barrel")),
                "item barrel update tag dropped stored count");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 40)
    public static void storageArtMteBarrelNotMissingCube(GameTestHelper helper) {
        helper.assertTrue(
                ModBlocks.mteInPlaceBlocksById().get(
                        MteInPlaceGameTestSupport.id("skyroot/item_barrel"))
                        == null,
                "skyroot/item_barrel dummy is still registered");
        String model = resource(
                "/assets/cruciblecraft/models/block/mte_inplace_barrel.json");
        helper.assertTrue(
                model.contains("storage_mass_barrel")
                        && !model.contains("cube_all")
                        && !model.contains("gt6_import/mte/barrel"),
                "in-place barrel still uses cube_all pointing at a missing png");
        BlockPos pos = new BlockPos(2, 2, 2);
        place(helper, pos, "cruciblecraft:item_barrel_6983");
        MassStorageBlockEntity skyroot =
                (MassStorageBlockEntity) helper.getBlockEntity(pos);
        helper.assertTrue(
                skyroot.inventory().insertAll(
                        new ItemStack(Items.IRON_INGOT, 8), false).isEmpty(),
                "skyroot item barrel rejected iron");
        CompoundTag skyrootTag =
                skyroot.getUpdateTag(helper.getLevel().registryAccess());
        helper.assertTrue(
                skyrootTag.getInt("stored") == 8 && skyrootTag.contains("filter"),
                "skyroot item barrel update tag dropped stored count");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 40)
    public static void storageArtBookshelfShowsBookDisplay(GameTestHelper helper) {
        helper.assertTrue(
                classpathExists(
                        "/assets/cruciblecraft/textures/block/gt6_import/"
                                + "storage/books/book_vanilla_back.png")
                        && classpathExists(
                                "/assets/cruciblecraft/textures/block/gt6_import/"
                                        + "storage/books/book_enchanted_side.png")
                        && classpathExists(
                                "/assets/cruciblecraft/textures/block/gt6_import/"
                                        + "storage/books/book_gt_back.png"),
                "GT6 book display textures were not imported");
        BlockPos pos = new BlockPos(2, 2, 2);
        place(helper, pos, "cruciblecraft:bookshelf_7100");
        BookshelfBlockEntity shelf =
                (BookshelfBlockEntity) helper.getBlockEntity(pos);
        helper.assertTrue(
                shelf.inventory().insertItem(
                        0, new ItemStack(Items.BOOK), false).isEmpty(),
                "bookshelf rejected a vanilla book");
        helper.assertTrue(
                shelf.inventory().insertItem(
                        1, new ItemStack(Items.ENCHANTED_BOOK), false).isEmpty(),
                "bookshelf rejected an enchanted book");
        helper.assertTrue(
                StorageBookDisplay.index(shelf.inventory().getStackInSlot(0))
                        == StorageBookDisplay.VANILLA
                        && StorageBookDisplay.index(
                                shelf.inventory().getStackInSlot(1))
                                == StorageBookDisplay.ENCHANTED,
                "bookshelf display ids drifted");
        CompoundTag tag = shelf.getUpdateTag(
                helper.getLevel().registryAccess());
        helper.assertTrue(
                tag.contains("inventory"),
                "bookshelf update tag dropped inventory");
        helper.assertTrue(
                ModBlocks.mteInPlaceBlocksById().get(
                        MteInPlaceGameTestSupport.id("furniture/bookshelf_lead"))
                        == null,
                "lead bookshelf dummy is still registered");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 40)
    public static void storageArtBottleCrateShowsBottleFluid(GameTestHelper helper) {
        helper.assertTrue(
                classpathExists(
                        "/assets/cruciblecraft/textures/block/gt6_import/"
                                + "storage/bottle/bottlecrate_bottle_sides.png")
                        && classpathExists(
                                "/assets/cruciblecraft/textures/block/gt6_import/"
                                        + "storage/bottle/bottlecrate_bottle_cap.png"),
                "GT6 bottle-crate display textures were not imported");
        BlockPos pos = new BlockPos(2, 2, 2);
        place(helper, pos, "cruciblecraft:bottle_crate_8600");
        BottleCrateBlockEntity crate =
                (BottleCrateBlockEntity) helper.getBlockEntity(pos);
        helper.assertTrue(
                crate.inventory().insertItem(
                        0, new ItemStack(Items.GLASS_BOTTLE), false).isEmpty(),
                "bottle crate rejected glass");
        helper.assertTrue(
                crate.inventory().insertItem(
                        1, new ItemStack(Items.HONEY_BOTTLE), false).isEmpty(),
                "bottle crate rejected honey");
        ItemStack glass = crate.inventory().getStackInSlot(0);
        ItemStack honey = crate.inventory().getStackInSlot(1);
        helper.assertTrue(
                StorageBottleDisplay.present(glass)
                        && !StorageBottleDisplay.hasFluid(glass)
                        && StorageBottleDisplay.hasFluid(honey)
                        && StorageBottleDisplay.fluidColor(honey)
                                == StorageBottleDisplay.HONEY,
                "bottle-crate fluid display drifted");
        CompoundTag tag = crate.getUpdateTag(
                helper.getLevel().registryAccess());
        helper.assertTrue(
                tag.contains("inventory"),
                "bottle crate update tag dropped inventory");
        helper.assertTrue(
                ModBlocks.mteInPlaceBlocksById().get(
                        MteInPlaceGameTestSupport.id("furniture/bottlecrate_lead"))
                        == null,
                "lead bottle crate dummy is still registered");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 40)
    public static void storageArtMassStorageFrontCountSync(GameTestHelper helper) {
        BlockPos pos = new BlockPos(2, 2, 2);
        place(helper, pos, "cruciblecraft:mass_storage_6000");
        MassStorageBlockEntity storage =
                (MassStorageBlockEntity) helper.getBlockEntity(pos);
        helper.assertTrue(
                storage.inventory().insertAll(
                        new ItemStack(Items.IRON_INGOT, 64), false).isEmpty(),
                "mass storage rejected iron");
        storage.inventory().load(new ItemStack(Items.IRON_INGOT), 12_345);
        helper.assertTrue(
                storage.inventory().stored() == 12_345
                        && "12k".equals(
                                StorageCountFormat.format(
                                        storage.inventory().stored())),
                "front count format drifted");
        CompoundTag tag = storage.getUpdateTag(
                helper.getLevel().registryAccess());
        helper.assertTrue(
                tag.getInt("stored") == 12_345 && tag.contains("filter"),
                "mass storage update tag dropped stored count");
        helper.assertTrue(
                ModBlocks.mteInPlaceBlocksById().get(
                        MteInPlaceGameTestSupport.id("furniture/mass_storage_lead"))
                        == null
                        && ModBlocks.mteInPlaceBlocksById().get(
                                MteInPlaceGameTestSupport.id("skyroot/item_barrel"))
                        == null,
                "folded T44 duals are still registered as in-place dummies");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 40)
    public static void storageArtMetalBookshelfIsLive(GameTestHelper helper) {
        BlockPos pos = new BlockPos(2, 2, 2);
        MteInPlaceBlockEntity host = placeInPlace(
                helper, pos, "furniture/bookshelf_aluminium");
        helper.assertTrue(
                host.items().getSlots() == 28,
                "aluminium bookshelf slot count drifted");
        helper.assertTrue(
                host.items().insertItem(
                        0, new ItemStack(Items.BOOK), false).isEmpty(),
                "aluminium bookshelf rejected a book");
        helper.assertTrue(
                !host.items().insertItem(
                        1, new ItemStack(Items.APPLE), false).isEmpty(),
                "aluminium bookshelf accepted a non-book");
        var player = helper.makeMockPlayer(GameType.SURVIVAL);
        helper.assertTrue(
                host.createMenu(1, player.getInventory(), player) != null,
                "aluminium bookshelf has no GUI");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 40)
    public static void storageArtMetalBottleCrateIsLive(GameTestHelper helper) {
        BlockPos pos = new BlockPos(2, 2, 2);
        MteInPlaceBlockEntity host = placeInPlace(
                helper, pos, "furniture/bottlecrate_aluminium");
        helper.assertTrue(
                host.items().getSlots() == 9,
                "aluminium bottle crate slot count drifted");
        helper.assertTrue(
                host.items().insertItem(
                        0, new ItemStack(Items.GLASS_BOTTLE), false).isEmpty(),
                "aluminium bottle crate rejected glass");
        helper.assertTrue(
                !host.items().insertItem(
                        1, new ItemStack(Items.APPLE), false).isEmpty(),
                "aluminium bottle crate accepted a non-bottle");
        var player = helper.makeMockPlayer(GameType.SURVIVAL);
        helper.assertTrue(
                host.createMenu(1, player.getInventory(), player) != null,
                "aluminium bottle crate has no GUI");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 40)
    public static void storageArtMetalDrawerIsQuad(GameTestHelper helper) {
        BlockPos pos = new BlockPos(2, 2, 2);
        MteInPlaceBlockEntity host = placeInPlace(
                helper, pos, "furniture/compartment_drawer_aluminium");
        helper.assertTrue(
                host.items().getSlots() == 144,
                "aluminium drawer is not 144 slots");
        helper.assertTrue(
                host.items().insertItem(
                        100, new ItemStack(Items.IRON_INGOT), false).isEmpty(),
                "aluminium drawer rejected slot 100");
        var player = helper.makeMockPlayer(GameType.SURVIVAL);
        host.setDrawerCompartment(3);
        helper.assertTrue(
                host.createMenu(1, player.getInventory(), player) != null,
                "aluminium drawer has no GUI");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 40)
    public static void storageArtMetalLockerSwapsArmor(GameTestHelper helper) {
        BlockPos pos = new BlockPos(2, 2, 2);
        MteInPlaceBlockEntity host = placeInPlace(
                helper, pos, "furniture/locker_aluminium");
        helper.assertTrue(
                host.items().getSlots() == 4,
                "aluminium locker slot count drifted");
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        player.setItemSlot(
                EquipmentSlot.HEAD,
                new ItemStack(Items.IRON_HELMET));
        host.swapArmor(player);
        helper.assertTrue(
                player.getItemBySlot(EquipmentSlot.HEAD).isEmpty()
                        && host.items().getStackInSlot(3).is(Items.IRON_HELMET),
                "aluminium locker did not swap helmet");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 40)
    public static void storageArtMetalMassStorageIsLive(GameTestHelper helper) {
        BlockPos pos = new BlockPos(2, 2, 2);
        MteInPlaceBlockEntity host = placeInPlace(
                helper, pos, "furniture/mass_storage_aluminium");
        helper.assertTrue(
                host.massStorage() != null
                        && host.massStorage().capacity() == 1_000_000,
                "aluminium mass storage is not 1e6");
        helper.assertTrue(
                host.massStorage().insertAll(
                        new ItemStack(Items.IRON_INGOT, 64), false).isEmpty(),
                "aluminium mass storage rejected iron");
        helper.assertTrue(
                host.massStorage().stored() == 64,
                "aluminium mass storage stored count drifted");
        CompoundTag tag = host.getUpdateTag(helper.getLevel().registryAccess());
        helper.assertTrue(
                tag.getInt("stored") == 64 && tag.contains("filter"),
                "aluminium mass storage update tag dropped stored count");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 40)
    public static void storageArtChestIsFiftyFour(GameTestHelper helper) {
        BlockPos pos = new BlockPos(2, 2, 2);
        MteInPlaceBlockEntity host = placeInPlace(helper, pos, "lead/chest");
        helper.assertTrue(
                host.items().getSlots() == 54,
                "lead chest is not 54 slots");
        var player = helper.makeMockPlayer(GameType.SURVIVAL);
        helper.assertTrue(
                host.createMenu(1, player.getInventory(), player) != null,
                "lead chest has no GUI");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 40)
    public static void storageArtSafeIsFifteen(GameTestHelper helper) {
        BlockPos pos = new BlockPos(2, 2, 2);
        MteInPlaceBlockEntity host = placeInPlace(
                helper, pos, "safe/mechanical_lead_safe");
        helper.assertTrue(
                host.items().getSlots() == 15,
                "lead safe is not 15 slots");
        var player = helper.makeMockPlayer(GameType.SURVIVAL);
        helper.assertTrue(
                host.createMenu(1, player.getInventory(), player) != null,
                "lead safe has no GUI");
        helper.succeed();
    }

    private static MteInPlaceBlockEntity placeInPlace(
            GameTestHelper helper, BlockPos pos, String path) {
        var block = ModBlocks.mteInPlaceBlocksById()
                .get(MteInPlaceGameTestSupport.id(path))
                .get();
        helper.setBlock(
                pos,
                block.defaultBlockState().setValue(
                        MteInPlaceBlock.FACING,
                        Direction.NORTH));
        return helper.getBlockEntity(pos);
    }

    private static void place(GameTestHelper helper, BlockPos pos, String id) {
        StorageVariant variant = StorageVariantCatalog.require(
                ResourceLocation.parse(id));
        BlockState state = ModBlocks.storageBlocksById()
                .get(variant.id())
                .get()
                .defaultBlockState()
                .setValue(StorageHostBlock.FACING, Direction.NORTH);
        helper.setBlock(pos, state);
    }

    private static boolean classpathExists(String path) {
        return StorageArtGameTests.class.getResource(path) != null;
    }

    private static String resource(String path) {
        try (InputStream stream =
                StorageArtGameTests.class.getResourceAsStream(path)) {
            if (stream == null) {
                throw new IllegalStateException("missing " + path);
            }
            return new String(stream.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException failure) {
            throw new IllegalStateException(path, failure);
        }
    }
}
