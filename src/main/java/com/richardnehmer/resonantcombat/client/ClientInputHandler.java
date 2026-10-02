package com.richardnehmer.resonantcombat.client;

import com.richardnehmer.resonantcombat.ResonantCombat;
import com.richardnehmer.resonantcombat.common.network.ModPayloads;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.Locale;

/**
 * Reads input and sends INTENT payloads (design doc 5.1 ClientInputHandler). It reads raw key STATE (isDown) and never
 * consumes clicks, so it does not steal input from Epic Fight. The server decides everything.
 */
@EventBusSubscriber(modid = ResonantCombat.MOD_ID, value = Dist.CLIENT)
public final class ClientInputHandler {
    private static boolean attackWasDown, dodgeWasDown;
    private static KeyMapping epicFightDodge;
    private static boolean dodgeLookedUp;

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;
        if (player == null || mc.level == null) { reset(); return; }

        boolean active = mc.screen == null && player.isAlive() && !player.isSpectator()
                && ClientProfileCache.get().hasCircuit() && ClientEpicFightBridge.isEpicFightMode();

        boolean attackDown = active && mc.options.keyAttack.isDown();
        boolean dodgeDown = active && isDodgeDown(mc);

        // ---- attack: ground press = start tracking a possible heavy, air press = possible plunge ----
        if (attackDown && !attackWasDown) {
            if (player.onGround()) {
                PacketDistributor.sendToServer(new ModPayloads.AttackHoldStart());
                ClientActionState.holdActive = true;
                ClientActionState.chargeTicks = 0;
            } else {
                PacketDistributor.sendToServer(new ModPayloads.BasicAttackIntent());
            }
        }
        if (ClientActionState.holdActive) {
            if (attackDown) {
                ClientActionState.chargeTicks++;
            } else {
                PacketDistributor.sendToServer(new ModPayloads.AttackRelease());
                ClientActionState.holdActive = false;
                ClientActionState.chargeTicks = 0;
            }
        }

        // ---- double jump: airborne press of the dodge key ----
        if (dodgeDown && !dodgeWasDown && !player.onGround() && !player.getAbilities().flying && !player.isFallFlying()
                && !player.isInWater() && !player.onClimbable() && !player.isPassenger()) {
            PacketDistributor.sendToServer(new ModPayloads.DoubleJumpIntent());
        }

        attackWasDown = attackDown;
        dodgeWasDown = dodgeDown;
    }

    private static boolean isDodgeDown(Minecraft mc) {
        if (!dodgeLookedUp) {
            dodgeLookedUp = true;
            for (KeyMapping key : mc.options.keyMappings) {
                String name = key.getName().toLowerCase(Locale.ROOT);
                if (name.contains("epicfight") && name.contains("dodge")) {
                    epicFightDodge = key;
                    ResonantCombat.LOGGER.info("Double jump bound to Epic Fight key: {}", key.getName());
                    break;
                }
            }
            if (epicFightDodge == null) {
                ResonantCombat.LOGGER.warn("Epic Fight dodge key not found by name; bind 'Double Jump' in Controls instead.");
            }
        }
        return (epicFightDodge != null && epicFightDodge.isDown()) || ClientKeyMappings.DOUBLE_JUMP.isDown();
    }

    private static void reset() {
        attackWasDown = dodgeWasDown = false;
        ClientActionState.holdActive = false;
        ClientActionState.chargeTicks = 0;
    }

    private ClientInputHandler() {}
}
