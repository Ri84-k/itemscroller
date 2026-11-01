package fi.dy.masa.itemscroller.shulker;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

/** Server-authoritative mode selection and inventory-slot locking. */
public final class AutoShulkerPickupPackets
{
    private AutoShulkerPickupPackets() {}

    public record Toggle(boolean enabled, boolean fixedMode) implements CustomPacketPayload
    {
        public static final Type<Toggle> TYPE =
                new Type<>(Identifier.fromNamespaceAndPath("itemscroller", "auto_shulker_pickup"));
        public static final StreamCodec<RegistryFriendlyByteBuf, Toggle> CODEC = StreamCodec.of(
                (buf, value) ->
                {
                    buf.writeBoolean(value.enabled());
                    buf.writeBoolean(value.fixedMode());
                },
                buf -> new Toggle(buf.readBoolean(), buf.readBoolean()));

        @Override
        public Type<Toggle> type() { return TYPE; }
    }

    /** References a real player-owned menu slot, not arbitrary item data. */
    public record LockBox(int menuId, int slotId) implements CustomPacketPayload
    {
        public static final Type<LockBox> TYPE =
                new Type<>(Identifier.fromNamespaceAndPath("itemscroller", "shulker_box_lock"));
        public static final StreamCodec<RegistryFriendlyByteBuf, LockBox> CODEC = StreamCodec.of(
                (buf, value) ->
                {
                    buf.writeVarInt(value.menuId());
                    buf.writeVarInt(value.slotId());
                },
                buf -> new LockBox(buf.readVarInt(), buf.readVarInt()));

        @Override
        public Type<LockBox> type() { return TYPE; }
    }
}
