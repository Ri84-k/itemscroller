package fi.dy.masa.itemscroller.mixin.shulker;

import fi.dy.masa.itemscroller.shulker.AutoShulkerPickup;
import fi.dy.masa.itemscroller.shulker.CarpetOrgDirectDropContext;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Routes only Carpet Org Addition's immediate block-break drops into carried
 * shulkers, without globally changing Inventory.add() behavior.
 */
@Mixin(Inventory.class)
public abstract class MixinInventoryDirectBlockDrops
{
    @Shadow @Final public Player player;

    @Inject(method = "add(Lnet/minecraft/world/item/ItemStack;)Z",
            at = @At("HEAD"), cancellable = true)
    private void itemscroller$storeCarpetOrgDirectDrops(ItemStack incoming,
                                                          CallbackInfoReturnable<Boolean> cir)
    {
        // The original Carpet rule runs on the server, but this mixin also
        // exists client-side; never modify a client's predicted inventory.
        if (!(this.player instanceof ServerPlayer serverPlayer) ||
                !CarpetOrgDirectDropContext.isDirectBlockDrop(serverPlayer))
        {
            return;
        }

        // This updates the SAME stack Carpet would pass to Inventory.add.
        // Shulker storage reuses the existing matching/partial/empty priority
        // and excludes fake players through AutoShulkerPickupServer.
        AutoShulkerPickup.store(serverPlayer, incoming);

        // Vanilla normally handles all remaining items. If everything was
        // stored in shulkers, avoid invoking vanilla add() on an empty stack.
        if (incoming.isEmpty()) cir.setReturnValue(true);
    }
}
