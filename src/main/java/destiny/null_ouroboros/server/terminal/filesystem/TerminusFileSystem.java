package destiny.null_ouroboros.server.terminal.filesystem;

import net.minecraft.nbt.CompoundTag;
import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;

public class TerminusFileSystem {
    private static final String DEFAULT_DRIVE = "T:";

    private TerminusDirectory root;
    private String currentPath;
    private boolean dirty = false;

    public TerminusFileSystem() {
        this.root = new TerminusDirectory(DEFAULT_DRIVE, null);
        this.currentPath = getRootPrefix();
    }

    public TerminusDirectory getRoot() { return root; }
    public String getCurrentPath() { return currentPath; }
    public void setCurrentPath(String path) { this.currentPath = path; }

    public boolean isDirty() { return dirty; }
    public void clearDirty() { dirty = false; }

    private void markDirty() { dirty = true; }

    public void markDirtyForEdit() { dirty = true; }

    public String getRootPrefix() {
        return root.getName() + "\\";
    }

    public String getDriveSpecifier() {
        return root.getName();
    }

    private boolean isAbsolutePath(String path) {
        if (path == null || path.length() < 3) return false;
        return path.charAt(1) == ':' && path.charAt(2) == '\\'
                && path.substring(0, 2).equalsIgnoreCase(root.getName());
    }

    public void normalizePathsForRoot() {
        String prefix = getRootPrefix();
        if (currentPath == null || currentPath.isEmpty()) {
            currentPath = prefix;
            return;
        }
        if (currentPath.length() >= 3 && currentPath.charAt(1) == ':' && currentPath.charAt(2) == '\\') {
            if (!currentPath.substring(0, 2).equalsIgnoreCase(root.getName())) {
                currentPath = prefix + currentPath.substring(3);
                markDirty();
            }
        } else {
            currentPath = prefix;
            markDirty();
        }
    }

    public TerminusNode resolvePath(String path) {
        if (path == null || path.isEmpty()) return null;
        if (isAbsolutePath(path)) {
            return resolveAbsolute(path);
        }
        return resolveRelative(currentPath, path);
    }

    private TerminusNode resolveAbsolute(String absolutePath) {
        String drive = absolutePath.substring(0, 2);
        if (!drive.equalsIgnoreCase(root.getName())) return null;
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
        markDirty();
        return true;
    }

    private String buildAbsolutePath(TerminusNode node) {
        List<String> parts = new ArrayList<>();
        TerminusNode current = node;
        while (current != null && current.getParent() != null) {
            parts.add(0, current.getName());
            current = current.getParent();
        }
        if (parts.isEmpty()) {
            return getRootPrefix();
        }
        StringBuilder sb = new StringBuilder(getRootPrefix());
        for (int i = 0; i < parts.size(); i++) {
            sb.append(parts.get(i));
            if (i < parts.size() - 1) {
                sb.append("\\");
            }
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
        String rootPrefix = getRootPrefix();
        if (path == null || path.isEmpty()) return rootPrefix;
        path = path.replaceAll("\\\\+$", "");
        if (path.equalsIgnoreCase(root.getName()) || path.equalsIgnoreCase(rootPrefix.substring(0, rootPrefix.length() - 1))) {
            return rootPrefix;
        }
        int lastBackslash = path.lastIndexOf('\\');
        if (lastBackslash <= 2) return rootPrefix;
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
        markDirty();
    }

    public static boolean hasExtension(String name) {
        return name.contains(".");
    }

    public static boolean isTextFileName(String name) {
        return name.toLowerCase().endsWith(".txt");
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

        if (!hasExtension(name)) {
            throw new FileSystemException("File name must have an extension (e.g., .txt).");
        }
        if (!isTextFileName(name)) {
            throw new FileSystemException("Cannot create a file with this format.");
        }
        TerminusTextFile file = new TerminusTextFile(name, parentDir, initialContent != null ? initialContent : "");
        parentDir.addChild(file);
        markDirty();
    }

    public String resolveDuplicateName(TerminusDirectory parent, String sourceName) {
        String base = sourceName;
        String extension = "";
        int dot = sourceName.lastIndexOf('.');
        if (dot > 0) {
            base = sourceName.substring(0, dot);
            extension = sourceName.substring(dot);
        }

        String candidate = base + "_copy" + extension;
        if (parent.getChild(candidate) == null) {
            return candidate;
        }
        for (int index = 0; ; index++) {
            candidate = base + "_copy" + index + extension;
            if (parent.getChild(candidate) == null) {
                return candidate;
            }
        }
    }

    public String resolveLogFileName(TerminusDirectory logDir) {
        for (int index = 0; ; index++) {
            String candidate = "p2p_log" + index + ".txt";
            if (logDir.getChild(candidate) == null) {
                return candidate;
            }
        }
    }

    public void duplicate(String sourcePath, @Nullable String destPath) throws FileSystemException {
        TerminusNode source = resolvePath(sourcePath);
        if (source == null) throw new FileSystemException("Source not found.");
        if (source == root) throw new FileSystemException("Cannot duplicate root.");

        TerminusDirectory destParent;
        String destName;
        if (destPath == null || destPath.isBlank()) {
            destParent = source.getParent();
            if (destParent == null) throw new FileSystemException("Parent directory not found.");
            destName = resolveDuplicateName(destParent, source.getName());
        } else {
            String targetPath = resolveToAbsolutePath(destPath.trim());
            if (targetPath.length() > 3 && targetPath.endsWith("\\")) {
                targetPath = targetPath.substring(0, targetPath.length() - 1);
            }
            int lastBackslash = targetPath.lastIndexOf('\\');
            String parentPath = targetPath.substring(0, lastBackslash + 1);
            destName = targetPath.substring(lastBackslash + 1);
            if (destName.isEmpty()) throw new FileSystemException("Empty destination name.");
            TerminusNode parentNode = resolvePath(parentPath);
            if (!(parentNode instanceof TerminusDirectory directory)) {
                throw new FileSystemException("Destination parent not found.");
            }
            destParent = directory;
            if (destParent.getChild(destName) != null) {
                throw new FileSystemException("Destination already exists.");
            }
        }

        TerminusNode copy = copyNode(source, destName, destParent);
        destParent.addChild(copy);
        markDirty();
    }

    private TerminusNode copyNode(TerminusNode source, String newName, TerminusDirectory parent) {
        if (source instanceof TerminusTextFile file) {
            return new TerminusTextFile(newName, parent, file.getContent());
        }
        TerminusDirectory copy = new TerminusDirectory(newName, parent);
        if (source instanceof TerminusDirectory directory) {
            for (TerminusNode child : directory.getChildren().values()) {
                copy.addChild(copyNode(child, child.getName(), copy));
            }
        }
        return copy;
    }

    public void delete(String path, boolean recursive) throws FileSystemException {
        TerminusNode node = resolvePath(path);
        if (node == null) throw new FileSystemException("Path not found.");
        if (node == root) throw new FileSystemException("Cannot delete root.");
        if (node.isDirectory() && !recursive) {
            throw new FileSystemException("Use -r to remove directories.");
        }
        TerminusDirectory parent = node.getParent();
        if (parent != null) {
            parent.removeChild(node.getName());
            markDirty();
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
        markDirty();
    }

    public void rename(String path, String newName) throws FileSystemException {
        TerminusNode node = resolvePath(path);
        if (node == null) throw new FileSystemException("Path not found.");
        if (node == root) {
            if (newName.length() == 1 && newName.matches("[A-Z]")) {
                root.setName(newName + ":");
                normalizePathsForRoot();
                markDirty();
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
        if (node == getCurrentDirectory()) {
            setCurrentDirectory(node);
        }
        markDirty();
    }

    public String resolveToAbsolutePath(String path) {
        if (isAbsolutePath(path)) {
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

    public List<String> searchByName(String name) {
        List<String> results = new ArrayList<>();
        searchByNameRecursive(root, name, results);
        results.sort(String::compareToIgnoreCase);
        return results;
    }

    private void searchByNameRecursive(TerminusNode node, String name, List<String> results) {
        if (node.getParent() != null && node.getName().equals(name)) {
            results.add(getAbsolutePath(node));
        }
        if (node instanceof TerminusDirectory directory) {
            for (TerminusNode child : directory.getChildren().values()) {
                searchByNameRecursive(child, name, results);
            }
        }
    }

    public CompoundTag toNBT() {
        CompoundTag tag = new CompoundTag();
        tag.put("root", root.toNBT());
        tag.putString("currentPath", currentPath);
        return tag;
    }

    public void fromNBT(CompoundTag tag) {
        this.root = new TerminusDirectory(DEFAULT_DRIVE, null);
        this.root.fromNBT(tag.getCompound("root"));
        this.currentPath = tag.getString("currentPath");
        normalizePathsForRoot();
    }
}
