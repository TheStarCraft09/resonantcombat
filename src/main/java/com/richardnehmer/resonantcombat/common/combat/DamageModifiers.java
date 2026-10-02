package com.richardnehmer.resonantcombat.common.combat;

import com.richardnehmer.resonantcombat.common.attachment.CombatRuntime;
import com.richardnehmer.resonantcombat.common.attachment.ModAttachments;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;

/** Scales damage dealt/taken by Resonant Combat states. Runs on LivingIncomingDamageEvent (before armor). */
public final class DamageModifiers {

    /** Multiplier applied to damage a player deals: animation-driven hit multiplier (optional), stance and mark. */
    public static float attackerMultiplier(ServerPlayer attacker, LivingEntity target, boolean includeAnimationMult) {
        CombatRuntime rt = attacker.getData(ModAttachments.RUNTIME);
        long now = attacker.level().getGameTime();
        float mult = 1.0F;
        if (includeAnimationMult && now <= rt.hitMultEnd) mult *= rt.hitDamageMult;
        if (now <= rt.stanceEnd) mult *= rt.stanceDamageMult;
        if (rt.markedEntityId == target.getId() && now <= rt.markEnd) mult *= rt.markBonus;
        return mult;
    }

    public static void onIncoming(LivingIncomingDamageEvent event) {
        LivingEntity victim = event.getEntity();

        if (event.getSource().getEntity() instanceof ServerPlayer attacker
                && !attacker.getData(ModAttachments.RUNTIME).damageGuard) {
            float mult = attackerMultiplier(attacker, victim, true);
            if (mult != 1.0F) event.setAmount(event.getAmount() * mult);
        }

        if (victim instanceof ServerPlayer player) {
            CombatRuntime rt = player.getData(ModAttachments.RUNTIME);
            long now = player.level().getGameTime();
            if (now <= rt.counterEnd && event.getSource().getEntity() instanceof LivingEntity attacker && attacker != player) {
                event.setCanceled(true); // successful counter: no damage taken, attacker is punished
                AbilityExecutors.triggerCounter(player, rt, attacker);
                return;
            }
            if (now <= rt.stanceEnd && rt.stanceTakenMult != 1.0F) event.setAmount(event.getAmount() * rt.stanceTakenMult);
        }
    }

    private DamageModifiers() {}
}
