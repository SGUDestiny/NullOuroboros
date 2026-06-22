package destiny.null_ouroboros.server.terminal.filesystem;

import net.minecraft.nbt.CompoundTag;

public class TerminusTextFile extends TerminusNode {
    private String content;

    public TerminusTextFile(String name, TerminusDirectory parent, String content) {
        super(name, parent);
        this.content = content;
    }

    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }

    @Override
    public boolean isDirectory() { return false; }

    @Override
    public CompoundTag toNBT() {
        CompoundTag tag = new CompoundTag();
        tag.putString("name", name);
        tag.putString("content", content);
        return tag;
    }

    @Override
    public void fromNBT(CompoundTag tag) {
        this.name = tag.getString("name");
        this.content = tag.getString("content");
    }
}