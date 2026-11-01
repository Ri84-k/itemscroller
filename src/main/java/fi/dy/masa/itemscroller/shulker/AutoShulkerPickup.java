package fi.dy.masa.itemscroller.shulker;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.core.NonNullList;
import net.minecraft.core.component.DataComponents;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemContainerContents;
import net.minecraft.world.level.block.ShulkerBoxBlock;

/** Stores world pickups in carried shulkers, matching contents before empty boxes. */
public final class AutoShulkerPickup
{
    private AutoShulkerPickup() {}

    /**
     * Consumes only the amount actually inserted. The caller passes the same
     * mutable stack that vanilla would attempt to add to the player inventory.
     * Vanilla handles any remainder normally.
     */
    public static int store(ServerPlayer player, ItemStack incoming)
    {
        if (!AutoShulkerPickupServer.isEnabled(player) || incoming.isEmpty() ||
                !incoming.getItem().canFitInsideContainerItems()) return 0;

        // Work on copies, then write back only changed shulker stacks. Armor
        // slots are excluded; 40 is the offhand in this Minecraft version.
        List<Box> matching = new ArrayList<>();
        List<Box> empty = new ArrayList<>();

        for (int index = 0; index < player.getInventory().getContainerSize(); index++)
        {
            if (index >= 36 && index != 40) continue;
            ItemStack carried = player.getInventory().getItem(index);
            if (carried.getCount() != 1 || !(carried.getItem() instanceof BlockItem block) ||
                    !(block.getBlock() instanceof ShulkerBoxBlock)) continue;

            NonNullList<ItemStack> slots = NonNullList.withSize(27, ItemStack.EMPTY);
            ItemContainerContents contents = carried.get(DataComponents.CONTAINER);
            if (contents != null) contents.copyInto(slots);

            boolean hasMatch = false;
            boolean hasOther = false;
            for (ItemStack stored : slots)
            {
                if (stored.isEmpty()) continue;
                if (ItemStack.isSameItemSameComponents(stored, incoming)) hasMatch = true;
                else hasOther = true;
            }

            Box candidate = new Box(index, carried.copy(), slots);
            if (hasMatch) matching.add(candidate);
            else if (!hasOther) empty.add(candidate);
            // Boxes with unrelated contents are left alone.
        }

        int originalCount = incoming.getCount();

        // First top off matching stacks, across all matching boxes.
        for (Box box : matching) fillExistingStacks(box, incoming);

        // Then use vacant slots in boxes that already hold this item.
        for (Box box : matching) fillEmptySlots(box, incoming);

        // Finally place remaining items into entirely empty shulker boxes.
        for (Box box : empty) fillEmptySlots(box, incoming);

        for (Box box : matching) commit(player, box);
        for (Box box : empty) commit(player, box);

        int moved = originalCount - incoming.getCount();
        if (moved > 0)
        {
            player.getInventory().setChanged();
            player.inventoryMenu.broadcastChanges();
            player.containerMenu.broadcastChanges();
        }
        return moved;
    }

    private static void fillExistingStacks(Box box, ItemStack incoming)
    {
        for (int slot = 0; slot < box.slots.size() && !incoming.isEmpty(); slot++)
        {
            ItemStack stored = box.slots.get(slot);
            if (stored.isEmpty() || !ItemStack.isSameItemSameComponents(stored, incoming)) continue;

            int capacity = Math.max(0, stored.getMaxStackSize() - stored.getCount());
            int amount = Math.min(incoming.getCount(), capacity);
            if (amount == 0) continue;

            ItemStack expanded = stored.copy();
            expanded.grow(amount);
            box.slots.set(slot, expanded);
            incoming.shrink(amount);
            box.changed = true;
        }
    }

    private static void fillEmptySlots(Box box, ItemStack incoming)
    {
        for (int slot = 0; slot < box.slots.size() && !incoming.isEmpty(); slot++)
        {
            if (!box.slots.get(slot).isEmpty()) continue;
            int amount = Math.min(incoming.getCount(), incoming.getMaxStackSize());
            if (amount <= 0) return;

            box.slots.set(slot, incoming.copyWithCount(amount));
            incoming.shrink(amount);
            box.changed = true;
        }
    }

    private static void commit(ServerPlayer player, Box box)
    {
        if (!box.changed) return;
        box.stack.set(DataComponents.CONTAINER, ItemContainerContents.fromItems(box.slots));
        player.getInventory().setItem(box.index, box.stack);
    }

    private static final class Box
    {
        final int index;
        final ItemStack stack;
        final NonNullList<ItemStack> slots;
        boolean changed;

        Box(int index, ItemStack stack, NonNullList<ItemStack> slots)
        {
            this.index = index;
            this.stack = stack;
            this.slots = slots;
        }
    }
}
