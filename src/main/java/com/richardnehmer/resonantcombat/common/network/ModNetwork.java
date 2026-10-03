package com.richardnehmer.resonantcombat.common.network;

import com.richardnehmer.resonantcombat.client.ClientPayloadHandler;
import com.richardnehmer.resonantcombat.common.attachment.ModAttachments;
import com.richardnehmer.resonantcombat.common.combat.AbilityController;
import com.richardnehmer.resonantcombat.common.combat.HeavyAttackController;
import com.richardnehmer.resonantcombat.common.combat.PlungeController;
import com.richardnehmer.resonantcombat.server.OnboardingService;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

public final class ModNetwork {
    private static final String PROTOCOL = "2";

    public static void onRegisterPayloads(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar registrar = event.registrar(PROTOCOL);

        // ---- server -> client (lambdas keep ClientPayloadHandler off dedicated servers) ----
        registrar.playToClient(ModPayloads.OpenClassSelection.TYPE, ModPayloads.OpenClassSelection.CODEC,
                (payload, ctx) -> ClientPayloadHandler.onOpenClassSelection(payload.respec()));
        registrar.playToClient(ModPayloads.CircuitAssigned.TYPE, ModPayloads.CircuitAssigned.CODEC,
                (payload, ctx) -> ClientPayloadHandler.onCircuitAssigned(payload));
        registrar.playToClient(ModPayloads.ProfileSync.TYPE, ModPayloads.ProfileSync.CODEC,
                (payload, ctx) -> ClientPayloadHandler.onProfileSync(payload));
        registrar.playToClient(ModPayloads.ActionResult.TYPE, ModPayloads.ActionResult.CODEC,
                (payload, ctx) -> ClientPayloadHandler.onActionResult(payload));
        registrar.playToClient(ModPayloads.PostureUpdate.TYPE, ModPayloads.PostureUpdate.CODEC,
                (payload, ctx) -> ClientPayloadHandler.onPostureUpdate(payload));

        registrar.playToClient(ModPayloads.AbilityCooldown.TYPE, ModPayloads.AbilityCooldown.CODEC,
                (payload, ctx) -> ClientPayloadHandler.onAbilityCooldown(payload));

        // ---- client -> server (main thread by default) ----
        registrar.playToServer(ModPayloads.ClassSelectionIntent.TYPE, ModPayloads.ClassSelectionIntent.CODEC,
                (payload, ctx) -> OnboardingService.handleSelection((ServerPlayer) ctx.player(), payload.classId()));
        registrar.playToServer(ModPayloads.AttackHoldStart.TYPE, ModPayloads.AttackHoldStart.CODEC,
                (payload, ctx) -> HeavyAttackController.onHoldStart((ServerPlayer) ctx.player()));
        registrar.playToServer(ModPayloads.AttackRelease.TYPE, ModPayloads.AttackRelease.CODEC,
                (payload, ctx) -> HeavyAttackController.onRelease((ServerPlayer) ctx.player()));
        registrar.playToServer(ModPayloads.DoubleJumpIntent.TYPE, ModPayloads.DoubleJumpIntent.CODEC,
                (payload, ctx) -> PlungeController.onDoubleJump((ServerPlayer) ctx.player()));
        registrar.playToServer(ModPayloads.BasicAttackIntent.TYPE, ModPayloads.BasicAttackIntent.CODEC,
                (payload, ctx) -> PlungeController.onBasicAttackIntent((ServerPlayer) ctx.player()));
        registrar.playToServer(ModPayloads.SkillIntent.TYPE, ModPayloads.SkillIntent.CODEC,
                (payload, ctx) -> AbilityController.onSkill((ServerPlayer) ctx.player()));
        registrar.playToServer(ModPayloads.UltimateIntent.TYPE, ModPayloads.UltimateIntent.CODEC,
                (payload, ctx) -> AbilityController.onUltimate((ServerPlayer) ctx.player()));
        registrar.playToServer(ModPayloads.EchoIntent.TYPE, ModPayloads.EchoIntent.CODEC,
                (payload, ctx) -> AbilityController.onEcho((ServerPlayer) ctx.player()));
    }

    /** NeoForge 1.21.1 has no automatic attachment sync, so we push snapshots manually. */
    public static void syncProfile(ServerPlayer player) {
        PacketDistributor.sendToPlayer(player, new ModPayloads.ProfileSync(player.getData(ModAttachments.PROFILE)));
    }

    private ModNetwork() {}
}
