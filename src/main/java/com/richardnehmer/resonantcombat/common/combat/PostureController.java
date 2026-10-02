package com.richardnehmer.resonantcombat.common.combat;

import com.richardnehmer.resonantcombat.common.attachment.ModAttachments;
import com.richardnehmer.resonantcombat.common.attachment.PostureData;
import com.richardnehmer.resonantcombat.common.network.ModPayloads;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.network.PacketDistributor;

/**
 * Stagger meter on hostile entities. Stagger is implemented with vanilla tools (heavy slowness + damage bonus) because
 * Epic Fight's own stun system is not hooked yet (see roadmap: replace with EF stun once the API probe confirms the call).
 */
public final class PostureController {

    public static boolean isEligible(LivingEntity entity) { return entity instanceof Enemy; }

    public static float maxPosture(LivingEntity entity) {
        return Math.max(CombatTuning.POSTURE_MIN_MAX, entity.getMaxHealth() * CombatTuning.POSTURE_HEALTH_FACTOR);
    }

    public static boolean isStaggered(LivingEntity entity) {
        return entity.hasData(ModAttachments.POSTURE)
                && entity.level().getGameTime() < entity.getData(ModAttachments.POSTURE).staggerEndTick;
    }

    public static void damage(LivingEntity target, float amount) {
        if (!isEligible(target) || amount <= 0.0F || !(target.level() instanceof ServerLevel level)) return;
        PostureData data = target.getData(ModAttachments.POSTURE);
        long now = level.getGameTime();
        if (now < data.staggerEndTick) return; // no posture damage during stagger

        data.posture += amount;
        data.lastDamageTick = now;
        float max = maxPosture(target);

        if (data.posture >= max) {
            data.posture = 0.0F;
            data.staggerEndTick = now + CombatTuning.STAGGER_TICKS;
            data.wasStaggered = true;
            target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, CombatTuning.STAGGER_TICKS, 6, false, false));
            level.playSound(null, target.getX(), target.getY(), target.getZ(), SoundEvents.SHIELD_BREAK, SoundSource.HOSTILE, 1.0F, 0.8F);
            level.sendParticles(ParticleTypes.CRIT, target.getX(), target.getY(0.6), target.getZ(), 20, 0.4, 0.5, 0.4, 0.3);
        }
        sync(target, data, max);
    }

    public static void tick(LivingEntity entity) {
        if (!(entity.level() instanceof ServerLevel level) || !entity.hasData(ModAttachments.POSTURE)) return;
        PostureData data = entity.getData(ModAttachments.POSTURE);
        long now = level.getGameTime();
        float max = maxPosture(entity);

        if (now < data.staggerEndTick) return;
        if (data.wasStaggered) { // stagger just ended
            data.wasStaggered = false;
            sync(entity, data, max);
        }
        if (data.posture > 0.0F && now - data.lastDamageTick > CombatTuning.POSTURE_REGEN_DELAY_TICKS) {
            data.posture = Math.max(0.0F, data.posture - CombatTuning.POSTURE_REGEN_PER_TICK);
            if (now % 4 == 0 || data.posture == 0.0F) sync(entity, data, max);
        }
    }

    /** Staggered targets take bonus damage from players (the "punish" window). */
    public static void onIncomingDamage(LivingIncomingDamageEvent event) {
        if (event.getSource().getEntity() instanceof Player && isStaggered(event.getEntity())) {
            event.setAmount(event.getAmount() * CombatTuning.STAGGER_DAMAGE_MULT);
        }
    }

    private static void sync(LivingEntity entity, PostureData data, float max) {
        if (Math.abs(data.lastSentPosture - data.posture) < 0.01F && !data.wasStaggered) return;
        data.lastSentPosture = data.posture;
        PacketDistributor.sendToPlayersTrackingEntity(entity,
                new ModPayloads.PostureUpdate(entity.getId(), data.posture, max, data.wasStaggered));
    }

    private PostureController() {}
}
