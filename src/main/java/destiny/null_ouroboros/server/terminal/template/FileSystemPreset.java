package destiny.null_ouroboros.server.terminal.template;

import destiny.null_ouroboros.server.terminal.filesystem.TerminusFileSystem;

public class FileSystemPreset {
    private final String name;
    private final int weight;
    private final TerminusFileSystem snapshot;

    public FileSystemPreset(String name, int weight, TerminusFileSystem snapshot) {
        this.name = name;
        this.weight = Math.max(1, weight);
        this.snapshot = snapshot;
    }

    public String getName() {
        return name;
    }

    public int getWeight() {
        return weight;
    }

    public void applyTo(TerminusFileSystem target) {
        target.copyFrom(snapshot);
    }

    public TerminusFileSystem createCopy() {
        TerminusFileSystem copy = new TerminusFileSystem();
        copy.copyFrom(snapshot);
        return copy;
    }
}
