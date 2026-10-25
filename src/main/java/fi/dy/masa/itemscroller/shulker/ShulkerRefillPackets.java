package fi.dy.masa.itemscroller.shulker;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;

/** A server-authoritative ingredient transfer request and acknowledgment. */
public final class ShulkerRefillPackets
{
    private ShulkerRefillPackets() {}

    public record Request(List<ItemStack> ingredients) implements CustomPacketPayload
    {
        public static final Type<Request> TYPE =
                new Type<>(Identifier.fromNamespaceAndPath("itemscroller", "shulker_refill"));
        public static final StreamCodec<RegistryFriendlyByteBuf, Request> CODEC = StreamCodec.of(
                (buf, value) ->
                {
                    if (value.ingredients.size() > 9) throw new IllegalArgumentException("Too many ingredients");
                    buf.writeVarInt(value.ingredients.size());
                    for (ItemStack stack : value.ingredients)
                    {
                        ItemStack.OPTIONAL_STREAM_CODEC.encode(buf, stack);
                    }
                },
                buf ->
                {
                    int size = buf.readVarInt();
                    if (size < 0 || size > 9) throw new IllegalArgumentException("Invalid ingredient count");
                    List<ItemStack> stacks = new ArrayList<>(size);
                    for (int i = 0; i < size; i++)
                    {
                        stacks.add(ItemStack.OPTIONAL_STREAM_CODEC.decode(buf));
                    }
                    return new Request(List.copyOf(stacks));
                });

        @Override
        public Type<Request> type() { return TYPE; }
    }

    public record Result(int moved) implements CustomPacketPayload
    {
        public static final Type<Result> TYPE =
                new Type<>(Identifier.fromNamespaceAndPath("itemscroller", "shulker_refill_result"));
        public static final StreamCodec<RegistryFriendlyByteBuf, Result> CODEC = StreamCodec.of(
                (buf, value) -> buf.writeVarInt(value.moved),
                buf -> new Result(buf.readVarInt()));

        @Override
        public Type<Result> type() { return TYPE; }
    }
}
