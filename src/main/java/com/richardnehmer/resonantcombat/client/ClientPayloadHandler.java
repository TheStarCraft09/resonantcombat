package com.richardnehmer.resonantcombat.client;

import com.richardnehmer.resonantcombat.client.gui.CircuitRevealScreen;
import com.richardnehmer.resonantcombat.client.gui.ClassSelectionScreen;
import com.richardnehmer.resonantcombat.common.network.ModPayloads;
import net.minecraft.client.Minecraft;

/** Client-side payload handlers. Only referenced from lambdas, so it never loads on a dedicated server. */
public final class ClientPayloadHandler {

    public static void onOpenClassSelection() {
        Minecraft.getInstance().setScreen(new ClassSelectionScreen());
    }

    public static void onCircuitAssigned(ModPayloads.CircuitAssigned payload) {
        Minecraft.getInstance().setScreen(new CircuitRevealScreen(payload.circuitId()));
    }

    public static void onProfileSync(ModPayloads.ProfileSync payload) {
        ClientProfileCache.set(payload.profile());
    }

    private ClientPayloadHandler() {}
}
