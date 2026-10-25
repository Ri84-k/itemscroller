package fi.dy.masa.itemscroller.shulker;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;

/** Strictly scoped packet contract for experimental direct-to-grid crafting. */
public final class ShulkerGridPackets
{
    private ShulkerGridPackets() {}

    public record Request(int menuId, List<ItemStack> pattern, ItemStack expectedOutput)
            implements CustomPacketPayload
    {
        public static final Type<Request> TYPE =
                new Type<>(Identifier.fromNamespaceAndPath("itemscroller", "shulker_grid_request"));
        public static final StreamCodec<RegistryFriendlyByteBuf, Request> CODEC = StreamCodec.of(
                (buf, value) ->
                {
                    buf.writeVarInt(value.menuId);
                    buf.writeVarInt(value.pattern.size());
                    for (ItemStack stack : value.pattern)
                    {
                        ItemStack.OPTIONAL_STREAM_CODEC.encode(buf, stack);
                    }
                    ItemStack.OPTIONAL_STREAM_CODEC.encode(buf, value.expectedOutput);
                },
                buf ->
                {
                    int menuId = buf.readVarInt();
                    int size = buf.readVarInt();
                    if (size != 4 && size != 9)
                    {
                        throw new IllegalArgumentException("Invalid crafting grid size");
                    }
                    List<ItemStack> stacks = new ArrayList<>(size);
                    for (int i = 0; i < size; i++)
                    {
                        stacks.add(ItemStack.OPTIONAL_STREAM_CODEC.decode(buf));
                    }
                    ItemStack output = ItemStack.OPTIONAL_STREAM_CODEC.decode(buf);
                    return new Request(menuId, List.copyOf(stacks), output);
                });

        @Override
        public Type<Request> type() { return TYPE; }
    }

    public record Result(int menuId, int moved, boolean ready) implements CustomPacketPayload
    {
        public static final Type<Result> TYPE =
                new Type<>(Identifier.fromNamespaceAndPath("itemscroller", "shulker_grid_result"));
        public static final StreamCodec<RegistryFriendlyByteBuf, Result> CODEC = StreamCodec.of(
                (buf, value) ->
                {
                    buf.writeVarInt(value.menuId);
                    buf.writeVarInt(value.moved);
                    buf.writeBoolean(value.ready);
                },
                buf -> new Result(buf.readVarInt(), buf.readVarInt(), buf.readBoolean()));

        @Override
        public Type<Result> type() { return TYPE; }
    }
}
