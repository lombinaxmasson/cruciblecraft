package com.masson.cruciblecraft.logistics.pipe.cover;

import java.util.Objects;
import java.util.Optional;

import net.minecraft.resources.ResourceLocation;

/** One immutable face attachment identified by a data definition. */
public record PipeCover(
        ResourceLocation definitionId,
        PipeCoverConfig config) {
    public PipeCover {
        Objects.requireNonNull(definitionId, "definitionId");
        config = config == null ? PipeCoverConfig.EMPTY : config;
        Optional<CoverDefinition> definition =
                CoverDefinitionCatalog.find(definitionId);
        if (definition.isPresent()) {
            definition.orElseThrow().resolve(config);
        }
    }

    /** Legacy source compatibility adapter; new persistence writes definition ids. */
    @Deprecated(forRemoval = false)
    public PipeCover(PipeCoverType type, Optional<String> matchId) {
        this(
                Objects.requireNonNull(type, "type").definitionId(),
                new PipeCoverConfig(
                        matchId,
                        Optional.empty(),
                        Optional.empty(),
                        Optional.empty(),
                        Optional.empty(),
                        Optional.empty(),
                        Optional.empty()));
    }

    public static PipeCover of(String definitionId) {
        return of(ResourceLocation.parse(definitionId));
    }

    public static PipeCover of(ResourceLocation definitionId) {
        CoverDefinitionCatalog.require(definitionId);
        return new PipeCover(definitionId, PipeCoverConfig.EMPTY);
    }

    public Optional<CoverDefinition> definition() {
        return CoverDefinitionCatalog.find(definitionId);
    }

    public CoverBehavior behavior() {
        return CoverBehaviorRegistry.resolve(definition().orElse(null));
    }

    public boolean supports(CoverDefinition.Medium medium) {
        return definition()
                .map(value -> value.medium().supports(medium))
                .orElse(false);
    }

    public PipeCover withConfig(PipeCoverConfig changed) {
        CoverDefinition definition = CoverDefinitionCatalog.require(
                definitionId);
        definition.resolve(changed);
        return new PipeCover(definitionId, changed);
    }

    public PipeCover configure(
            CoverDefinition.ConfigField field, int value) {
        CoverDefinition definition = CoverDefinitionCatalog.require(
                definitionId);
        return behavior().configure(
                this,
                definition,
                new CoverBehavior.ConfigRequest(field, value));
    }

    public Optional<String> matchId() {
        return config.matchId();
    }

    /**
     * Legacy inspection adapter. New behavior definitions do not have an enum
     * representation.
     */
    @Deprecated(forRemoval = false)
    public PipeCoverType type() {
        return PipeCoverType.fromDefinition(definitionId)
                .orElseThrow(() -> new IllegalStateException(
                        "Cover has no legacy PipeCoverType: " + definitionId));
    }

    public static PipeCover filter(String matchId) {
        return new PipeCover(
                PipeCoverType.FILTER,
                Optional.of(matchId));
    }

    public static PipeCover valve() {
        return new PipeCover(
                PipeCoverType.ONE_WAY_VALVE, Optional.empty());
    }

    public static PipeCover pump() {
        return new PipeCover(
                PipeCoverType.OUTPUT_PUMP, Optional.empty());
    }
}
