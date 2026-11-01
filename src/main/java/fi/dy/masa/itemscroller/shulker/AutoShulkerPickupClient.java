package fi.dy.masa.itemscroller.shulker;

import fi.dy.masa.itemscroller.config.Configs;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.Minecraft;
import net.minecraft.world.inventory.Slot;

/** Synchronizes client preference; item locks are only updated on the server. */
public final class AutoShulkerPickupClient
{
    private static Boolean lastEnabled;
    private static Boolean lastFixed;

    private AutoShulkerPickupClient() {}

    public static void initialize()
    {
        ClientPlayConnectionEvents.JOIN.register((handler, sender, client) -> clear());
        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> clear());

        ClientTickEvents.END_CLIENT_TICK.register(client ->
        {
            if (client.player == null || client.level == null ||
                    !ClientPlayNetworking.canSend(AutoShulkerPickupPackets.Toggle.TYPE))
            {
                clear();
                return;
            }

            boolean enabled = Configs.Generic.AUTO_SHULKER_PICKUP.getBooleanValue();
            boolean fixed = Configs.Generic.AUTO_SHULKER_PICKUP_MODE.getOptionListValue() ==
                    AutoShulkerStorageMode.FIXED;
            if (lastEnabled == null || lastFixed == null ||
                    lastEnabled != enabled || lastFixed != fixed)
            {
                ClientPlayNetworking.send(new AutoShulkerPickupPackets.Toggle(enabled, fixed));
                lastEnabled = enabled;
                lastFixed = fixed;
            }
        });
    }

    public static boolean toggleLock(Minecraft client, Slot hovered)
    {
        if (client.player == null || hovered == null ||
                hovered.container != client.player.getInventory() ||
                !AutoShulkerLocks.isShulker(hovered.getItem()) ||
                !ClientPlayNetworking.canSend(AutoShulkerPickupPackets.LockBox.TYPE)) return false;

        ClientPlayNetworking.send(new AutoShulkerPickupPackets.LockBox(
                client.player.containerMenu.containerId, hovered.index));
        return true;
    }

    private static void clear()
    {
        lastEnabled = null;
        lastFixed = null;
    }
}
