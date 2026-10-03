package com.richardnehmer.resonantcombat.common.item;

import com.richardnehmer.resonantcombat.server.RespecService;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

import java.util.List;

/**
 * Right-click: opens class selection in respec mode (design doc 5.7 "Class Respec"). The sigil is only consumed when the
 * server accepts the new class, so cancelling the screen costs nothing.
 */
public class ClassSigilItem extends Item {
    public ClassSigilItem(Properties properties) { super(properties); }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (level.isClientSide || !(player instanceof ServerPlayer sp)) return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);

        RespecService.Result result = RespecService.beginClassRespec(sp);
        if (result != RespecService.Result.OK) {
            RespecService.notify(sp, result);
            return InteractionResultHolder.fail(stack);
        }
        return InteractionResultHolder.success(stack);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("item.resonantcombat.class_sigil.desc"));
    }
}
