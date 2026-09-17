package com.masson.cruciblecraft.worldgen;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import com.masson.cruciblecraft.api.material.MaterialLookup;
import com.masson.cruciblecraft.api.material.MaterialPrefix;
import com.masson.cruciblecraft.api.material.MaterialPrefixes;
import com.masson.cruciblecraft.content.block.GtSmallOreBlock;
import com.masson.cruciblecraft.content.block.OreStoneHost;
import com.masson.cruciblecraft.material.MaterialCatalog;
import com.masson.cruciblecraft.material.def.GT6MaterialMetadata;
import com.masson.cruciblecraft.material.prefix.MaterialPrefixCatalog;
import com.masson.cruciblecraft.registry.ModItems;

import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemStack;

/**
 * GT6 {@code Drops_SmallOre}: gem-weight table plus crushed, optional host dust.
 */
public final class DropsSmallOre {
    private DropsSmallOre() {}

    public static List<ItemStack> drops(
            String materialId,
            OreStoneHost host,
            BlockPos pos,
            int fortune,
            boolean silkTouch) {
        if (materialId == null || materialId.isEmpty()) {
            return List.of();
        }
        String crushedId = crushingTarget(materialId);
        Random random = new Random(pos.getX() ^ pos.getY() ^ pos.getZ());
        for (int i = 0; i < 16; i++) {
            random.nextInt(10000);
        }
        List<ItemStack> result = new ArrayList<>();
        if ("gneiss".equals(crushedId) || "petrified_wood".equals(crushedId)) {
            ItemStack rock = rock(crushedId);
            int count = Math.max(1, multiplier() + fortuneBonus(random, fortune) + random.nextInt(2));
            for (int i = 0; i < count; i++) {
                if (!rock.isEmpty()) {
                    result.add(rock.copy());
                }
            }
        } else {
            ItemStack legendary = item(crushedId, MaterialPrefixes.GEM_LEGENDARY);
            if (!legendary.isEmpty() && random.nextInt(silkTouch ? 5000 : 10000) <= fortune) {
                result.add(legendary);
            } else {
                List<ItemStack> selector = selector(crushedId, silkTouch);
                if (!selector.isEmpty()) {
                    int count = Math.max(1, multiplier() + fortuneBonus(random, fortune));
                    for (int i = 0; i < count; i++) {
                        result.add(selector.get(random.nextInt(selector.size())).copy());
                    }
                }
            }
        }
        String secondary = secondaryDust(host);
        if (secondary != null && random.nextInt(3 + fortune) > 1) {
            ItemStack dust = item(crushingTarget(secondary), MaterialPrefixes.DUST);
            if (dust.isEmpty()) {
                dust = item(secondary, MaterialPrefixes.DUST);
            }
            if (!dust.isEmpty()) {
                result.add(dust);
            }
        }
        if (result.isEmpty()) {
            ItemStack fallback = GtSmallOreBlock.drop(materialId);
            if (!fallback.isEmpty()) {
                result.add(fallback);
            }
        }
        return List.copyOf(result);
    }

    static String crushingTarget(String materialId) {
        return MaterialCatalog.find(materialId)
                .flatMap(definition -> definition.gt6Metadata())
                .map(GT6MaterialMetadata::processingTargets)
                .map(targets -> targets.get("crushing"))
                .map(GT6MaterialMetadata.MaterialAmount::material)
                .filter(id -> id != null && !id.isBlank())
                .orElse(materialId);
    }

    static List<ItemStack> selector(String materialId, boolean silkTouch) {
        List<ItemStack> selector = new ArrayList<>();
        ItemStack exquisite = item(materialId, MaterialPrefixes.GEM_EXQUISITE);
        if (exquisite.isEmpty()) {
            ItemStack gem = item(materialId, MaterialPrefixes.GEM);
            if (!gem.isEmpty()) {
                gem = gem.copy();
                gem.setCount(4);
                exquisite = gem;
            }
        }
        addWeighted(selector, exquisite, silkTouch ? 3 : 1);
        ItemStack flawless = item(materialId, MaterialPrefixes.GEM_FLAWLESS);
        if (flawless.isEmpty()) {
            ItemStack gem = item(materialId, MaterialPrefixes.GEM);
            if (!gem.isEmpty()) {
                gem = gem.copy();
                gem.setCount(2);
                flawless = gem;
            }
        }
        addWeighted(selector, flawless, silkTouch ? 6 : 2);
        addWeighted(selector, item(materialId, MaterialPrefixes.GEM), silkTouch ? 6 : 12);
        ItemStack flawed = item(materialId, MaterialPrefixes.GEM_FLAWED);
        ItemStack crushed = item(materialId, MaterialPrefixes.CRUSHED_ORE);
        if (!flawed.isEmpty()) {
            ItemStack two = flawed.copy();
            two.setCount(2);
            addWeighted(selector, two, silkTouch ? 10 : 5);
            addWeighted(selector, crushed, silkTouch ? 5 : 10);
        } else {
            addWeighted(selector, crushed, 15);
        }
        ItemStack chipped = item(materialId, MaterialPrefixes.GEM_CHIPPED);
        if (!chipped.isEmpty()) {
            ItemStack four = chipped.copy();
            four.setCount(4);
            addWeighted(selector, four, silkTouch ? 10 : 5);
            ItemStack crushedOrDust = crushed.isEmpty()
                    ? item(materialId, MaterialPrefixes.DUST)
                    : crushed;
            addWeighted(selector, crushedOrDust, silkTouch ? 5 : 10);
        } else {
            addWeighted(selector, crushed, 15);
        }
        return selector;
    }

    private static int multiplier() {
        return 1;
    }

    private static int fortuneBonus(Random random, int fortune) {
        if (fortune <= 0) {
            return 0;
        }
        return random.nextInt((1 + fortune) * multiplier()) / 2;
    }

    private static void addWeighted(List<ItemStack> selector, ItemStack stack, int copies) {
        if (stack == null || stack.isEmpty() || copies < 1) {
            return;
        }
        for (int i = 0; i < copies; i++) {
            selector.add(stack);
        }
    }

    private static ItemStack item(String materialId, MaterialPrefix prefix) {
        return MaterialLookup.tryStack(materialId, prefix, 1)
                .orElse(ItemStack.EMPTY);
    }

    private static ItemStack rock(String materialId) {
        MaterialPrefix rock = MaterialPrefixCatalog.require("rock");
        if (!ModItems.hasMaterialItem(materialId, rock)) {
            return ItemStack.EMPTY;
        }
        return new ItemStack(ModItems.materialItem(materialId, rock).get());
    }

    private static String secondaryDust(OreStoneHost host) {
        return switch (host) {
            case NETHERRACK -> "netherrack";
            case DEEPSLATE -> "deepslate";
            case STONE -> "stone";
        };
    }
}
