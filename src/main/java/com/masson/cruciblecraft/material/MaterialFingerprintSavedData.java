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

    private String fingerprint = "";
    private boolean changedOnLoad;
    private Set<String> materialIds = Set.of();
    private Set<String> missingOnLoad = Set.of();

    private MaterialFingerprintSavedData() {}

    private static MaterialFingerprintSavedData load(
            CompoundTag tag,
            HolderLookup.Provider registries) {
        MaterialFingerprintSavedData data = new MaterialFingerprintSavedData();
        data.fingerprint = tag.getString("fingerprint");
        if (tag.contains("material_ids", Tag.TAG_LIST)) {
            LinkedHashSet<String> ids = new LinkedHashSet<>();
            ListTag list = tag.getList("material_ids", Tag.TAG_STRING);
            list.forEach(value -> ids.add(value.getAsString()));
            data.materialIds = Set.copyOf(ids);
        }
        return data;
    }

    public void compareWithCurrent() {
        String current = MaterialFingerprint.compute(MaterialCatalog.values());
        Set<String> currentIds = MaterialCatalog.values().stream()
                .map(definition -> definition.id())
                .collect(java.util.stream.Collectors.toCollection(java.util.TreeSet::new));
        missingOnLoad = materialIds.stream()
                .filter(id -> !currentIds.contains(id))
                .collect(java.util.stream.Collectors.toUnmodifiableSet());
        changedOnLoad = !fingerprint.isEmpty() && !fingerprint.equals(current);
        if (fingerprint.isEmpty() || changedOnLoad) {
            if (changedOnLoad) {
                CrucibleCraft.LOGGER.warn(
                        "Material definitions changed for this world (saved {}, current {}). "
                                + "Unknown contents will be preserved and affected machines paused.",
                        fingerprint,
                        current);
                if (!missingOnLoad.isEmpty()) {
                    CrucibleCraft.LOGGER.warn("Missing world materials: {}", missingOnLoad);
                }
            } else {
                CrucibleCraft.LOGGER.info("Recording material fingerprint {} for this world", current);
            }
            fingerprint = current;
            materialIds = Set.copyOf(currentIds);
            setDirty();
        } else {
            CrucibleCraft.LOGGER.info("Material fingerprint matches this world ({})", current);
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
        tag.putString("fingerprint", fingerprint);
        ListTag ids = new ListTag();
        materialIds.stream().sorted().map(StringTag::valueOf).forEach(ids::add);
        tag.put("material_ids", ids);
        return tag;
    }
}
