package destiny.null_ouroboros.server.block;

import net.minecraft.util.StringRepresentable;

import java.util.Locale;

public enum OutputVentMode implements StringRepresentable {
    OFF,
    ON,
    ON_BROKEN;

    @Override
    public String getSerializedName() {
        return name().toLowerCase(Locale.ROOT);
    }
}
