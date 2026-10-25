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

    private ShulkerRefillClient() {}

    public static void initialize()
    {
        ClientPlayNetworking.registerGlobalReceiver(ShulkerRefillPackets.Result.TYPE, (result, context) ->
        {
            context.client().execute(() ->
            {
                pending = false;
                pendingTicks = 0;
                // The server broadcasts changed slots before sending this
                // acknowledgment. No extra post-refill pause is necessary.
                cooldown = result.moved() > 0 ? 0 : 10;
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
                cooldown = 20;
                return false;
            }
            return true;
        }
        if (cooldown > 0)
        {
            cooldown--;
            // Throttle requests only when no shulker materials were found.
            return false;
        }

        List<ItemStack> needed = new ArrayList<>();
        for (ItemStack input : recipe.getRecipeItems())
        {
            if (input == null || input.isEmpty()) continue;
            if (needed.stream().anyMatch(item -> ItemStack.isSameItemSameComponents(item, input))) continue;

            int perCraft = 0;
            for (ItemStack ingredient : recipe.getRecipeItems())
            {
                if (ingredient != null && !ingredient.isEmpty() &&
                        ItemStack.isSameItemSameComponents(ingredient, input))
                {
                    perCraft++;
                }
            }

            int amount = 0;
            for (int i = 0; i < 36; i++)
            {
                ItemStack loose = mc.player.getInventory().getItem(i);
                if (ItemStack.isSameItemSameComponents(loose, input)) amount += loose.getCount();
            }
            // Keep at least four recipes' worth loose, and request enough for
            // larger batches, rather than refilling each ingredient 1 stack at a time.
            if (amount < Math.max(8, perCraft * 4))
            {
                needed.add(input.copyWithCount(perCraft));
            }
            if (needed.size() == 9) break;
        }
        if (needed.isEmpty()) return false;

        pending = true;
        pendingTicks = 0;
        ClientPlayNetworking.send(new ShulkerRefillPackets.Request(needed));
        return true;
    }
}
