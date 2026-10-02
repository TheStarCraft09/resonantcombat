package com.richardnehmer.resonantcombat.common.combat;

import com.richardnehmer.resonantcombat.common.attachment.CombatRuntime;
import com.richardnehmer.resonantcombat.common.attachment.ModAttachments;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

/** Single entry point for server-calculated Resonant Combat damage. */
public final class CombatDamage {

    /**
     * @param push horizontal knockback direction (normalized) or null
     * @return true if the target took damage
     */
    public static boolean strike(ServerPlayer attacker, LivingEntity target, float damage, float posture, Vec3 push, double pushStrength) {
        CombatRuntime rt = attacker.getData(ModAttachments.RUNTIME);
        rt.damageGuard = true; // our hit must not trigger the normal on-hit resource/posture gain
        try {
            target.invulnerableTime = 0; // vanilla i-frames would swallow back-to-back strikes
            boolean hit = target.hurt(attacker.damageSources().playerAttack(attacker), damage);
            if (hit) {
                PostureController.damage(target, posture);
                if (push != null && pushStrength > 0.0) {
                    target.setDeltaMovement(target.getDeltaMovement().add(push.x * pushStrength, 0.25, push.z * pushStrength));
                    target.hurtMarked = true;
                }
            }
            return hit;
        } finally {
            rt.damageGuard = false;
        }
    }

    private CombatDamage() {}
}
