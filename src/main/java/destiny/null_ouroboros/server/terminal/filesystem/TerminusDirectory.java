package destiny.null_ouroboros.server.terminal.filesystem;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;

import java.util.HashMap;
import java.util.Map;

public class TerminusDirectory extends TerminusNode {
    private final Map<String, TerminusNode> children = new HashMap<>();

    public TerminusDirectory(String name, TerminusDirectory parent) {
        super(name, parent);
    }

    public Map<String, TerminusNode> getChildren() {
        return children;
    }

    public void addChild(TerminusNode node) {
        children.put(node.getName(), node);
    }

    public void removeChild(String name) {
        children.remove(name);
    }

    public TerminusNode getChild(String name) {
        return children.get(name);
    }

    public void renameChild(String oldName, String newName) {
        TerminusNode child = children.remove(oldName);
        if (child != null) {
            child.setName(newName);
            children.put(newName, child);
        }
    }

    @Override
    public boolean isDirectory() {
        return true;
    }

    @Override
    public CompoundTag toNBT() {
        CompoundTag tag = new CompoundTag();
        tag.putString("name", name);
        ListTag childList = new ListTag();
        for (TerminusNode child : children.values()) {
            CompoundTag childTag = child.toNBT();
            childTag.putString("type", child.isDirectory() ? "dir" : "file");
            childList.add(childTag);
        }
        tag.put("children", childList);
        return tag;
    }

    @Override
    public void fromNBT(CompoundTag tag) {
        this.name = tag.getString("name");
        children.clear();
        ListTag childList = tag.getList("children", 10);
        for (int i = 0; i < childList.size(); i++) {
            CompoundTag childTag = childList.getCompound(i);
            String type = childTag.getString("type");
            TerminusNode child;
            if ("dir".equals(type)) {
                child = new TerminusDirectory("", this);
            } else {
                child = new TerminusTextFile("", this, "");
            }
            child.fromNBT(childTag);
            child.setParent(this);
            children.put(child.getName(), child);
        }
    }
}