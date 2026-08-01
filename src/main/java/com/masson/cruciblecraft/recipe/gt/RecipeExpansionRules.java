package com.masson.cruciblecraft.recipe.gt;

final class RecipeExpansionRules {
    private RecipeExpansionRules() {}

    static int comparePriority(
            boolean leftMaterialSpecific,
            String leftId,
            boolean rightMaterialSpecific,
            String rightId) {
        int specificity = Boolean.compare(
                rightMaterialSpecific,
                leftMaterialSpecific);
        return specificity != 0 ? specificity : leftId.compareTo(rightId);
    }

    static int chanceToTenThousandths(double chance) {
        if (!Double.isFinite(chance) || chance < 0.0 || chance > 1.0) {
            throw new IllegalArgumentException("Chance must be between 0 and 1");
        }
        return (int) Math.round(chance * GTRecipe.GUARANTEED_CHANCE);
    }

    static boolean formsAvailable(
            boolean primaryInput,
            boolean primaryOutput,
            boolean secondInputRequired,
            boolean secondInput,
            boolean secondaryOutputRequired,
            boolean secondaryOutput) {
        return primaryInput
                && primaryOutput
                && (!secondInputRequired || secondInput)
                && (!secondaryOutputRequired || secondaryOutput);
    }

    static String expandedPath(String rulePath, String materialId) {
        if (rulePath == null || rulePath.isBlank()
                || materialId == null || materialId.isBlank()) {
            throw new IllegalArgumentException("Rule path and material id must not be blank");
        }
        return rulePath + "/" + materialId;
    }
}
