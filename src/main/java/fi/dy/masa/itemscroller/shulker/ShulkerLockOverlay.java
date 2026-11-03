package fi.dy.masa.itemscroller.shulker;

import fi.dy.masa.itemscroller.config.Configs;
import fi.dy.masa.itemscroller.util.AccessorUtils;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

/**
 * Gold: manually locked shulker (in player or external inventories).
 * Blue: unlocked shulker affected by global Fixed Mode (carried boxes only).
 * An optional green match overlay complements component-sensitive highlight
 * mods without changing vanilla item equality, stacking, or sorting behavior.
 */
public final class ShulkerLockOverlay
{
    private ShulkerLockOverlay() {}

    public static void render(GuiGraphicsExtractor graphics)
    {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || !(mc.gui.screen() instanceof AbstractContainerScreen<?> gui)) return;

        boolean fixed = Configs.Generic.AUTO_SHULKER_PICKUP_MODE.getOptionListValue() ==
                AutoShulkerStorageMode.FIXED;
        int left = AccessorUtils.getGuiLeft(gui);
        int top = AccessorUtils.getGuiTop(gui);

        Slot hovered = AccessorUtils.getSlotUnderMouse(gui);
        ItemStack focused = hovered != null ? hovered.getItem() : ItemStack.EMPTY;

        // This visual complement applies only when Item Scroller's manual
        // lock marker is the difference; it does NOT make items stackable or
        // alter other mods' underlying item-comparison rules.
        if (Configs.Generic.AUTO_SHULKER_LOCK_MATCH_HIGHLIGHT.getBooleanValue() &&
                AutoShulkerLocks.isShulker(focused))
        {
            boolean focusedLocked = AutoShulkerLocks.isLocked(focused);
            for (Slot slot : gui.getMenu().slots)
            {
                if (slot == hovered) continue;
                ItemStack candidate = slot.getItem();
                if (!AutoShulkerLocks.isShulker(candidate)) continue;
                if (focusedLocked == AutoShulkerLocks.isLocked(candidate)) continue;
                if (!sameShulkerGroup(focused, candidate)) continue;

                // Tint only the cases another component-sensitive highlighter
                // could omit because the two boxes have different lock tags.
                int x = left + slot.x;
                int y = top + slot.y;
                graphics.fill(x, y, x + 16, y + 16, 0x5530C858);
            }
        }

        // Draw manual locks on shulkers anywhere a container menu displays
        // them. Global Fixed Mode only affects shulkers carried by the player.
        for (Slot slot : gui.getMenu().slots)
        {
            ItemStack shulker = slot.getItem();
            if (!AutoShulkerLocks.isShulker(shulker)) continue;
            boolean manual = AutoShulkerLocks.isLocked(shulker);
            boolean carried = slot.container == mc.player.getInventory();
            if (!manual && !(fixed && carried)) continue;

            int x = left + slot.x + 10;
            int y = top + slot.y;
            int color = manual ? 0xFFFFCB4C : 0xFF8DD6FF;

            // Compact padlock in the upper-right corner of a 16px item slot.
            graphics.fill(x, y, x + 6, y + 8, 0xD0101820);
            graphics.fill(x + 2, y + 1, x + 4, y + 2, color);
            graphics.fill(x + 1, y + 2, x + 2, y + 5, color);
            graphics.fill(x + 4, y + 2, x + 5, y + 5, color);
            graphics.fill(x + 1, y + 4, x + 5, y + 7, color);
            graphics.fill(x + 2, y + 5, x + 3, y + 6, 0xFF27313B);
        }
    }

    private static boolean sameShulkerGroup(ItemStack a, ItemStack b)
    {
        // Same colored shulker item: match by item ID, not the lock tag.
        if (ItemStack.isSameItem(a, b)) return true;

        // Different-colored boxes also qualify if both hold only the same
        // item type. Empty or mixed-content boxes aren't grouped this way.
        String typeA = AutoShulkerLocks.singleType(a);
        String typeB = AutoShulkerLocks.singleType(b);
        return typeA != null && !typeA.isEmpty() && typeA.equals(typeB);
    }
}
