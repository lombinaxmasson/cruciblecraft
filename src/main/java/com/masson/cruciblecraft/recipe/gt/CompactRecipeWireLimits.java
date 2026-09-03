package com.masson.cruciblecraft.recipe.gt;

/**
 * Product ceilings for compact family network encoding. These are not vanilla
 * packet limits; they keep one RecipeHolder well below the 2 MiB NBT accounter.
 */
public final class CompactRecipeWireLimits {
    public static final int WIRE_VERSION = 2;
    public static final byte WIRE_FORM_INLINE = 0;
    public static final byte WIRE_FORM_MATRIX_V1 = 1;
    public static final int MAX_RECIPE_ENTRY_WIRE_BYTES = 524_288;
    public static final int DECODE_RELATIONS_CEILING = 4_096;
    public static final int MAX_IO_PER_RELATION = 16;
    public static final int MAX_DICTIONARY_ENTRIES = 8_192;
    public static final int MAX_PROVENANCE_KEYS = 256;
    public static final int MAX_ACTIONS = 64;
    public static final int MAX_STABLE_ID_PREFIXES = 256;
    public static final int MAX_PARAMETERIZED_ENTRIES = 16;
    public static final int MAX_FAMILY_ID_LENGTH = 256;
    public static final int MAX_SOURCE_REVISION_LENGTH = 128;
    public static final int MAX_SOURCE_KIND_LENGTH = 64;
    public static final int MAX_SELECTED_SOURCE_LENGTH = 256;
    public static final int MAX_TEMPLATE_LENGTH = 128;
    public static final int MAX_PARAMETER_KEY_LENGTH = 64;
    public static final int MAX_PARAMETER_VALUE_LENGTH = 256;
    public static final int STABLE_ID_SUFFIX_HEX_LENGTH = 16;

    private CompactRecipeWireLimits() {}
}
