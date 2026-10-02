package com.richardnehmer.resonantcombat.common.combat;

import com.richardnehmer.resonantcombat.common.attachment.CombatRuntime;
import com.richardnehmer.resonantcombat.common.attachment.ModAttachments;
import com.richardnehmer.resonantcombat.common.network.ModPayloads;
import com.richardnehmer.resonantcombat.common.registry.WeaponClassResolver;
import com.richardnehmer.resonantcombat.integration.epicfight.EpicFightStamina;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.event.entity.living.LivingFallEvent;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.List;

/**
 * Airborne dodge double jump (design doc 3.3) and Plunge Attack (3.4).
 * Vanilla player movement is client-authoritative, so the server validates the intent and then PUSHES the velocity to the
 * client with a motion packet (hurtMarked), exactly like knockback.
 */
public final class PlungeController {

    // ---------------- double jump ----------------
    public static void onDoubleJump(ServerPlayer player) {
        if (!ActionValidator.baseChecks(player) || !ActionValidator.rateLimit(player, ActionValidator.SLOT_DOUBLE_JUMP)) return;
        CombatRuntime rt = player.getData(ModAttachments.RUNTIME);
        if (player.onGround() || rt.doubleJumpUsed || rt.airTicks < 2) return;
        if (player.isInWater() || player.isInLava() || player.onClimbable()) return;
        if (rt.state != CombatActionState.NEUTRAL) return;
        if (!WeaponClassResolver.isCircuitActive(player)) return;
        if (!EpicFightStamina.tryConsume(player, CombatTuning.DOUBLE_JUMP_STAMINA)) {
            ActionValidator.reject(player, ModPayloads.ActionResult.ACTION_DOUBLE_JUMP, "no_stamina");
            return;
        }

        long now = player.level().getGameTime();
        rt.doubleJumpUsed = true;
        rt.state = CombatActionState.DOUBLE_JUMP_ACTIVE;
        rt.plungeWindowEndTick = now + CombatTuning.PLUNGE_WINDOW_TICKS;

        Vec3 look = player.getLookAngle();
        // keep the horizontal speed the player already had (server estimate), add the forward impulse
        player.setDeltaMovement(rt.velX + look.x * CombatTuning.DOUBLE_JUMP_FORWARD,
                CombatTuning.DOUBLE_JUMP_VERTICAL,
                rt.velZ + look.z * CombatTuning.DOUBLE_JUMP_FORWARD);
        player.hurtMarked = true;
        player.fallDistance = 0.0F;

        ServerLevel level = player.serverLevel();
        level.sendParticles(ParticleTypes.CLOUD, player.getX(), player.getY(), player.getZ(), 8, 0.3, 0.05, 0.3, 0.02);
        level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.PLAYER_ATTACK_SWEEP, SoundSource.PLAYERS, 0.6F, 1.5F);
        PacketDistributor.sendToPlayer(player, new ModPayloads.ActionResult(
                ModPayloads.ActionResult.ACTION_DOUBLE_JUMP, true, "", CombatTuning.PLUNGE_WINDOW_TICKS));
    }

    // ---------------- plunge ----------------
    public static void onBasicAttackIntent(ServerPlayer player) {
        if (!ActionValidator.baseChecks(player) || !ActionValidator.rateLimit(player, ActionValidator.SLOT_ATTACK)) return;
        CombatRuntime rt = player.getData(ModAttachments.RUNTIME);
        long now = player.level().getGameTime();

        if (player.onGround() || rt.state != CombatActionState.DOUBLE_JUMP_ACTIVE || now > rt.plungeWindowEndTick) return;
        if (rt.velY > CombatTuning.PLUNGE_APEX_VY) return; // still rising: must be descending or at the apex
        if (!WeaponClassResolver.isCircuitActive(player)) return;
        if (groundDistance(player) < CombatTuning.PLUNGE_MIN_DISTANCE) {
            ActionValidator.reject(player, ModPayloads.ActionResult.ACTION_PLUNGE, "too_low");
            return;
        }
        if (!EpicFightStamina.tryConsume(player, CombatTuning.PLUNGE_STAMINA)) {
            ActionValidator.reject(player, ModPayloads.ActionResult.ACTION_PLUNGE, "no_stamina");
            return;
        }

        rt.state = CombatActionState.PLUNGE_ACTIVE;
        player.swing(InteractionHand.MAIN_HAND, true);
        // TODO(PlungeAnimationHook): play the Circuit's aerial animation through Epic Fight here.
        PacketDistributor.sendToPlayer(player, new ModPayloads.ActionResult(ModPayloads.ActionResult.ACTION_PLUNGE, true, "", 0));
    }

    /** Server-controlled descent, called every tick while PLUNGE_ACTIVE. */
    public static void tick(ServerPlayer player, CombatRuntime rt) {
        if (player.onGround() || player.isInWater() || player.isInLava() || player.onClimbable()) {
            land(player, rt);
            return;
        }
        // reduced horizontal steering: keep 60% of the current horizontal velocity, force the downward speed
        player.setDeltaMovement(rt.velX * 0.6, -CombatTuning.PLUNGE_SPEED, rt.velZ * 0.6);
        player.hurtMarked = true;
        player.fallDistance = 0.0F; // the consumed plunge descent never deals fall damage
    }

    /** Safety net: fall damage is negated for the whole plunge, including the landing packet that arrives before our tick. */
    public static void onFall(LivingFallEvent event) {
        if (event.getEntity() instanceof ServerPlayer player
                && player.getData(ModAttachments.RUNTIME).state == CombatActionState.PLUNGE_ACTIVE) {
            event.setDamageMultiplier(0.0F);
        }
    }

    private static void land(ServerPlayer player, CombatRuntime rt) {
        ServerLevel level = player.serverLevel();
        long now = level.getGameTime();
        player.fallDistance = 0.0F;

        float base = (float) player.getAttributeValue(Attributes.ATTACK_DAMAGE);
        float posture = base * CombatTuning.POSTURE_PER_DAMAGE * CombatTuning.PLUNGE_POSTURE_MULT;
        AABB directBox = player.getBoundingBox().inflate(0.4, 0.6, 0.4);
        AABB areaBox = player.getBoundingBox().inflate(CombatTuning.PLUNGE_RADIUS, 1.5, CombatTuning.PLUNGE_RADIUS);

        List<LivingEntity> targets = level.getEntitiesOfClass(LivingEntity.class, areaBox,
                t -> t != player && t.isAlive() && player.canAttack(t) && !t.isAlliedTo(player));
        for (LivingEntity target : targets) {
            if (target.distanceToSqr(player) > CombatTuning.PLUNGE_RADIUS * CombatTuning.PLUNGE_RADIUS + 1.0) continue;
            boolean direct = target.getBoundingBox().intersects(directBox);
            float mult = direct ? CombatTuning.PLUNGE_DIRECT_MULT : CombatTuning.PLUNGE_AREA_MULT;
            Vec3 away = new Vec3(target.getX() - player.getX(), 0.0, target.getZ() - player.getZ());
            away = away.lengthSqr() < 1.0E-4 ? Vec3.ZERO : away.normalize();
            CombatDamage.strike(player, target, base * mult, direct ? posture : posture * 0.5F, away, direct ? 0.2 : 0.6);
        }

        level.sendParticles(ParticleTypes.EXPLOSION, player.getX(), player.getY() + 0.1, player.getZ(), 1, 0.0, 0.0, 0.0, 0.0);
        level.sendParticles(ParticleTypes.CLOUD, player.getX(), player.getY() + 0.1, player.getZ(), 24, CombatTuning.PLUNGE_RADIUS * 0.4, 0.05, CombatTuning.PLUNGE_RADIUS * 0.4, 0.05);
        level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.ANVIL_LAND, SoundSource.PLAYERS, 0.8F, 0.7F);

        // landing recovery (reuses HEAVY_RECOVERY: no pending impact, just the lock-out + slowness)
        rt.state = CombatActionState.HEAVY_RECOVERY;
        rt.heavyImpactTick = -1;
        rt.recoveryEndTick = now + CombatTuning.PLUNGE_RECOVERY_TICKS;
        player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, CombatTuning.PLUNGE_RECOVERY_TICKS, 3, false, false));
    }

    /** Blocks between the player's feet and the first collider below (64 block cap). */
    private static double groundDistance(ServerPlayer player) {
        Vec3 from = player.position();
        Vec3 to = from.add(0.0, -64.0, 0.0);
        var hit = player.level().clip(new ClipContext(from, to, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, player));
        return hit.getType() == HitResult.Type.MISS ? 64.0 : from.y - hit.getLocation().y;
    }

    private PlungeController() {}
}
