package com.richardnehmer.resonantcombat.integration.epicfight;

import com.richardnehmer.resonantcombat.ResonantCombat;
import net.minecraft.server.level.ServerPlayer;
import yesman.epicfight.world.capabilities.EpicFightCapabilities;
import yesman.epicfight.world.capabilities.entitypatch.player.ServerPlayerPatch;

import java.lang.reflect.Method;

/**
 * Plays an Epic Fight animation on a player from the server (probe-confirmed: playAnimationSynchronized(AssetAccessor, float)).
 * Animation names:
 *   "SWORD_AUTO2"                    -> built-in animation, static field on yesman.epicfight.gameasset.Animations
 *   "resonantcombat:biped/foo/bar"   -> custom animation looked up through AnimationManager (see ANIMATIONS.md; needs probe v2)
 * Reflection keeps this compile-safe while the animation registry API is still being confirmed.
 */
public final class AnimationHook {
    private static boolean warnedPlay, warnedCustom;

    /** @return true if Epic Fight started the animation (its hit phases then deal the damage). */
    public static boolean play(ServerPlayer player, String animation, float transitionSeconds) {
        if (animation == null || animation.isBlank()) return false;
        try {
            ServerPlayerPatch patch = EpicFightCapabilities.getEntityPatch(player, ServerPlayerPatch.class);
            if (patch == null) return false;
            Object accessor = resolve(animation);
            if (accessor == null) return false;

            for (Method m : patch.getClass().getMethods()) {
                if (m.getName().equals("playAnimationSynchronized") && m.getParameterCount() == 2
                        && m.getParameterTypes()[0].isInstance(accessor)) {
                    m.invoke(patch, accessor, transitionSeconds);
                    return true;
                }
            }
            if (!warnedPlay) {
                warnedPlay = true;
                ResonantCombat.LOGGER.error("No compatible playAnimationSynchronized on {} for accessor {}", patch.getClass().getName(), accessor.getClass().getName());
            }
        } catch (Throwable t) {
            if (!warnedPlay) {
                warnedPlay = true;
                ResonantCombat.LOGGER.error("Epic Fight animation '{}' failed", animation, t);
            }
        }
        return false;
    }

    private static Object resolve(String animation) throws ReflectiveOperationException {
        if (animation.contains(":")) {
            try {
                Class<?> manager = Class.forName("yesman.epicfight.api.animation.AnimationManager");
                Object instance = manager.getMethod("getInstance").invoke(null);
                return manager.getMethod("byKey", String.class).invoke(instance, animation);
            } catch (ReflectiveOperationException | LinkageError e) {
                if (!warnedCustom) {
                    warnedCustom = true;
                    ResonantCombat.LOGGER.error("Custom animation lookup '{}' failed - run /resonantcombat debug probe (v2) and send the file", animation, e);
                }
                return null;
            }
        }
        Class<?> animations = Class.forName("yesman.epicfight.gameasset.Animations");
        return animations.getField(animation).get(null);
    }

    private AnimationHook() {}
}
