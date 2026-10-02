package com.richardnehmer.resonantcombat.client;

import com.richardnehmer.resonantcombat.ResonantCombat;
import com.richardnehmer.resonantcombat.client.gui.CircuitRevealScreen;
import net.minecraft.client.Minecraft;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;

@EventBusSubscriber(modid = ResonantCombat.MOD_ID, value = Dist.CLIENT)
public final class ClientGameEvents {

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.screen != null) return;

        while (ClientKeyMappings.INSPECT.consumeClick()) {
            ClientProfileCache.get().assignedCircuit().ifPresent(id -> mc.setScreen(new CircuitRevealScreen(id)));
        }
        // TODO(Phase 5): consume SKILL / ULTIMATE / ECHO clicks and send intent payloads.
    }

    @SubscribeEvent
    public static void onLogout(ClientPlayerNetworkEvent.LoggingOut event) {
        ClientProfileCache.clear();
        ClientActionState.clear();
    }

    private ClientGameEvents() {}
}
