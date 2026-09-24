package com.masson.cruciblecraft.content.mte;

import java.util.Locale;
import java.util.Map;
import java.util.Optional;

import com.masson.cruciblecraft.content.block.FoundryHosts;
import com.masson.cruciblecraft.content.block.LargeCrucibleHosts;
import com.masson.cruciblecraft.localization.LanguageNames;
import com.masson.cruciblecraft.material.MaterialCatalog;
import com.masson.cruciblecraft.material.MaterialZhNames;

/**
 * Compose zh_cn names for GT6 foundry / large-crucible / wall identities whose
 * catalog {@code chinese_name} is still the English GT6 string.
 */
public final class MteInPlaceDisplayNames {
    private static final Map<String, String> LOCAL_ZH = Map.ofEntries(
            Map.entry("tungstensteel", "钨钢"),
            Map.entry("adamantium", "艾德曼合金"),
            Map.entry("ceramic", "陶瓷"),
            Map.entry("steel_galvanized", "镀锌钢"),
            Map.entry("netherite", "下界合金"),
            Map.entry("wood_treated", "防腐木"),
            Map.entry("nichrome", "镍铬"),
            Map.entry("carborundum", "碳化硅"),
            Map.entry("niobium_titanium", "铌钛"),
            Map.entry("fiery_steel", "炽钢"),
            Map.entry("hslasteel", "HSLA钢"),
            Map.entry("knightmetal", "骑士金属"),
            Map.entry("octine", "奥金"),
            Map.entry("tantalum_hafnium_carbide", "碳化钽铪"),
            Map.entry("thaumium", "神秘金属"),
            Map.entry("void_metal", "虚空金属"));

    private MteInPlaceDisplayNames() {}

    public static Optional<String> chinese(String registryPath) {
        if (registryPath == null || registryPath.isEmpty()) {
            return Optional.empty();
        }
        Optional<String> fluidAttachment = chineseFluidAttachment(registryPath);
        if (fluidAttachment.isPresent()) {
            return fluidAttachment;
        }
        if (registryPath.startsWith("multiblock/large_")
                && registryPath.endsWith("_coil")) {
            String token = registryPath.substring(
                    "multiblock/large_".length(),
                    registryPath.length() - "_coil".length());
            return chineseMaterialName(token).map(name -> "大型" + name + "线圈");
        }
        Optional<String> material = chineseMaterial(registryPath);
        if (material.isEmpty()) {
            return Optional.empty();
        }
        String name = material.orElseThrow();
        if (registryPath.startsWith("multiblock/large_")
                && registryPath.endsWith("_crucible")) {
            return Optional.of("大型" + name + "坩埚");
        }
        if (isDenseWall(registryPath)) {
            return Optional.of("致密" + name + "壁");
        }
        if (isWallPath(registryPath)) {
            return Optional.of(name + "壁");
        }
        return FoundryHosts.kindTemplate(registryPath)
                .map(template -> template.replace("%s", name));
    }

    private static Optional<String> chineseFluidAttachment(String registryPath) {
        String materialToken;
        String kind;
        if (registryPath.contains("/crucible_faucet")) {
            kind = "faucet";
            int slash = registryPath.indexOf('/');
            materialToken = slash > 0
                    ? registryPath.substring(0, slash)
                    : registryPath.substring(
                            registryPath.lastIndexOf("crucible_faucet_")
                                    + "crucible_faucet_".length());
            if (registryPath.startsWith("fluid_attachment/")) {
                materialToken = registryPath.substring(
                        registryPath.lastIndexOf("crucible_faucet_")
                                + "crucible_faucet_".length());
            }
        } else {
            String[] suffixes = {
                "/cap_nozzle", "/nozzle", "/funnel", "/tap"
            };
            kind = null;
            for (String suffix : suffixes) {
                if (registryPath.endsWith(suffix)) {
                    kind = suffix.substring(1);
                    break;
                }
            }
            if (kind == null && registryPath.startsWith("fluid_attachment/")) {
                for (String candidate : new String[] {
                    "cap_nozzle", "nozzle", "funnel", "tap"
                }) {
                    if (registryPath.endsWith("_" + candidate)) {
                        kind = candidate;
                        break;
                    }
                }
            }
            if (kind == null) {
                return Optional.empty();
            }
            int slash = registryPath.indexOf('/');
            if (slash <= 0) {
                return Optional.empty();
            }
            materialToken = registryPath.substring(0, slash);
            if ("fluid_attachment".equals(materialToken)) {
                String remainder = registryPath.substring(slash + 1);
                String kindSuffix = "_" + kind;
                if (!remainder.endsWith(kindSuffix)
                        || remainder.length() <= kindSuffix.length()) {
                    return Optional.empty();
                }
                materialToken = remainder.substring(
                        0, remainder.length() - kindSuffix.length());
            }
        }
        String resolvedMaterialToken = "stainless".equals(materialToken)
                ? "stainless_steel"
                : materialToken;
        String resolvedKind = kind;
        return chineseMaterialName(resolvedMaterialToken).map(material -> {
            if ("faucet".equals(resolvedKind)) {
                return "stone".equals(resolvedMaterialToken)
                        ? "石制浇铸口"
                        : material + "浇铸口";
            }
            return material + switch (resolvedKind) {
                case "tap" -> "龙头";
                case "funnel" -> "漏斗";
                case "nozzle" -> "喷嘴";
                case "cap_nozzle" -> "有盖喷嘴";
                default -> "";
            };
        });
    }

    static boolean isWallPath(String registryPath) {
        return registryPath.endsWith("/wall") || registryPath.endsWith("_wall");
    }

    static boolean isDenseWall(String registryPath) {
        return registryPath.contains("dense_") && registryPath.endsWith("_wall");
    }

    static String materialToken(String registryPath) {
        if (FoundryHosts.isSmeltingCrucible(registryPath)
                || registryPath.startsWith("foundry/")) {
            return FoundryHosts.token(registryPath);
        }
        return LargeCrucibleHosts.token(registryPath);
    }

    public static String english(String sourceName, String registryPath) {
        if (registryPath != null && registryPath.startsWith("panel/wood")) {
            if (sourceName != null && !sourceName.isBlank()) {
                return sourceName.strip();
            }
            return "Wooden Panel";
        }
        String named = LanguageNames.playerEnglish(sourceName, registryPath);
        if (FoundryHosts.isSmeltingCrucible(registryPath)) {
            return FoundryHosts.englishName(named);
        }
        return named;
    }

    public static Optional<String> chineseMaterialName(String materialId) {
        if (materialId == null || materialId.isEmpty()) {
            return Optional.empty();
        }
        Optional<String> table = MaterialZhNames.material(materialId);
        if (table.isPresent()) {
            return table;
        }
        if (!materialId.endsWith("_elemental")) {
            table = MaterialZhNames.material(materialId + "_elemental");
            if (table.isPresent()) {
                return table;
            }
        }
        return Optional.ofNullable(LOCAL_ZH.get(materialId));
    }

    static Optional<String> chineseMaterial(String registryPath) {
        String token = materialToken(registryPath).toLowerCase(Locale.ROOT);
        if (token.contains("galvanized")) {
            token = "steel_galvanized";
        } else if ("wood".equals(token) && isWallPath(registryPath)) {
            token = "wood_treated";
        }
        Optional<String> named = chineseMaterialName(token);
        if (named.isPresent() || !MaterialCatalog.isBootstrapped()) {
            return named;
        }
        return MteInPlaceMaterials.resolve(token)
                .flatMap(MteInPlaceDisplayNames::chineseMaterialName);
    }
}
