package com.masson.cruciblecraft.logistics.pipe.cover;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import com.masson.cruciblecraft.logistics.machinecover.MachineCoverCovers;
import com.masson.cruciblecraft.logistics.machinecover.MachineCoverBehaviors;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.fluids.FluidStack;

/** Controlled registry of the small behavior plugins used by cover data. */
public final class CoverBehaviorRegistry {
    private static final Map<ResourceLocation, CoverBehavior> BEHAVIORS =
            new LinkedHashMap<>();
    private static final CoverBehavior FAIL_CLOSED = new CoverBehavior() {
        @Override
        public boolean allowsIncoming(
                PipeCover cover,
                CoverDefinition definition,
                Access access) {
            return false;
        }

        @Override
        public boolean allowsOutgoing(
                PipeCover cover,
                CoverDefinition definition,
                Access access) {
            return false;
        }

        @Override
        public boolean matchesItem(
                PipeCover cover,
                CoverDefinition definition,
                ItemStack stack) {
            return false;
        }

        @Override
        public boolean matchesFluid(
                PipeCover cover,
                CoverDefinition definition,
                FluidStack stack) {
            return false;
        }

        @Override
        public PipeCover configure(
                PipeCover cover,
                CoverDefinition definition,
                ConfigRequest request) {
            throw new IllegalStateException(
                    "Unresolved cover behavior cannot be configured");
        }
    };

    static {
        registerBuiltin("filter", matchingBehavior());
        registerBuiltin("shutter", new CoverBehavior() {
            @Override
            public boolean allowsIncoming(
                    PipeCover cover,
                    CoverDefinition definition,
                    Access access) {
                return false;
            }
        });
        registerBuiltin("pump_adapter", activeTransfer(true, true));
        registerBuiltin("conveyor", activeTransfer(true, false));
        registerBuiltin("retriever_item", new RetrieverCoverBehavior());
        registerBuiltin("robot_arm", activeTransfer(true, false));
        registerBuiltin("pressure_valve", new CoverBehavior() {
            @Override
            public boolean allowsIncoming(
                    PipeCover cover,
                    CoverDefinition definition,
                    Access access) {
                if (access.medium() != CoverDefinition.Medium.FLUID) {
                    return false;
                }
                int threshold = definition.resolve(
                        cover.config()).pressureThreshold();
                return threshold > 0
                        && access.storedAmount()
                                < Math.min(threshold, access.capacity());
            }

            @Override
            public int limitIncoming(
                    PipeCover cover,
                    CoverDefinition definition,
                    Access access,
                    int requested) {
                int threshold = Math.min(
                        definition.resolve(cover.config())
                                .pressureThreshold(),
                        access.capacity());
                return Math.max(
                        0,
                        Math.min(
                                requested,
                                threshold - access.storedAmount()));
            }
        });
        registerBuiltin("selector_manual", new CoverBehavior() {});
    }

    public static synchronized void register(
            ResourceLocation id, CoverBehavior behavior) {
        if (id == null || behavior == null) {
            throw new NullPointerException("Cover behavior id/plugin");
        }
        if (!"cruciblecraft".equals(id.getNamespace())) {
            throw new IllegalArgumentException(
                    "External cover plugins require an explicit API contract");
        }
        if (BEHAVIORS.putIfAbsent(id, behavior) != null) {
            throw new IllegalStateException(
                    "Duplicate cover behavior plugin " + id);
        }
    }

    public static synchronized CoverBehavior resolve(
            CoverDefinition definition) {
        if (definition == null) {
            return FAIL_CLOSED;
        }
        return BEHAVIORS.getOrDefault(
                definition.behaviorId(), FAIL_CLOSED);
    }

    public static synchronized Set<ResourceLocation> registeredIds() {
        return Collections.unmodifiableSet(
                new LinkedHashSet<>(BEHAVIORS.keySet()));
    }

    public static synchronized void validateDefinitions() {
        com.masson.cruciblecraft.logistics.itemnet.ItemNetworkCovers.bootstrap();
        com.masson.cruciblecraft.logistics.fluidnet.FluidNetworkCovers.bootstrap();
        com.masson.cruciblecraft.logistics.genericnet.GenericNetworkCovers.bootstrap();
        com.masson.cruciblecraft.logistics.core.LogisticsDumpCovers.bootstrap();
        com.masson.cruciblecraft.logistics.displaycpu.DisplayCpuCovers.bootstrap();
        MachineCoverCovers.bootstrap();
        MachineCoverBehaviors.validateDefinitions();
        for (CoverDefinition definition
                : CoverDefinitionCatalog.definitions()) {
            if (!BEHAVIORS.containsKey(definition.behaviorId())) {
                throw new IllegalStateException(
                        definition.id()
                                + ": unregistered cover behavior "
                                + definition.behaviorId());
            }
        }
    }

    private static void registerBuiltin(
            String path, CoverBehavior behavior) {
        register(ResourceLocation.fromNamespaceAndPath(
                "cruciblecraft", path), behavior);
    }

    private static CoverBehavior matchingBehavior() {
        return new CoverBehavior() {
            @Override
            public boolean matchesItem(
                    PipeCover cover,
                    CoverDefinition definition,
                    ItemStack stack) {
                return matches(
                        cover.config().matchId(),
                        BuiltInRegistries.ITEM.getKey(
                                stack.getItem()).toString());
            }

            @Override
            public boolean matchesFluid(
                    PipeCover cover,
                    CoverDefinition definition,
                    FluidStack stack) {
                return matches(
                        cover.config().matchId(),
                        BuiltInRegistries.FLUID.getKey(
                                stack.getFluid()).toString());
            }
        };
    }

    private static CoverBehavior activeTransfer(
            boolean items, boolean fluids) {
        return new CoverBehavior() {
            @Override
            public boolean matchesItem(
                    PipeCover cover,
                    CoverDefinition definition,
                    ItemStack stack) {
                return matches(
                        cover.config().matchId(),
                        BuiltInRegistries.ITEM.getKey(
                                stack.getItem()).toString());
            }

            @Override
            public boolean matchesFluid(
                    PipeCover cover,
                    CoverDefinition definition,
                    FluidStack stack) {
                return matches(
                        cover.config().matchId(),
                        BuiltInRegistries.FLUID.getKey(
                                stack.getFluid()).toString());
            }

            @Override
            public void tick(
                    PipeCover cover,
                    CoverDefinition definition,
                    TransferContext context) {
                CoverDefinition.Values values = definition.resolve(
                        cover.config());
                if (!CoverTransferTiming.due(
                        context.world(), values.interval())) {
                    return;
                }
                int amount = values.exactCount() > 0
                        ? values.exactCount()
                        : values.rate();
                if (amount <= 0) {
                    return;
                }
                if (items
                        && context.medium()
                                == CoverDefinition.Medium.ITEM) {
                    context.transferItems(
                            amount,
                            cover.config().matchId(),
                            values.mode());
                } else if (fluids
                        && context.medium()
                                == CoverDefinition.Medium.FLUID) {
                    context.transferFluids(
                            amount,
                            cover.config().matchId(),
                            values.mode());
                }
            }
        };
    }

    private static boolean matches(
            Optional<String> expected, String actual) {
        return expected.isEmpty()
                || expected.orElseThrow().equals(actual);
    }

    private CoverBehaviorRegistry() {}
}
