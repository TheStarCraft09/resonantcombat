package com.richardnehmer.resonantcombat.client.gui;

import com.richardnehmer.resonantcombat.common.data.WeaponClassDefinition;
import com.richardnehmer.resonantcombat.common.network.ModPayloads;
import com.richardnehmer.resonantcombat.common.registry.ModRegistries;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.ConfirmScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * First-join class selection (design doc 2.4). Cannot be dismissed with ESC; the server reopens it on every join
 * until onboarding is complete. Sends INTENT only - the server rolls the Circuit.
 */
public class ClassSelectionScreen extends Screen {
    private record Entry(ResourceLocation id, WeaponClassDefinition def) {}

    private final List<Entry> entries = new ArrayList<>();
    private Entry selected;
    private Button confirmButton;

    private final boolean respec;

    public ClassSelectionScreen(boolean respec) {
        super(Component.translatable("screen.resonantcombat.class_selection.title"));
        this.respec = respec;
    }

    @Override
    protected void init() {
        entries.clear();
        ClientPacketListener connection = Minecraft.getInstance().getConnection();
        if (connection != null) {
            ModRegistries.weaponClasses(connection.registryAccess()).entrySet()
                    .forEach(e -> entries.add(new Entry(e.getKey().location(), e.getValue())));
            entries.sort(Comparator.comparingInt((Entry e) -> e.def().order()).thenComparing(Entry::id));
        }

        int top = 40;
        for (Entry entry : entries) {
            addRenderableWidget(Button.builder(ModRegistries.className(entry.id()), b -> select(entry))
                    .bounds(20, top, 120, 20).build());
            top += 24;
        }

        confirmButton = addRenderableWidget(Button.builder(Component.translatable("screen.resonantcombat.class_selection.confirm"),
                b -> askConfirmation()).bounds(this.width / 2 - 100, this.height - 30, 200, 20).build());
        confirmButton.active = selected != null;
    }

    private void select(Entry entry) {
        this.selected = entry;
        this.confirmButton.active = true;
    }

    private void askConfirmation() {
        if (selected == null) return;
        Entry choice = selected;
        Minecraft.getInstance().setScreen(new ConfirmScreen(
                accepted -> {
                    if (accepted) {
                        PacketDistributor.sendToServer(new ModPayloads.ClassSelectionIntent(choice.id()));
                        Minecraft.getInstance().setScreen(null); // server answers with CircuitAssigned
                    } else {
                        Minecraft.getInstance().setScreen(this);
                    }
                },
                Component.translatable("screen.resonantcombat.class_selection.confirm_title"),
                Component.translatable("screen.resonantcombat.class_selection.confirm_message", ModRegistries.className(choice.id()))));
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        super.render(g, mouseX, mouseY, partialTick); // draws background + widgets
        g.drawCenteredString(this.font, this.title, this.width / 2, 15, 0xFFFFFF);

        int panelX = 160;
        int panelWidth = this.width - panelX - 20;
        if (selected == null) {
            g.drawWordWrap(this.font, Component.translatable("screen.resonantcombat.class_selection.select_prompt"),
                    panelX, 50, panelWidth, 0xAAAAAA);
            return;
        }

        g.drawString(this.font, ModRegistries.className(selected.id()), panelX, 45, 0xFFD54F);
        g.drawWordWrap(this.font, ModRegistries.classDesc(selected.id()), panelX, 62, panelWidth, 0xFFFFFF);

        g.drawString(this.font, Component.translatable("screen.resonantcombat.class_selection.starter"), panelX, 110, 0xAAAAAA);
        g.renderItem(new ItemStack(BuiltInRegistries.ITEM.get(selected.def().icon())), panelX + 90, 106);

        g.drawWordWrap(this.font, Component.translatable("screen.resonantcombat.class_selection.warning"),
                panelX, this.height - 70, panelWidth, 0xFF6E6E);
    }

    /** First-join selection cannot be dismissed; a respec (Class Sigil) can be cancelled for free. */
    @Override public boolean shouldCloseOnEsc() { return respec; }
    @Override public boolean isPauseScreen() { return false; }
}
