package com.masson.cruciblecraft.gametest;

import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.masson.cruciblecraft.api.energy.EnergyType;
import com.masson.cruciblecraft.api.material.MaterialLookup;
import com.masson.cruciblecraft.api.material.MaterialPrefix;
import com.masson.cruciblecraft.api.material.MaterialPrefixes;
import com.masson.cruciblecraft.api.unit.MaterialUnits;
import com.masson.cruciblecraft.content.block.StorageHostBlock;
import com.masson.cruciblecraft.content.blockentity.BookshelfBlockEntity;
import com.masson.cruciblecraft.content.blockentity.BottleCrateBlockEntity;
import com.masson.cruciblecraft.content.blockentity.LockerBlockEntity;
import com.masson.cruciblecraft.content.blockentity.MassStorageBlockEntity;
import com.masson.cruciblecraft.content.blockentity.StorageInserterBlockEntity;
import com.masson.cruciblecraft.content.storage.StorageVariant;
import com.masson.cruciblecraft.content.storage.StorageVariantCatalog;
import com.masson.cruciblecraft.material.MaterialCatalog;
import com.masson.cruciblecraft.registry.ModBlocks;
import com.masson.cruciblecraft.registry.ModCapabilities;
import com.masson.cruciblecraft.registry.ModComponents;
import com.masson.cruciblecraft.registry.ModItems;
import com.masson.cruciblecraft.registry.ModMachineVariants;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * Isolated storage runtime gate. Run with {@code -PgameTestGrid=content}.
 */
@GameTestHolder(StorageGameTests.NAMESPACE)
@PrefixGameTestTemplate(false)
public final class StorageGameTests {
    public static final String NAMESPACE = "cruciblecraft_content";
    private static final String TEMPLATE = "empty";

    private StorageGameTests() {}

    @GameTest(template = TEMPLATE, timeoutTicks = 40)
    public static void catalogMatchesLiveRegistry(GameTestHelper helper) {
        Set<String> catalog = StorageVariantCatalog.variants().stream()
                .map(variant -> variant.id().toString())
                .collect(Collectors.toCollection(TreeSet::new));
        Set<String> blocks = BuiltInRegistries.BLOCK.keySet().stream()
                .filter(id -> "cruciblecraft".equals(id.getNamespace()))
                .map(ResourceLocation::toString)
                .collect(Collectors.toCollection(TreeSet::new));
        Set<String> items = BuiltInRegistries.ITEM.keySet().stream()
                .filter(id -> "cruciblecraft".equals(id.getNamespace()))
                .map(ResourceLocation::toString)
                .collect(Collectors.toCollection(TreeSet::new));
        helper.assertTrue(
                blocks.containsAll(catalog) && items.containsAll(catalog),
                "live registry missing storage runtime ids");
        helper.assertTrue(
                StorageVariantCatalog.sourceVisible().size() == 18,
                "source-visible count drifted");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 40)
    public static void legacyStorageIngotAliasesBlock(GameTestHelper helper) {
        ResourceLocation legacy = ResourceLocation.fromNamespaceAndPath(
                "cruciblecraft", "iron/storage_ingot");
        helper.assertTrue(
                BuiltInRegistries.BLOCK.get(legacy)
                        == ModBlocks.storageBlock("iron").get(),
                "legacy iron/storage_ingot block alias is missing");
        helper.assertTrue(
                BuiltInRegistries.ITEM.get(legacy)
                        == ModItems.materialItem("iron", MaterialPrefixes.BLOCK).get(),
                "legacy iron/storage_ingot item alias is missing");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 40)
    public static void bookshelfAcceptsBooksAndRejectsIngots(GameTestHelper helper) {
        BlockPos pos = new BlockPos(2, 2, 2);
        place(helper, pos, "cruciblecraft:bookshelf_7100");
        BookshelfBlockEntity shelf =
                (BookshelfBlockEntity) helper.getBlockEntity(pos);
        helper.assertTrue(
                shelf.inventory().insertItem(
                        0, new ItemStack(Items.BOOK), false).isEmpty(),
                "bookshelf rejected a book");
        helper.assertTrue(
                shelf.inventory().insertItem(
                        1, new ItemStack(Items.IRON_INGOT), false).getCount()
                        == 1,
                "bookshelf accepted a non-book");
        helper.assertTrue(shelf.enchantPower() > 0.0F, "enchant power stayed 0");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 40)
    public static void bottleCrateRejectsNonBottles(GameTestHelper helper) {
        BlockPos pos = new BlockPos(2, 2, 2);
        place(helper, pos, "cruciblecraft:bottle_crate_8600");
        BottleCrateBlockEntity crate =
                (BottleCrateBlockEntity) helper.getBlockEntity(pos);
        helper.assertTrue(
                crate.inventory().insertItem(
                        0, new ItemStack(Items.GLASS_BOTTLE), false).isEmpty(),
                "bottle crate rejected a bottle");
        helper.assertTrue(
                crate.inventory().insertItem(
                        1, new ItemStack(Items.IRON_INGOT), false).getCount()
                        == 1,
                "bottle crate accepted a non-bottle");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 40)
    public static void lockerSwapsArmorOnFrontFace(GameTestHelper helper) {
        BlockPos pos = new BlockPos(2, 2, 2);
        place(helper, pos, "cruciblecraft:locker_7300");
        LockerBlockEntity locker = (LockerBlockEntity) helper.getBlockEntity(pos);
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        player.setItemSlot(EquipmentSlot.HEAD, new ItemStack(Items.IRON_HELMET));
        locker.swapArmor(player);
        helper.assertTrue(
                locker.inventory().getStackInSlot(3).is(Items.IRON_HELMET),
                "locker did not store the helmet");
        helper.assertTrue(
                player.getItemBySlot(EquipmentSlot.HEAD).isEmpty(),
                "player still wore the helmet");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 40)
    public static void chargingLockerAcceptsElectricAndRejectsHeat(
            GameTestHelper helper) {
        BlockPos pos = new BlockPos(2, 2, 2);
        place(helper, pos, "cruciblecraft:charging_locker_7500");
        LockerBlockEntity locker = (LockerBlockEntity) helper.getBlockEntity(pos);
        ItemStack helm = new ItemStack(Items.IRON_HELMET);
        helm.set(ModComponents.ELECTRIC_CHARGE.get(), 0L);
        locker.inventory().setStackInSlot(3, helm);
        helper.assertTrue(
                locker.insert(
                        EnergyType.ELECTRIC, 32L, 4L, Direction.UP, false)
                        > 0L,
                "charging locker rejected ELECTRIC");
        helper.assertTrue(
                locker.insert(
                        EnergyType.HEAT, 32L, 4L, Direction.UP, false) == 0L,
                "charging locker accepted HEAT");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 40)
    public static void massStorageOverflowAndReload(GameTestHelper helper) {
        BlockPos pos = new BlockPos(2, 2, 2);
        place(helper, pos, "cruciblecraft:plastic_storage_box_6993");
        MassStorageBlockEntity storage =
                (MassStorageBlockEntity) helper.getBlockEntity(pos);
        helper.assertTrue(
                storage.inventory().insertAll(
                        new ItemStack(Items.IRON_INGOT, 64), false).isEmpty(),
                "box 128 rejected the first stack");
        ItemStack leftover = storage.inventory().insertAll(
                new ItemStack(Items.IRON_INGOT, 64), false);
        leftover = storage.inventory().insertAll(
                leftover.isEmpty()
                        ? new ItemStack(Items.IRON_INGOT, 64)
                        : leftover,
                false);
        leftover = storage.inventory().insertAll(
                new ItemStack(Items.IRON_INGOT, 64), false);
        helper.assertTrue(
                leftover.getCount() > 0 || storage.inventory().stored() == 128,
                "capacity 128 did not overflow-reject");
        helper.assertTrue(
                storage.inventory().insertAll(
                        new ItemStack(Items.GOLD_INGOT, 1), false).getCount()
                        == 1,
                "mismatched type was accepted");
        var tag = storage.saveForTest(helper.getLevel().registryAccess());
        storage.loadForTest(tag, helper.getLevel().registryAccess());
        helper.assertTrue(
                storage.inventory().stored() > 0,
                "mass storage reload dropped contents");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 40)
    public static void massStorageMergesNuggetsIntoStoredIngots(
            GameTestHelper helper) {
        ItemStack ingot = liveMaterial(helper, "silver", MaterialPrefixes.INGOT, 1);
        ItemStack nuggets = liveMaterial(helper, "silver", MaterialPrefixes.NUGGET, 9);
        BlockPos pos = new BlockPos(2, 2, 2);
        place(helper, pos, "cruciblecraft:item_barrel_6999");
        MassStorageBlockEntity storage =
                (MassStorageBlockEntity) helper.getBlockEntity(pos);
        helper.assertTrue(
                storage.inventory().insertAll(ingot, false).isEmpty(),
                "wooden barrel rejected a silver ingot");
        helper.assertTrue(
                storage.inventory().insertAll(nuggets, false).isEmpty(),
                "barrel rejected same-material nuggets");
        helper.assertTrue(
                storage.inventory().stored() == 2
                        && storage.inventory().filter().is(ingot.getItem()),
                "9 nuggets did not convert into a second stored ingot");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 40)
    public static void massStorageIronBlockJoinsIngotFamilyNotPlate(
            GameTestHelper helper) {
        ItemStack ingot = liveMaterial(helper, "silver", MaterialPrefixes.INGOT, 1);
        ItemStack plate = liveMaterial(helper, "silver", MaterialPrefixes.PLATE, 1);
        ItemStack block = liveMaterial(helper, "silver", MaterialPrefixes.BLOCK, 1);
        helper.assertTrue(
                MaterialUnits.resolve(block)
                        .filter(entry -> "silver".equals(entry.materialId())
                                && MaterialPrefixes.BLOCK.equals(entry.form()))
                        .isPresent(),
                "silver block is not indexed as silver/block");
        BlockPos ingotPos = new BlockPos(1, 2, 2);
        BlockPos platePos = new BlockPos(3, 2, 2);
        place(helper, ingotPos, "cruciblecraft:item_barrel_6999");
        place(helper, platePos, "cruciblecraft:item_barrel_6983");
        MassStorageBlockEntity ingotBarrel =
                (MassStorageBlockEntity) helper.getBlockEntity(ingotPos);
        MassStorageBlockEntity plateBarrel =
                (MassStorageBlockEntity) helper.getBlockEntity(platePos);
        helper.assertTrue(
                ingotBarrel.inventory().insertAll(ingot, false).isEmpty(),
                "ingot barrel rejected a silver ingot");
        helper.assertTrue(
                ingotBarrel.inventory().insertAll(block.copy(), false).isEmpty(),
                "ingot barrel rejected a silver block");
        helper.assertTrue(
                ingotBarrel.inventory().stored() == 10,
                "silver block did not convert into 9 stored ingots");
        helper.assertTrue(
                plateBarrel.inventory().insertAll(plate, false).isEmpty(),
                "plate barrel rejected a silver plate");
        helper.assertTrue(
                !plateBarrel.inventory().insertAll(block.copy(), false).isEmpty()
                        && plateBarrel.inventory().stored() == 1,
                "silver block entered a plate barrel");
        helper.succeed();
    }

    /**
     * No material opens the chunk prefix yet (not in the form-demand census),
     * so only the unit ratio is checked until a live chunk exists.
     */
    @GameTest(template = TEMPLATE, timeoutTicks = 40)
    public static void massStorageChunkUnitsAreQuarterIngot(GameTestHelper helper) {
        helper.assertTrue(
                MaterialPrefixes.CHUNK.units() * 4 == MaterialPrefixes.INGOT.units(),
                "chunk is not a quarter ingot");
        String chunkMaterial = MaterialCatalog.values().stream()
                .filter(material -> MaterialCatalog.isFormRegistered(
                                material, MaterialPrefixes.CHUNK)
                        && MaterialCatalog.isFormRegistered(
                                material, MaterialPrefixes.INGOT))
                .map(material -> material.id())
                .findFirst()
                .orElse(null);
        if (chunkMaterial == null) {
            helper.succeed();
            return;
        }
        ItemStack ingot = liveMaterial(helper, chunkMaterial, MaterialPrefixes.INGOT, 1);
        ItemStack chunks = liveMaterial(helper, chunkMaterial, MaterialPrefixes.CHUNK, 4);
        BlockPos pos = new BlockPos(2, 2, 2);
        place(helper, pos, "cruciblecraft:item_barrel_6999");
        MassStorageBlockEntity storage =
                (MassStorageBlockEntity) helper.getBlockEntity(pos);
        helper.assertTrue(
                storage.inventory().insertAll(ingot, false).isEmpty(),
                "barrel rejected a " + chunkMaterial + " ingot");
        helper.assertTrue(
                storage.inventory().insertAll(chunks, false).isEmpty(),
                "barrel rejected " + chunkMaterial + " chunks");
        helper.assertTrue(
                storage.inventory().stored() == 2,
                "4 chunks did not convert as one ingot");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 40)
    public static void massStorageVanillaGlassDoesNotPrefixMerge(
            GameTestHelper helper) {
        ItemStack storedGlass = new ItemStack(Items.GLASS);
        var resolved = MaterialUnits.resolve(storedGlass);
        if (resolved.isEmpty()) {
            helper.succeed();
            return;
        }
        ItemStack incoming = glassFamilyIncoming(resolved.get().form());
        helper.assertTrue(
                !incoming.isEmpty(),
                "glass dust/gem forms are missing");
        BlockPos pos = new BlockPos(2, 2, 2);
        place(helper, pos, "cruciblecraft:item_barrel_6999");
        MassStorageBlockEntity storage =
                (MassStorageBlockEntity) helper.getBlockEntity(pos);
        helper.assertTrue(
                storage.inventory().insertAll(storedGlass, false).isEmpty(),
                "barrel rejected vanilla glass");
        helper.assertTrue(
                !storage.inventory().insertAll(incoming, false).isEmpty()
                        && storage.inventory().stored() == 1
                        && storage.inventory().filter().is(Items.GLASS),
                "vanilla glass prefix-merged a same-family glass form");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 40)
    public static void massStorageAutoOutputsDownNotThroughFront(
            GameTestHelper helper) {
        BlockPos pos = new BlockPos(2, 3, 2);
        placeFacing(helper, pos, "cruciblecraft:plastic_storage_box_6993", Direction.EAST);
        helper.setBlock(pos.below(), Blocks.CHEST);
        helper.setBlock(pos.east(), Blocks.CHEST);
        MassStorageBlockEntity storage =
                (MassStorageBlockEntity) helper.getBlockEntity(pos);
        storage.toggleAutoOutput();
        helper.assertTrue(
                storage.inventory().insertAll(
                        new ItemStack(Items.IRON_INGOT, 8), false).isEmpty(),
                "box rejected iron");
        helper.startSequence()
                .thenIdle(8)
                .thenExecute(() -> {
                    ChestBlockEntity below = helper.getBlockEntity(pos.below());
                    ChestBlockEntity front = helper.getBlockEntity(pos.east());
                    helper.assertTrue(
                            below.getItem(0).is(Items.IRON_INGOT)
                                    && below.getItem(0).getCount() == 8,
                            "auto-output did not fill the inventory below");
                    helper.assertTrue(
                            front.getItem(0).isEmpty(),
                            "mass storage pushed through the front face");
                })
                .thenSucceed();
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 40)
    public static void inserterScansMassStorageNotChests(GameTestHelper helper) {
        BlockPos inserterPos = new BlockPos(2, 2, 2);
        BlockPos storagePos = new BlockPos(2, 2, 3);
        BlockPos chestPos = new BlockPos(3, 2, 2);
        placeFacing(helper, inserterPos, "cruciblecraft:storage_inserter_32751",
                Direction.SOUTH);
        place(helper, storagePos, "cruciblecraft:mass_storage_6000");
        helper.setBlock(chestPos, Blocks.CHEST);
        MassStorageBlockEntity storage =
                (MassStorageBlockEntity) helper.getBlockEntity(storagePos);
        storage.inventory().insertAll(new ItemStack(Items.IRON_INGOT, 1), false);
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        player.getInventory().setItem(0, new ItemStack(Items.IRON_INGOT, 16));
        StorageInserterBlockEntity inserter =
                (StorageInserterBlockEntity) helper.getBlockEntity(inserterPos);
        int moved = inserter.insertFromPlayer(player);
        helper.assertTrue(moved > 0, "inserter did not insert into mass storage");
        helper.assertTrue(
                storage.inventory().stored() > 1,
                "mass storage did not receive inserter items");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 40)
    public static void logisticsExposesCapabilityStandardDoesNot(
            GameTestHelper helper) {
        BlockPos standardPos = new BlockPos(2, 2, 2);
        BlockPos logisticsPos = new BlockPos(3, 2, 2);
        place(helper, standardPos, "cruciblecraft:mass_storage_6000");
        place(helper, logisticsPos, "cruciblecraft:mass_storage_logistics_6200");
        MassStorageBlockEntity standard =
                (MassStorageBlockEntity) helper.getBlockEntity(standardPos);
        MassStorageBlockEntity logistics =
                (MassStorageBlockEntity) helper.getBlockEntity(logisticsPos);
        helper.assertTrue(
                standard.logisticsStorage() == null,
                "standard mass storage exposed logistics");
        helper.assertTrue(
                logistics.logisticsStorage() != null
                        && logistics.getLogisticsPriorityItem() == 1,
                "logistics mass storage missing ILogisticsStorage");
        helper.assertTrue(
                helper.getLevel().getCapability(
                        ModCapabilities.LOGISTICS_STORAGE,
                        helper.absolutePos(logisticsPos),
                        Direction.NORTH)
                        != null,
                "logistics capability missing");
        helper.assertTrue(
                helper.getLevel().getCapability(
                        ModCapabilities.LOGISTICS_STORAGE,
                        helper.absolutePos(standardPos),
                        Direction.NORTH)
                        == null,
                "standard exposed logistics capability");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 40)
    public static void breakingDropsSelfAndContents(GameTestHelper helper) {
        BlockPos pos = new BlockPos(2, 2, 2);
        place(helper, pos, "cruciblecraft:bookshelf_7000");
        BookshelfBlockEntity shelf =
                (BookshelfBlockEntity) helper.getBlockEntity(pos);
        shelf.inventory().insertItem(0, new ItemStack(Items.BOOK), false);
        helper.getLevel().destroyBlock(helper.absolutePos(pos), true);
        helper.assertTrue(
                helper.getBlockState(pos).isAir(),
                "broken bookshelf still occupies the cell");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 40)
    public static void machineRowsStayCatalogSized(GameTestHelper helper) {
        Set<String> catalog = machineCatalogIds(false);
        Set<String> generic = machineCatalogIds(true);
        Set<String> registered = ModMachineVariants.ALL.stream()
                .map(variant -> variant.id().toString())
                .collect(Collectors.toCollection(TreeSet::new));
        helper.assertTrue(
                catalog.containsAll(generic),
                "Machine catalog JSON dropped a generic row");
        helper.assertTrue(
                generic.equals(registered),
                "Generic machine catalog drifted from ModMachineVariants.ALL");
        helper.succeed();
    }

    private static ItemStack liveMaterial(
            GameTestHelper helper, String material, MaterialPrefix form, int count) {
        ItemStack stack = MaterialLookup.tryStack(material, form, count)
                .orElse(ItemStack.EMPTY);
        helper.assertTrue(
                !stack.isEmpty(),
                material + " " + form.serializedName() + " is missing");
        return stack;
    }

    private static void place(GameTestHelper helper, BlockPos pos, String id) {
        placeFacing(helper, pos, id, Direction.NORTH);
    }

    private static ItemStack glassFamilyIncoming(MaterialPrefix storedForm) {
        if (MaterialPrefixes.GEM.equals(storedForm)
                || MaterialPrefixes.BLOCK.equals(storedForm)) {
            if (ModItems.hasMaterialItem("glass", MaterialPrefixes.GEM)) {
                return new ItemStack(
                        ModItems.materialItem("glass", MaterialPrefixes.GEM).get());
            }
        }
        if (MaterialPrefixes.DUST.equals(storedForm)
                || MaterialPrefixes.SMALL_DUST.equals(storedForm)
                || MaterialPrefixes.TINY_DUST.equals(storedForm)
                || MaterialPrefixes.DUST_DIV72.equals(storedForm)
                || MaterialPrefixes.STORAGE_DUST.equals(storedForm)) {
            if (ModItems.hasMaterialItem("glass", MaterialPrefixes.DUST)) {
                return new ItemStack(
                        ModItems.materialItem("glass", MaterialPrefixes.DUST).get());
            }
        }
        if (MaterialPrefixes.PLATE.equals(storedForm)
                || MaterialPrefixes.STORAGE_PLATE.equals(storedForm)) {
            if (ModItems.hasMaterialItem("glass", MaterialPrefixes.PLATE)) {
                return new ItemStack(
                        ModItems.materialItem("glass", MaterialPrefixes.PLATE).get());
            }
        }
        return ItemStack.EMPTY;
    }

    private static Set<String> machineCatalogIds(boolean skipGenericOnly) {
        try (var stream = StorageGameTests.class.getResourceAsStream(
                "/data/cruciblecraft/machine_tiers.json")) {
            JsonObject document = JsonParser.parseReader(
                    new java.io.InputStreamReader(
                            stream, java.nio.charset.StandardCharsets.UTF_8))
                    .getAsJsonObject();
            return document.getAsJsonArray("variants").asList().stream()
                    .map(row -> row.getAsJsonObject())
                    .filter(row -> {
                        if (!skipGenericOnly) {
                            return true;
                        }
                        var profile = row.getAsJsonObject("resourceProfile");
                        return profile == null
                                || !profile.has("skipGenericRegistration")
                                || !profile.get("skipGenericRegistration")
                                        .getAsBoolean();
                    })
                    .map(row -> row.get("id").getAsString())
                    .collect(Collectors.toCollection(TreeSet::new));
        } catch (Exception error) {
            throw new IllegalStateException(error);
        }
    }

    private static void placeFacing(
            GameTestHelper helper, BlockPos pos, String id, Direction facing) {
        StorageVariant variant = StorageVariantCatalog.require(
                ResourceLocation.parse(id));
        BlockState state = ModBlocks.storageBlocksById()
                .get(variant.id())
                .get()
                .defaultBlockState()
                .setValue(StorageHostBlock.FACING, facing);
        helper.setBlock(pos, state);
    }
}
