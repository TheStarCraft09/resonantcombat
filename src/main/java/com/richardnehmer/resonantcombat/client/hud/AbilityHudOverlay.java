package com.richardnehmer.resonantcombat.client.hud;

import com.richardnehmer.resonantcombat.client.ClientActionState;
import com.richardnehmer.resonantcombat.client.ClientKeyMappings;
import com.richardnehmer.resonantcombat.client.ClientProfileCache;
import com.richardnehmer.resonantcombat.common.attachment.PlayerProfile;
import com.richardnehmer.resonantcombat.common.data.AbilityDefinition;
import com.richardnehmer.resonantcombat.common.data.CombatCircuitDefinition;
import com.richardnehmer.resonantcombat.common.registry.ModRegistries;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.resources.ResourceLocation;

/** Skill / Ultimate / Echo boxes above the Resonance bars (design doc 5.5). Placeholder visuals until icons exist. */
public final class AbilityHudOverlay {
    private static final int BOX = 26;

    public static void render(GuiGraphics g, DeltaTracker delta) {
        Minecraft mc = Minecraft.getInstance();
        ClientPacketListener connection = mc.getConnection();
        if (mc.options.hideGui || mc.player == null || mc.level == null || connection == null) return;
        PlayerProfile profile = ClientProfileCache.get();
        if (!profile.hasCircuit()) return;

        CombatCircuitDefinition circuit = ModRegistries.circuits(connection.registryAccess()).get(profile.assignedCircuit().orElseThrow());
        if (circuit == null) return;
        var abilities = ModRegistries.abilities(connection.registryAccess());
        long now = mc.level.getGameTime();
        int x = 10, y = g.guiHeight() - 48 - BOX - 6;

        // ---- skill ----
        AbilityDefinition skill = abilities.get(circuit.resonanceSkill());
        if (skill != null) {
            boolean affordable = profile.resonance() >= skill.resonanceCost();
            box(g, mc, x, y, ClientKeyMappings.SKILL, circuit.resonanceSkill(), affordable ? 0xFF4FC3F7 : 0xFF555555,
                    cooldown(0, now), (int) skill.resonanceCost() + "", null);
        }
        // ---- ultimate ----
        boolean ready = profile.liberationEnergy() >= PlayerProfile.MAX_RESOURCE;
        boolean pulse = ready && (mc.player.tickCount / 5) % 2 == 0;
        box(g, mc, x + BOX + 4, y, ClientKeyMappings.ULTIMATE, circuit.ultimate(), ready ? (pulse ? 0xFFFFFFFF : 0xFFFFC107) : 0xFF555555,
                0.0F, null, null);
        // ---- echo ----
        ResourceLocation echoId = profile.equippedEcho().orElse(null);
        if (echoId != null) {
            AbilityDefinition echo = abilities.get(echoId);
            int charges = profile.echoCharge(echoId);
            box(g, mc, x + 2 * (BOX + 4), y, ClientKeyMappings.ECHO, echoId, charges > 0 ? 0xFFB388FF : 0xFF555555,
                    cooldown(2, now), null, "x" + charges + (echo != null ? "/" + echo.maxCharges() : ""));
        }
    }

    private static float cooldown(int slot, long now) {
        long end = ClientActionState.cooldownEnd[slot];
        int length = ClientActionState.cooldownLength[slot];
        return end > now && length > 0 ? (end - now) / (float) length : 0.0F;
    }

    private static void box(GuiGraphics g, Minecraft mc, int x, int y, KeyMapping key, ResourceLocation id,
                            int border, float cooldownFraction, String cost, String charges) {
        g.fill(x - 1, y - 1, x + BOX + 1, y + BOX + 1, border);
        g.fill(x, y, x + BOX, y + BOX, 0xFF1A1A1A);
        if (cooldownFraction > 0.0F) {
            int h = Math.round(BOX * cooldownFraction);
            g.fill(x, y + BOX - h, x + BOX, y + BOX, 0xCC000000);
        }
        String name = ModRegistries.abilityName(id).getString();
        g.drawCenteredString(mc.font, name.isEmpty() ? "?" : name.substring(0, 1), x + BOX / 2, y + 4, 0xFFFFFFFF);
        g.drawCenteredString(mc.font, key.getTranslatedKeyMessage(), x + BOX / 2, y + BOX - 10, 0xFFAAAAAA);
        if (cost != null) g.drawString(mc.font, cost, x + 1, y - 9, 0xFF4FC3F7, false);
        if (charges != null) g.drawString(mc.font, charges, x + 1, y - 9, 0xFFB388FF, false);
    }

    private AbilityHudOverlay() {}
}
