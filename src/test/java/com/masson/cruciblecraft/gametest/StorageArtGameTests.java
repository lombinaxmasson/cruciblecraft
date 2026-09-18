package com.masson.cruciblecraft.gametest;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import com.masson.cruciblecraft.content.block.StorageHostBlock;
import com.masson.cruciblecraft.content.blockentity.BookshelfBlockEntity;
import com.masson.cruciblecraft.content.blockentity.BottleCrateBlockEntity;
import com.masson.cruciblecraft.content.blockentity.MassStorageBlockEntity;
import com.masson.cruciblecraft.content.blockentity.MteInPlaceBlockEntity;
import com.masson.cruciblecraft.content.mte.MteInPlaceKind;
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
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * Isolated GT6 storage art. Run with
 * {@code -PwaveRecipes=content/gt6-storage-art}.
 */
@GameTestHolder(StorageArtGameTests.NAMESPACE)
@PrefixGameTestTemplate(false)
public final class StorageArtGameTests {
    public static final String NAMESPACE =
            "cruciblecraft_wave_content_gt6_storage_art";
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
                "/assets/cruciblecraft/blockstates/mass_storage_barrel_6999.json");
        helper.assertTrue(
                model.contains("gt6_import/storage/mass_storage_barrel")
                        && model.contains("tintindex")
                        && !model.contains("minecraft:block/barrel")
                        && !model.contains("minecraft:block/oak_planks")
                        && state.contains("storage_mass_barrel")
                        && !state.contains("minecraft:block/barrel"),
                "T44 item barrel still uses the vanilla oak barrel");
        BlockPos pos = new BlockPos(2, 2, 2);
        helper.setBlock(
                pos,
                ModBlocks.storageBlocksById()
                        .get(MteInPlaceGameTestSupport.id(
                                "mass_storage_barrel_6999"))
                        .get()
                        .defaultBlockState());
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 40)
    public static void storageArtMteBarrelNotMissingCube(GameTestHelper helper) {
        MteInPlaceGameTestSupport.assertLive(
                helper, "skyroot/item_barrel", MteInPlaceKind.BARREL);
        String model = resource(
                "/assets/cruciblecraft/models/block/mte_inplace_barrel.json");
        helper.assertTrue(
                model.contains("storage_mass_barrel")
                        && !model.contains("cube_all")
                        && !model.contains("gt6_import/mte/barrel"),
                "in-place barrel still uses cube_all pointing at a missing png");
        BlockPos pos = new BlockPos(2, 2, 2);
        helper.setBlock(
                pos,
                ModBlocks.mteInPlaceBlocksById()
                        .get(MteInPlaceGameTestSupport.id("skyroot/item_barrel"))
                        .get()
                        .defaultBlockState());
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
        MteInPlaceGameTestSupport.assertLive(
                helper, "furniture/bookshelf_lead", MteInPlaceKind.BOOKSHELF);
        BlockPos mte = new BlockPos(3, 2, 2);
        placeMte(helper, mte, "furniture/bookshelf_lead");
        MteInPlaceBlockEntity host =
                (MteInPlaceBlockEntity) helper.getBlockEntity(mte);
        helper.assertTrue(
                host.items().insertItem(
                        0, new ItemStack(Items.BOOK), false).isEmpty(),
                "in-place bookshelf rejected a book");
        helper.assertTrue(
                StorageBookDisplay.index(host.items().getStackInSlot(0))
                        == StorageBookDisplay.VANILLA,
                "in-place bookshelf display id drifted");
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
        MteInPlaceGameTestSupport.assertLive(
                helper, "furniture/bottlecrate_lead", MteInPlaceKind.BOTTLE_CRATE);
        BlockPos mte = new BlockPos(3, 2, 2);
        placeMte(helper, mte, "furniture/bottlecrate_lead");
        MteInPlaceBlockEntity host =
                (MteInPlaceBlockEntity) helper.getBlockEntity(mte);
        helper.assertTrue(
                host.items().insertItem(
                        0, new ItemStack(Items.HONEY_BOTTLE), false).isEmpty(),
                "in-place bottle crate rejected honey");
        helper.assertTrue(
                StorageBottleDisplay.hasFluid(host.items().getStackInSlot(0)),
                "in-place bottle crate lost fluid display");
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
        MteInPlaceGameTestSupport.assertLive(
                helper, "furniture/mass_storage_lead", MteInPlaceKind.MASS_STORAGE);
        BlockPos mte = new BlockPos(3, 2, 2);
        placeMte(helper, mte, "furniture/mass_storage_lead");
        MteInPlaceBlockEntity host =
                (MteInPlaceBlockEntity) helper.getBlockEntity(mte);
        helper.assertTrue(
                host.items().getSlots() == 1,
                "in-place mass storage is not a single-item dummy");
        helper.assertTrue(
                host.items().insertItem(
                        0, new ItemStack(Items.IRON_INGOT, 8), false).isEmpty(),
                "in-place mass storage rejected iron");
        helper.assertTrue(
                host.items().getStackInSlot(0).getCount() == 8,
                "in-place mass storage lost the face count");
        MteInPlaceGameTestSupport.assertLive(
                helper, "skyroot/item_barrel", MteInPlaceKind.BARREL);
        BlockPos barrel = new BlockPos(4, 2, 2);
        placeMte(helper, barrel, "skyroot/item_barrel");
        MteInPlaceBlockEntity dummy =
                (MteInPlaceBlockEntity) helper.getBlockEntity(barrel);
        helper.assertTrue(
                dummy.spec().kind() == MteInPlaceKind.BARREL
                        && dummy.items().getSlots() == 27,
                "in-place barrel was folded into mass-storage TESR");
        helper.succeed();
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

    private static void placeMte(GameTestHelper helper, BlockPos pos, String path) {
        helper.setBlock(
                pos,
                ModBlocks.mteInPlaceBlocksById()
                        .get(MteInPlaceGameTestSupport.id(path))
                        .get()
                        .defaultBlockState());
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
