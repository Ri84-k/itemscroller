package fi.dy.masa.itemscroller.mixin.screen;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.MerchantScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.ChatFormatting;
import net.minecraft.world.item.BundleItem;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import fi.dy.masa.itemscroller.util.InventoryUtils;
import fi.dy.masa.itemscroller.shulker.AutoShulkerLocks;
import fi.dy.masa.itemscroller.shulker.AutoShulkerStorageMode;
import fi.dy.masa.itemscroller.config.Configs;
import fi.dy.masa.itemscroller.villager.VillagerDataStorage;

@Mixin(AbstractContainerScreen.class)
public class MixinHandledScreen
{
	@Inject(method = "removed", at = @At("HEAD"))
	private void itemscroller_saveVillagerDataOnClose(CallbackInfo ci)
	{
		if ((Object) this instanceof MerchantScreen)
		{
			VillagerDataStorage.getInstance().writeToDisk();
		}
	}

	@Inject(method = "getTooltipFromContainerItem(Lnet/minecraft/world/item/ItemStack;)Ljava/util/List;", at = @At("HEAD"))
	private void itemscroller_ignore_bundleTooltipsForScrolling(ItemStack itemStack, CallbackInfoReturnable<List<Component>> cir)
	{
		InventoryUtils.setIgnoreScrollingInsideOfBundles(itemStack.getItem() instanceof BundleItem);
	}

    @Inject(method = "getTooltipFromContainerItem(Lnet/minecraft/world/item/ItemStack;)Ljava/util/List;",
            at = @At("RETURN"), cancellable = true)
    private void itemscroller_shulkerLockHint(ItemStack stack, CallbackInfoReturnable<List<Component>> cir)
    {
        if (!AutoShulkerLocks.isShulker(stack)) return;
        List<Component> lines = new ArrayList<>(cir.getReturnValue());
        if (AutoShulkerLocks.isLocked(stack))
        {
            String type = AutoShulkerLocks.lockedType(stack);
            lines.add(Component.literal(type.isEmpty() ? "Locked: assigns first picked-up item" :
                    "Locked to " + type).withStyle(ChatFormatting.GOLD));
        }
        else if (Configs.Generic.AUTO_SHULKER_PICKUP_MODE.getOptionListValue() == AutoShulkerStorageMode.FIXED)
        {
            lines.add(Component.literal("Global Fixed Mode: same item types only").withStyle(ChatFormatting.AQUA));
        }
        lines.add(Component.literal("Item Scroller Hotkeys > Toggle Shulker Lock (default K)")
                .withStyle(ChatFormatting.GRAY));
        cir.setReturnValue(lines);
    }
}
