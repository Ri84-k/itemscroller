package fi.dy.masa.itemscroller;

import net.fabricmc.api.ClientModInitializer;
import fi.dy.masa.itemscroller.shulker.ShulkerRefillClient;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import fi.dy.masa.malilib.event.InitializationHandler;
import fi.dy.masa.itemscroller.config.Configs;

public class ItemScroller implements ClientModInitializer
{
    public static final Logger LOGGER = LogManager.getLogger(Reference.MOD_ID);

    @Override
    public void onInitializeClient()
    {
        InitializationHandler.getInstance().registerInitializationHandler(new InitHandler());
        ShulkerRefillClient.initialize();
    }

    public static void debugLog(String key, Object... args)
    {
        if (Configs.Generic.DEBUG_MESSAGES.getBooleanValue())
        {
            LOGGER.info(key, args);
        }
    }
}
