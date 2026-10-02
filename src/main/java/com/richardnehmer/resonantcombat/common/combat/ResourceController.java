package com.richardnehmer.resonantcombat.common.combat;

import com.richardnehmer.resonantcombat.common.attachment.CombatRuntime;
import com.richardnehmer.resonantcombat.common.attachment.ModAttachments;
import com.richardnehmer.resonantcombat.common.attachment.PlayerProfile;
import com.richardnehmer.resonantcombat.common.network.ModNetwork;
import com.richardnehmer.resonantcombat.common.registry.WeaponClassResolver;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.monster.Enemy;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;

/** Resonance / Liberation gain (design doc 1.3, 3.1) + basic-hit posture damage. */
public final class ResourceController {
    public static final float HIT_RESONANCE = 6.0F;
    public static final float HIT_LIBERATION = 4.0F;

    public static void onHit(LivingDamageEvent.Post event) {
        if (!(event.getSource().getEntity() instanceof ServerPlayer player)) return;
        CombatRuntime rt = player.getData(ModAttachments.RUNTIME);
        if (rt.damageGuard) return; // damage dealt by our own heavy/plunge strikes
        if (!(event.getEntity() instanceof Enemy) || event.getNewDamage() <= 0.0F) return;
        if (!WeaponClassResolver.isCircuitActive(player)) return;

        // TODO(Phase 3+): once-per-target-per-swing gating; multi-hit attacks currently grant per damage event.
        add(player, HIT_RESONANCE, HIT_LIBERATION);
        float postureFactor = 1.0F;
        if (player.level().getGameTime() <= rt.hitMultEnd && rt.hitDamageMult > 0.0F) {
            postureFactor = rt.hitPostureMult / rt.hitDamageMult; // animation-driven heavy/ability hit
        }
        PostureController.damage(event.getEntity(), event.getNewDamage() * CombatTuning.POSTURE_PER_DAMAGE * postureFactor);
    }

    public static void add(ServerPlayer player, float resonance, float liberation) {
        PlayerProfile profile = player.getData(ModAttachments.PROFILE);
        profile.setResonance(profile.resonance() + resonance);
        profile.setLiberationEnergy(profile.liberationEnergy() + liberation);
        // TODO(Phase 5): send a tiny delta payload instead of the whole profile.
        ModNetwork.syncProfile(player);
    }

    private ResourceController() {}
}
