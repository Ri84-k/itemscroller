package fi.dy.masa.itemscroller.shulker;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

/** Client preference sent to the authoritative (integrated) server. */
public final class AutoShulkerPickupPackets
{
    private AutoShulkerPickupPackets() {}

    public record Toggle(boolean enabled) implements CustomPacketPayload
    {
        public static final Type<Toggle> TYPE =
                new Type<>(Identifier.fromNamespaceAndPath("itemscroller", "auto_shulker_pickup"));
        public static final StreamCodec<RegistryFriendlyByteBuf, Toggle> CODEC = StreamCodec.of(
                (buf, value) -> buf.writeBoolean(value.enabled()),
                buf -> new Toggle(buf.readBoolean()));

        @Override
        public Type<Toggle> type() { return TYPE; }
    }
}
