package com.richardnehmer.resonantcombat.client.gui;

import com.richardnehmer.resonantcombat.common.data.CombatCircuitDefinition;
import com.richardnehmer.resonantcombat.common.registry.ModRegistries;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

/** Shows the server-assigned Circuit: after onboarding (reveal) and via the inspect key. */
public class CircuitRevealScreen extends Screen {
    private final ResourceLocation circuitId;
    private CombatCircuitDefinition circuit;

    public CircuitRevealScreen(ResourceLocation circuitId) {
        super(Component.translatable("screen.resonantcombat.reveal.title"));
        this.circuitId = circuitId;
    }

    @Override
    protected void init() {
        ClientPacketListener connection = Minecraft.getInstance().getConnection();
        if (connection != null) circuit = ModRegistries.circuits(connection.registryAccess()).get(circuitId);
        addRenderableWidget(Button.builder(Component.translatable("screen.resonantcombat.reveal.continue"),
                b -> onClose()).bounds(this.width / 2 - 60, this.height - 40, 120, 20).build());
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        super.render(g, mouseX, mouseY, partialTick);
        int cx = this.width / 2;
        int y = 40;
        g.drawCenteredString(this.font, this.title, cx, y, 0xAAAAAA);
        y += 22;
        g.drawCenteredString(this.font, ModRegistries.circuitName(circuitId), cx, y, 0xFFD54F);
        y += 16;

        if (circuit != null) {
            g.drawCenteredString(this.font, Component.translatable("screen.resonantcombat.reveal.class", ModRegistries.className(circuit.weaponClass())), cx, y, 0xFFFFFF);
            y += 12;
            g.drawCenteredString(this.font, Component.translatable("screen.resonantcombat.reveal.tier",
                    Component.translatable("tier.resonantcombat." + circuit.tier().getSerializedName())), cx, y, 0xFFFFFF);
            y += 20;
        }
        int width = Math.min(this.width - 40, 320);
        g.drawWordWrap(this.font, ModRegistries.circuitDesc(circuitId), cx - width / 2, y, width, 0xDDDDDD);
        y += 36;

        if (circuit != null) {
            // Ids are shown through lang keys later (Phase 6); for now the raw profile ids are enough to verify the roll.
            g.drawCenteredString(this.font, Component.literal("Combo: " + circuit.basicCombo().getPath() + "  |  Heavy: " + circuit.heavyProfile().getPath()), cx, y, 0x888888);
            g.drawCenteredString(this.font, Component.literal(
                    "Skill: " + circuit.resonanceSkill().getPath() + "  |  Ultimate: " + circuit.ultimate().getPath()), cx, y + 12, 0x888888);
        }
    }

    @Override public boolean isPauseScreen() { return false; }
}
