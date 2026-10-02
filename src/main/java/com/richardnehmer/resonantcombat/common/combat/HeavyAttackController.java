package com.richardnehmer.resonantcombat.common.combat;

import com.richardnehmer.resonantcombat.ResonantCombat;
import com.richardnehmer.resonantcombat.common.attachment.CombatRuntime;
import com.richardnehmer.resonantcombat.common.attachment.ModAttachments;
import com.richardnehmer.resonantcombat.common.network.ModPayloads;
import com.richardnehmer.resonantcombat.common.registry.WeaponClassResolver;
import com.richardnehmer.resonantcombat.integration.epicfight.AnimationHook;
import com.richardnehmer.resonantcombat.integration.epicfight.EpicFightStamina;
import com.richardnehmer.resonantcombat.common.data.WeaponClassDefinition;
import com.richardnehmer.resonantcombat.common.attachment.PlayerProfile;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.Comparator;
import java.util.List;

/**
 * Heavy attack (design doc 3.2). The SERVER measures the hold duration from AttackHoldStart/AttackRelease, so the client
 * cannot claim a longer charge. Execution = windup -> server-side cone sweep -> recovery.
 * Animation is a TODO (HeavyAnimationHook): until the Epic Fight API probe confirms how to play animations, the swing
 * uses the vanilla arm swing + particles/sounds.
 */
public final class HeavyAttackController {
    private static final ResourceLocation CHARGE_SLOW_ID = ResonantCombat.id("heavy_charge");

    public static void onHoldStart(ServerPlayer player) {
        if (!ActionValidator.baseChecks(player) || !ActionValidator.rateLimit(player, ActionValidator.SLOT_HOLD)) return;
        CombatRuntime rt = player.getData(ModAttachments.RUNTIME);
        if (rt.state != CombatActionState.NEUTRAL || !player.onGround()) return;
        if (!WeaponClassResolver.isCircuitActive(player)) return;

        rt.state = CombatActionState.CHARGING_HEAVY;
        rt.heavyChargeStartTick = player.level().getGameTime();
        setChargeSlow(player, true);
    }

    public static void onRelease(ServerPlayer player) {
        CombatRuntime rt = player.getData(ModAttachments.RUNTIME);
        if (rt.state != CombatActionState.CHARGING_HEAVY) return;
        int held = (int) (player.level().getGameTime() - rt.heavyChargeStartTick);
        endCharge(player, rt);
        if (held < CombatTuning.HEAVY_MIN_TICKS) return; // short press: Epic Fight's own basic attack handles it
        execute(player, rt, tierFor(held));
    }

    /** Called every tick for players whose state is CHARGING_HEAVY or HEAVY_RECOVERY. */
    public static void tick(ServerPlayer player, CombatRuntime rt, long now) {
        if (rt.state == CombatActionState.CHARGING_HEAVY) {
            int held = (int) (now - rt.heavyChargeStartTick);
            if (!player.onGround() || !WeaponClassResolver.isCircuitActive(player) || held > CombatTuning.HEAVY_TIMEOUT_TICKS) {
                endCharge(player, rt); // weapon swapped, left ground, left battle mode, or lost release packet
            } else if (held >= CombatTuning.HEAVY_III_TICKS) {
                endCharge(player, rt);
                execute(player, rt, 2); // Heavy III auto-executes
            }
        } else if (rt.state == CombatActionState.HEAVY_RECOVERY) {
            if (rt.heavyImpactTick >= 0 && now >= rt.heavyImpactTick) {
                impact(player, rt);
                rt.heavyImpactTick = -1;
            }
            if (now >= rt.recoveryEndTick) rt.state = CombatActionState.NEUTRAL;
        }
    }

    /** Strong impacts interrupt charging (design doc 3.2). */
    public static void onPlayerHurt(ServerPlayer victim, float amount) {
        CombatRuntime rt = victim.getData(ModAttachments.RUNTIME);
        if (rt.state == CombatActionState.CHARGING_HEAVY && amount >= 3.0F) endCharge(victim, rt);
    }

    public static void cleanup(ServerPlayer player, CombatRuntime rt) {
        setChargeSlow(player, false);
        rt.reset();
    }

    // ------------------------------------------------------------------

    private static int tierFor(int held) {
        if (held >= CombatTuning.HEAVY_III_TICKS) return 2;
        return held >= CombatTuning.HEAVY_II_TICKS ? 1 : 0;
    }

    private static void endCharge(ServerPlayer player, CombatRuntime rt) {
        rt.state = CombatActionState.NEUTRAL;
        setChargeSlow(player, false);
    }

    private static void execute(ServerPlayer player, CombatRuntime rt, int tier) {
        long nowTick = player.level().getGameTime();
        boolean free = rt.stanceHeavyFree && nowTick <= rt.stanceEnd;
        if (!free && !EpicFightStamina.tryConsume(player, CombatTuning.HEAVY_STAMINA[tier])) {
            ActionValidator.reject(player, ModPayloads.ActionResult.ACTION_HEAVY, "no_stamina");
            return;
        }
        long now = player.level().getGameTime();
        rt.state = CombatActionState.HEAVY_RECOVERY;
        rt.heavyTier = tier;
        rt.heavyImpactTick = now + CombatTuning.HEAVY_WINDUP_TICKS[tier];
        rt.recoveryEndTick = rt.heavyImpactTick + CombatTuning.HEAVY_RECOVERY_TICKS[tier];

        // Animation path: Epic Fight's hit phases deal the damage, we only scale damage/posture. No animation -> own sweep.
        if (playHeavyAnimation(player, tier)) {
            rt.heavyImpactTick = -1;
            rt.hitDamageMult = CombatTuning.HEAVY_DAMAGE[tier];
            rt.hitPostureMult = CombatTuning.HEAVY_POSTURE[tier];
            rt.hitMultEnd = rt.recoveryEndTick;
        } else {
            player.swing(InteractionHand.MAIN_HAND, true);
        }
        player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.PLAYER_ATTACK_STRONG, SoundSource.PLAYERS, 1.0F, 0.8F - 0.1F * tier);
    }

    private static boolean playHeavyAnimation(ServerPlayer player, int tier) {
        PlayerProfile profile = player.getData(ModAttachments.PROFILE);
        if (profile.selectedClass().isEmpty()) return false;
        WeaponClassDefinition def = com.richardnehmer.resonantcombat.common.registry.ModRegistries
                .weaponClasses(player.level().registryAccess()).get(profile.selectedClass().get());
        if (def == null || def.heavyAnimations().size() <= tier) return false;
        return AnimationHook.play(player, def.heavyAnimations().get(tier), 0.1F);
    }

    private static void impact(ServerPlayer player, CombatRuntime rt) {
        int tier = rt.heavyTier;
        ServerLevel level = player.serverLevel();
        double range = CombatTuning.HEAVY_RANGE[tier];

        Vec3 look = player.getLookAngle();
        Vec3 facing = new Vec3(look.x, 0.0, look.z);
        facing = facing.lengthSqr() < 1.0E-4 ? Vec3.directionFromRotation(0.0F, player.getYRot()) : facing.normalize();

        float base = (float) player.getAttributeValue(Attributes.ATTACK_DAMAGE);
        AABB box = player.getBoundingBox().inflate(range, 1.0, range);
        List<LivingEntity> candidates = level.getEntitiesOfClass(LivingEntity.class, box,
                t -> t != player && t.isAlive() && player.canAttack(t) && !t.isAlliedTo(player));
        candidates.sort(Comparator.comparingDouble(t -> t.distanceToSqr(player)));

        int hits = 0;
        for (LivingEntity target : candidates) {
            if (hits >= CombatTuning.HEAVY_MAX_TARGETS) break;
            Vec3 to = new Vec3(target.getX() - player.getX(), 0.0, target.getZ() - player.getZ());
            double dist = to.length();
            if (dist > range + target.getBbWidth() * 0.5) continue;
            if (dist > 0.01 && facing.dot(to.scale(1.0 / dist)) < CombatTuning.HEAVY_CONE_COS) continue;

            float posture = base * CombatTuning.POSTURE_PER_DAMAGE * CombatTuning.HEAVY_POSTURE[tier];
            if (CombatDamage.strike(player, target, base * CombatTuning.HEAVY_DAMAGE[tier], posture, facing, 0.4 + 0.2 * tier)) hits++;
        }

        Vec3 fx = player.getEyePosition().add(facing.scale(1.6));
        level.sendParticles(ParticleTypes.SWEEP_ATTACK, fx.x, fx.y - 0.4, fx.z, 1 + tier, 0.4, 0.2, 0.4, 0.0);
        level.playSound(null, player.getX(), player.getY(), player.getZ(),
                hits > 0 ? SoundEvents.PLAYER_ATTACK_KNOCKBACK : SoundEvents.PLAYER_ATTACK_SWEEP, SoundSource.PLAYERS, 1.0F, 0.9F);
    }

    private static void setChargeSlow(ServerPlayer player, boolean on) {
        AttributeInstance speed = player.getAttribute(Attributes.MOVEMENT_SPEED);
        if (speed == null) return;
        boolean present = speed.hasModifier(CHARGE_SLOW_ID);
        if (on && !present) {
            speed.addTransientModifier(new AttributeModifier(CHARGE_SLOW_ID, -(1.0 - CombatTuning.CHARGE_SPEED_MULT),
                    AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));
        } else if (!on && present) {
            speed.removeModifier(CHARGE_SLOW_ID);
        }
    }

    private HeavyAttackController() {}
}
