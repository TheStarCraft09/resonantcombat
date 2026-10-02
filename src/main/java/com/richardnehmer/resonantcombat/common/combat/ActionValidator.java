package com.richardnehmer.resonantcombat.common.combat;

import com.richardnehmer.resonantcombat.common.attachment.CombatRuntime;
import com.richardnehmer.resonantcombat.common.attachment.ModAttachments;
import com.richardnehmer.resonantcombat.common.network.ModPayloads;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.PacketDistributor;

/** Shared server-side checks (design doc 5.3). */
public final class ActionValidator {
    public static final int SLOT_HOLD = 0;
    public static final int SLOT_DOUBLE_JUMP = 1;
    public static final int SLOT_ATTACK = 2;

    public static boolean baseChecks(ServerPlayer player) {
        return player.isAlive() && !player.isSpectator() && !player.isPassenger()
                && !player.getAbilities().flying && !player.isFallFlying() && !player.isSleeping();
    }

    /** Drops intents that arrive faster than MIN_INTENT_INTERVAL_TICKS (spam / forged packets). */
    public static boolean rateLimit(ServerPlayer player, int slot) {
        CombatRuntime rt = player.getData(ModAttachments.RUNTIME);
        long now = player.level().getGameTime();
        if (now - rt.lastIntentTick[slot] < CombatTuning.MIN_INTENT_INTERVAL_TICKS) return false;
        rt.lastIntentTick[slot] = now;
        return true;
    }

    /** Tells the client why an action failed (shown as an action-bar message). */
    public static void reject(ServerPlayer player, int action, String reasonKey) {
        PacketDistributor.sendToPlayer(player, new ModPayloads.ActionResult(action, false, reasonKey, 0));
    }

    private ActionValidator() {}
}
