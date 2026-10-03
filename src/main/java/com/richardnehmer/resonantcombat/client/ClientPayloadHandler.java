package com.richardnehmer.resonantcombat.client;

import com.richardnehmer.resonantcombat.client.gui.CircuitRevealScreen;
import com.richardnehmer.resonantcombat.client.gui.ClassSelectionScreen;
import com.richardnehmer.resonantcombat.common.network.ModPayloads;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;

/** Client-side payload handlers. Only referenced from lambdas, so it never loads on a dedicated server. */
public final class ClientPayloadHandler {

    public static void onOpenClassSelection(boolean respec) {
        Minecraft.getInstance().setScreen(new ClassSelectionScreen(respec));
    }

    public static void onCircuitAssigned(ModPayloads.CircuitAssigned payload) {
        Minecraft.getInstance().setScreen(new CircuitRevealScreen(payload.circuitId()));
    }

    public static void onProfileSync(ModPayloads.ProfileSync payload) {
        ClientProfileCache.set(payload.profile());
    }

    public static void onActionResult(ModPayloads.ActionResult payload) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) return;
        if (payload.accepted()) {
            if (payload.action() == ModPayloads.ActionResult.ACTION_DOUBLE_JUMP) {
                ClientActionState.plungeWindowEnd = mc.level.getGameTime() + payload.value();
            } else if (payload.action() == ModPayloads.ActionResult.ACTION_PLUNGE) {
                ClientActionState.plungeWindowEnd = 0L;
            }
        } else if (!payload.reason().isEmpty()) {
            mc.gui.setOverlayMessage(Component.translatable("message.resonantcombat.reject." + payload.reason()), false);
        }
    }

    public static void onAbilityCooldown(ModPayloads.AbilityCooldown payload) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || payload.slot() < 0 || payload.slot() >= ClientActionState.cooldownEnd.length) return;
        ClientActionState.cooldownEnd[payload.slot()] = mc.level.getGameTime() + payload.ticks();
        ClientActionState.cooldownLength[payload.slot()] = payload.ticks();
    }

    public static void onPostureUpdate(ModPayloads.PostureUpdate payload) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) return;
        ClientActionState.POSTURE.put(payload.entityId(), new ClientActionState.PostureView(
                payload.posture(), payload.max(), payload.staggered(), mc.level.getGameTime()));
    }

    private ClientPayloadHandler() {}
}
