package com.masson.cruciblecraft.content.mold;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import com.masson.cruciblecraft.api.material.MaterialPrefixes;
import com.masson.cruciblecraft.content.block.CoinageMoldHosts;
import com.masson.cruciblecraft.content.mte.MteInPlaceKind;
import com.masson.cruciblecraft.content.mte.MteInPlaceSpec;
import com.masson.cruciblecraft.material.MaterialCatalog;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import net.minecraft.SharedConstants;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.Bootstrap;
import net.neoforged.fml.loading.LoadingModList;

class SmelteryGt6AlignmentTest {
    @BeforeAll
    static void bootstrapCatalog(@TempDir Path configDirectory) {
        LoadingModList.of(List.of(), List.of(), List.of(), List.of(), Map.of());
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();
        if (!MaterialCatalog.isBootstrapped()) {
            MaterialCatalog.bootstrap(configDirectory);
        }
    }

    @Test
    void glassPlateCool2CrystalRemapsToPlateGem() {
        assertEquals(
                MaterialPrefixes.PLATE_GEM,
                MoldRecipes.cool2Crystal(
                        MaterialPrefixes.PLATE, MaterialCatalog.require("glass")));
        assertEquals(
                MaterialPrefixes.TINY_PLATE_GEM,
                MoldRecipes.cool2Crystal(
                        MaterialPrefixes.TINY_PLATE, MaterialCatalog.require("glass")));
        assertEquals(
                MaterialPrefixes.INGOT,
                MoldRecipes.outputForm(
                        MaterialPrefixes.INGOT, MaterialCatalog.require("glass")));
        assertEquals(
                MaterialPrefixes.PLATE,
                MoldRecipes.cool2Crystal(
                        MaterialPrefixes.PLATE, MaterialCatalog.require("iron")));
    }

    @Test
    void coinageHostMatchesGt6MoldCoinageDummy() {
        MteInPlaceSpec spec = new MteInPlaceSpec(
                ResourceLocation.fromNamespaceAndPath(
                        "cruciblecraft", CoinageMoldHosts.REGISTRY_PATH),
                CoinageMoldHosts.REGISTRY_PATH,
                0,
                MteInPlaceKind.MISC_TOOL,
                "misc_tool",
                "Coinage Mold",
                "铸币模具",
                "gregtech.tileentity.tools.MultiTileEntityMoldCoinage");
        assertTrue(CoinageMoldHosts.isCoinage(spec));
        assertFalse(
                CoinageMoldHosts.isCoinage(
                        new MteInPlaceSpec(
                                ResourceLocation.fromNamespaceAndPath(
                                        "cruciblecraft", "stone/anvil"),
                                "stone/anvil",
                                0,
                                MteInPlaceKind.MISC_TOOL,
                                "misc_tool",
                                "Anvil",
                                "砧",
                                "gregtech.tileentity.tools.MultiTileEntityAnvil")));
    }

    @Test
    void cruciblesAreMoldInletsAndFaucetSkipsEmptyCollision() throws Exception {
        String crucible = Files.readString(Path.of(
                "src/main/java/com/masson/cruciblecraft/content/blockentity/CrucibleBlockEntity.java"));
        assertTrue(crucible.contains("implements IEnergyHandler, CruciblePour, MoldHost"));
        assertTrue(crucible.contains("process.acceptMoldPour"));
        String large = Files.readString(Path.of(
                "src/main/java/com/masson/cruciblecraft/content/blockentity/LargeCrucibleBlockEntity.java"));
        assertTrue(large.contains("implements MultiblockControllerBinding, MultiblockPortHost, CruciblePour, MoldHost"));
        assertTrue(large.contains("CastingMolds.isMoldItem"));
        assertTrue(large.contains("MoldRecipes.outputForm"));
        String faucet = Files.readString(Path.of(
                "src/main/java/com/masson/cruciblecraft/content/blockentity/MteInPlaceBlockEntity.java"));
        String dest = faucet.substring(
                faucet.indexOf("private BlockPos faucetDestination()"),
                faucet.indexOf("private String faucetMaterialId()"));
        assertTrue(dest.contains("MoldHost.at"));
        assertTrue(dest.contains("getCollisionShape"));
        assertTrue(dest.contains("MteInPlaceKind.FAUCET"));
        String pourDown = faucet.substring(
                faucet.indexOf("private void pourDown(Direction facing)"),
                faucet.indexOf("private void fillAttached"));
        assertTrue(pourDown.contains("capabilityFluid"));
        assertFalse(pourDown.contains("MoldHost.at"));
        String moldHost = Files.readString(Path.of(
                "src/main/java/com/masson/cruciblecraft/content/mold/MoldHost.java"));
        assertTrue(moldHost.contains("pourHostAtWall"));
        String ceramic = Files.readString(Path.of(
                "src/main/java/com/masson/cruciblecraft/content/blockentity/CeramicMoldBlockEntity.java"));
        assertTrue(ceramic.contains("MoldRecipes.outputForm"));
        String foundry = Files.readString(Path.of(
                "src/main/java/com/masson/cruciblecraft/content/blockentity/FoundryCastingBlockEntity.java"));
        assertTrue(foundry.contains("MoldRecipes.outputForm"));
        String coinage = Files.readString(Path.of(
                "src/main/java/com/masson/cruciblecraft/content/blockentity/CoinageMoldBlockEntity.java"));
        assertTrue(coinage.contains("isBlankTinyPlate"));
        assertTrue(coinage.contains("CoinItem.stack"));
        String scrap = Files.readString(Path.of(
                "src/main/java/com/masson/cruciblecraft/content/blockentity/CruciblePlayerInteraction.java"));
        assertTrue(scrap.contains("giveToSelected"));
        assertTrue(scrap.contains("matchesHeldScrap"));
    }

    @Test
    void emptyHandScrapGivesToSelectedSlot() throws Exception {
        String source = Files.readString(Path.of(
                "src/main/java/com/masson/cruciblecraft/content/blockentity/CruciblePlayerInteraction.java"));
        String empty = source.substring(
                source.indexOf("public static InteractionResult useEmpty"),
                source.indexOf("private static boolean takeBuffer"));
        assertTrue(empty.contains("giveScrap(player, process, 1, false)"));
        String give = source.substring(source.indexOf("private static boolean giveScrap"));
        assertTrue(give.contains("giveToSelected"));
        assertTrue(give.contains("player.setItemInHand(InteractionHand.MAIN_HAND, given)"));
    }
}
