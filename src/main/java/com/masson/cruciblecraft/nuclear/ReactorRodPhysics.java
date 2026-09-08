package com.masson.cruciblecraft.nuclear;

import com.masson.cruciblecraft.content.item.ReactorRodItem;

import net.minecraft.world.item.ItemStack;

/** GT6 neutron emission / reflection / reaction for one rod slot. */
public final class ReactorRodPhysics {
    private ReactorRodPhysics() {}

    public static long divUp(long value, long divisor) {
        if (divisor <= 0L) {
            throw new IllegalArgumentException("divisor");
        }
        if (value <= 0L) {
            return 0L;
        }
        return (value + divisor - 1L) / divisor;
    }

    public static int emit(
            ReactorCoreHost core, int slot, ItemStack stack) {
        if (!(stack.getItem() instanceof ReactorRodItem rod)) {
            return 0;
        }
        ReactorRodCatalog.Entry entry = rod.entry();
        if (entry.kind() != ReactorRodCatalog.Kind.NUCLEAR) {
            return 0;
        }
        ReactorCoolant coolant = core.coolant();
        int other = entry.neutronOther();
        int self = entry.neutronSelf();
        int div = Math.max(1, entry.neutronDiv());
        if (coolant == ReactorCoolant.CARBON_DIOXIDE) {
            self *= 3;
        } else if (coolant == ReactorCoolant.HELIUM
                || coolant == ReactorCoolant.MOLTEN_LICL) {
            other -= (int) divUp(other, 2L);
        }
        if (coolant == ReactorCoolant.MOLTEN_LICL) {
            self *= 5;
        }
        if (coolant == ReactorCoolant.MOLTEN_TIN
                || coolant == ReactorCoolant.MOLTEN_SODIUM) {
            div = Math.max(1, div - 1);
        }
        core.addNeutrons(slot, self);
        long extra = divUp(
                Math.max(core.oldNeutrons(slot) - self, 0L), div);
        return bindInt(other + extra);
    }

    public static boolean react(
            ReactorCoreHost core, int slot, ItemStack stack) {
        if (!(stack.getItem() instanceof ReactorRodItem rod)) {
            return false;
        }
        ReactorRodCatalog.Entry entry = rod.entry();
        ReactorRodState state = ReactorRodItem.state(stack);
        return switch (entry.kind()) {
            case ABSORBER -> {
                core.addHeat(core.oldNeutrons(slot) * 2L);
                yield true;
            }
            case PRODUCT -> {
                core.addHeat(core.oldNeutrons(slot) / 2L);
                yield true;
            }
            case BREEDER -> {
                core.addHeat(core.oldNeutrons(slot) / 2L);
                long remaining = state.durability() - core.oldNeutrons(slot);
                if (remaining <= 0L) {
                    core.replaceRod(
                            slot,
                            ReactorRodItem.transform(stack, entry.productPath()));
                } else {
                    ReactorRodItem.writeState(
                            stack, state.withDurability(remaining));
                }
                yield true;
            }
            case NUCLEAR -> {
                core.addHeat(core.oldNeutrons(slot));
                ReactorCoolant coolant = core.coolant();
                ReactorRodState next = state;
                if (coolant != null && coolant.moderatesFuel()) {
                    next = next.markModerated();
                }
                int maximum = neutronMaximum(entry, coolant);
                long loss = core.oldNeutrons(slot) <= maximum
                        ? 100L
                        : divUp(400L * core.oldNeutrons(slot), maximum);
                if (next.previouslyModerated()) {
                    loss *= 4L;
                }
                long remaining = next.durability() - loss;
                if (remaining <= 0L) {
                    core.replaceRod(
                            slot,
                            ReactorRodItem.transform(
                                    stack, entry.depletedPath()));
                } else {
                    ReactorRodItem.writeState(
                            stack, next.withDurability(remaining));
                }
                yield true;
            }
            default -> false;
        };
    }

    public static int reflect(
            ReactorCoreHost core,
            int slot,
            ItemStack stack,
            int neutrons,
            boolean moderated) {
        if (!(stack.getItem() instanceof ReactorRodItem rod) || neutrons == 0) {
            return 0;
        }
        ReactorRodCatalog.Entry entry = rod.entry();
        ReactorRodState state = ReactorRodItem.state(stack);
        return switch (entry.kind()) {
            case REFLECTOR -> neutrons;
            case MODERATOR -> {
                if (neutrons > 0) {
                    ReactorRodItem.writeState(stack, state.incrementModeration());
                }
                yield state.previousModerationHits() * neutrons;
            }
            case BREEDER -> {
                if (!moderated && neutrons > entry.neutronLoss()) {
                    core.addNeutrons(slot, neutrons - entry.neutronLoss());
                }
                yield 0;
            }
            case NUCLEAR -> {
                ReactorRodState next = moderated ? state.markModerated() : state;
                ReactorRodItem.writeState(stack, next);
                core.addNeutrons(slot, neutrons);
                yield 0;
            }
            case ABSORBER, PRODUCT -> {
                core.addNeutrons(slot, neutrons);
                yield 0;
            }
            default -> 0;
        };
    }

    public static boolean moderated(ItemStack stack) {
        if (!(stack.getItem() instanceof ReactorRodItem rod)) {
            return false;
        }
        return switch (rod.entry().kind()) {
            case MODERATOR -> true;
            case NUCLEAR -> ReactorRodItem.state(stack).previouslyModerated();
            default -> false;
        };
    }

    public static void updateModeration(ItemStack stack) {
        if (!(stack.getItem() instanceof ReactorRodItem rod)) {
            return;
        }
        ReactorRodState state = ReactorRodItem.state(stack);
        if (rod.entry().kind() == ReactorRodCatalog.Kind.MODERATOR) {
            ReactorRodItem.writeState(stack, state.snapshotModeratorHits());
            return;
        }
        if (rod.entry().kind() == ReactorRodCatalog.Kind.NUCLEAR) {
            ReactorRodItem.writeState(stack, state.cycleModeration());
        }
    }

    public static int neutronMaximum(
            ReactorRodCatalog.Entry entry, ReactorCoolant coolant) {
        if (coolant == ReactorCoolant.MOLTEN_LICL) {
            return entry.neutronMax() + (int) divUp(entry.neutronMax(), 4L);
        }
        if (coolant == ReactorCoolant.HEAVY_WATER) {
            return (int) divUp(entry.neutronMax(), 8L);
        }
        if (coolant == ReactorCoolant.TRITIATED_WATER) {
            return (int) divUp(entry.neutronMax(), 16L);
        }
        return entry.neutronMax();
    }

    private static int bindInt(long value) {
        if (value > Integer.MAX_VALUE) {
            return Integer.MAX_VALUE;
        }
        if (value < 0L) {
            return 0;
        }
        return (int) value;
    }
}
