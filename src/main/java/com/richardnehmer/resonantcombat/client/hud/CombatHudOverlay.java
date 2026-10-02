package com.richardnehmer.resonantcombat.client.hud;

import com.richardnehmer.resonantcombat.client.ClientActionState;
import com.richardnehmer.resonantcombat.common.combat.CombatTuning;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;

/** Heavy meter (near crosshair), plunge indicator, and target posture bar (design doc 5.5). */
public final class CombatHudOverlay {

    public static void render(GuiGraphics g, DeltaTracker delta) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.options.hideGui || mc.player == null || mc.level == null) return;
        int cx = g.guiWidth() / 2;
        int cy = g.guiHeight() / 2;
        long now = mc.level.getGameTime();

        // ---- heavy meter ----
        if (ClientActionState.holdActive && ClientActionState.chargeTicks >= 3) {
            int ticks = ClientActionState.chargeTicks;
            int width = 60;
            int x = cx - width / 2, y = cy + 14;
            float fraction = Math.min(1.0F, ticks / (float) CombatTuning.HEAVY_III_TICKS);
            int tier = ticks >= CombatTuning.HEAVY_III_TICKS ? 3 : ticks >= CombatTuning.HEAVY_II_TICKS ? 2 : ticks >= CombatTuning.HEAVY_MIN_TICKS ? 1 : 0;
            int color = switch (tier) { case 3 -> 0xFFFF5252; case 2 -> 0xFFFFC107; case 1 -> 0xFF4FC3F7; default -> 0xFF777777; };
            g.fill(x - 1, y - 1, x + width + 1, y + 5, 0xAA000000);
            g.fill(x, y, x + Math.round(width * fraction), y + 4, color);
            // threshold ticks
            g.fill(x + width * CombatTuning.HEAVY_MIN_TICKS / CombatTuning.HEAVY_III_TICKS, y - 2, x + width * CombatTuning.HEAVY_MIN_TICKS / CombatTuning.HEAVY_III_TICKS + 1, y + 6, 0xFFFFFFFF);
            g.fill(x + width * CombatTuning.HEAVY_II_TICKS / CombatTuning.HEAVY_III_TICKS, y - 2, x + width * CombatTuning.HEAVY_II_TICKS / CombatTuning.HEAVY_III_TICKS + 1, y + 6, 0xFFFFFFFF);
            if (tier > 0) g.drawCenteredString(mc.font, "I".repeat(tier), cx, y + 8, color);
        }

        // ---- plunge indicator ----
        if (ClientActionState.plungeWindowEnd > now && !mc.player.onGround()) {
            g.drawCenteredString(mc.font, Component.translatable("hud.resonantcombat.plunge_ready"), cx, cy + 34, 0xFFFFD54F);
        }

        // ---- posture bar of the targeted entity ----
        Entity target = mc.crosshairPickEntity;
        if (target != null) {
            ClientActionState.PostureView view = ClientActionState.POSTURE.get(target.getId());
            if (view != null && now - view.updatedAt() < 120 && (view.posture() > 0.0F || view.staggered())) {
                int width = 120;
                int x = cx - width / 2, y = 26;
                g.fill(x - 1, y - 1, x + width + 1, y + 5, 0xAA000000);
                if (view.staggered()) {
                    boolean flash = (now / 3) % 2 == 0;
                    g.fill(x, y, x + width, y + 4, flash ? 0xFFFF5252 : 0xFFB71C1C);
                } else {
                    g.fill(x, y, x + Math.round(width * Math.min(1.0F, view.posture() / view.max())), y + 4, 0xFFFFA726);
                }
                g.drawCenteredString(mc.font, Component.translatable("hud.resonantcombat.posture"), cx, y + 7, 0xFFCCCCCC);
            }
        }
    }

    private CombatHudOverlay() {}
}
