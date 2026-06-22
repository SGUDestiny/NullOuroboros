package destiny.null_ouroboros.server.terminal.filesystem;

import net.minecraft.nbt.CompoundTag;
import java.util.ArrayList;
import java.util.List;

public class TerminusFileSystem {
    private TerminusDirectory root;
    private String currentPath;

    public TerminusFileSystem() {
        this.root = new TerminusDirectory("T:", null);
        this.currentPath = "T:\\";
    }

    public TerminusDirectory getRoot() { return root; }
    public String getCurrentPath() { return currentPath; }
    public void setCurrentPath(String path) { this.currentPath = path; }

    public TerminusNode resolvePath(String path) {
        if (path == null || path.isEmpty()) return null;
        if (path.startsWith("T:\\") || path.startsWith("D:\\")) {
            return resolveAbsolute(path);
        }
        return resolveRelative(currentPath, path);
    }

    private TerminusNode resolveAbsolute(String absolutePath) {
        String drive = absolutePath.substring(0, 2);
        if (!drive.equals("T:")) return null;
        String remainder = absolutePath.substring(2);
        String[] parts = remainder.split("\\\\");
        TerminusNode current = root;
        for (String part : parts) {
            if (part.isEmpty() || part.equals(".")) continue;
            if (part.equals("..")) {
                if (current.getParent() != null) current = current.getParent();
                continue;
            }
            if (!current.isDirectory()) return null;
            TerminusDirectory dir = (TerminusDirectory) current;
            current = dir.getChild(part);
            if (current == null) return null;
        }
        return current;
    }

    private TerminusNode resolveRelative(String basePath, String relativePath) {
        if (!basePath.endsWith("\\")) basePath += "\\";
        String combined = basePath + relativePath;
        return resolveAbsolute(combined);
    }

    public boolean setCurrentDirectory(TerminusNode node) {
        if (node == null || !node.isDirectory()) return false;
        this.currentPath = buildAbsolutePath(node);
        return true;
    }

    private String buildAbsolutePath(TerminusNode node) {
        List<String> parts = new ArrayList<>();
        TerminusNode current = node;
        while (current != null && current.getParent() != null) {
            parts.add(0, current.getName());
            current = current.getParent();
        }
        StringBuilder sb = new StringBuilder("T:\\");
        for (String part : parts) {
            sb.append(part).append("\\");
        }
        return sb.toString();
    }

    public TerminusDirectory getCurrentDirectory() {
        TerminusNode node = resolvePath(currentPath);
        if (node instanceof TerminusDirectory dir) return dir;
        return root;
    }

    public String getAbsolutePath(TerminusNode node) {
        return buildAbsolutePath(node);
    }

    public String getParentPath(String path) {
        if (path == null || path.isEmpty()) return "T:\\";
        path = path.replaceAll("\\\\+$", "");
        if (path.equals("T:\\")) return "T:\\";
        int lastBackslash = path.lastIndexOf('\\');
        if (lastBackslash <= 2) return "T:\\";
        return path.substring(0, lastBackslash + 1);
    }

    public boolean exists(String path) {
        return resolvePath(path) != null;
    }

    public void createDirectory(String path) throws FileSystemException {
        if (path.isEmpty() || path.equals(".") || path.equals("..")) {
            throw new FileSystemException("Invalid directory name.");
        }
        String targetPath = resolveToAbsolutePath(path);

        if (targetPath.length() > 3 && targetPath.endsWith("\\")) {
            targetPath = targetPath.substring(0, targetPath.length() - 1);
        }

        int lastBackslash = targetPath.lastIndexOf('\\');
        String parentPath = targetPath.substring(0, lastBackslash + 1);
        String name = targetPath.substring(lastBackslash + 1);
        if (name.isEmpty()) {
            throw new FileSystemException("Empty directory name.");
        }
        TerminusNode parentNode = resolvePath(parentPath);
        if (!(parentNode instanceof TerminusDirectory parentDir)) {
            throw new FileSystemException("Parent directory not found: " + parentPath);
        }
        if (parentDir.getChild(name) != null) {
            throw new FileSystemException("'" + name + "' already exists.");
        }
        TerminusDirectory newDir = new TerminusDirectory(name, parentDir);
        parentDir.addChild(newDir);
    }

    public void createTextFile(String path, String initialContent) throws FileSystemException {
        if (path.isEmpty() || path.equals(".") || path.equals("..")) {
            throw new FileSystemException("Invalid file name.");
        }
        String targetPath = resolveToAbsolutePath(path);
        int lastBackslash = targetPath.lastIndexOf('\\');
        String parentPath = targetPath.substring(0, lastBackslash + 1);
        String name = targetPath.substring(lastBackslash + 1);
        if (name.isEmpty()) throw new FileSystemException("Empty name.");
        TerminusNode parentNode = resolvePath(parentPath);
        if (!(parentNode instanceof TerminusDirectory parentDir)) {
            throw new FileSystemException("Parent directory not found.");
        }
        if (parentDir.getChild(name) != null) {
            throw new FileSystemException("'" + name + "' already exists.");
        }

        if (!name.contains(".")) {
            throw new FileSystemException("File name must have an extension (e.g., .txt).");
        }
        String extension = name.substring(name.lastIndexOf('.') + 1).toLowerCase();
        if (extension.equals("itemimg")) {
            throw new FileSystemException("Item image files cannot be created manually.");
        }
        TerminusTextFile file = new TerminusTextFile(name, parentDir, initialContent != null ? initialContent : "");
        parentDir.addChild(file);
    }

    public void delete(String path, boolean recursive, boolean force) throws FileSystemException {
        TerminusNode node = resolvePath(path);
        if (node == null) throw new FileSystemException("Path not found.");
        if (node == root) throw new FileSystemException("Cannot delete root.");
        if (node.isDirectory() && !recursive) {
            throw new FileSystemException("Use -r to remove directories.");
        }
        TerminusDirectory parent = node.getParent();
        if (parent != null) {
            parent.removeChild(node.getName());
        }
    }

    public void move(String sourcePath, String destDirPath) throws FileSystemException {
        TerminusNode source = resolvePath(sourcePath);
        if (source == null) throw new FileSystemException("Source not found.");
        if (source == root) throw new FileSystemException("Cannot move root.");
        TerminusNode destDir = resolvePath(destDirPath);
        if (!(destDir instanceof TerminusDirectory destDirectory)) {
            throw new FileSystemException("Destination is not a directory.");
        }
        if (destDirectory.getChild(source.getName()) != null) {
            throw new FileSystemException("Destination already contains an item named '" + source.getName() + "'.");
        }

        source.getParent().removeChild(source.getName());

        destDirectory.addChild(source);
        source.setParent(destDirectory);
    }

    public void rename(String path, String newName) throws FileSystemException {
        TerminusNode node = resolvePath(path);
        if (node == null) throw new FileSystemException("Path not found.");
        if (node == root) {
            if (newName.length() == 1 && newName.matches("[A-Z]")) {
                root.setName(newName + ":");
                return;
            } else {
                throw new FileSystemException("Root name must be a single capital letter.");
            }
        }
        TerminusDirectory parent = node.getParent();
        if (parent.getChild(newName) != null) {
            throw new FileSystemException("An item named '" + newName + "' already exists.");
        }
        parent.renameChild(node.getName(), newName);
    }

    public String resolveToAbsolutePath(String path) {
        if (path.startsWith("T:\\") || path.startsWith("D:\\")) {
            return path;
        }
        String base = currentPath;
        if (!base.endsWith("\\")) base += "\\";
        return base + path;
    }

    public String getAbsolutePathForNewItem(String name) {
        if (name.contains("\\")) throw new IllegalArgumentException("Name must not contain path separators.");
        String base = currentPath;
        if (!base.endsWith("\\")) base += "\\";
        return base + name;
    }

    public CompoundTag toNBT() {
        CompoundTag tag = new CompoundTag();
        tag.put("root", root.toNBT());
        tag.putString("currentPath", currentPath);
        return tag;
    }

    public void fromNBT(CompoundTag tag) {
        this.root = new TerminusDirectory("T:", null);
        this.root.fromNBT(tag.getCompound("root"));
        this.currentPath = tag.getString("currentPath");
        if (currentPath.isEmpty()) this.currentPath = "T:\\";
    }
}