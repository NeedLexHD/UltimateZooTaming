package com.lex3d.ultimatezootaming.capability;

import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.CapabilityManager;
import net.minecraftforge.common.capabilities.CapabilityToken;

public class CapabilityHandler {
    public static final Capability<TamingData> TAMING_DATA = CapabilityManager.get(new CapabilityToken<>() {});
}
