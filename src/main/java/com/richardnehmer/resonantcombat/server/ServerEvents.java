package com.richardnehmer.resonantcombat.server;

import com.richardnehmer.resonantcombat.ResonantCombat;
import com.richardnehmer.resonantcombat.common.combat.CombatTicker;
import com.richardnehmer.resonantcombat.common.combat.HeavyAttackController;
import com.richardnehmer.resonantcombat.common.combat.PlungeController;
import com.richardnehmer.resonantcombat.common.combat.PostureController;
import com.richardnehmer.resonantcombat.common.combat.ResourceController;
import com.richardnehmer.resonantcombat.common.network.ModNetwork;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import net.neoforged.neoforge.event.entity.living.LivingFallEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.EntityTickEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

@EventBusSubscriber(modid = ResonantCombat.MOD_ID)
public final class ServerEvents {

    @SubscribeEvent
    public static void onLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) OnboardingService.onJoin(player);
    }

    @SubscribeEvent
    public static void onRespawn(PlayerEvent.PlayerRespawnEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) ModNetwork.syncProfile(player);
    }

    @SubscribeEvent
    public static void onDimensionChange(PlayerEvent.PlayerChangedDimensionEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) ModNetwork.syncProfile(player);
    }

    @SubscribeEvent
    public static void onCommands(RegisterCommandsEvent event) {
        ModCommands.register(event.getDispatcher());
    }

    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        if (event.getEntity() instanceof ServerPlayer player) CombatTicker.tick(player);
    }

    @SubscribeEvent
    public static void onEntityTick(EntityTickEvent.Post event) {
        if (event.getEntity() instanceof LivingEntity living && !living.level().isClientSide()) PostureController.tick(living);
    }

    @SubscribeEvent
    public static void onIncomingDamage(LivingIncomingDamageEvent event) {
        PostureController.onIncomingDamage(event);
    }

    @SubscribeEvent
    public static void onDamage(LivingDamageEvent.Post event) {
        if (event.getEntity() instanceof ServerPlayer victim) HeavyAttackController.onPlayerHurt(victim, event.getNewDamage());
        ResourceController.onHit(event);
    }

    @SubscribeEvent
    public static void onFall(LivingFallEvent event) {
        PlungeController.onFall(event);
    }

    private ServerEvents() {}
}
