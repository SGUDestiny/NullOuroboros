package destiny.null_ouroboros.server.terminal.template;

import net.minecraft.util.RandomSource;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class TerminusTemplate {
    private final String name;
    private final List<FileSystemPreset> presets;

    public TerminusTemplate(String name, List<FileSystemPreset> presets) {
        this.name = name;
        this.presets = Collections.unmodifiableList(new ArrayList<>(presets));
    }

    public String getName() {
        return name;
    }

    public List<FileSystemPreset> getPresets() {
        return presets;
    }

    @Nullable
    public FileSystemPreset roll(RandomSource random) {
        if (presets.isEmpty()) {
            return null;
        }
        int total = 0;
        for (FileSystemPreset preset : presets) {
            total += preset.getWeight();
        }
        if (total <= 0) {
            return presets.get(0);
        }
        int pick = random.nextInt(total);
        int cursor = 0;
        for (FileSystemPreset preset : presets) {
            cursor += preset.getWeight();
            if (pick < cursor) {
                return preset;
            }
        }
        return presets.get(presets.size() - 1);
    }
}
