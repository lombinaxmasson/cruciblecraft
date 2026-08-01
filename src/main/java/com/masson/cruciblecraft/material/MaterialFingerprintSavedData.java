package com.masson.cruciblecraft.material;

import java.util.LinkedHashSet;
import java.util.Set;

import com.masson.cruciblecraft.CrucibleCraft;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.util.datafix.DataFixTypes;
import net.minecraft.world.level.saveddata.SavedData;

public final class MaterialFingerprintSavedData extends SavedData {
    public static final String NAME = CrucibleCraft.MODID + "_material_fingerprint";
    public static final Factory<MaterialFingerprintSavedData> FACTORY = new Factory<>(
            MaterialFingerprintSavedData::new,
            MaterialFingerprintSavedData::load,
            DataFixTypes.LEVEL);

    private String structureFingerprint = "";
    private String tuningFingerprint = "";
    private String legacyFingerprint = "";
    private boolean changedOnLoad;
    private Set<String> materialIds = Set.of();
    private Set<String> missingOnLoad = Set.of();

    private MaterialFingerprintSavedData() {}

    private static MaterialFingerprintSavedData load(
            CompoundTag tag,
            HolderLookup.Provider registries) {
        MaterialFingerprintSavedData data = new MaterialFingerprintSavedData();
        data.structureFingerprint = tag.getString("structure_fingerprint");
        data.tuningFingerprint = tag.getString("tuning_fingerprint");
        data.legacyFingerprint = tag.getString("fingerprint");
        if (tag.contains("material_ids", Tag.TAG_LIST)) {
            LinkedHashSet<String> ids = new LinkedHashSet<>();
            ListTag list = tag.getList("material_ids", Tag.TAG_STRING);
            list.forEach(value -> ids.add(value.getAsString()));
            data.materialIds = Set.copyOf(ids);
        }
        return data;
    }

    public void compareWithCurrent() {
        String currentStructure = MaterialFingerprint.structure(MaterialCatalog.values());
        String currentTuning = MaterialFingerprint.tuning(MaterialCatalog.values());
        Set<String> currentIds = MaterialCatalog.values().stream()
                .map(definition -> definition.id())
                .collect(java.util.stream.Collectors.toCollection(java.util.TreeSet::new));
        missingOnLoad = materialIds.stream()
                .filter(id -> !currentIds.contains(id))
                .collect(java.util.stream.Collectors.toUnmodifiableSet());
        boolean legacyMigration = structureFingerprint.isEmpty() && !legacyFingerprint.isEmpty();
        if (legacyMigration) {
            changedOnLoad = !missingOnLoad.isEmpty();
            CrucibleCraft.LOGGER.info(
                    "Migrating legacy material fingerprint {} to structural {} and tuning {}",
                    legacyFingerprint,
                    currentStructure,
                    currentTuning);
        } else {
            changedOnLoad = !structureFingerprint.isEmpty()
                    && !structureFingerprint.equals(currentStructure);
        }

        boolean tuningChanged = !tuningFingerprint.isEmpty()
                && !tuningFingerprint.equals(currentTuning);
        if (changedOnLoad) {
            CrucibleCraft.LOGGER.warn(
                    "Material registry/save structure changed for this world (saved {}, current {}). "
                            + "Unknown contents will be preserved and affected machines paused.",
                    structureFingerprint,
                    currentStructure);
        }
        if (!missingOnLoad.isEmpty()) {
            CrucibleCraft.LOGGER.warn("Missing world materials: {}", missingOnLoad);
        }
        if (tuningChanged) {
            CrucibleCraft.LOGGER.info(
                    "Material tuning changed for this world (saved {}, current {})",
                    tuningFingerprint,
                    currentTuning);
        }

        boolean needsSave = structureFingerprint.isEmpty()
                || tuningFingerprint.isEmpty()
                || !structureFingerprint.equals(currentStructure)
                || !tuningFingerprint.equals(currentTuning)
                || !materialIds.equals(currentIds);
        if (needsSave) {
            structureFingerprint = currentStructure;
            tuningFingerprint = currentTuning;
            legacyFingerprint = "";
            materialIds = Set.copyOf(currentIds);
            setDirty();
        } else {
            CrucibleCraft.LOGGER.info(
                    "Material structure and tuning fingerprints match this world");
        }
    }

    public boolean changedOnLoad() {
        return changedOnLoad;
    }

    public Set<String> missingOnLoad() {
        return missingOnLoad;
    }

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
        tag.putString("structure_fingerprint", structureFingerprint);
        tag.putString("tuning_fingerprint", tuningFingerprint);
        ListTag ids = new ListTag();
        materialIds.stream().sorted().map(StringTag::valueOf).forEach(ids::add);
        tag.put("material_ids", ids);
        return tag;
    }
}
