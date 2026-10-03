package com.richardnehmer.resonantcombat.common.item;

import com.richardnehmer.resonantcombat.common.attachment.ModAttachments;
import com.richardnehmer.resonantcombat.common.attachment.PlayerProfile;
import com.richardnehmer.resonantcombat.common.data.AbilityDefinition;
import com.richardnehmer.resonantcombat.common.data.AbilityKind;
import com.richardnehmer.resonantcombat.common.network.ModNetwork;
import com.richardnehmer.resonantcombat.common.registry.ModRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

import java.util.List;

/** Right-click: equip the Echo stored in the stack. A previously equipped Echo is handed back as its own imprint. */
public class EchoImprintItem extends Item {
    public EchoImprintItem(Properties properties) { super(properties); }

    @Override
    public Component getName(ItemStack stack) {
        ResourceLocation id = stack.get(ModItems.ECHO.get());
        return id == null ? super.getName(stack)
                : Component.translatable("item.resonantcombat.echo_imprint.named", ModRegistries.abilityName(id));
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (level.isClientSide || !(player instanceof ServerPlayer sp)) return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);

        ResourceLocation id = stack.get(ModItems.ECHO.get());
        AbilityDefinition def = id == null ? null : ModRegistries.abilities(level.registryAccess()).get(id);
        PlayerProfile profile = sp.getData(ModAttachments.PROFILE);
        if (def == null || def.kind() != AbilityKind.ECHO || !profile.hasCircuit()) {
            sp.displayClientMessage(Component.translatable("message.resonantcombat.echo.invalid"), true);
            return InteractionResultHolder.fail(stack);
        }
        ResourceLocation previous = profile.equippedEcho().orElse(null);
        if (id.equals(previous)) {
            sp.displayClientMessage(Component.translatable("message.resonantcombat.echo.already"), true);
            return InteractionResultHolder.fail(stack);
        }

        // keep charges the player already earned for this Echo; first time = full charges
        int charges = profile.echoCharges().containsKey(id) ? profile.echoCharge(id) : def.maxCharges();
        profile.equipEcho(id, charges);
        if (!sp.getAbilities().instabuild) stack.shrink(1);
        if (previous != null) {
            ItemStack back = ModItems.imprint(previous);
            if (!sp.getInventory().add(back)) sp.drop(back, false);
        }

        ModNetwork.syncProfile(sp);
        sp.displayClientMessage(Component.translatable("message.resonantcombat.echo.equipped", ModRegistries.abilityName(id)), true);
        level.playSound(null, sp.getX(), sp.getY(), sp.getZ(), SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.PLAYERS, 1.0F, 1.2F);
        return InteractionResultHolder.consume(stack);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("item.resonantcombat.echo_imprint.desc"));
    }
}
