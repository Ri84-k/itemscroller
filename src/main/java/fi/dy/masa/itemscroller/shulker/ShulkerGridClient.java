package fi.dy.masa.itemscroller.shulker;

import java.util.ArrayList;
import java.util.List;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

import fi.dy.masa.itemscroller.config.Configs;
import fi.dy.masa.itemscroller.recipes.CraftingHandler;
import fi.dy.masa.itemscroller.recipes.RecipePattern;
import fi.dy.masa.itemscroller.util.InventoryUtils;

/**
 * Experimental direct-grid path. This does not modify client inventory or
 * simulate recipe placement. The server validates and fills all grid slots.
 */
public final class ShulkerGridClient
{
    private static boolean pending;
    private static int pendingMenu = -1;
    private static int pendingTicks;
    private static int retryDelay;

    private ShulkerGridClient() {}

    public static void initialize()
    {
        ClientPlayNetworking.registerGlobalReceiver(ShulkerGridPackets.Result.TYPE, (result, context) ->
        {
            context.client().execute(() ->
            {
                if (pending && result.menuId() == pendingMenu)
                {
                    pending = false;
                    pendingTicks = 0;
                    // Acknowledgment is sent after grid/result slot broadcasts.
                    retryDelay = result.ready() ? 0 : 12;
                }
            });
        });
    }

    public static boolean handleMassCraft(Minecraft mc,
                                          AbstractContainerScreen<?> gui,
                                          CraftingHandler.SlotRange range,
                                          Slot outputSlot,
                                          RecipePattern recipe)
    {
        if (!Configs.Generic.SHULKER_BOX_ITEM_HANDLING.getBooleanValue() ||
            !Configs.Generic.DIRECT_SHULKER_CRAFTING.getBooleanValue() ||
            mc.player == null || !ClientPlayNetworking.canSend(ShulkerGridPackets.Request.TYPE))
        {
            pending = false;
            return false; // fall back to stable v2 behavior
        }

        final int count = range.getSlotCount();
        final ItemStack[] pattern = recipe.getRecipeItems();
        if ((count != 4 && count != 9) || pattern.length != count ||
            recipe.getResult().isEmpty())
        {
            return true; // unsupported grid; never use unsafe fallback
        }

        if (pending && pendingMenu != gui.getMenu().containerId)
        {
            pending = false;
        }

        boolean allSlotsPopulated = true;
        for (int i = 0; i < count; i++)
        {
            ItemStack expected = pattern[i];
            ItemStack actual = gui.getMenu().getSlot(range.getFirst() + i).getItem();

            if (expected.isEmpty())
            {
                if (!actual.isEmpty()) return true; // no overwriting unrelated items
            }
            else if (actual.isEmpty())
            {
                allSlotsPopulated = false;
            }
            else if (!ItemStack.isSameItemSameComponents(actual, expected))
            {
                return true; // preserve unrelated or different-component contents
            }
        }

        if (pending)
        {
            if (++pendingTicks <= 40) return true;
            pending = false;
            pendingTicks = 0;
            retryDelay = 20;
        }

        // Only take the selected recipe's exact output, never a partial recipe
        // such as wooden slabs while preparing a piston pattern.
        ItemStack output = outputSlot.getItem();
        if (allSlotsPopulated && !output.isEmpty() &&
            ItemStack.isSameItemSameComponents(output, recipe.getResult()) &&
            output.getCount() == recipe.getResult().getCount())
        {
            // THROW is handled by the vanilla crafting result slot on the
            // server, so no free ordinary inventory slot is required.
            InventoryUtils.dropStack(gui, outputSlot.index);
            return true;
        }

        if (retryDelay > 0)
        {
            retryDelay--;
            return true;
        }

        List<ItemStack> items = new ArrayList<>(count);
        for (ItemStack ingredient : pattern)
        {
            items.add(ingredient.isEmpty() ? ItemStack.EMPTY : ingredient.copyWithCount(1));
        }

        pending = true;
        pendingMenu = gui.getMenu().containerId;
        pendingTicks = 0;
        ClientPlayNetworking.send(new ShulkerGridPackets.Request(
                pendingMenu, List.copyOf(items), recipe.getResult().copy()));
        return true;
    }
}
