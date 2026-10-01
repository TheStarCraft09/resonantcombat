package com.richardnehmer.resonantcombat.server;

import com.richardnehmer.resonantcombat.ResonantCombat;
import com.richardnehmer.resonantcombat.common.data.StarterEntry;
import com.richardnehmer.resonantcombat.common.data.WeaponClassDefinition;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import java.util.Optional;

public final class StarterKitService {

    /** Caller must check/set the starterKitGranted flag - this method only hands out items. */
    public static void grant(ServerPlayer player, WeaponClassDefinition def) {
        for (StarterEntry entry : def.starterKit()) {
            Optional<Item> item = entry.items().stream()
                    .map(BuiltInRegistries.ITEM::getOptional)
                    .flatMap(Optional::stream)
                    .findFirst();
            if (item.isEmpty()) {
                ResonantCombat.LOGGER.warn("Starter kit entry has no valid item: {}", entry.items().stream().map(ResourceLocation::toString).toList());
                continue;
            }
            int remaining = entry.count();
            while (remaining > 0) {
                ItemStack stack = new ItemStack(item.get(), Math.min(remaining, item.get().getDefaultMaxStackSize()));
                remaining -= stack.getCount();
                if (!player.getInventory().add(stack)) player.drop(stack, false);
            }
        }
    }

    private StarterKitService() {}
}
