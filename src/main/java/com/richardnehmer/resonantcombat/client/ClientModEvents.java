package com.richardnehmer.resonantcombat.client;

import com.richardnehmer.resonantcombat.ResonantCombat;
import com.richardnehmer.resonantcombat.client.hud.AbilityHudOverlay;
import com.richardnehmer.resonantcombat.client.hud.CombatHudOverlay;
import com.richardnehmer.resonantcombat.client.hud.ResonanceHudOverlay;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;

@EventBusSubscriber(modid = ResonantCombat.MOD_ID, value = Dist.CLIENT)
public final class ClientModEvents {

    @SubscribeEvent
    public static void onKeys(RegisterKeyMappingsEvent event) {
        event.register(ClientKeyMappings.SKILL);
        event.register(ClientKeyMappings.ULTIMATE);
        event.register(ClientKeyMappings.ECHO);
        event.register(ClientKeyMappings.INSPECT);
        event.register(ClientKeyMappings.DOUBLE_JUMP);
    }

    @SubscribeEvent
    public static void onGuiLayers(RegisterGuiLayersEvent event) {
        event.registerAboveAll(ResonantCombat.id("resonance_hud"), ResonanceHudOverlay::render);
        event.registerAboveAll(ResonantCombat.id("combat_hud"), CombatHudOverlay::render);
        event.registerAboveAll(ResonantCombat.id("ability_hud"), AbilityHudOverlay::render);
    }

    private ClientModEvents() {}
}
