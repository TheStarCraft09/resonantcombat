package com.richardnehmer.resonantcombat.common.registry;

import com.richardnehmer.resonantcombat.ResonantCombat;
import com.richardnehmer.resonantcombat.common.data.EchoDropEntry;
import com.richardnehmer.resonantcombat.common.data.WeaponClassEntry;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.Item;
import net.neoforged.neoforge.registries.datamaps.DataMapType;
import net.neoforged.neoforge.registries.datamaps.RegisterDataMapTypesEvent;

public final class ModDataMaps {
    /** data/resonantcombat/data_maps/item/weapon_class.json - supports "#tag" keys, so item tags are covered. */
    public static final DataMapType<Item, WeaponClassEntry> WEAPON_CLASS = DataMapType.builder(
                    ResonantCombat.id("weapon_class"), Registries.ITEM, WeaponClassEntry.CODEC)
            .synced(WeaponClassEntry.CODEC, false)
            .build();

    /** data/resonantcombat/data_maps/entity_type/echo_drop.json - which mobs drop which Echo Imprint. Server-side only. */
    public static final DataMapType<EntityType<?>, EchoDropEntry> ECHO_DROP = DataMapType.builder(
                    ResonantCombat.id("echo_drop"), Registries.ENTITY_TYPE, EchoDropEntry.CODEC)
            .build();

    public static void onRegister(RegisterDataMapTypesEvent event) {
        event.register(WEAPON_CLASS);
        event.register(ECHO_DROP);
    }

    private ModDataMaps() {}
}
