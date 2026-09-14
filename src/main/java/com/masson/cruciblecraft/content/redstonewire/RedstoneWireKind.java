package com.masson.cruciblecraft.content.redstonewire;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

import com.masson.cruciblecraft.CrucibleCraft;
import com.masson.cruciblecraft.api.material.MaterialPrefix;
import com.masson.cruciblecraft.api.material.MaterialPrefixes;

import net.minecraft.resources.ResourceLocation;

/**
 * GT6 Redstone Wires 27000 / 27050 / 27500 ({@code OP.wireGt01}) and insulated
 * cables 27006 / 27056 / 27506 ({@code OP.cableGt01}). Not EU {@code CableBlock}.
 */
public enum RedstoneWireKind {
    RED_ALLOY(
            27000,
            "red_alloy",
            MaterialPrefixes.WIRE,
            "wire",
            "Red Alloy Wire",
            "红合金红石线",
            16,
            false,
            false),
    SIGNALUM(
            27050,
            "signalum",
            MaterialPrefixes.WIRE,
            "wire",
            "Signalum Wire",
            "信素红石线",
            64,
            false,
            false),
    LUMIUM_WIRELAMP(
            27500,
            "lumium",
            MaterialPrefixes.WIRE,
            "wire",
            "Lumium Wirelamp",
            "流明灯线",
            16,
            true,
            false),
    RED_ALLOY_CABLE(
            27006,
            "red_alloy",
            MaterialPrefixes.CABLE,
            "cable",
            "Red Alloy Cable",
            "红合金绝缘红石线",
            16,
            false,
            true),
    SIGNALUM_CABLE(
            27056,
            "signalum",
            MaterialPrefixes.CABLE,
            "cable",
            "Signalum Cable",
            "信素绝缘红石线",
            64,
            false,
            true),
    LUMIUM_CABLE(
            27506,
            "lumium",
            MaterialPrefixes.CABLE,
            "cable",
            "Lumium Cable",
            "流明绝缘红石线",
            16,
            false,
            true);

    public static final int EXPECTED_SIZE = 3;
    public static final int EXPECTED_INSULATED = 3;
    public static final int EXPECTED_CATALOG = 6;
    public static final MaterialPrefix FORM = MaterialPrefixes.WIRE;

    private static final List<RedstoneWireKind> BARE =
            List.of(RED_ALLOY, SIGNALUM, LUMIUM_WIRELAMP);
    private static final List<RedstoneWireKind> INSULATED =
            List.of(RED_ALLOY_CABLE, SIGNALUM_CABLE, LUMIUM_CABLE);
    private static final List<RedstoneWireKind> CATALOG = List.of(values());
    private static final Map<String, RedstoneWireKind> BY_PATH =
            Arrays.stream(values()).collect(Collectors.toUnmodifiableMap(
                    RedstoneWireKind::path, Function.identity()));

    private final int sourceId;
    private final String path;
    private final ResourceLocation id;
    private final String materialId;
    private final MaterialPrefix form;
    private final String langEn;
    private final String langZh;
    private final int range;
    private final boolean glowing;
    private final boolean insulated;

    RedstoneWireKind(
            int sourceId,
            String materialId,
            MaterialPrefix form,
            String formPath,
            String langEn,
            String langZh,
            int range,
            boolean glowing,
            boolean insulated) {
        this.sourceId = sourceId;
        this.materialId = materialId;
        this.form = form;
        this.path = materialId + "/" + formPath;
        this.id = ResourceLocation.fromNamespaceAndPath(CrucibleCraft.MODID, path);
        this.langEn = langEn;
        this.langZh = langZh;
        this.range = range;
        this.glowing = glowing;
        this.insulated = insulated;
    }

    public static List<RedstoneWireKind> all() {
        return BARE;
    }

    public static List<RedstoneWireKind> insulatedKinds() {
        return INSULATED;
    }

    public static List<RedstoneWireKind> catalog() {
        return CATALOG;
    }

    public static boolean owns(String registryPath) {
        return BY_PATH.containsKey(registryPath);
    }

    public static boolean owns(String materialId, MaterialPrefix form) {
        return byPath(materialId + "/" + form.serializedName()).isPresent();
    }

    public static Optional<RedstoneWireKind> byPath(String registryPath) {
        return Optional.ofNullable(BY_PATH.get(registryPath));
    }

    public int sourceId() {
        return sourceId;
    }

    public String path() {
        return path;
    }

    public ResourceLocation id() {
        return id;
    }

    public String materialId() {
        return materialId;
    }

    public MaterialPrefix form() {
        return form;
    }

    public String langEn() {
        return langEn;
    }

    public String langZh() {
        return langZh;
    }

    public int range() {
        return range;
    }

    public boolean glowing() {
        return glowing;
    }

    public boolean insulated() {
        return insulated;
    }

    public int widthPixels() {
        return insulated ? 4 : 2;
    }

    public long loss() {
        return RedstoneWireNetwork.MAX_RANGE / range;
    }
}
