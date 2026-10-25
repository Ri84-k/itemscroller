package fi.dy.masa.itemscroller.shulker;

import java.util.List;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.core.NonNullList;
import net.minecraft.core.component.DataComponents;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.inventory.CraftingMenu;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemContainerContents;
import net.minecraft.world.level.block.ShulkerBoxBlock;

/** Moves real items out of shulker boxes carried by the requesting player. */
public final class ShulkerRefillServer implements ModInitializer
{
    @Override
    public void onInitialize()
    {
        PayloadTypeRegistry.serverboundPlay().register(ShulkerRefillPackets.Request.TYPE, ShulkerRefillPackets.Request.CODEC);
        PayloadTypeRegistry.clientboundPlay().register(ShulkerRefillPackets.Result.TYPE, ShulkerRefillPackets.Result.CODEC);
        ShulkerGridServer.register();
        ServerPlayNetworking.registerGlobalReceiver(ShulkerRefillPackets.Request.TYPE, (request, context) ->
        {
            int moved = refill(context.player(), request.ingredients());
            context.player().getInventory().setChanged();
            context.player().containerMenu.broadcastChanges();
            context.player().inventoryMenu.broadcastChanges();
            ServerPlayNetworking.send(context.player(), new ShulkerRefillPackets.Result(moved));
        });
    }

    private static int refill(ServerPlayer player, List<ItemStack> requested)
    {
        if (!(player.containerMenu instanceof CraftingMenu || player.containerMenu instanceof InventoryMenu)) return 0;
        if (requested.size() > 9) return 0;

        int movedTotal = 0;
        for (int index = 0; index < requested.size(); index++)
        {
            ItemStack wanted = requested.get(index);
            if (wanted.isEmpty()) continue;
            boolean alreadySeen = false;
            for (int i = 0; i < index; i++)
            {
                if (ItemStack.isSameItemSameComponents(wanted, requested.get(i)))
                {
                    alreadySeen = true;
                    break;
                }
            }
            if (alreadySeen) continue;
            // One representative item per crafting-grid slot, at most 9.
            int perCraft = wanted.getCount();
            if (perCraft < 1 || perCraft > 9) continue;

            int existing = 0;
            for (int i = 0; i < 36; i++)
            {
                ItemStack stack = player.getInventory().getItem(i);
                if (ItemStack.isSameItemSameComponents(stack, wanted)) existing += stack.getCount();
            }
            if (existing >= Math.max(8, perCraft * 4)) continue;

            // Prefetch up to 32 recipes of this ingredient, capped to four
            // stacks; the inventory's actual available space is authoritative.
            int target = Math.min(256, perCraft * 32);
            int toMove = Math.max(0, target - existing);
            for (int boxIndex = 0; boxIndex < player.getInventory().getContainerSize() && toMove > 0; boxIndex++)
            {
                if (boxIndex >= 36 && boxIndex != 40) continue; // Main inventory, hotbar, offhand
                ItemStack box = player.getInventory().getItem(boxIndex);
                if (!(box.getItem() instanceof BlockItem blockItem) ||
                    !(blockItem.getBlock() instanceof ShulkerBoxBlock) || box.getCount() != 1) continue;
                ItemContainerContents container = box.get(DataComponents.CONTAINER);
                if (container == null) continue;

                NonNullList<ItemStack> slots = NonNullList.withSize(27, ItemStack.EMPTY);
                container.copyInto(slots);
                boolean changed = false;
                for (int i = 0; i < slots.size() && toMove > 0; i++)
                {
                    ItemStack contained = slots.get(i);
                    if (contained.isEmpty() || !ItemStack.isSameItemSameComponents(wanted, contained)) continue;
                    int attempt = Math.min(toMove, contained.getCount());
                    ItemStack extracted = contained.copyWithCount(attempt);
                    player.getInventory().add(extracted);
                    int moved = attempt - extracted.getCount();
                    if (moved <= 0) continue;
                    ItemStack remaining = contained.copy();
                    remaining.shrink(moved);
                    slots.set(i, remaining);
                    changed = true;
                    toMove -= moved;
                    movedTotal += moved;
                }
                if (changed)
                {
                    box.set(DataComponents.CONTAINER, ItemContainerContents.fromItems(slots));
                    player.getInventory().setItem(boxIndex, box);
                }
            }
        }
        return movedTotal;
    }
}
