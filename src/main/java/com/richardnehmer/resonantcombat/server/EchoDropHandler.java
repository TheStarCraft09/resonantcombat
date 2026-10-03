package com.richardnehmer.resonantcombat.server;

import com.richardnehmer.resonantcombat.Config;
import com.richardnehmer.resonantcombat.ResonantCombat;
import com.richardnehmer.resonantcombat.common.data.EchoDropEntry;
import com.richardnehmer.resonantcombat.common.item.ModItems;
import com.richardnehmer.resonantcombat.common.registry.ModDataMaps;
import com.richardnehmer.resonantcombat.common.registry.ModRegistries;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingDropsEvent;

/** Echo acquisition by mob drops (design doc 3.7). Drop table: data/resonantcombat/data_maps/entity_type/echo_drop.json. */
@EventBusSubscriber(modid = ResonantCombat.MOD_ID)
public final class EchoDropHandler {

    @SubscribeEvent
    public static void onDrops(LivingDropsEvent event) {
        if (!(event.getSource().getEntity() instanceof ServerPlayer)) return;
        LivingEntity dead = event.getEntity();
        EchoDropEntry entry = BuiltInRegistries.ENTITY_TYPE.wrapAsHolder(dead.getType()).getData(ModDataMaps.ECHO_DROP);
        if (entry == null) return;
        if (dead.getRandom().nextDouble() >= entry.chance() * Config.ECHO_DROP_MULTIPLIER.get()) return;
        if (!ModRegistries.abilities(dead.level().registryAccess()).containsKey(entry.echo())) return; // datapack typo guard

        event.getDrops().add(new ItemEntity(dead.level(), dead.getX(), dead.getY(), dead.getZ(), ModItems.imprint(entry.echo())));
    }

    private EchoDropHandler() {}
}
