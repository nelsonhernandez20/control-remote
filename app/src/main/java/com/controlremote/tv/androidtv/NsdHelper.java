package com.controlremote.tv.androidtv;

import android.net.nsd.NsdManager;

import java.lang.reflect.Method;

/**
 * Invoca {@code stopDiscovery} por reflexión: algunos stubs de android.jar no exponen el método al compilador.
 */
public final class NsdHelper {
    private NsdHelper() {}

    public static void stopDiscovery(NsdManager manager, NsdManager.DiscoveryListener listener) {
        if (manager == null || listener == null) return;
        try {
            Method m = NsdManager.class.getMethod("stopDiscovery", NsdManager.DiscoveryListener.class);
            m.invoke(manager, listener);
        } catch (ReflectiveOperationException ignored) {
        }
    }
}
