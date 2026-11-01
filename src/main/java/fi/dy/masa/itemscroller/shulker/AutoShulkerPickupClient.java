package fi.dy.masa.itemscroller.shulker;

import fi.dy.masa.itemscroller.config.Configs;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;

/** Synchronizes the local config toggle to the integrated server on change. */
public final class AutoShulkerPickupClient
{
    private static Boolean lastSent;

    private AutoShulkerPickupClient() {}

    public static void initialize()
    {
        ClientPlayConnectionEvents.JOIN.register((handler, sender, client) -> lastSent = null);
        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> lastSent = null);

        ClientTickEvents.END_CLIENT_TICK.register(client ->
        {
            if (client.player == null || client.level == null ||
                    !ClientPlayNetworking.canSend(AutoShulkerPickupPackets.Toggle.TYPE))
            {
                lastSent = null;
                return;
            }

            boolean enabled = Configs.Generic.AUTO_SHULKER_PICKUP.getBooleanValue();
            if (lastSent == null || lastSent != enabled)
            {
                ClientPlayNetworking.send(new AutoShulkerPickupPackets.Toggle(enabled));
                lastSent = enabled;
            }
        });
    }
}
