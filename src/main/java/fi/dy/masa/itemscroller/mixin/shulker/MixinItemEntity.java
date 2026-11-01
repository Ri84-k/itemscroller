package fi.dy.masa.itemscroller.mixin.shulker;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import fi.dy.masa.itemscroller.shulker.AutoShulkerPickup;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/**
 * Intercept only vanilla ground-item pickups, after vanilla has checked
 * pickup delays and ownership. Never intercept normal inventory insertion.
 */
@Mixin(ItemEntity.class)
public abstract class MixinItemEntity
{
    @WrapOperation(
            method = "playerTouch",
            at = @At(value = "INVOKE",
                    target = "Lnet/minecraft/world/entity/player/Inventory;add(Lnet/minecraft/world/item/ItemStack;)Z"))
    private boolean itemscroller$autoShulkerPickup(Inventory inventory, ItemStack incoming,
                                                    Operation<Boolean> original, Player player)
    {
        if (player instanceof ServerPlayer serverPlayer)
        {
            AutoShulkerPickup.store(serverPlayer, incoming);
            // Vanilla should finish its normal pickup/stat/discard path even
            // when the shulker consumed the entire ground-item stack.
            if (incoming.isEmpty()) return true;
        }
        // Respect vanilla inventory insertion and all remaining-item behavior.
        return original.call(inventory, incoming);
    }
}
