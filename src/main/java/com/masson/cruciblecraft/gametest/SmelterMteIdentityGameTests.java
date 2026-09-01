package com.masson.cruciblecraft.gametest;

import java.util.List;

import com.masson.cruciblecraft.content.item.SmelterMteIdentityCatalog;
import com.masson.cruciblecraft.registry.ModFeatures;
import com.masson.cruciblecraft.registry.ModItems;
import com.masson.cruciblecraft.registry.ModRecipeMaps;
import com.masson.cruciblecraft.worldgen.ItemScatterConfiguration;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.TagKey;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.feature.ConfiguredFeature;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * Isolated Smelter MTE identity / acquisition gate. Run with
 * {@code -PwaveRecipes=recycling/smelter-mte-identity}.
 */
@GameTestHolder(SmelterMteIdentityGameTests.NAMESPACE)
@PrefixGameTestTemplate(false)
public final class SmelterMteIdentityGameTests {
    public static final String NAMESPACE =
            "cruciblecraft_wave_recycling_smelter_mte_identity";
    private static final String TEMPLATE = "empty";
    private static final ResourceLocation SCATTER_FEATURE =
            ResourceLocation.fromNamespaceAndPath("cruciblecraft", "smelter_mte_scatter");
    private static final TagKey<Item> SCATTER_ITEMS = TagKey.create(
            Registries.ITEM,
            ResourceLocation.fromNamespaceAndPath("cruciblecraft", "smelter_mte_items"));

    private SmelterMteIdentityGameTests() {}

    @GameTest(template = TEMPLATE, timeoutTicks = 40)
    public static void catalogMatchesProvenFamilyCount(GameTestHelper helper) {
        helper.assertTrue(
                SmelterMteIdentityCatalog.identities().size()
                        == SmelterMteIdentityCatalog.SOURCE_META_COUNT,
                "Smelter MTE catalog drifted from 1817 identities");
        helper.assertTrue(
                !SmelterMteIdentityCatalog.newItems().isEmpty(),
                "Smelter MTE catalog registered no new items");
        helper.assertTrue(
                ModItems.smelterMteItemsById().size()
                        == SmelterMteIdentityCatalog.newItems().size(),
                "Smelter MTE item registration drifted from catalog newItems");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 40)
    public static void noRecoveryRecipesPublished(GameTestHelper helper) {
        long recovery = ModRecipeMaps.SMELTER.entries().stream()
                .filter(entry -> entry.id().getPath()
                        .startsWith("smelter/deferred_recycling/"))
                .count();
        helper.assertTrue(
                recovery == 0,
                "identity child published recovery recipes: " + recovery);
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 120)
    public static void itemScatterPlacesFromRuntimeTag(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        var registry = level.registryAccess().registryOrThrow(Registries.CONFIGURED_FEATURE);
        ConfiguredFeature<?, ?> configured = registry.get(
                ResourceKey.create(Registries.CONFIGURED_FEATURE, SCATTER_FEATURE));
        helper.assertTrue(
                configured != null
                        && configured.config() instanceof ItemScatterConfiguration,
                "Runtime registry lacks decoded smelter_mte_scatter");
        ItemScatterConfiguration config = (ItemScatterConfiguration) configured.config();
        helper.assertTrue(config.rarity() == 128, "smelter/mte item scatter rarity drifted");
        helper.assertTrue(
                config.itemTag().equals(SCATTER_ITEMS),
                "smelter/mte item scatter tag drifted");
        ItemStack acquired = scatterOneTaggedItem(helper);
        helper.assertTrue(
                acquired.is(SCATTER_ITEMS),
                "Scatter placed an item outside cruciblecraft:smelter_mte_items");
        helper.succeed();
    }

    private static ItemStack scatterOneTaggedItem(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        BlockPos chunkOrigin = chunkAlignedOrigin(helper);
        int surfaceY = 64;
        prepareItemScatterPad(level, chunkOrigin, surfaceY);
        ConfiguredFeature<ItemScatterConfiguration, ?> forced =
                new ConfiguredFeature<>(
                        ModFeatures.SMELTER_MTE_SCATTER.get(),
                        new ItemScatterConfiguration(1, SCATTER_ITEMS));
        helper.assertTrue(
                forced.place(
                        level,
                        level.getChunkSource().getGenerator(),
                        RandomSource.create(1L),
                        chunkOrigin),
                "smelter/mte item scatter did not place any item entity");
        AABB box = new AABB(
                chunkOrigin.getX(),
                surfaceY,
                chunkOrigin.getZ(),
                chunkOrigin.getX() + 16,
                surfaceY + 4,
                chunkOrigin.getZ() + 16);
        List<ItemEntity> entities = level.getEntitiesOfClass(ItemEntity.class, box);
        helper.assertTrue(
                !entities.isEmpty(),
                "smelter/mte item scatter placed no ItemEntity");
        return entities.getFirst().getItem().copy();
    }

    private static BlockPos chunkAlignedOrigin(GameTestHelper helper) {
        BlockPos anchor = helper.absolutePos(BlockPos.ZERO);
        return new BlockPos(
                (anchor.getX() >> 4) << 4,
                0,
                (anchor.getZ() >> 4) << 4);
    }

    private static void prepareItemScatterPad(
            ServerLevel level,
            BlockPos chunkOrigin,
            int surfaceY) {
        for (int dx = 0; dx < 16; dx++) {
            for (int dz = 0; dz < 16; dz++) {
                BlockPos surface = chunkOrigin.offset(dx, surfaceY, dz);
                level.setBlock(
                        surface.below(),
                        Blocks.STONE.defaultBlockState(),
                        Block.UPDATE_ALL);
                level.setBlock(
                        surface,
                        Blocks.DIRT.defaultBlockState(),
                        Block.UPDATE_ALL);
                level.setBlock(
                        surface.above(),
                        Blocks.AIR.defaultBlockState(),
                        Block.UPDATE_ALL);
            }
        }
    }
}
