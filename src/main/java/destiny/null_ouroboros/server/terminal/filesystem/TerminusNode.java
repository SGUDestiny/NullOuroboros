package destiny.null_ouroboros.server.terminal.filesystem;

import net.minecraft.nbt.CompoundTag;

public abstract class TerminusNode {
    protected String name;
    protected TerminusDirectory parent;

    protected TerminusNode(String name, TerminusDirectory parent) {
        this.name = name;
        this.parent = parent;
    }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public TerminusDirectory getParent() { return parent; }
    public void setParent(TerminusDirectory parent) { this.parent = parent; }

    public abstract boolean isDirectory();
    public abstract CompoundTag toNBT();
    public abstract void fromNBT(CompoundTag tag);
}