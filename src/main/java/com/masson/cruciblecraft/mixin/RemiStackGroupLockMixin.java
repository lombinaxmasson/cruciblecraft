package com.masson.cruciblecraft.mixin;

import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/**
 * Reliable EMI 4.7.3 fills {@code itemToGroupedStacks} on EMI's reload worker
 * and iterates those same ArrayLists on the render thread when the creative
 * inventory opens. Snapshot the shared lists under one lock so that overlap
 * does not throw {@link java.util.ConcurrentModificationException}.
 *
 * <p>Injection counts match reliable-emi 4.7.3. Local group maps are left
 * live; copying them would duplicate every member of a group once per stack.
 */
@Mixin(targets = "com.evandev.remi.feature.stackgroup.StackGroupManager", remap = false)
abstract class RemiStackGroupLockMixin {
    private static final Object GROUP_INDEX = new Object();

    @WrapOperation(
            method = {
                "buildGroupedStacks(Ljava/util/List;)Ljava/util/List;",
                "buildGroupedIngredients(Ljava/util/List;Ldev/emi/emi/config/SidebarType;)Ljava/util/List;"
            },
            at = @At(
                    value = "INVOKE",
                    target = "Ljava/util/Map;get(Ljava/lang/Object;)Ljava/lang/Object;"),
            require = 6,
            allow = 6)
    private static Object cruciblecraft$snapshotMapGet(
            Map<Object, Object> map, Object key, Operation<Object> original) {
        synchronized (GROUP_INDEX) {
            return copyIfList(original.call(map, key));
        }
    }

    @WrapOperation(
            method = {
                "buildGroupedStacks(Ljava/util/List;)Ljava/util/List;",
                "buildGroupedIngredients(Ljava/util/List;Ldev/emi/emi/config/SidebarType;)Ljava/util/List;"
            },
            at = @At(
                    value = "INVOKE",
                    target = "Ljava/util/IdentityHashMap;get(Ljava/lang/Object;)Ljava/lang/Object;"),
            require = 6,
            allow = 6)
    private static Object cruciblecraft$snapshotIdentityGet(
            IdentityHashMap<Object, Object> map, Object key, Operation<Object> original) {
        synchronized (GROUP_INDEX) {
            Object value = original.call(map, key);
            if (map == RemiStackGroupFields.cruciblecraft$stackToGroupedStacks()) {
                return copyIfList(value);
            }
            return value;
        }
    }

    @WrapOperation(
            method = "buildGroupedEmiStacksAndStackGroupToContents(Ljava/util/List;)V",
            at = @At(value = "INVOKE", target = "Ljava/util/Map;clear()V"),
            require = 3,
            allow = 3)
    private static void cruciblecraft$lockedMapClear(
            Map<Object, Object> map, Operation<Void> original) {
        synchronized (GROUP_INDEX) {
            original.call(map);
        }
    }

    @WrapOperation(
            method = "buildGroupedEmiStacksAndStackGroupToContents(Ljava/util/List;)V",
            at = @At(value = "INVOKE", target = "Ljava/util/IdentityHashMap;clear()V"),
            require = 1,
            allow = 1)
    private static void cruciblecraft$lockedIdentityClear(
            IdentityHashMap<Object, Object> map, Operation<Void> original) {
        synchronized (GROUP_INDEX) {
            original.call(map);
        }
    }

    @WrapOperation(
            method = "buildGroupedEmiStacksAndStackGroupToContents(Ljava/util/List;)V",
            at = @At(value = "INVOKE", target = "Ljava/util/List;clear()V"),
            require = 1,
            allow = 1)
    private static void cruciblecraft$lockedListClear(List<Object> list, Operation<Void> original) {
        synchronized (GROUP_INDEX) {
            original.call(list);
        }
    }

    @WrapOperation(
            method = "registerMatch(Lcom/evandev/remi/feature/stackgroup/data/StackGroup;Ldev/emi/emi/api/stack/EmiStack;Ljava/util/Map;)V",
            at = @At(
                    value = "INVOKE",
                    target = "Ljava/util/Map;computeIfAbsent(Ljava/lang/Object;Ljava/util/function/Function;)Ljava/lang/Object;"),
            require = 1,
            allow = 1)
    private static Object cruciblecraft$lockedComputeIfAbsent(
            Map<Object, Object> map,
            Object key,
            Function<Object, Object> mapping,
            Operation<Object> original) {
        synchronized (GROUP_INDEX) {
            return original.call(map, key, mapping);
        }
    }

    @WrapOperation(
            method = "registerMatch(Lcom/evandev/remi/feature/stackgroup/data/StackGroup;Ldev/emi/emi/api/stack/EmiStack;Ljava/util/Map;)V",
            at = @At(
                    value = "INVOKE",
                    target = "Ljava/util/IdentityHashMap;computeIfAbsent(Ljava/lang/Object;Ljava/util/function/Function;)Ljava/lang/Object;"),
            require = 1,
            allow = 1)
    private static Object cruciblecraft$lockedIdentityComputeIfAbsent(
            IdentityHashMap<Object, Object> map,
            Object key,
            Function<Object, Object> mapping,
            Operation<Object> original) {
        synchronized (GROUP_INDEX) {
            return original.call(map, key, mapping);
        }
    }

    @WrapOperation(
            method = "registerMatch(Lcom/evandev/remi/feature/stackgroup/data/StackGroup;Ldev/emi/emi/api/stack/EmiStack;Ljava/util/Map;)V",
            at = @At(value = "INVOKE", target = "Ljava/util/List;add(Ljava/lang/Object;)Z"),
            require = 3,
            allow = 3)
    private static boolean cruciblecraft$lockedAdd(
            List<Object> list, Object element, Operation<Boolean> original) {
        synchronized (GROUP_INDEX) {
            return original.call(list, element);
        }
    }

    private static Object copyIfList(Object value) {
        if (value instanceof List<?> list) {
            return new ArrayList<>(list);
        }
        return value;
    }
}
