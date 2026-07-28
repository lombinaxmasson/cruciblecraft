package com.masson.cruciblecraft.compat.kubejs;

public final class KubeJSCompat {
    private KubeJSCompat() {}

    public static void fireMaterialRegistration() {
        if (CrucibleCraftKubeJSPlugin.ADD.hasListeners()) {
            CrucibleCraftKubeJSPlugin.ADD.post(new MaterialRegistrationEventJS());
        }
    }
}
