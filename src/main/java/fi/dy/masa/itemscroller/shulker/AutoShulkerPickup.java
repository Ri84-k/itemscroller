package fi.dy.masa.itemscroller.shulker;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.core.NonNullList;
import net.minecraft.core.component.DataComponents;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemContainerContents;

/** Routes normal ground pickups and Carpet Org's immediate mining drops. */
public final class AutoShulkerPickup
{
    private AutoShulkerPickup() {}

    /**
     * OVERFLOW: matching stacks -> matching boxes -> other partially used
     * boxes -> completely empty boxes -> vanilla inventory.
     * FIXED: matching boxes -> unused empty boxes -> vanilla inventory.
     * A manually locked box rejects every other item type in both modes.
     */
    public static int store(ServerPlayer player, ItemStack incoming)
    {
        if (!AutoShulkerPickupServer.isEnabled(player) || incoming.isEmpty() ||
                !incoming.getItem().canFitInsideContainerItems()) return 0;

        boolean fixedMode = AutoShulkerPickupServer.isFixedMode(player);
        String type = AutoShulkerLocks.itemType(incoming.getItem());

        List<Box> matching = new ArrayList<>();
        List<Box> partial = new ArrayList<>();
        List<Box> empty = new ArrayList<>();

        for (int index = 0; index < player.getInventory().getContainerSize(); index++)
        {
            if (index >= 36 && index != 40) continue;
            ItemStack carried = player.getInventory().getItem(index);
            if (!AutoShulkerLocks.isShulker(carried) ||
                    !AutoShulkerLocks.accepts(carried, incoming)) continue;

            NonNullList<ItemStack> slots = NonNullList.withSize(27, ItemStack.EMPTY);
            ItemContainerContents contents = carried.get(DataComponents.CONTAINER);
            if (contents != null) contents.copyInto(slots);

            boolean hasMatch = false;
            boolean hasOther = false;
            for (ItemStack stored : slots)
            {
                if (stored.isEmpty()) continue;
                if (AutoShulkerLocks.itemType(stored.getItem()).equals(type)) hasMatch = true;
                else hasOther = true;
            }

            Box candidate = new Box(index, carried.copy(), slots, type);
            String assigned = AutoShulkerLocks.lockedType(carried);

            // An explicitly locked box assigned to this type is a matching
            // destination, even if currently empty.
            if (hasMatch || (!assigned.isEmpty() && assigned.equals(type)))
            {
                matching.add(candidate);
            }
            else if (hasOther)
            {
                // Fixed Mode never introduces new item types into used boxes.
                // Overflow uses other boxes only if they are not manually locked.
                if (!fixedMode && !AutoShulkerLocks.isLocked(carried))
                    partial.add(candidate);
            }
            else
            {
                // Entirely empty box: in Fixed Mode it becomes assigned to
                // this type by its first inserted item. In Overflow it is
                // used only after all partially filled boxes.
                empty.add(candidate);
            }
        }

        int originalCount = incoming.getCount();

        // Matching stacks are filled everywhere before adding a new stack.
        for (Box box : matching) fillExistingStacks(box, incoming);
        for (Box box : matching) fillEmptySlots(box, incoming);

        // Only Overflow Mode uses unrelated partially filled shulkers.
        if (!fixedMode)
        {
            for (Box box : partial) fillEmptySlots(box, incoming);
        }

        // Both modes prefer filling existing boxes over opening an empty one.
        for (Box box : empty) fillEmptySlots(box, incoming);

        for (Box box : matching) commit(player, box);
        for (Box box : partial) commit(player, box);
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
            if (amount <= 0) continue;

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
        if (AutoShulkerLocks.isLocked(box.stack) && AutoShulkerLocks.lockedType(box.stack).isEmpty())
        {
            // Reserve the item type permanently on the first insertion.
            AutoShulkerLocks.setLock(box.stack, true, box.incomingType);
        }
        player.getInventory().setItem(box.index, box.stack);
    }

    private static final class Box
    {
        final int index;
        final ItemStack stack;
        final NonNullList<ItemStack> slots;
        final String incomingType;
        boolean changed;

        Box(int index, ItemStack stack, NonNullList<ItemStack> slots, String incomingType)
        {
            this.index = index;
            this.stack = stack;
            this.slots = slots;
            this.incomingType = incomingType;
        }
    }
}
