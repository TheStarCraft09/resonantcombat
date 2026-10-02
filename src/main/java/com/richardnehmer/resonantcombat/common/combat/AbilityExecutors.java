package com.richardnehmer.resonantcombat.common.combat;

import com.richardnehmer.resonantcombat.ResonantCombat;
import com.richardnehmer.resonantcombat.common.attachment.CombatRuntime;
import com.richardnehmer.resonantcombat.common.data.AbilityDefinition;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.ResourceLocation;
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
import com.richardnehmer.resonantcombat.integration.epicfight.AnimationHook;

/**
 * Server-side executors selected by AbilityDefinition#type. Everything here is server-authoritative: positions, hit
 * detection, damage and teleport destinations are all computed from server state.
 */
public final class AbilityExecutors {

    /** Checks that must pass BEFORE any cost is paid. @return a lang reason key, or null if the ability can start. */
    public static String precheck(ServerPlayer player, AbilityDefinition def) {
        return switch (def.type()) {
            case "mark" -> findMarkTarget(player, def) == null ? "no_target" : null;
            case "blink" -> blinkDestination(player, def) == null ? "no_space" : null;
            default -> null;
        };
    }

    public static void start(ServerPlayer player, CombatRuntime rt, ResourceLocation id, AbilityDefinition def, long now) {
        Vec3 look = player.getLookAngle();
        Vec3 facing = new Vec3(look.x, 0.0, look.z);
        facing = facing.lengthSqr() < 1.0E-4 ? Vec3.directionFromRotation(0.0F, player.getYRot()) : facing.normalize();

        rt.activeAbility = id;
        rt.abilityHit.clear();
        rt.abilityDirX = facing.x;
        rt.abilityDirZ = facing.z;
        rt.state = switch (def.kind()) {
            case SKILL -> CombatActionState.SKILL_ACTIVE;
            case ULTIMATE -> CombatActionState.ULTIMATE_ACTIVE;
            case ECHO -> CombatActionState.ECHO_ACTIVE;
        };

        rt.abilityAnimated = def.animation().map(a -> AnimationHook.play(player, a, 0.1F)).orElse(false);
        if (!rt.abilityAnimated) player.swing(InteractionHand.MAIN_HAND, true);

        switch (def.type()) {
            case "dash_strike" -> {
                int dashTicks = (int) def.param("dash_ticks", 6);
                rt.abilityDashEnd = now + dashTicks;
                rt.abilityEndTick = now + dashTicks + 4;
                invisibility(player, def);
            }
            case "area_burst" -> {
                int repeats = Math.max(1, (int) def.param("repeats", 1));
                int interval = (int) def.param("interval", 4);
                rt.burstsLeft = repeats;
                rt.nextBurstTick = now + (long) def.param("delay", 8);
                rt.abilityEndTick = rt.nextBurstTick + (long) (repeats - 1) * interval + 8;
            }
            case "stance" -> {
                rt.stanceEnd = now + (long) def.param("duration", 200);
                rt.stanceDamageMult = (float) def.param("damage_mult", 1.0);
                rt.stanceTakenMult = (float) def.param("taken_mult", 1.0);
                rt.stanceHeavyFree = def.param("heavy_free", 0.0) > 0.0;
                rt.abilityEndTick = now + (long) def.param("cast_ticks", 10);
                invisibility(player, def);
                player.serverLevel().sendParticles(ParticleTypes.ENCHANTED_HIT, player.getX(), player.getY(0.5), player.getZ(), 24, 0.5, 0.7, 0.5, 0.2);
                player.serverLevel().playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.PLAYER_LEVELUP, SoundSource.PLAYERS, 0.5F, 1.4F);
            }
            case "counter" -> {
                rt.counterEnd = now + (long) def.param("window", 20);
                rt.counterDamageMult = (float) def.param("damage_mult", 2.0);
                rt.counterPostureMult = (float) def.param("posture_mult", 4.0);
                rt.abilityEndTick = now + 6;
                player.serverLevel().sendParticles(ParticleTypes.CRIT, player.getX(), player.getY(1.0), player.getZ(), 10, 0.4, 0.4, 0.4, 0.1);
            }
            case "mark" -> {
                LivingEntity target = findMarkTarget(player, def);
                rt.abilityEndTick = now + 8;
                if (target != null) {
                    int duration = (int) def.param("duration", 240);
                    rt.markedEntityId = target.getId();
                    rt.markEnd = now + duration;
                    rt.markBonus = (float) def.param("bonus", 1.25);
                    target.addEffect(new MobEffectInstance(MobEffects.GLOWING, duration, 0, false, false));
                }
            }
            case "blink" -> {
                Vec3 dest = blinkDestination(player, def);
                rt.abilityEndTick = now + 4;
                if (dest != null) {
                    ServerLevel level = player.serverLevel();
                    level.sendParticles(ParticleTypes.PORTAL, player.getX(), player.getY(1.0), player.getZ(), 30, 0.3, 0.6, 0.3, 0.5);
                    player.teleportTo(dest.x, dest.y, dest.z);
                    player.fallDistance = 0.0F;
                    rt.hasLast = false; // velocity estimate is invalid after a teleport
                    level.sendParticles(ParticleTypes.PORTAL, dest.x, dest.y + 1.0, dest.z, 30, 0.3, 0.6, 0.3, 0.5);
                    level.playSound(null, dest.x, dest.y, dest.z, SoundEvents.ENDERMAN_TELEPORT, SoundSource.PLAYERS, 0.8F, 1.2F);
                }
            }
            default -> {
                ResonantCombat.LOGGER.warn("Unknown ability type '{}' for {}", def.type(), id);
                rt.abilityEndTick = now + 10;
            }
        }

        // Animation-driven hits: Epic Fight's phases deal the damage, we only scale it.
        if (rt.abilityAnimated && (def.type().equals("dash_strike") || def.type().equals("area_burst"))) {
            rt.hitDamageMult = (float) def.param("damage_mult", 1.0);
            rt.hitPostureMult = (float) def.param("posture_mult", 1.0);
            rt.hitMultEnd = rt.abilityEndTick;
        }
    }

    public static void tick(ServerPlayer player, CombatRuntime rt, AbilityDefinition def, long now) {
        switch (def.type()) {
            case "dash_strike" -> tickDash(player, rt, def, now);
            case "area_burst" -> tickBurst(player, rt, def, now);
            default -> { }
        }
    }

    // ---------------------------------------------------------------- dash_strike

    private static void tickDash(ServerPlayer player, CombatRuntime rt, AbilityDefinition def, long now) {
        if (now < rt.abilityDashEnd) {
            double dir = def.param("dir", 1.0);
            double speed = def.param("dash_speed", 0.9);
            player.setDeltaMovement(rt.abilityDirX * dir * speed, def.param("dash_vy", 0.0), rt.abilityDirZ * dir * speed);
            player.hurtMarked = true;
            player.fallDistance = 0.0F;

            double damageMult = def.param("damage_mult", 0.0);
            if (damageMult > 0.0 && !rt.abilityAnimated) {
                double r = def.param("range", 1.6);
                float base = (float) player.getAttributeValue(Attributes.ATTACK_DAMAGE);
                AABB box = player.getBoundingBox().inflate(r, 0.6, r);
                for (LivingEntity target : player.serverLevel().getEntitiesOfClass(LivingEntity.class, box, t -> isHostileTarget(player, t))) {
                    if (!rt.abilityHit.add(target.getId())) continue; // once per target per dash
                    CombatDamage.strike(player, target, base * (float) damageMult,
                            base * CombatTuning.POSTURE_PER_DAMAGE * (float) def.param("posture_mult", 2.0),
                            new Vec3(rt.abilityDirX, 0.0, rt.abilityDirZ), def.param("knockback", 0.5));
                    ignite(target, def);
                }
            }
        } else if (now == rt.abilityDashEnd) {
            player.setDeltaMovement(0.0, rt.velY, 0.0); // end of dash: kill horizontal momentum
            player.hurtMarked = true;
        }
    }

    // ---------------------------------------------------------------- area_burst

    private static void tickBurst(ServerPlayer player, CombatRuntime rt, AbilityDefinition def, long now) {
        if (rt.burstsLeft <= 0 || now < rt.nextBurstTick) return;
        rt.burstsLeft--;
        rt.nextBurstTick += (long) def.param("interval", 4);

        ServerLevel level = player.serverLevel();
        double forward = def.param("forward", 0.0);
        double radius = def.param("radius", 3.0);
        Vec3 center = player.position().add(rt.abilityDirX * forward, 0.0, rt.abilityDirZ * forward);

        level.sendParticles(radius > 3.5 ? ParticleTypes.EXPLOSION : ParticleTypes.SWEEP_ATTACK,
                center.x, center.y + 0.5, center.z, 1 + (int) (radius / 2), radius * 0.3, 0.2, radius * 0.3, 0.0);
        level.playSound(null, center.x, center.y, center.z, SoundEvents.PLAYER_ATTACK_STRONG, SoundSource.PLAYERS, 0.9F, 0.8F);
        if (rt.abilityAnimated) return; // the animation's hit phases deal the damage

        float base = (float) player.getAttributeValue(Attributes.ATTACK_DAMAGE);
        AABB box = new AABB(center, center).inflate(radius, 2.0, radius);
        for (LivingEntity target : level.getEntitiesOfClass(LivingEntity.class, box, t -> isHostileTarget(player, t))) {
            double dx = target.getX() - center.x, dz = target.getZ() - center.z;
            double dist = Math.sqrt(dx * dx + dz * dz);
            if (dist > radius + target.getBbWidth() * 0.5) continue;
            Vec3 push = dist < 0.01 ? new Vec3(rt.abilityDirX, 0.0, rt.abilityDirZ) : new Vec3(dx / dist, 0.0, dz / dist);
            if (CombatDamage.strike(player, target, base * (float) def.param("damage_mult", 1.0),
                    base * CombatTuning.POSTURE_PER_DAMAGE * (float) def.param("posture_mult", 2.0), push, def.param("knockback", 0.6))) {
                double launch = def.param("launch", 0.0);
                if (launch > 0.0) {
                    target.setDeltaMovement(target.getDeltaMovement().add(0.0, launch, 0.0));
                    target.hurtMarked = true;
                }
                ignite(target, def);
            }
        }
    }

    // ---------------------------------------------------------------- counter

    public static void triggerCounter(ServerPlayer player, CombatRuntime rt, LivingEntity attacker) {
        rt.counterEnd = 0L;
        float base = (float) player.getAttributeValue(Attributes.ATTACK_DAMAGE);
        Vec3 push = new Vec3(attacker.getX() - player.getX(), 0.0, attacker.getZ() - player.getZ());
        push = push.lengthSqr() < 1.0E-4 ? Vec3.ZERO : push.normalize();
        CombatDamage.strike(player, attacker, base * rt.counterDamageMult,
                base * CombatTuning.POSTURE_PER_DAMAGE * rt.counterPostureMult, push, 0.6);

        ServerLevel level = player.serverLevel();
        level.sendParticles(ParticleTypes.ENCHANTED_HIT, attacker.getX(), attacker.getY(0.6), attacker.getZ(), 20, 0.4, 0.5, 0.4, 0.3);
        level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.SHIELD_BLOCK, SoundSource.PLAYERS, 1.0F, 1.3F);
        player.invulnerableTime = Math.max(player.invulnerableTime, 10);
    }

    // ---------------------------------------------------------------- mark / blink helpers

    private static LivingEntity findMarkTarget(ServerPlayer player, AbilityDefinition def) {
        double range = def.param("range", 24.0);
        Vec3 eye = player.getEyePosition();
        Vec3 look = player.getLookAngle();
        AABB box = player.getBoundingBox().expandTowards(look.scale(range)).inflate(2.0);
        LivingEntity best = null;
        double bestDot = 0.97;
        for (LivingEntity t : player.serverLevel().getEntitiesOfClass(LivingEntity.class, box,
                e -> isHostileTarget(player, e) && player.hasLineOfSight(e))) {
            Vec3 to = t.getEyePosition().subtract(eye);
            double dist = to.length();
            if (dist > range || dist < 0.1) continue;
            double dot = look.dot(to.scale(1.0 / dist));
            if (dot > bestDot) { bestDot = dot; best = t; }
        }
        return best;
    }

    /** Furthest safe destination along the look vector: loaded chunk, no collision, no blocks in between. */
    private static Vec3 blinkDestination(ServerPlayer player, AbilityDefinition def) {
        double max = def.param("distance", 12.0);
        Vec3 look = player.getLookAngle();
        Vec3 start = player.position();
        for (double d = max; d >= 2.0; d -= 0.5) {
            Vec3 dest = start.add(look.x * d, look.y * d, look.z * d);
            if (!player.level().isLoaded(BlockPos.containing(dest))) continue;
            if (!player.level().noCollision(player, player.getBoundingBox().move(dest.subtract(start)))) continue;
            var path = player.level().clip(new ClipContext(player.getEyePosition(), dest.add(0.0, player.getEyeHeight(), 0.0),
                    ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, player));
            if (path.getType() == HitResult.Type.MISS) return dest;
        }
        return null;
    }

    // ---------------------------------------------------------------- small helpers

    private static boolean isHostileTarget(ServerPlayer player, LivingEntity t) {
        return t != player && t.isAlive() && player.canAttack(t) && !t.isAlliedTo(player);
    }

    private static void invisibility(ServerPlayer player, AbilityDefinition def) {
        int ticks = (int) def.param("invis_ticks", 0);
        if (ticks > 0) player.addEffect(new MobEffectInstance(MobEffects.INVISIBILITY, ticks, 0, false, false));
    }

    private static void ignite(LivingEntity target, AbilityDefinition def) {
        int ticks = (int) def.param("ignite", 0);
        if (ticks > 0) target.setRemainingFireTicks(ticks);
    }

    private AbilityExecutors() {}
}
