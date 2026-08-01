package com.masson.cruciblecraft;

import java.lang.reflect.Method;
import java.util.stream.Stream;

import com.masson.cruciblecraft.registry.ModItemTags;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/** Installs a tag-backed shape fixture without mutating the frozen item registry. */
public final class TestExtruderShapes {
    private static boolean installed;

    private TestExtruderShapes() {}

    public static synchronized ItemStack stack() {
        if (!installed) {
            var holder = BuiltInRegistries.ITEM.wrapAsHolder(Items.TRIAL_KEY);
            var tags = Stream.concat(
                    holder.tags(),
                    Stream.of(ModItemTags.EXTRUDER_SHAPES)).distinct().toList();
            try {
                Method bindTags =
                        holder.getClass().getDeclaredMethod("bindTags", java.util.Collection.class);
                bindTags.setAccessible(true);
                bindTags.invoke(holder, tags);
            } catch (ReflectiveOperationException exception) {
                throw new AssertionError("Unable to install extruder shape test tag", exception);
            }
            installed = true;
        }
        return new ItemStack(Items.TRIAL_KEY);
    }
}
