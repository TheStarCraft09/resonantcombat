package com.richardnehmer.resonantcombat.integration.epicfight;

import com.richardnehmer.resonantcombat.ResonantCombat;
import net.minecraft.server.level.ServerPlayer;
import yesman.epicfight.world.capabilities.EpicFightCapabilities;
import yesman.epicfight.world.capabilities.entitypatch.player.ServerPlayerPatch;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Locale;

/**
 * Reads/spends Epic Fight stamina via reflection (getStamina()/setStamina(float)).
 * If the API cannot be resolved, costs are NOT enforced (one error is logged, run /resonantcombat debug probe).
 */
public final class EpicFightStamina {
    private static Method getter, setter;
    private static boolean resolved;

    /** @return true if the cost was paid (or stamina cannot be read), false if the player lacks stamina. */
    public static boolean tryConsume(ServerPlayer player, float cost) {
        try {
            ServerPlayerPatch patch = EpicFightCapabilities.getEntityPatch(player, ServerPlayerPatch.class);
            if (patch == null) return true;
            if (!resolve(patch.getClass())) return true;
            float current = ((Number) getter.invoke(patch)).floatValue();
            if (current < cost) return false;
            setter.invoke(patch, current - cost);
            return true;
        } catch (ReflectiveOperationException | LinkageError e) {
            ResonantCombat.LOGGER.error("Epic Fight stamina access failed", e);
            return true;
        }
    }

    public static float get(ServerPlayer player) {
        try {
            ServerPlayerPatch patch = EpicFightCapabilities.getEntityPatch(player, ServerPlayerPatch.class);
            if (patch == null || !resolve(patch.getClass())) return -1F;
            return ((Number) getter.invoke(patch)).floatValue();
        } catch (ReflectiveOperationException | LinkageError e) {
            return -1F;
        }
    }

    private static boolean resolve(Class<?> cls) {
        if (resolved) return getter != null && setter != null;
        resolved = true;
        try {
            getter = cls.getMethod("getStamina");
            setter = cls.getMethod("setStamina", float.class);
            ResonantCombat.LOGGER.info("Epic Fight stamina resolved on {}", cls.getName());
            return true;
        } catch (NoSuchMethodException e) {
            getter = setter = null;
            ResonantCombat.LOGGER.error("Epic Fight stamina API not found on {} - stamina costs are NOT enforced. Candidates: {}",
                    cls.getName(), Arrays.stream(cls.getMethods())
                            .filter(m -> m.getName().toLowerCase(Locale.ROOT).contains("stamina"))
                            .map(m -> m.getName() + "/" + m.getParameterCount()).distinct().sorted().toList());
            return false;
        }
    }

    private EpicFightStamina() {}
}
