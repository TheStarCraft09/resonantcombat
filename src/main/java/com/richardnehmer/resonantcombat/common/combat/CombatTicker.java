package com.richardnehmer.resonantcombat.common.combat;

import com.richardnehmer.resonantcombat.common.attachment.CombatRuntime;
import com.richardnehmer.resonantcombat.common.attachment.ModAttachments;
import net.minecraft.server.level.ServerPlayer;

/** Per-player server tick: velocity estimate, landing resets, and state-machine updates. */
public final class CombatTicker {

    public static void tick(ServerPlayer player) {
        CombatRuntime rt = player.getData(ModAttachments.RUNTIME);
        if (!player.isAlive()) {
            if (rt.state != CombatActionState.NEUTRAL) HeavyAttackController.cleanup(player, rt);
            rt.hasLast = false;
            return;
        }

        long now = player.level().getGameTime();
        if (rt.hasLast) {
            rt.velX = player.getX() - rt.lastX;
            rt.velY = player.getY() - rt.lastY;
            rt.velZ = player.getZ() - rt.lastZ;
        }
        rt.lastX = player.getX(); rt.lastY = player.getY(); rt.lastZ = player.getZ();
        rt.hasLast = true;

        if (player.onGround()) {
            rt.airTicks = 0;
            rt.doubleJumpUsed = false;
            if (rt.state == CombatActionState.DOUBLE_JUMP_ACTIVE || rt.state == CombatActionState.DOUBLE_JUMP_READY) {
                rt.state = CombatActionState.NEUTRAL;
            }
        } else {
            rt.airTicks++;
        }

        switch (rt.state) {
            case CHARGING_HEAVY, HEAVY_RECOVERY -> HeavyAttackController.tick(player, rt, now);
            case PLUNGE_ACTIVE -> PlungeController.tick(player, rt);
            default -> { }
        }
    }

    private CombatTicker() {}
}
