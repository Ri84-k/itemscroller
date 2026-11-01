package fi.dy.masa.itemscroller.shulker;

import java.util.Objects;

import net.minecraft.core.NonNullList;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.component.ItemContainerContents;
import net.minecraft.world.level.block.ShulkerBoxBlock;

/**
 * Lock metadata travels with the shulker item and never changes its contents.
 * Empty manually locked boxes acquire their item type on first successful
 * Auto Shulker Pickup insertion. Unlocking clears only Item Scroller's data.
 */
public final class AutoShulkerLocks
{
    private static final String DATA_KEY = "itemscroller_auto_shulker";
    private static final String LOCK_KEY = "locked";
    private static final String TYPE_KEY = "item_type";

    private AutoShulkerLocks() {}

    public static boolean isShulker(ItemStack stack)
    {
        return stack.getCount() == 1 &&
                stack.getItem() instanceof BlockItem block &&
                block.getBlock() instanceof ShulkerBoxBlock;
    }

    public static String itemType(Item item)
    {
        return BuiltInRegistries.ITEM.getKey(item).toString();
    }

    private static CompoundTag state(ItemStack stack)
    {
        CustomData data = stack.get(DataComponents.CUSTOM_DATA);
        return data == null ? new CompoundTag() : data.copyTag().getCompoundOrEmpty(DATA_KEY);
    }

    public static boolean isLocked(ItemStack stack)
    {
        return isShulker(stack) && state(stack).getBooleanOr(LOCK_KEY, false);
    }

    public static String lockedType(ItemStack stack)
    {
        return isLocked(stack) ? state(stack).getStringOr(TYPE_KEY, "") : "";
    }

    public static boolean accepts(ItemStack stack, ItemStack incoming)
    {
        if (!isLocked(stack)) return true;
        String type = lockedType(stack);
        return type.isEmpty() || Objects.equals(type, itemType(incoming.getItem()));
    }

    public static void setLock(ItemStack stack, boolean enabled, String type)
    {
        CustomData current = stack.get(DataComponents.CUSTOM_DATA);
        CompoundTag root = current == null ? new CompoundTag() : current.copyTag();
        if (enabled)
        {
            CompoundTag state = new CompoundTag();
            state.putBoolean(LOCK_KEY, true);
            if (type != null && !type.isEmpty()) state.putString(TYPE_KEY, type);
            root.put(DATA_KEY, state);
        }
        else root.remove(DATA_KEY);

        if (root.isEmpty()) stack.remove(DataComponents.CUSTOM_DATA);
        else stack.set(DataComponents.CUSTOM_DATA, CustomData.of(root));
    }

    /**
     * Lock a box only if its contents contain at most one distinct item type.
     * Returns null for mixed-item boxes, an empty string for empty boxes,
     * or the Minecraft item ID of the existing contents.
     */
    public static String singleType(ItemStack stack)
    {
        if (!isShulker(stack)) return null;
        ItemContainerContents contents = stack.get(DataComponents.CONTAINER);
        if (contents == null) return "";
        NonNullList<ItemStack> slots = NonNullList.withSize(27, ItemStack.EMPTY);
        contents.copyInto(slots);
        String onlyType = "";
        for (ItemStack item : slots)
        {
            if (item.isEmpty()) continue;
            String type = itemType(item.getItem());
            if (!onlyType.isEmpty() && !onlyType.equals(type)) return null;
            onlyType = type;
        }
        return onlyType;
    }
}
