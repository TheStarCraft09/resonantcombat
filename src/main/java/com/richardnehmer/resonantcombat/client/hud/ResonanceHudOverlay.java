package com.richardnehmer.resonantcombat.client.hud;

import com.richardnehmer.resonantcombat.client.ClientProfileCache;
import com.richardnehmer.resonantcombat.common.attachment.PlayerProfile;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;

/** Phase 1 placeholder HUD: Resonance + Liberation bars, bottom-left. Reposition next to Epic Fight's stamina bar later. */
public final class ResonanceHudOverlay {
    private static final int RESONANCE_COLOR = 0xFF4FC3F7;
    private static final int LIBERATION_COLOR = 0xFFFFC107;

    public static void render(GuiGraphics graphics, DeltaTracker delta) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.options.hideGui || mc.player == null) return;
        PlayerProfile profile = ClientProfileCache.get();
        if (!profile.hasCircuit()) return;

        int x = 10;
        int y = graphics.guiHeight() - 48;
        bar(graphics, x, y, 80, 5, profile.resonance() / PlayerProfile.MAX_RESOURCE, RESONANCE_COLOR);
        bar(graphics, x, y + 8, 80, 5, profile.liberationEnergy() / PlayerProfile.MAX_RESOURCE, LIBERATION_COLOR);

        // Pulse the Liberation bar border when the Ultimate is ready.
        if (profile.liberationEnergy() >= PlayerProfile.MAX_RESOURCE && (mc.player.tickCount / 5) % 2 == 0) {
            graphics.renderOutline(x - 1, y + 7, 82, 7, 0xFFFFFFFF);
        }
    }

    private static void bar(GuiGraphics g, int x, int y, int width, int height, float fraction, int color) {
        g.fill(x - 1, y - 1, x + width + 1, y + height + 1, 0xAA000000);
        g.fill(x, y, x + width, y + height, 0xFF202020);
        int filled = Math.round(width * Math.max(0.0F, Math.min(1.0F, fraction)));
        if (filled > 0) g.fill(x, y, x + filled, y + height, color);
    }

    private ResonanceHudOverlay() {}
}
