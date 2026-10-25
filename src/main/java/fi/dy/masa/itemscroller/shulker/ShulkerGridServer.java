package fi.dy.masa.itemscroller.shulker;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.core.NonNullList;
import net.minecraft.core.component.DataComponents;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.CraftingMenu;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemContainerContents;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.block.ShulkerBoxBlock;

/**
 * Atomically assembles a complete verified crafting grid from carried shulkers.
 * Source items are only removed if the entire grid can be made craftable.
 */
public final class ShulkerGridServer
{
    private ShulkerGridServer() {}

    public static void register()
    {
        PayloadTypeRegistry.serverboundPlay().register(
                ShulkerGridPackets.Request.TYPE, ShulkerGridPackets.Request.CODEC);
        PayloadTypeRegistry.clientboundPlay().register(
                ShulkerGridPackets.Result.TYPE, ShulkerGridPackets.Result.CODEC);
        ServerPlayNetworking.registerGlobalReceiver(ShulkerGridPackets.Request.TYPE, (request, context) ->
        {
            boolean ready = false;
            int moved = 0;
            if (context.player().containerMenu.containerId == request.menuId())
            {
                moved = fill(context.player(), request);
                ready = moved >= 0;
                if (moved < 0) moved = 0;
            }
            // Inventory updates are broadcast before the acknowledgment.
            context.player().getInventory().setChanged();
            context.player().containerMenu.broadcastChanges();
            context.player().inventoryMenu.broadcastChanges();
            ServerPlayNetworking.send(context.player(),
                    new ShulkerGridPackets.Result(request.menuId(), moved, ready));
        });
    }

    /**
     * Returns number of items moved (>=0) on successful validation, or -1
     * if unable to assemble even one complete recipe without changing state.
     */
    private static int fill(ServerPlayer player, ShulkerGridPackets.Request request)
    {
        AbstractContainerMenu menu = player.containerMenu;
        final int width;
        final int height;
        if (menu instanceof CraftingMenu)
        {
            width = 3;
            height = 3;
        }
        else if (menu instanceof InventoryMenu)
        {
            width = 2;
            height = 2;
        }
        else
        {
            return -1;
        }

        final int size = width * height;
        final List<ItemStack> pattern = request.pattern();
        if (pattern.size() != size || request.expectedOutput().isEmpty()) return -1;
        if (menu.slots.size() < 1 + size) return -1;

        // Never trust a client-provided output or ingredient choice without
        // verifying the complete pattern against the server's recipe registry.
        ServerLevel level = player.serverLevel();
        List<ItemStack> normalized = new ArrayList<>(size);
        boolean hasIngredient = false;
        for (ItemStack item : pattern)
        {
            if (item.getCount() > 1) return -1;
            normalized.add(item.isEmpty() ? ItemStack.EMPTY : item.copyWithCount(1));
            if (!item.isEmpty()) hasIngredient = true;
        }
        if (!hasIngredient) return -1;

        CraftingInput input = CraftingInput.of(width, height, normalized);
        Optional<RecipeHolder<CraftingRecipe>> found =
                level.recipeAccess().getRecipeFor(RecipeType.CRAFTING, input, level);
        if (found.isEmpty()) return -1;
        ItemStack actualOutput = found.get().value().assemble(input);
        if (actualOutput.isEmpty() ||
            !ItemStack.isSameItemSameComponents(actualOutput, request.expectedOutput()) ||
            actualOutput.getCount() != request.expectedOutput().getCount())
        {
            return -1;
        }

        int[] counts = new int[size];
        int maxTarget = 64;
        for (int i = 0; i < size; i++)
        {
            Slot grid = menu.getSlot(i + 1);
            ItemStack wanted = normalized.get(i);
            ItemStack present = grid.getItem();
            if (wanted.isEmpty())
            {
                if (!present.isEmpty()) return -1; // Never erase unrelated items
            }
            else
            {
                if (!present.isEmpty() && !ItemStack.isSameItemSameComponents(present, wanted))
                {
                    return -1;
                }
                counts[i] = present.getCount();
                maxTarget = Math.min(maxTarget, Math.min(grid.getMaxStackSize(wanted), wanted.getMaxStackSize()));
            }
        }

        // Work entirely on copies until the whole recipe can be filled.
        List<BoxContents> boxes = new ArrayList<>();
        for (int index = 0; index < player.getInventory().getContainerSize(); index++)
        {
            if (index >= 36 && index != 40) continue; // exclude armor
            ItemStack shulker = player.getInventory().getItem(index);
            if (shulker.getCount() != 1 ||
                !(shulker.getItem() instanceof BlockItem block) ||
                !(block.getBlock() instanceof ShulkerBoxBlock)) continue;
            ItemContainerContents contents = shulker.get(DataComponents.CONTAINER);
            if (contents == null) continue;
            NonNullList<ItemStack> slots = NonNullList.withSize(27, ItemStack.EMPTY);
            contents.copyInto(slots);
            boxes.add(new BoxContents(index, shulker.copy(), slots));
        }

        int target = 0;
        for (int candidate = maxTarget; candidate >= 1; candidate--)
        {
            if (hasSupplies(candidate, normalized, counts, boxes))
            {
                target = candidate;
                break;
            }
        }
        if (target == 0) return -1; // Cannot craft even once; change nothing.

        ItemStack[] planned = new ItemStack[size];
        int moved = 0;

        for (int i = 0; i < size; i++)
        {
            ItemStack wanted = normalized.get(i);
            if (wanted.isEmpty()) continue;
            int missing = Math.max(0, target - counts[i]);
            if (missing == 0) continue;

            for (BoxContents box : boxes)
            {
                for (int slot = 0; slot < box.slots.size() && missing > 0; slot++)
                {
                    ItemStack stored = box.slots.get(slot);
                    if (stored.isEmpty() || !ItemStack.isSameItemSameComponents(stored, wanted))
                    {
                        continue;
                    }

                    int used = Math.min(missing, stored.getCount());
                    ItemStack remaining = stored.copy();
                    remaining.shrink(used);
                    box.slots.set(slot, remaining.isEmpty() ? ItemStack.EMPTY : remaining);
                    box.changed = true;
                    missing -= used;
                    moved += used;
                }
                if (missing == 0) break;
            }
            if (missing != 0)
            {
                // Should be impossible after the preflight; no live inventory
                // or crafting grid was modified yet.
                return -1;
            }
            planned[i] = wanted.copyWithCount(target);
        }

        // All source deductions and grid additions happen on the server thread.
        for (BoxContents box : boxes)
        {
            if (box.changed)
            {
                box.stack.set(DataComponents.CONTAINER, ItemContainerContents.fromItems(box.slots));
                player.getInventory().setItem(box.index, box.stack);
            }
        }
        for (int i = 0; i < size; i++)
        {
            if (planned[i] != null)
            {
                menu.getSlot(i + 1).set(planned[i]);
            }
        }
        menu.slotsChanged(menu.getSlot(1).container);
        return moved;
    }

    private static boolean hasSupplies(int target, List<ItemStack> pattern,
                                       int[] current, List<BoxContents> boxes)
    {
        for (int i = 0; i < pattern.size(); i++)
        {
            ItemStack item = pattern.get(i);
            if (item.isEmpty()) continue;
            boolean seen = false;
            for (int k = 0; k < i; k++)
            {
                if (ItemStack.isSameItemSameComponents(item, pattern.get(k)))
                {
                    seen = true;
                    break;
                }
            }
            if (seen) continue;

            int required = 0;
            for (int j = 0; j < pattern.size(); j++)
            {
                if (ItemStack.isSameItemSameComponents(item, pattern.get(j)))
                {
                    required += Math.max(0, target - current[j]);
                }
            }
            if (required == 0) continue;
            int available = 0;
            for (BoxContents box : boxes)
            {
                for (ItemStack stored : box.slots)
                {
                    if (ItemStack.isSameItemSameComponents(item, stored))
                    {
                        available += stored.getCount();
                    }
                }
            }
            if (available < required) return false;
        }
        return true;
    }

    private static final class BoxContents
    {
        final int index;
        final ItemStack stack;
        final NonNullList<ItemStack> slots;
        boolean changed;

        BoxContents(int index, ItemStack stack, NonNullList<ItemStack> slots)
        {
            this.index = index;
            this.stack = stack;
            this.slots = slots;
        }
    }
}
