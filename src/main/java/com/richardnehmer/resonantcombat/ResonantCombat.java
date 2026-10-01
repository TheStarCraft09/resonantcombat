package com.richardnehmer.resonantcombat;

import com.mojang.logging.LogUtils;
import com.richardnehmer.resonantcombat.common.attachment.ModAttachments;
import com.richardnehmer.resonantcombat.common.network.ModNetwork;
import com.richardnehmer.resonantcombat.common.registry.ModDataMaps;
import com.richardnehmer.resonantcombat.common.registry.ModRegistries;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import org.slf4j.Logger;

@Mod(ResonantCombat.MOD_ID)
public class ResonantCombat {
    public static final String MOD_ID = "resonantcombat";
    public static final Logger LOGGER = LogUtils.getLogger();

    public ResonantCombat(IEventBus modBus) {
        ModAttachments.ATTACHMENTS.register(modBus);
        modBus.addListener(ModRegistries::onNewDatapackRegistries);
        modBus.addListener(ModDataMaps::onRegister);
        modBus.addListener(ModNetwork::onRegisterPayloads);
    }

    public static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath(MOD_ID, path);
    }
}
