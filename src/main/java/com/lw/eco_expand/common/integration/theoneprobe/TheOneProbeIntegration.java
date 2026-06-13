package com.lw.eco_expand.common.integration.theoneprobe;

import mcjty.theoneprobe.TheOneProbe;

public final class TheOneProbeIntegration {

    public static void registerProvider() {
        TheOneProbe.theOneProbeImp.registerProvider(EStorageInfoProvider.INSTANCE);
    }
}
