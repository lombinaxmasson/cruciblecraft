package com.masson.cruciblecraft.material.def;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

/** Immutable, primitive-only metadata imported from an authoritative GT6 dump. */
public record GT6MaterialMetadata(
        int sourceId,
        String sourceName,
        List<String> aliases,
        String state,
        Optional<String> formula,
        SourceThermal sourceThermal,
        ToolStats tool,
        List<MaterialReference> byproducts,
        Map<String, MaterialAmount> processingTargets,
        List<String> materialTags,
        List<String> generationTags,
        double explosionDamage,
        double heatDamage,
        Optional<Double> blastFurnaceTemperatureCelsius,
        Map<String, ElectricalProperties> electricalBySpecification) {
    public static final Codec<GT6MaterialMetadata> CODEC =
            RecordCodecBuilder.create(instance -> instance.group(
                    Codec.INT.fieldOf("source_id").forGetter(GT6MaterialMetadata::sourceId),
                    Codec.STRING.fieldOf("source_name").forGetter(GT6MaterialMetadata::sourceName),
                    Codec.STRING.listOf().optionalFieldOf("aliases", List.of())
                            .forGetter(GT6MaterialMetadata::aliases),
                    Codec.STRING.optionalFieldOf("state", "solid")
                            .forGetter(GT6MaterialMetadata::state),
                    Codec.STRING.optionalFieldOf("formula").forGetter(GT6MaterialMetadata::formula),
                    SourceThermal.CODEC.fieldOf("source_thermal")
                            .forGetter(GT6MaterialMetadata::sourceThermal),
                    ToolStats.CODEC.optionalFieldOf("tool", ToolStats.EMPTY)
                            .forGetter(GT6MaterialMetadata::tool),
                    MaterialReference.CODEC.listOf().optionalFieldOf("byproducts", List.of())
                            .forGetter(GT6MaterialMetadata::byproducts),
                    Codec.unboundedMap(Codec.STRING, MaterialAmount.CODEC)
                            .optionalFieldOf("processing_targets", Map.of())
                            .forGetter(GT6MaterialMetadata::processingTargets),
                    Codec.STRING.listOf().optionalFieldOf("material_tags", List.of())
                            .forGetter(GT6MaterialMetadata::materialTags),
                    Codec.STRING.listOf().optionalFieldOf("generation_tags", List.of())
                            .forGetter(GT6MaterialMetadata::generationTags),
                    Codec.DOUBLE.optionalFieldOf("explosion_damage", 0.0)
                            .forGetter(GT6MaterialMetadata::explosionDamage),
                    Codec.DOUBLE.optionalFieldOf("heat_damage", 0.0)
                            .forGetter(GT6MaterialMetadata::heatDamage),
                    Codec.DOUBLE.optionalFieldOf("blast_furnace_temperature_celsius")
                            .forGetter(GT6MaterialMetadata::blastFurnaceTemperatureCelsius),
                    Codec.unboundedMap(Codec.STRING, ElectricalProperties.CODEC)
                            .optionalFieldOf("electrical_by_specification", Map.of())
                            .forGetter(GT6MaterialMetadata::electricalBySpecification)
            ).apply(instance, GT6MaterialMetadata::new));

    public GT6MaterialMetadata {
        aliases = List.copyOf(aliases);
        byproducts = List.copyOf(byproducts);
        processingTargets = Map.copyOf(processingTargets);
        materialTags = List.copyOf(materialTags);
        generationTags = List.copyOf(generationTags);
        electricalBySpecification = Map.copyOf(electricalBySpecification);
        if (sourceName == null || sourceName.isBlank()) {
            throw new IllegalArgumentException("GT6 source name must not be blank");
        }
        if (!Double.isFinite(explosionDamage) || !Double.isFinite(heatDamage)) {
            throw new IllegalArgumentException("GT6 damage metadata must be finite");
        }
        blastFurnaceTemperatureCelsius.ifPresent(value -> {
            if (!Double.isFinite(value)) {
                throw new IllegalArgumentException("GT6 blast temperature must be finite");
            }
        });
    }

    public record SourceThermal(
            double meltingPointKelvin,
            double meltingPointCelsius,
            double boilingPointKelvin,
            double boilingPointCelsius,
            double plasmaPointKelvin,
            double plasmaPointCelsius,
            double density) {
        public static final Codec<SourceThermal> CODEC =
                RecordCodecBuilder.create(instance -> instance.group(
                        Codec.DOUBLE.fieldOf("melting_point_kelvin")
                                .forGetter(SourceThermal::meltingPointKelvin),
                        Codec.DOUBLE.fieldOf("melting_point_celsius")
                                .forGetter(SourceThermal::meltingPointCelsius),
                        Codec.DOUBLE.fieldOf("boiling_point_kelvin")
                                .forGetter(SourceThermal::boilingPointKelvin),
                        Codec.DOUBLE.fieldOf("boiling_point_celsius")
                                .forGetter(SourceThermal::boilingPointCelsius),
                        Codec.DOUBLE.fieldOf("plasma_point_kelvin")
                                .forGetter(SourceThermal::plasmaPointKelvin),
                        Codec.DOUBLE.fieldOf("plasma_point_celsius")
                                .forGetter(SourceThermal::plasmaPointCelsius),
                        Codec.DOUBLE.fieldOf("density").forGetter(SourceThermal::density)
                ).apply(instance, SourceThermal::new));

        public SourceThermal {
            if (!Double.isFinite(meltingPointKelvin)
                    || !Double.isFinite(meltingPointCelsius)
                    || !Double.isFinite(boilingPointKelvin)
                    || !Double.isFinite(boilingPointCelsius)
                    || !Double.isFinite(plasmaPointKelvin)
                    || !Double.isFinite(plasmaPointCelsius)
                    || !Double.isFinite(density)) {
                throw new IllegalArgumentException("Invalid GT6 source thermal metadata");
            }
        }
    }

    public record ToolStats(long durability, double speed, int quality, long types) {
        public static final ToolStats EMPTY = new ToolStats(0, 1.0, 0, 0);
        public static final Codec<ToolStats> CODEC =
                RecordCodecBuilder.create(instance -> instance.group(
                        Codec.LONG.optionalFieldOf("durability", 0L)
                                .forGetter(ToolStats::durability),
                        Codec.DOUBLE.optionalFieldOf("speed", 1.0)
                                .forGetter(ToolStats::speed),
                        Codec.INT.optionalFieldOf("quality", 0)
                                .forGetter(ToolStats::quality),
                        Codec.LONG.optionalFieldOf("types", 0L)
                                .forGetter(ToolStats::types)
                ).apply(instance, ToolStats::new));

        public ToolStats {
            if (durability < 0 || quality < 0 || types < 0
                    || !Double.isFinite(speed) || speed < 0.0) {
                throw new IllegalArgumentException("Invalid GT6 tool metadata");
            }
        }
    }

    /** Ordered source reference resolved to an active CrucibleCraft material id. */
    public record MaterialReference(String material, int sourceId, String sourceName) {
        public static final Codec<MaterialReference> CODEC =
                RecordCodecBuilder.create(instance -> instance.group(
                        Codec.STRING.fieldOf("material").forGetter(MaterialReference::material),
                        Codec.INT.fieldOf("source_id").forGetter(MaterialReference::sourceId),
                        Codec.STRING.fieldOf("source_name").forGetter(MaterialReference::sourceName)
                ).apply(instance, MaterialReference::new));

        public MaterialReference {
            if (material == null || material.isBlank()
                    || sourceName == null || sourceName.isBlank()) {
                throw new IllegalArgumentException("Invalid GT6 material reference");
            }
        }
    }

    /** Exact GT6 material amount; ccUnits is present only for an integral conversion. */
    public record MaterialAmount(
            String material,
            int sourceMaterialId,
            String sourceMaterialName,
            long numeratorU,
            Optional<Long> ccUnits) {
        public static final Codec<MaterialAmount> CODEC =
                RecordCodecBuilder.create(instance -> instance.group(
                        Codec.STRING.fieldOf("material").forGetter(MaterialAmount::material),
                        Codec.INT.fieldOf("source_material_id")
                                .forGetter(MaterialAmount::sourceMaterialId),
                        Codec.STRING.fieldOf("source_material_name")
                                .forGetter(MaterialAmount::sourceMaterialName),
                        Codec.LONG.fieldOf("numerator_u").forGetter(MaterialAmount::numeratorU),
                        Codec.LONG.optionalFieldOf("cc_units").forGetter(MaterialAmount::ccUnits)
                ).apply(instance, MaterialAmount::new));

        public MaterialAmount {
            if (material == null || material.isBlank()
                    || sourceMaterialName == null || sourceMaterialName.isBlank()
                    || numeratorU < 0) {
                throw new IllegalArgumentException("Invalid GT6 material amount");
            }
        }
    }

    /**
     * Direct GT6 wire/cable registration values keyed by source specification.
     *
     * <p>GT6 defines integral EU loss per traversed block, not physical
     * resistance. Insulation and contact damage are specification properties,
     * not material-wide scalars.
     */
    public record ElectricalProperties(
            long maxVoltage,
            long maxAmperage,
            long lossPerMeter,
            boolean insulated,
            boolean contactDamage) {
        public static final Codec<ElectricalProperties> CODEC =
                RecordCodecBuilder.create(instance -> instance.group(
                        Codec.LONG.fieldOf("max_voltage")
                                .forGetter(ElectricalProperties::maxVoltage),
                        Codec.LONG.fieldOf("max_amperage")
                                .forGetter(ElectricalProperties::maxAmperage),
                        Codec.LONG.fieldOf("loss_per_meter")
                                .forGetter(ElectricalProperties::lossPerMeter),
                        Codec.BOOL.fieldOf("insulated")
                                .forGetter(ElectricalProperties::insulated),
                        Codec.BOOL.fieldOf("contact_damage")
                                .forGetter(ElectricalProperties::contactDamage)
                ).apply(instance, ElectricalProperties::new));

        public ElectricalProperties {
            if (maxVoltage <= 0 || maxAmperage <= 0 || lossPerMeter < 0) {
                throw new IllegalArgumentException("Invalid GT6 electrical metadata");
            }
        }
    }
}
