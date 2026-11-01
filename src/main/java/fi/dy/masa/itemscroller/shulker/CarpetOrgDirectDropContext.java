package fi.dy.masa.itemscroller.shulker;

import net.minecraft.server.level.ServerPlayer;

/**
 * Optional, zero-dependency compatibility with Carpet Org Addition's
 * blockDropsDirectlyEnterInventory rule.
 *
 * The 26.2 Carpet Org Addition server wraps destroyBlock in a ScopedValue
 * bound to the breaking player. Its block-drop handler then calls the
 * player's Inventory.add(ItemStack) instead of waiting for world pickup.
 * Checking that very same scope lets us distinguish these direct drops from
 * /give, crafting results, inventory transfers, and other insertions.
 */
public final class CarpetOrgDirectDropContext
{
    private static final ScopedValue<?> BLOCK_BREAKER = findBlockBreaker();

    private CarpetOrgDirectDropContext() {}

    public static boolean isDirectBlockDrop(ServerPlayer player)
    {
        return BLOCK_BREAKER != null && BLOCK_BREAKER.isBound() &&
                BLOCK_BREAKER.get() == player;
    }

    private static ScopedValue<?> findBlockBreaker()
    {
        try
        {
            // No hard dependency: Item Scroller still works if Carpet Org
            // Addition is absent, and the compatibility hook simply stops.
            Class<?> settings = Class.forName("boat.carpetorgaddition.CarpetOrgAdditionSettings");
            Object scoped = settings.getField("BLOCK_BREAKER").get(null);
            return scoped instanceof ScopedValue<?> value ? value : null;
        }
        catch (ReflectiveOperationException | LinkageError exception)
        {
            return null;
        }
    }
}
