package com.planetgachi;

import com.planetgachi.command.PriceCommand;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Environment(EnvType.CLIENT)
public class PlanetgachiMod implements ClientModInitializer {

    public static final String MOD_ID = "planetgachi";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    @Override
    public void onInitializeClient() {
        // ✅ 여기 안으로 이동
        com.planetgachi.command.CommandRegistrar.register();

        PriceCommand.register();
        LOGGER.info("[Planetgachi] 모드 로드 완료 (v1.2.0)");
    }
}
