package com.masson.cruciblecraft.registry;

import java.util.Locale;
import java.util.Set;

/**
 * Routes leftover bath/semantic/MTE identities onto the typed creative tabs.
 * {@code null} means skip (already owned elsewhere, e.g. tool heads).
 */
public final class RemainderCreativeTabs {
    public enum Tab {
        MACHINES,
        TOOLS,
        COMPONENTS,
        BUILDING,
        NATURE,
        WIRES,
        CABLES,
        PIPES,
        MISC
    }

    private static final Set<String> NATURE_ROOTS = Set.of(
            "food",
            "chocolate",
            "cheese",
            "boiled",
            "potato",
            "apple",
            "applewood",
            "rice",
            "rye",
            "oat",
            "barley",
            "butter",
            "salted_butter",
            "peanut",
            "pistachio",
            "honey",
            "mint",
            "vanilla",
            "maple",
            "cherrywood",
            "meat_cooked",
            "tofu",
            "ice");

    private RemainderCreativeTabs() {}

    public static Tab of(String registryPath, String kind) {
        if (registryPath == null || registryPath.isBlank()) {
            return Tab.MISC;
        }
        String normalizedKind = kind == null ? "" : kind;
        if ("tool_head".equals(normalizedKind)) {
            return null;
        }
        String path = registryPath.toLowerCase(Locale.ROOT);
        int slash = path.indexOf('/');
        String root = slash < 0 ? path : path.substring(0, slash);

        if (path.startsWith("foundry/")
                || path.endsWith("/wall")
                || path.endsWith("_wall")
                || (path.startsWith("multiblock/large_") && path.endsWith("_crucible"))) {
            return Tab.MACHINES;
        }
        if ("armor".equals(normalizedKind)) {
            return Tab.TOOLS;
        }
        if ("object".equals(normalizedKind)) {
            return Tab.NATURE;
        }
        if ("block".equals(normalizedKind)) {
            if (path.contains("glowtus") || path.contains("lilypad")) {
                return Tab.NATURE;
            }
            return Tab.BUILDING;
        }
        if ("part".equals(root) || path.contains("crystal_circuit")) {
            return Tab.COMPONENTS;
        }
        if ("tool".equals(root) || path.endsWith("/key") || path.contains("/key")) {
            return Tab.TOOLS;
        }
        if ("processing".equals(root)) {
            return Tab.MACHINES;
        }
        if ("electric_wire".equals(root)) {
            return Tab.CABLES;
        }
        if ("panel".equals(root)
                || path.startsWith("asphalt/")
                || path.startsWith("concrete")) {
            return Tab.BUILDING;
        }
        if ("quadruple".equals(root)
                || "nonuple".equals(root)
                || "fluid_pipe_tile".equals(root)
                || path.contains("fluid_pipe")) {
            return Tab.PIPES;
        }
        if (NATURE_ROOTS.contains(root)
                || path.contains("raisin")
                || path.contains("grape")
                || path.contains("ice_cream")
                || path.contains("dough")
                || path.contains("/comb")
                || path.endsWith("_comb")
                || path.contains("chum")) {
            return Tab.NATURE;
        }
        if ("untyped".equals(root)) {
            return Tab.MACHINES;
        }
        return Tab.MISC;
    }
}
