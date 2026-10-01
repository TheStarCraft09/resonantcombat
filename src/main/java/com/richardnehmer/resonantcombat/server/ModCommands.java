package com.richardnehmer.resonantcombat.server;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.FloatArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import com.richardnehmer.resonantcombat.common.attachment.ModAttachments;
import com.richardnehmer.resonantcombat.common.attachment.PlayerProfile;
import com.richardnehmer.resonantcombat.common.data.CombatCircuitDefinition;
import com.richardnehmer.resonantcombat.common.network.ModNetwork;
import com.richardnehmer.resonantcombat.common.network.ModPayloads;
import com.richardnehmer.resonantcombat.common.registry.ModRegistries;
import com.richardnehmer.resonantcombat.common.registry.WeaponClassResolver;
import com.richardnehmer.resonantcombat.integration.epicfight.EpicFightBridge;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.commands.arguments.ResourceLocationArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.Optional;
import java.util.Set;

/** Design doc 5.6 (minus echo give, which needs the Echo system). All subcommands require permission level 2. */
public final class ModCommands {
    private static final SuggestionProvider<CommandSourceStack> CLASSES = (ctx, builder) -> SharedSuggestionProvider.suggestResource(
            ModRegistries.weaponClasses(ctx.getSource().registryAccess()).keySet(), builder);
    private static final SuggestionProvider<CommandSourceStack> CIRCUITS = (ctx, builder) -> SharedSuggestionProvider.suggestResource(
            ModRegistries.circuits(ctx.getSource().registryAccess()).keySet(), builder);

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("resonantcombat")
                .requires(src -> src.hasPermission(2))
                .then(Commands.literal("class")
                        .then(Commands.literal("get")
                                .then(Commands.argument("player", EntityArgument.player()).executes(ModCommands::classGet)))
                        .then(Commands.literal("set")
                                .then(Commands.argument("player", EntityArgument.player())
                                        .then(Commands.argument("class", ResourceLocationArgument.id()).suggests(CLASSES)
                                                .executes(ModCommands::classSet)))))
                .then(Commands.literal("circuit")
                        .then(Commands.literal("get")
                                .then(Commands.argument("player", EntityArgument.player()).executes(ModCommands::circuitGet)))
                        .then(Commands.literal("set")
                                .then(Commands.argument("player", EntityArgument.player())
                                        .then(Commands.argument("circuit", ResourceLocationArgument.id()).suggests(CIRCUITS)
                                                .executes(ModCommands::circuitSet))))
                        .then(Commands.literal("reroll")
                                .then(Commands.argument("player", EntityArgument.player()).executes(ModCommands::circuitReroll)))
                        .then(Commands.literal("reset")
                                .then(Commands.argument("player", EntityArgument.player()).executes(ModCommands::circuitReset))))
                .then(Commands.literal("resonance").then(Commands.literal("set")
                        .then(Commands.argument("player", EntityArgument.player())
                                .then(Commands.argument("amount", FloatArgumentType.floatArg(0, 100))
                                        .executes(ctx -> setResource(ctx, true))))))
                .then(Commands.literal("liberation").then(Commands.literal("set")
                        .then(Commands.argument("player", EntityArgument.player())
                                .then(Commands.argument("amount", FloatArgumentType.floatArg(0, 100))
                                        .executes(ctx -> setResource(ctx, false))))))
                .then(Commands.literal("debug").then(Commands.literal("state")
                        .then(Commands.argument("player", EntityArgument.player()).executes(ModCommands::debugState)))));
    }

    private static int classGet(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        ServerPlayer p = EntityArgument.getPlayer(ctx, "player");
        String value = p.getData(ModAttachments.PROFILE).selectedClass().map(ResourceLocation::toString).orElse("<none>");
        ctx.getSource().sendSuccess(() -> Component.literal(p.getGameProfile().getName() + " class: " + value), false);
        return 1;
    }

    /** Admin assignment: sets class, rolls a fresh Circuit for it, no starter kit. */
    private static int classSet(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        ServerPlayer p = EntityArgument.getPlayer(ctx, "player");
        ResourceLocation classId = ResourceLocationArgument.getId(ctx, "class");
        if (ModRegistries.weaponClasses(p.level().registryAccess()).get(classId) == null) return fail(ctx, "Unknown class " + classId);
        long seed = p.getRandom().nextLong();
        Optional<ResourceLocation> circuit = CircuitAssignmentService.roll(p.level().registryAccess(), classId, seed, Set.of());
        if (circuit.isEmpty()) return fail(ctx, "Class " + classId + " has no Circuits");
        CircuitAssignmentService.assign(p, classId, circuit.get(), seed);
        p.getData(ModAttachments.PROFILE).setOnboardingComplete(true);
        ModNetwork.syncProfile(p);
        ctx.getSource().sendSuccess(() -> Component.literal("Set " + p.getGameProfile().getName() + " to " + classId + " / " + circuit.get()), true);
        return 1;
    }

    private static int circuitGet(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        ServerPlayer p = EntityArgument.getPlayer(ctx, "player");
        String value = p.getData(ModAttachments.PROFILE).assignedCircuit().map(ResourceLocation::toString).orElse("<none>");
        ctx.getSource().sendSuccess(() -> Component.literal(p.getGameProfile().getName() + " circuit: " + value), false);
        return 1;
    }

    private static int circuitSet(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        ServerPlayer p = EntityArgument.getPlayer(ctx, "player");
        ResourceLocation circuitId = ResourceLocationArgument.getId(ctx, "circuit");
        CombatCircuitDefinition def = ModRegistries.circuits(p.level().registryAccess()).get(circuitId);
        if (def == null) return fail(ctx, "Unknown circuit " + circuitId);
        CircuitAssignmentService.assign(p, def.weaponClass(), circuitId, 0L);
        p.getData(ModAttachments.PROFILE).setOnboardingComplete(true);
        ModNetwork.syncProfile(p);
        ctx.getSource().sendSuccess(() -> Component.literal("Assigned " + circuitId + " (class " + def.weaponClass() + ")"), true);
        return 1;
    }

    private static int circuitReroll(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        ServerPlayer p = EntityArgument.getPlayer(ctx, "player");
        if (!CircuitAssignmentService.reroll(p)) return fail(ctx, "Player has no class, or the class has no Circuits");
        String now = p.getData(ModAttachments.PROFILE).assignedCircuit().map(ResourceLocation::toString).orElse("?");
        PacketDistributor.sendToPlayer(p, new ModPayloads.CircuitAssigned(p.getData(ModAttachments.PROFILE).assignedCircuit().orElseThrow()));
        ctx.getSource().sendSuccess(() -> Component.literal("Rerolled to " + now), true);
        return 1;
    }

    /** Development reset: reopens onboarding. starterKitGranted is kept so the kit is never duplicated. */
    private static int circuitReset(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        ServerPlayer p = EntityArgument.getPlayer(ctx, "player");
        PlayerProfile profile = p.getData(ModAttachments.PROFILE);
        profile.clearClassAndCircuit();
        profile.setOnboardingComplete(false);
        ModNetwork.syncProfile(p);
        PacketDistributor.sendToPlayer(p, new ModPayloads.OpenClassSelection());
        ctx.getSource().sendSuccess(() -> Component.literal("Reset " + p.getGameProfile().getName() + "; onboarding reopened"), true);
        return 1;
    }

    private static int setResource(CommandContext<CommandSourceStack> ctx, boolean resonance) throws CommandSyntaxException {
        ServerPlayer p = EntityArgument.getPlayer(ctx, "player");
        float amount = FloatArgumentType.getFloat(ctx, "amount");
        PlayerProfile profile = p.getData(ModAttachments.PROFILE);
        if (resonance) profile.setResonance(amount); else profile.setLiberationEnergy(amount);
        ModNetwork.syncProfile(p);
        ctx.getSource().sendSuccess(() -> Component.literal((resonance ? "Resonance" : "Liberation") + " = " + amount), true);
        return 1;
    }

    /** Phase 0 spike output: shows whether the Epic Fight bridge works and how the held item resolves. */
    private static int debugState(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        ServerPlayer p = EntityArgument.getPlayer(ctx, "player");
        PlayerProfile profile = p.getData(ModAttachments.PROFILE);
        ItemStack held = p.getMainHandItem();
        String report = String.join("\n",
                "== " + p.getGameProfile().getName() + " ==",
                "class: " + profile.selectedClass().map(ResourceLocation::toString).orElse("<none>"),
                "circuit: " + profile.assignedCircuit().map(ResourceLocation::toString).orElse("<none>"),
                "resonance/liberation: " + profile.resonance() + " / " + profile.liberationEnergy(),
                "onboarding/starterKit: " + profile.isOnboardingComplete() + " / " + profile.isStarterKitGranted(),
                "EF battle mode: " + EpicFightBridge.isBattleMode(p),
                "held item: " + held.getItem() + " | EF category: " + EpicFightBridge.weaponCategory(held).orElse("<unknown>"),
                "resolved class: " + WeaponClassResolver.resolve(p.level().registryAccess(), held).map(ResourceLocation::toString).orElse("<unsupported>"),
                "circuit active: " + WeaponClassResolver.isCircuitActive(p));
        ctx.getSource().sendSuccess(() -> Component.literal(report), false);
        return 1;
    }

    private static int fail(CommandContext<CommandSourceStack> ctx, String message) {
        ctx.getSource().sendFailure(Component.literal(message));
        return 0;
    }

    private ModCommands() {}
}
