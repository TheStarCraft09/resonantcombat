package com.richardnehmer.resonantcombat.integration.epicfight;

import com.richardnehmer.resonantcombat.ResonantCombat;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import yesman.epicfight.world.capabilities.EpicFightCapabilities;
import yesman.epicfight.world.capabilities.entitypatch.player.ServerPlayerPatch;
import yesman.epicfight.world.capabilities.item.CapabilityItem;

import java.util.Locale;
import java.util.Optional;

/**
 * THE ONLY CLASS THAT TOUCHES EPIC FIGHT TYPES (all other code goes through this).
 * Epic Fight 21.17.x reworked weapon capabilities (builder inheritance, WeaponCategory inheritance, DeferredRegister),
 * so every call here is wrapped: if a signature changes, you get one log line and "unknown", not a crash.
 *
 * PHASE 0 CHECKLIST - verify against the pinned EF version (run /resonantcombat debug state):
 *  1. EpicFightCapabilities.getEntityPatch(player, ServerPlayerPatch.class) returns non-null
 *  2. ServerPlayerPatch#isBattleMode() exists
 *  3. EpicFightCapabilities.getItemStackCapability(stack).getWeaponCategory() exists and its toString()
 *     yields the lower-case category name (sword, longsword, greatsword, tachi, ...). With WeaponCategory
 *     inheritance this may need to change to an explicit isWeaponCategory(...) style check.
 */
public final class EpicFightBridge {
    private static boolean warnedBattle, warnedCategory;

    public static boolean isBattleMode(ServerPlayer player) {
        try {
            ServerPlayerPatch patch = EpicFightCapabilities.getEntityPatch(player, ServerPlayerPatch.class);
            return patch != null && patch.isEpicFightMode();
        } catch (LinkageError e) {
            if (!warnedBattle) {
                warnedBattle = true;
                ResonantCombat.LOGGER.error("Epic Fight API mismatch while reading battle mode (adjust EpicFightBridge)", e);
            }
            return false;
        }
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
                ResonantCombat.LOGGER.error("Epic Fight API mismatch while reading weapon category (adjust EpicFightBridge)", e);
            }
            return Optional.empty();
        }
    }

    private EpicFightBridge() {}
}
