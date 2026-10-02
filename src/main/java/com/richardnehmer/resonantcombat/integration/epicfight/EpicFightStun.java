package com.richardnehmer.resonantcombat.integration.epicfight;

import com.richardnehmer.resonantcombat.ResonantCombat;
import net.minecraft.world.entity.LivingEntity;
import yesman.epicfight.world.capabilities.EpicFightCapabilities;
import yesman.epicfight.world.capabilities.entitypatch.LivingEntityPatch;

import java.lang.reflect.Method;

/**
 * Applies Epic Fight's own NEUTRALIZE stun (probe: LivingEntityPatch#applyStun(StunType, float)) so a posture break uses
 * the native "neutralized" animations (BIPED_COMMON_NEUTRALIZED, RAVAGER_STUN, ...). Falls back to LONG, then to nothing.
 */
public final class EpicFightStun {
    private static boolean warned;

    /** @return true if Epic Fight applied a stun. The float is passed through as-is (units to be verified in-game). */
    @SuppressWarnings({"rawtypes", "unchecked"})
    public static boolean applyNeutralize(LivingEntity entity, float time) {
        try {
            LivingEntityPatch patch = EpicFightCapabilities.getEntityPatch(entity, LivingEntityPatch.class);
            if (patch == null) return false;
            for (Method m : patch.getClass().getMethods()) {
                if (!m.getName().equals("applyStun") || m.getParameterCount() != 2 || !m.getParameterTypes()[0].isEnum()) continue;
                Object[] constants = m.getParameterTypes()[0].getEnumConstants();
                for (String wanted : new String[]{"NEUTRALIZE", "LONG"}) {
                    for (Object c : constants) {
                        if (((Enum<?>) c).name().equals(wanted)) {
                            Object result = m.invoke(patch, c, time);
                            return !(result instanceof Boolean b) || b;
                        }
                    }
                }
            }
        } catch (Throwable t) {
            if (!warned) {
                warned = true;
                ResonantCombat.LOGGER.error("Epic Fight stun failed; falling back to vanilla slowness", t);
            }
        }
        return false;
    }

    private EpicFightStun() {}
}
