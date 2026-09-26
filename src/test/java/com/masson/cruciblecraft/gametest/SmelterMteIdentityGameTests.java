package com.masson.cruciblecraft.gametest;

import com.masson.cruciblecraft.content.item.SmelterMteIdentityCatalog;
import com.masson.cruciblecraft.registry.ModItems;
import com.masson.cruciblecraft.registry.ModRecipeMaps;

import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * Isolated Smelter MTE identity gate. Run with
 * {@code -PgameTestGrid=machines}.
 * Catalog items are identities, not overworld scatter obtain.
 */
@GameTestHolder(SmelterMteIdentityGameTests.NAMESPACE)
@PrefixGameTestTemplate(false)
public final class SmelterMteIdentityGameTests {
    public static final String NAMESPACE =
            "cruciblecraft_machines";
    private static final String TEMPLATE = "empty";

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
}
