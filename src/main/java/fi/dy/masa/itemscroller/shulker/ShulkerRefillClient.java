package fi.dy.masa.itemscroller.shulker;

import java.util.ArrayList;
import java.util.List;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.Minecraft;
import net.minecraft.world.item.ItemStack;

import fi.dy.masa.itemscroller.config.Configs;
import fi.dy.masa.itemscroller.recipes.RecipePattern;

/**
 * Fetches ingredient refills before the existing massCraft code sends clicks.
 * Never changes inventory directly on the client.
 */
public final class ShulkerRefillClient
{
    private static boolean pending;
    private static int pendingTicks;
    private static int cooldown;
    private static boolean syncPause;

    private ShulkerRefillClient() {}

    public static void initialize()
    {
        ClientPlayNetworking.registerGlobalReceiver(ShulkerRefillPackets.Result.TYPE, (result, context) ->
        {
            context.client().execute(() ->
            {
                pending = false;
                pendingTicks = 0;
                cooldown = result.moved() > 0 ? 3 : 20;
                syncPause = result.moved() > 0;
            });
        });
    }

    public static boolean pauseMassCraft(Minecraft mc, RecipePattern recipe)
    {
        if (!Configs.Generic.SHULKER_BOX_ITEM_HANDLING.getBooleanValue() || mc.player == null)
        {
            pending = false;
            return false;
        }
        if (!ClientPlayNetworking.canSend(ShulkerRefillPackets.Request.TYPE))
        {
            pending = false;
            return false;
        }
        if (pending)
        {
            if (++pendingTicks > 40)
            {
                pending = false;
                pendingTicks = 0;
                cooldown = 40;
                syncPause = false;
                return false;
            }
            return true;
        }
        if (cooldown > 0)
        {
            cooldown--;
            boolean pause = syncPause;
            if (cooldown == 0) syncPause = false;
            return pause;
        }

        List<ItemStack> needed = new ArrayList<>();
        for (ItemStack input : recipe.getRecipeItems())
        {
            if (input == null || input.isEmpty()) continue;
            if (needed.stream().anyMatch(item -> ItemStack.isSameItemSameComponents(item, input))) continue;
            int amount = 0;
            for (int i = 0; i < 36; i++)
            {
                ItemStack loose = mc.player.getInventory().getItem(i);
                if (ItemStack.isSameItemSameComponents(loose, input)) amount += loose.getCount();
            }
            if (amount < 16) needed.add(input.copyWithCount(1));
            if (needed.size() == 9) break;
        }
        if (needed.isEmpty()) return false;

        pending = true;
        pendingTicks = 0;
        ClientPlayNetworking.send(new ShulkerRefillPackets.Request(needed));
        return true;
    }
}
