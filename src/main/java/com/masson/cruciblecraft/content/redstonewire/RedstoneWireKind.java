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
 * GT6 Redstone Wires 27000 / 27050 / 27500. These are {@code OP.wireGt01}
 * BlockItems, not a second dummy id and not EU {@code CableBlock}. Insulated
 * 27006 / 27056 / 27506 are laminator extras and stay out of this catalog.
 */
public enum RedstoneWireKind {
    RED_ALLOY(
            27000,
            "red_alloy",
            "Red Alloy Wire",
            "红合金红石线",
            16,
            false),
    SIGNALUM(
            27050,
            "signalum",
            "Signalum Wire",
            "信素红石线",
            64,
            false),
    LUMIUM_WIRELAMP(
            27500,
            "lumium",
            "Lumium Wirelamp",
            "流明灯线",
            16,
            true);

    public static final int EXPECTED_SIZE = 3;
    public static final MaterialPrefix FORM = MaterialPrefixes.WIRE;

    private static final List<RedstoneWireKind> ALL = List.of(values());
    private static final Map<String, RedstoneWireKind> BY_PATH =
            Arrays.stream(values()).collect(Collectors.toUnmodifiableMap(
                    RedstoneWireKind::path, Function.identity()));
    private static final Map<String, RedstoneWireKind> BY_MATERIAL =
            Arrays.stream(values()).collect(Collectors.toUnmodifiableMap(
                    RedstoneWireKind::materialId, Function.identity()));

    private final int sourceId;
    private final String path;
    private final ResourceLocation id;
    private final String materialId;
    private final String langEn;
    private final String langZh;
    private final int range;
    private final boolean glowing;

    RedstoneWireKind(
            int sourceId,
            String materialId,
            String langEn,
            String langZh,
            int range,
            boolean glowing) {
        this.sourceId = sourceId;
        this.materialId = materialId;
        this.path = materialId + "/wire";
        this.id = ResourceLocation.fromNamespaceAndPath(CrucibleCraft.MODID, path);
        this.langEn = langEn;
        this.langZh = langZh;
        this.range = range;
        this.glowing = glowing;
    }

    public static List<RedstoneWireKind> all() {
        return ALL;
    }

    public static boolean owns(String registryPath) {
        return BY_PATH.containsKey(registryPath);
    }

    public static boolean owns(String materialId, MaterialPrefix form) {
        return FORM.equals(form) && BY_MATERIAL.containsKey(materialId);
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
        return FORM;
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

    public long loss() {
        return RedstoneWireNetwork.MAX_RANGE / range;
    }
}
