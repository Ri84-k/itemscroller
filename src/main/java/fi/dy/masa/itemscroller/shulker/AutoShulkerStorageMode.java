package fi.dy.masa.itemscroller.shulker;

import javax.annotation.Nonnull;

import fi.dy.masa.itemscroller.Reference;
import fi.dy.masa.malilib.config.IConfigOptionListEntry;
import fi.dy.masa.malilib.util.StringUtils;

/** Global choice between space-saving overflow and item-separated storage. */
public enum AutoShulkerStorageMode implements IConfigOptionListEntry
{
    OVERFLOW("overflow"),
    FIXED("fixed");

    private final String value;

    AutoShulkerStorageMode(String value) { this.value = value; }

    @Override
    public String getStringValue() { return value; }

    @Override
    public String getDisplayName()
    {
        return StringUtils.getTranslatedOrFallback(
                Reference.MOD_ID + ".gui.label.auto_shulker_mode." + value,
                value.equals("overflow") ? "Overflow (mixed)" : "Fixed (one item type)");
    }

    @Override
    public IConfigOptionListEntry cycle(boolean forward)
    {
        return this == OVERFLOW ? FIXED : OVERFLOW;
    }

    @Override
    public AutoShulkerStorageMode fromString(String string)
    {
        return "fixed".equalsIgnoreCase(string) ? FIXED : OVERFLOW;
    }
}
