package com.richardnehmer.resonantcombat.common.combat;

import com.richardnehmer.resonantcombat.ResonantCombat;
import com.richardnehmer.resonantcombat.common.attachment.CombatRuntime;
import com.richardnehmer.resonantcombat.common.attachment.ModAttachments;
import com.richardnehmer.resonantcombat.common.attachment.PlayerProfile;
import com.richardnehmer.resonantcombat.common.data.AbilityDefinition;
import com.richardnehmer.resonantcombat.common.data.AbilityKind;
import com.richardnehmer.resonantcombat.common.data.CombatCircuitDefinition;
import com.richardnehmer.resonantcombat.common.network.ModNetwork;
import com.richardnehmer.resonantcombat.common.network.ModPayloads;
import com.richardnehmer.resonantcombat.common.registry.ModRegistries;
import com.richardnehmer.resonantcombat.common.registry.WeaponClassResolver;
import com.richardnehmer.resonantcombat.integration.epicfight.EpicFightBridge;
import com.richardnehmer.resonantcombat.integration.epicfight.EpicFightStamina;
import net.minecraft.core.RegistryAccess;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.Optional;

/** Resonance Skill (R), Resonance Liberation (V) and Echo Skill (G): validation, costs, cooldowns, echo recharge. */
public final class AbilityController {
    public static final int SLOT_SKILL = 3, SLOT_ULTIMATE = 4, SLOT_ECHO = 5;
    /** HUD cooldown slots sent to the client. */
    public static final int HUD_SKILL = 0, HUD_ECHO = 2;

    public static void onSkill(ServerPlayer player) { cast(player, AbilityKind.SKILL, SLOT_SKILL); }
    public static void onUltimate(ServerPlayer player) { cast(player, AbilityKind.ULTIMATE, SLOT_ULTIMATE); }
    public static void onEcho(ServerPlayer player) { cast(player, AbilityKind.ECHO, SLOT_ECHO); }

    private static void cast(ServerPlayer player, AbilityKind kind, int slot) {
        if (!ActionValidator.baseChecks(player) || !ActionValidator.rateLimit(player, slot)) return;
        CombatRuntime rt = player.getData(ModAttachments.RUNTIME);
        PlayerProfile profile = player.getData(ModAttachments.PROFILE);
        long now = player.level().getGameTime();
        RegistryAccess access = player.level().registryAccess();

        if (!profile.hasCircuit() || rt.state != CombatActionState.NEUTRAL) return; // silent: busy or no Circuit
        if (!EpicFightBridge.isBattleMode(player)) return;

        Optional<ResourceLocation> idOpt = abilityId(profile, access, kind);
        if (idOpt.isEmpty()) {
            reject(player, kind == AbilityKind.ECHO ? "no_echo" : "wrong_weapon");
            return;
        }
        ResourceLocation id = idOpt.get();
        AbilityDefinition def = ModRegistries.abilities(access).get(id);
        if (def == null) {
            ResonantCombat.LOGGER.warn("Ability {} is not defined", id);
            return;
        }

        // ---- gating ----
        if (kind != AbilityKind.ECHO && !WeaponClassResolver.isCircuitActive(player)) { reject(player, "wrong_weapon"); return; }
        switch (kind) {
            case SKILL -> {
                if (now < rt.skillCooldownEnd) { reject(player, "cooldown"); return; }
                if (profile.resonance() < def.resonanceCost()) { reject(player, "no_resonance"); return; }
            }
            case ULTIMATE -> {
                if (profile.liberationEnergy() < PlayerProfile.MAX_RESOURCE) { reject(player, "no_liberation"); return; }
            }
            case ECHO -> {
                if (now < rt.echoCooldownEnd) { reject(player, "cooldown"); return; }
                if (profile.echoCharge(id) <= 0) { reject(player, "no_charges"); return; }
            }
        }
        String blocked = AbilityExecutors.precheck(player, def);
        if (blocked != null) { reject(player, blocked); return; }
        if (def.staminaCost() > 0.0F && !EpicFightStamina.tryConsume(player, def.staminaCost())) { reject(player, "no_stamina"); return; }

        // ---- pay ----
        switch (kind) {
            case SKILL -> {
                profile.setResonance(profile.resonance() - def.resonanceCost());
                rt.skillCooldownEnd = now + def.cooldownTicks();
                PacketDistributor.sendToPlayer(player, new ModPayloads.AbilityCooldown(HUD_SKILL, def.cooldownTicks()));
            }
            case ULTIMATE -> profile.setLiberationEnergy(0.0F); // consumes all Liberation Energy
            case ECHO -> {
                profile.setEchoCharge(id, profile.echoCharge(id) - 1);
                rt.echoCooldownEnd = now + def.cooldownTicks();
                if (rt.echoRechargeEnd == 0L) rt.echoRechargeEnd = now + def.rechargeTicks();
                PacketDistributor.sendToPlayer(player, new ModPayloads.AbilityCooldown(HUD_ECHO, def.cooldownTicks()));
            }
        }
        ModNetwork.syncProfile(player);
        AbilityExecutors.start(player, rt, id, def, now);
    }

    /** Per tick while SKILL_ACTIVE / ULTIMATE_ACTIVE / ECHO_ACTIVE. */
    public static void tick(ServerPlayer player, CombatRuntime rt, long now) {
        AbilityDefinition def = rt.activeAbility == null ? null
                : ModRegistries.abilities(player.level().registryAccess()).get(rt.activeAbility);
        if (def == null) { rt.state = CombatActionState.NEUTRAL; rt.activeAbility = null; return; }
        AbilityExecutors.tick(player, rt, def, now);
        if (now >= rt.abilityEndTick && rt.burstsLeft <= 0) {
            rt.state = CombatActionState.NEUTRAL;
            rt.activeAbility = null;
        }
    }

    /** Echo charge recharge (called every few ticks from the ticker). */
    public static void tickEchoRecharge(ServerPlayer player, CombatRuntime rt, long now) {
        PlayerProfile profile = player.getData(ModAttachments.PROFILE);
        Optional<ResourceLocation> echo = profile.equippedEcho();
        if (echo.isEmpty()) return;
        AbilityDefinition def = ModRegistries.abilities(player.level().registryAccess()).get(echo.get());
        if (def == null) return;

        int charges = profile.echoCharge(echo.get());
        if (charges >= def.maxCharges()) { rt.echoRechargeEnd = 0L; return; }
        if (rt.echoRechargeEnd == 0L) { rt.echoRechargeEnd = now + def.rechargeTicks(); return; }
        if (now >= rt.echoRechargeEnd) {
            profile.setEchoCharge(echo.get(), charges + 1);
            rt.echoRechargeEnd = charges + 1 < def.maxCharges() ? now + def.rechargeTicks() : 0L;
            ModNetwork.syncProfile(player);
        }
    }

    private static Optional<ResourceLocation> abilityId(PlayerProfile profile, RegistryAccess access, AbilityKind kind) {
        if (kind == AbilityKind.ECHO) return profile.equippedEcho();
        CombatCircuitDefinition circuit = ModRegistries.circuits(access).get(profile.assignedCircuit().orElseThrow());
        if (circuit == null) return Optional.empty();
        return Optional.of(kind == AbilityKind.SKILL ? circuit.resonanceSkill() : circuit.ultimate());
    }

    private static void reject(ServerPlayer player, String reason) {
        ActionValidator.reject(player, ModPayloads.ActionResult.ACTION_ABILITY, reason);
    }

    private AbilityController() {}
}
