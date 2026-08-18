package com.anthonyahellman.silenttinkers;

import com.mojang.logging.LogUtils;
import net.minecraftforge.fml.common.Mod;
import org.slf4j.Logger;

@Mod(SilentTinkersMod.MOD_ID)
public final class SilentTinkersMod {
    public static final String MOD_ID = "silenttinkers";
    public static final Logger LOGGER = LogUtils.getLogger();

    public SilentTinkersMod() {
        LOGGER.info("Silent Tinkers compatibility foundation loaded");
    }
}
