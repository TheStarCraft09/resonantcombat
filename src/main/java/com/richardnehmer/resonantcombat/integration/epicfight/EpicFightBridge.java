package com.richardnehmer.resonantcombat.integration.epicfight;

import com.richardnehmer.resonantcombat.ResonantCombat;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import yesman.epicfight.world.capabilities.EpicFightCapabilities;
import yesman.epicfight.world.capabilities.entitypatch.player.ServerPlayerPatch;
import yesman.epicfight.world.capabilities.item.CapabilityItem;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Locale;
import java.util.Optional;

/**
 * THE ONLY CLASS THAT TOUCHES EPIC FIGHT TYPES.
 * Mode lookup is reflective on purpose until the real method name is confirmed (see /resonantcombat debug state
 * and the log). Once confirmed, replace it with a direct call.
 */
public final class EpicFightBridge {
    private static final String[] MODE_METHOD_CANDIDATES = {"isEpicFightMode", "isBattleMode"};

    private static Method modeMethod;
    private static boolean modeLookedUp;
    private static boolean warnedCategory;

    public static boolean isBattleMode(ServerPlayer player) {
        try {
            ServerPlayerPatch patch = EpicFightCapabilities.getEntityPatch(player, ServerPlayerPatch.class);
            if (patch == null) return false;
            Method method = resolveModeMethod(patch.getClass());
            return method != null && (Boolean) method.invoke(patch);
        } catch (ReflectiveOperationException | LinkageError e) {
            ResonantCombat.LOGGER.error("Epic Fight mode check failed", e);
            return false;
        }
    }

    private static Method resolveModeMethod(Class<?> patchClass) {
        if (modeLookedUp) return modeMethod;
        modeLookedUp = true;
        for (String name : MODE_METHOD_CANDIDATES) {
            try {
                Method m = patchClass.getMethod(name);
                if (m.getReturnType() == boolean.class) {
                    modeMethod = m;
                    ResonantCombat.LOGGER.info("Epic Fight mode method resolved: {}#{}", patchClass.getName(), name);
                    return modeMethod;
                }
            } catch (NoSuchMethodException ignored) {}
        }
        ResonantCombat.LOGGER.error("No Epic Fight mode method found on {}. Candidates: {}", patchClass.getName(),
                Arrays.stream(patchClass.getMethods())
                        .filter(m -> m.getName().toLowerCase(Locale.ROOT).contains("mode"))
                        .map(m -> m.getReturnType().getSimpleName() + " " + m.getName() + "(" + m.getParameterCount() + " args)")
                        .distinct().sorted().toList());
        return null;
    }

    /** Lower-case Epic Fight weapon category of the stack, if Epic Fight has a capability for it. */
    public static Optional<String> weaponCategory(ItemStack stack) {
        if (stack.isEmpty()) return Optional.empty();
        try {
            CapabilityItem cap = EpicFightCapabilities.getItemStackCapability(stack);
            if (cap == null) return Optional.empty();
            Object category = cap.getWeaponCategory();
            return category == null ? Optional.empty() : Optional.of(category.toString().toLowerCase(Locale.ROOT));
        } catch (LinkageError e) {
            if (!warnedCategory) {
                warnedCategory = true;
                ResonantCombat.LOGGER.error("Epic Fight API mismatch while reading weapon category", e);
            }
            return Optional.empty();
        }
    }

    private EpicFightBridge() {}
}