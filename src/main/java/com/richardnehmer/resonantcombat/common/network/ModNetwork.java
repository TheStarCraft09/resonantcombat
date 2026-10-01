package com.richardnehmer.resonantcombat.common.network;

import com.richardnehmer.resonantcombat.client.ClientPayloadHandler;
import com.richardnehmer.resonantcombat.common.attachment.ModAttachments;
import com.richardnehmer.resonantcombat.server.OnboardingService;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

public final class ModNetwork {
    private static final String PROTOCOL = "1";

    public static void onRegisterPayloads(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar registrar = event.registrar(PROTOCOL);

        // Client handlers go through lambdas so ClientPayloadHandler is never loaded on a dedicated server.
        registrar.playToClient(ModPayloads.OpenClassSelection.TYPE, ModPayloads.OpenClassSelection.CODEC,
                (payload, ctx) -> ClientPayloadHandler.onOpenClassSelection());
        registrar.playToClient(ModPayloads.CircuitAssigned.TYPE, ModPayloads.CircuitAssigned.CODEC,
                (payload, ctx) -> ClientPayloadHandler.onCircuitAssigned(payload));
        registrar.playToClient(ModPayloads.ProfileSync.TYPE, ModPayloads.ProfileSync.CODEC,
                (payload, ctx) -> ClientPayloadHandler.onProfileSync(payload));

        // Handlers run on the main thread by default in NeoForge 1.21.
        registrar.playToServer(ModPayloads.ClassSelectionIntent.TYPE, ModPayloads.ClassSelectionIntent.CODEC,
                (payload, ctx) -> OnboardingService.handleSelection((ServerPlayer) ctx.player(), payload.classId()));
    }

    /** NeoForge 1.21.1 has no automatic attachment sync, so we push snapshots manually. */
    public static void syncProfile(ServerPlayer player) {
        PacketDistributor.sendToPlayer(player, new ModPayloads.ProfileSync(player.getData(ModAttachments.PROFILE)));
    }

    private ModNetwork() {}
}
