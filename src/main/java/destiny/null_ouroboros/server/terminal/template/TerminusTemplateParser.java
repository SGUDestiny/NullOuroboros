package destiny.null_ouroboros.server.terminal.template;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import destiny.null_ouroboros.NullOuroboros;
import destiny.null_ouroboros.server.terminal.filesystem.FileSystemException;
import destiny.null_ouroboros.server.terminal.filesystem.TerminusDirectory;
import destiny.null_ouroboros.server.terminal.filesystem.TerminusFileSystem;
import destiny.null_ouroboros.server.terminal.filesystem.TerminusNode;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;

public final class TerminusTemplateParser {
    private TerminusTemplateParser() {
    }

    public static Map<ResourceLocation, TerminusTemplate> parseDatapackTemplates(ResourceManager resourceManager) {
        Map<ResourceLocation, TerminusTemplate> result = new HashMap<>();
        Map<ResourceLocation, ?> resources = resourceManager.listResources("terminus_templates", location -> true);

        Map<ResourceLocation, Map<String, ResourceLocation>> filesByTemplate = new HashMap<>();
        for (ResourceLocation location : resources.keySet()) {
            String path = location.getPath();
            if (!path.startsWith("terminus_templates/")) {
                continue;
            }
            String remainder = path.substring("terminus_templates/".length());
            int slash = remainder.indexOf('/');
            if (slash < 0) {
                continue;
            }
            String templateName = remainder.substring(0, slash);
            ResourceLocation templateId = ResourceLocation.fromNamespaceAndPath(location.getNamespace(), templateName);
            filesByTemplate.computeIfAbsent(templateId, id -> new HashMap<>())
                    .put(remainder.substring(slash + 1), location);
        }

        for (Map.Entry<ResourceLocation, Map<String, ResourceLocation>> entry : filesByTemplate.entrySet()) {
            ResourceLocation templateId = entry.getKey();
            Map<String, ResourceLocation> files = entry.getValue();
            ResourceLocation templateJsonLoc = files.get("template.json");
            if (templateJsonLoc == null) {
                NullOuroboros.LOGGER.warn("Skipping terminus template {} — missing template.json", templateId);
                continue;
            }
            try {
                JsonObject root;
                try (BufferedReader reader = resourceManager.openAsReader(templateJsonLoc)) {
                    root = JsonParser.parseReader(reader).getAsJsonObject();
                }
                Map<String, Integer> declared = readDeclaredPresets(root);
                List<FileSystemPreset> presets = new ArrayList<>();
                for (Map.Entry<String, Integer> presetEntry : declared.entrySet()) {
                    String presetName = presetEntry.getKey();
                    if (files.keySet().stream().noneMatch(rel -> rel.startsWith(presetName + "/") || rel.equals(presetName))) {
                        NullOuroboros.LOGGER.warn("Terminus template {} declares missing preset {}", templateId, presetName);
                        continue;
                    }
                    TerminusFileSystem snapshot = buildPresetFromDatapack(resourceManager, files, presetName);
                    presets.add(new FileSystemPreset(presetName, presetEntry.getValue(), snapshot));
                }
                if (!presets.isEmpty()) {
                    result.put(templateId, new TerminusTemplate(templateId.getPath(), presets));
                }
            } catch (Exception e) {
                NullOuroboros.LOGGER.error("Failed to load terminus template {}", templateId, e);
            }
        }
        return result;
    }

    public static Map<String, TerminusTemplate> parseWorldSaveTemplates(Path root) {
        Map<String, TerminusTemplate> result = new HashMap<>();
        if (root == null || !Files.isDirectory(root)) {
            return result;
        }
        try (Stream<Path> children = Files.list(root)) {
            children.filter(Files::isDirectory).forEach(templateDir -> {
                String templateName = templateDir.getFileName().toString();
                Path templateJson = templateDir.resolve("template.json");
                if (!Files.isRegularFile(templateJson)) {
                    return;
                }
                try {
                    JsonObject rootJson = JsonParser.parseString(Files.readString(templateJson, StandardCharsets.UTF_8)).getAsJsonObject();
                    Map<String, Integer> declared = readDeclaredPresets(rootJson);
                    List<FileSystemPreset> presets = new ArrayList<>();
                    for (Map.Entry<String, Integer> presetEntry : declared.entrySet()) {
                        String presetName = presetEntry.getKey();
                        Path presetDir = templateDir.resolve(presetName);
                        if (!Files.isDirectory(presetDir)) {
                            NullOuroboros.LOGGER.warn("Terminus template {} declares missing preset folder {}", templateName, presetName);
                            continue;
                        }
                        TerminusFileSystem snapshot = buildPresetFromDisk(presetDir);
                        presets.add(new FileSystemPreset(presetName, presetEntry.getValue(), snapshot));
                    }
                    if (!presets.isEmpty()) {
                        result.put(templateName, new TerminusTemplate(templateName, presets));
                    }
                } catch (Exception e) {
                    NullOuroboros.LOGGER.error("Failed to load world-save terminus template {}", templateName, e);
                }
            });
        } catch (IOException e) {
            NullOuroboros.LOGGER.error("Failed to scan world-save terminus_templates at {}", root, e);
        }
        return result;
    }

    private static Map<String, Integer> readDeclaredPresets(JsonObject root) {
        Map<String, Integer> declared = new HashMap<>();
        if (!root.has("presets") || !root.get("presets").isJsonObject()) {
            return declared;
        }
        JsonObject presets = root.getAsJsonObject("presets");
        for (Map.Entry<String, JsonElement> entry : presets.entrySet()) {
            int weight = 1;
            JsonElement value = entry.getValue();
            if (value.isJsonObject() && value.getAsJsonObject().has("weight")) {
                weight = value.getAsJsonObject().get("weight").getAsInt();
            } else if (value.isJsonPrimitive() && value.getAsJsonPrimitive().isNumber()) {
                weight = value.getAsInt();
            }
            declared.put(entry.getKey(), Math.max(1, weight));
        }
        return declared;
    }

    private static TerminusFileSystem buildPresetFromDatapack(ResourceManager resourceManager,
                                                             Map<String, ResourceLocation> files,
                                                             String presetName) throws IOException {
        TerminusFileSystem fs = new TerminusFileSystem();
        String prefix = presetName + "/";
        Set<String> dirs = new HashSet<>();
        Map<String, String> textFiles = new HashMap<>();

        for (Map.Entry<String, ResourceLocation> entry : files.entrySet()) {
            String relative = entry.getKey();
            if (!relative.startsWith(prefix) && !relative.equals(presetName)) {
                continue;
            }
            if (relative.equals(presetName)) {
                continue;
            }
            String underPreset = relative.substring(prefix.length());
            if (underPreset.isEmpty()) {
                continue;
            }
            String dosPath = underPreset.replace('/', '\\');
            if (underPreset.toLowerCase().endsWith(".txt")) {
                try (InputStream stream = resourceManager.open(entry.getValue())) {
                    textFiles.put(dosPath, normalizeNewlines(new String(stream.readAllBytes(), StandardCharsets.UTF_8)));
                }
                addParentDirs(dirs, underPreset);
            } else if (!underPreset.contains(".")) {
                dirs.add(dosPath);
                addParentDirs(dirs, underPreset);
            } else {
                NullOuroboros.LOGGER.warn("Skipping unsupported terminus preset file {}", entry.getValue());
                addParentDirs(dirs, underPreset);
            }
        }

        materialize(fs, dirs, textFiles);
        return fs;
    }

    private static TerminusFileSystem buildPresetFromDisk(Path presetDir) throws IOException {
        TerminusFileSystem fs = new TerminusFileSystem();
        Set<String> dirs = new HashSet<>();
        Map<String, String> textFiles = new HashMap<>();
        try (Stream<Path> walk = Files.walk(presetDir)) {
            for (Path path : walk.toList()) {
                if (path.equals(presetDir)) {
                    continue;
                }
                Path relative = presetDir.relativize(path);
                String dosPath = toDosPath(relative);
                if (Files.isDirectory(path)) {
                    dirs.add(dosPath);
                } else if (Files.isRegularFile(path)) {
                    String fileName = path.getFileName().toString();
                    if (fileName.toLowerCase().endsWith(".txt")) {
                        textFiles.put(dosPath, normalizeNewlines(Files.readString(path, StandardCharsets.UTF_8)));
                    } else {
                        NullOuroboros.LOGGER.warn("Skipping unsupported terminus preset file {}", path);
                    }
                    if (relative.getParent() != null) {
                        dirs.add(toDosPath(relative.getParent()));
                        addParentDirs(dirs, relative.getParent().toString().replace('\\', '/'));
                    }
                }
            }
        }
        materialize(fs, dirs, textFiles);
        return fs;
    }

    private static String normalizeNewlines(String content) {
        if (content == null || content.isEmpty()) {
            return content;
        }
        return content.replace("\r\n", "\n").replace("\r", "\n");
    }

    private static String toDosPath(Path relative) {
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < relative.getNameCount(); i++) {
            if (i > 0) {
                builder.append('\\');
            }
            builder.append(relative.getName(i));
        }
        return builder.toString();
    }

    private static void addParentDirs(Set<String> dirs, String underPresetSlash) {
        String normalized = underPresetSlash.replace('\\', '/');
        int idx = normalized.lastIndexOf('/');
        while (idx > 0) {
            dirs.add(normalized.substring(0, idx).replace('/', '\\'));
            normalized = normalized.substring(0, idx);
            idx = normalized.lastIndexOf('/');
        }
        if (!normalized.isEmpty() && !normalized.contains(".")) {
            dirs.add(normalized.replace('/', '\\'));
        }
    }

    private static void materialize(TerminusFileSystem fs, Set<String> dirs, Map<String, String> textFiles) {
        List<String> orderedDirs = new ArrayList<>(dirs);
        orderedDirs.sort(Comparator.comparingInt(a -> (int) a.chars().filter(ch -> ch == '\\').count()));
        for (String dirPath : orderedDirs) {
            ensureDirectory(fs, dirPath);
        }
        for (Map.Entry<String, String> file : textFiles.entrySet()) {
            String path = file.getKey();
            int last = path.lastIndexOf('\\');
            if (last >= 0) {
                ensureDirectory(fs, path.substring(0, last));
            }
            try {
                fs.createTextFile(fs.getRootPrefix() + path, file.getValue());
            } catch (FileSystemException e) {
                NullOuroboros.LOGGER.warn("Failed to create terminus preset file {}: {}", path, e.getMessage());
            }
        }
        fs.clearDirty();
    }

    private static void ensureDirectory(TerminusFileSystem fs, String relativeDosPath) {
        if (relativeDosPath == null || relativeDosPath.isEmpty()) {
            return;
        }
        String[] parts = relativeDosPath.split("\\\\");
        TerminusDirectory current = fs.getRoot();
        for (String part : parts) {
            if (part.isEmpty()) {
                continue;
            }
            TerminusNode child = current.getChild(part);
            if (child instanceof TerminusDirectory directory) {
                current = directory;
            } else if (child == null) {
                TerminusDirectory created = new TerminusDirectory(part, current);
                current.addChild(created);
                current = created;
            } else {
                return;
            }
        }
    }
}
