package com.masson.cruciblecraft.recipe.gt;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.codecs.RecordCodecBuilder;

/** Side effect applied to the slot allocated to one item input row. */
public record ItemInputAction(Kind kind, int damage) {
    public static final ItemInputAction CONSUME = new ItemInputAction(Kind.CONSUME, 0);
    public static final ItemInputAction PRESERVE = new ItemInputAction(Kind.PRESERVE, 0);

    public static final Codec<ItemInputAction> CODEC =
            RecordCodecBuilder.create(instance -> instance.group(
                    Kind.CODEC.fieldOf("kind").forGetter(ItemInputAction::kind),
                    Codec.INT.optionalFieldOf("damage", 0)
                            .forGetter(ItemInputAction::damage)
            ).apply(instance, ItemInputAction::new));

    public ItemInputAction {
        if (kind == null) {
            throw new NullPointerException("kind");
        }
        if (damage < 0 || kind != Kind.WEAR && damage != 0) {
            throw new IllegalArgumentException(
                    "Only WEAR actions may declare non-negative damage");
        }
        if (kind == Kind.WEAR && damage == 0) {
            throw new IllegalArgumentException("WEAR damage must be positive");
        }
    }

    public static ItemInputAction wear(int damage) {
        return new ItemInputAction(Kind.WEAR, damage);
    }

    public enum Kind {
        CONSUME,
        PRESERVE,
        WEAR;

        private static final Codec<Kind> CODEC = Codec.STRING.comapFlatMap(
                value -> java.util.Arrays.stream(values())
                        .filter(kind -> kind.name().equalsIgnoreCase(value))
                        .findFirst()
                        .map(DataResult::success)
                        .orElseGet(() -> DataResult.error(
                                () -> "Unknown item input action kind: " + value)),
                value -> value.name().toLowerCase(java.util.Locale.ROOT));
    }
}
