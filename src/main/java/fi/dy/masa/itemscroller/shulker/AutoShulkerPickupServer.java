package fi.dy.masa.itemscroller.shulker;

import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

/** Each player's mode is synchronized; locks are persisted on actual shulkers. */
public final class AutoShulkerPickupServer
{
    private static final Set<UUID> ENABLED_PLAYERS = ConcurrentHashMap.newKeySet();
    private static final Set<UUID> FIXED_PLAYERS = ConcurrentHashMap.newKeySet();

    private AutoShulkerPickupServer() {}

    public static void register()
    {
        PayloadTypeRegistry.serverboundPlay().register(
                AutoShulkerPickupPackets.Toggle.TYPE, AutoShulkerPickupPackets.Toggle.CODEC);
        PayloadTypeRegistry.serverboundPlay().register(
                AutoShulkerPickupPackets.LockBox.TYPE, AutoShulkerPickupPackets.LockBox.CODEC);

        ServerPlayNetworking.registerGlobalReceiver(AutoShulkerPickupPackets.Toggle.TYPE, (request, context) ->
        {
            ServerPlayer player = context.player();
            if (player.getClass() != ServerPlayer.class) return;
            if (request.enabled()) ENABLED_PLAYERS.add(player.getUUID());
            else ENABLED_PLAYERS.remove(player.getUUID());
            if (request.fixedMode()) FIXED_PLAYERS.add(player.getUUID());
            else FIXED_PLAYERS.remove(player.getUUID());
        });

        ServerPlayNetworking.registerGlobalReceiver(AutoShulkerPickupPackets.LockBox.TYPE, (request, context) ->
        {
            ServerPlayer player = context.player();
            if (player.getClass() != ServerPlayer.class ||
                    request.menuId() != player.containerMenu.containerId ||
                    request.slotId() < 0 ||
                    request.slotId() >= player.containerMenu.slots.size()) return;

            Slot slot = player.containerMenu.getSlot(request.slotId());
            if (slot.container != player.getInventory()) return;
            ItemStack current = slot.getItem();
            if (!AutoShulkerLocks.isShulker(current)) return;

            ItemStack updated = current.copy();
            if (AutoShulkerLocks.isLocked(updated))
            {
                AutoShulkerLocks.setLock(updated, false, "");
                player.sendSystemMessage(Component.literal("Shulker unlocked: Overflow allowed"), true);
            }
            else
            {
                String type = AutoShulkerLocks.singleType(updated);
                if (type == null)
                {
                    player.sendSystemMessage(
                            Component.literal("Cannot lock mixed-item shulker; empty it or sort it first"), true);
                    return;
                }
                AutoShulkerLocks.setLock(updated, true, type);
                player.sendSystemMessage(Component.literal(type.isEmpty() ?
                        "Shulker locked: first picked-up item will assign its type" :
                        "Shulker locked to " + type), true);
            }

            slot.set(updated);
            player.getInventory().setChanged();
            player.containerMenu.broadcastChanges();
            player.inventoryMenu.broadcastChanges();
        });

        ServerPlayConnectionEvents.DISCONNECT.register((handler, server) ->
        {
            ENABLED_PLAYERS.remove(handler.getPlayer().getUUID());
            FIXED_PLAYERS.remove(handler.getPlayer().getUUID());
        });
    }

    public static boolean isEnabled(ServerPlayer player)
    {
        return player.getClass() == ServerPlayer.class &&
                ENABLED_PLAYERS.contains(player.getUUID());
    }

    public static boolean isFixedMode(ServerPlayer player)
    {
        return player.getClass() == ServerPlayer.class &&
                FIXED_PLAYERS.contains(player.getUUID());
    }
}
