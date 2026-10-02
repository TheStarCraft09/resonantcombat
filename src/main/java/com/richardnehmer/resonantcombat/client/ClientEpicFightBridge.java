package com.richardnehmer.resonantcombat.client;

import com.richardnehmer.resonantcombat.ResonantCombat;

import java.lang.reflect.Method;

/** Reflective client-side Epic Fight lookups: cannot break compilation if the API shifts. */
public final class ClientEpicFightBridge {
    private static final String[] MODE_METHODS = {"isEpicFightMode", "isBattleMode"};
    private static Object engine;
    private static Method modeMethod;
    private static boolean resolved, failed;

    /** True while the local player is in Epic Fight (battle) mode. */
    public static boolean isEpicFightMode() {
        if (failed) return false;
        try {
            if (!resolved) resolve();
            if (failed) return false;
            return (Boolean) modeMethod.invoke(engine);
        } catch (ReflectiveOperationException | LinkageError e) {
            failed = true;
            ResonantCombat.LOGGER.error("Client Epic Fight mode check failed", e);
            return false;
        }
    }

    private static void resolve() {
        resolved = true;
        try {
            Class<?> cls = Class.forName("yesman.epicfight.client.ClientEngine");
            engine = cls.getMethod("getInstance").invoke(null);
            for (String name : MODE_METHODS) {
                try {
                    Method m = cls.getMethod(name);
                    if (m.getReturnType() == boolean.class) { modeMethod = m; break; }
                } catch (NoSuchMethodException ignored) { }
            }
            if (modeMethod == null) throw new NoSuchMethodException("no mode method on ClientEngine");
            ResonantCombat.LOGGER.info("Client Epic Fight mode method resolved: ClientEngine#{}", modeMethod.getName());
        } catch (ReflectiveOperationException | LinkageError e) {
            failed = true;
            ResonantCombat.LOGGER.error("Could not resolve ClientEngine mode check - client combat input disabled. Run /resonantcombat debug probe.", e);
        }
    }

    private ClientEpicFightBridge() {}
}
