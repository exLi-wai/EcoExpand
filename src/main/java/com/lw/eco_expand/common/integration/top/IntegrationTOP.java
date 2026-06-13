package com.lw.eco_expand.common.integration.top;

import mcjty.theoneprobe.TheOneProbe;

public final class IntegrationTOP {
    public static void registerProvider() {
        TheOneProbe.theOneProbeImp.registerProvider(EStorageInfoProvider.INSTANCE);
    }
}
