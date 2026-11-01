package fi.dy.masa.itemscroller.shulker;

import fi.dy.masa.itemscroller.config.Configs;
import fi.dy.masa.itemscroller.util.AccessorUtils;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

/**
 * Draws a tiny pixel padlock on carried shulker slots.
 * Gold = manually locked, blue = separated by global Fixed Mode.
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

        for (Slot slot : gui.getMenu().slots)
        {
            if (slot.container != mc.player.getInventory()) continue;
            ItemStack shulker = slot.getItem();
            if (!AutoShulkerLocks.isShulker(shulker)) continue;
            boolean manual = AutoShulkerLocks.isLocked(shulker);
            if (!manual && !fixed) continue;

            // Top-right of the item slot, away from vanilla stack counts
            // drawn in the bottom-right corner of each 16x16 icon.
            int x = left + slot.x + 10;
            int y = top + slot.y;
            int color = manual ? 0xFFFFCB4C : 0xFF8DD6FF;

            // Small 6x8-pixel padlock; tooltip/preview strata render after
            // the inventory layer and cover these badges as expected.
            graphics.fill(x, y, x + 6, y + 8, 0xD0101820);
            graphics.fill(x + 2, y + 1, x + 4, y + 2, color);
            graphics.fill(x + 1, y + 2, x + 2, y + 5, color);
            graphics.fill(x + 4, y + 2, x + 5, y + 5, color);
            graphics.fill(x + 1, y + 4, x + 5, y + 7, color);
            graphics.fill(x + 2, y + 5, x + 3, y + 6, 0xFF27313B);
        }
    }
}
