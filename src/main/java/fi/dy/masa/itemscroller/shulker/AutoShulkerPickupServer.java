package fi.dy.masa.itemscroller.shulker;

import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.server.level.ServerPlayer;

/** Per-player opt-in, controlled by the owner's client configuration. */
public final class AutoShulkerPickupServer
{
    private static final Set<UUID> ENABLED_PLAYERS = ConcurrentHashMap.newKeySet();

    private AutoShulkerPickupServer() {}

    public static void register()
    {
        PayloadTypeRegistry.serverboundPlay().register(
                AutoShulkerPickupPackets.Toggle.TYPE, AutoShulkerPickupPackets.Toggle.CODEC);

        ServerPlayNetworking.registerGlobalReceiver(AutoShulkerPickupPackets.Toggle.TYPE, (request, context) ->
        {
            ServerPlayer player = context.player();
            // Carpet/GCA fake players subclass ServerPlayer. Never opt them in.
            if (player.getClass() != ServerPlayer.class) return;
            if (request.enabled()) ENABLED_PLAYERS.add(player.getUUID());
            else ENABLED_PLAYERS.remove(player.getUUID());
        });

        ServerPlayConnectionEvents.DISCONNECT.register((handler, server) ->
                ENABLED_PLAYERS.remove(handler.getPlayer().getUUID()));
    }

    public static boolean isEnabled(ServerPlayer player)
    {
        // Fail closed for any non-vanilla player subclass, including fake players.
        return player.getClass() == ServerPlayer.class &&
                ENABLED_PLAYERS.contains(player.getUUID());
    }
}
