package destiny.null_ouroboros.server.terminal.template;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import destiny.null_ouroboros.NullOuroboros;
import destiny.null_ouroboros.server.terminal.filesystem.TerminusDirectory;
import destiny.null_ouroboros.server.terminal.filesystem.TerminusFileSystem;
import destiny.null_ouroboros.server.terminal.filesystem.TerminusNode;
import destiny.null_ouroboros.server.terminal.filesystem.TerminusTextFile;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;

public final class TerminusTemplateSaver {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    private TerminusTemplateSaver() {
    }

    public static boolean save(Path templatesRoot, String templateName, String presetName, int weight, TerminusFileSystem fileSystem) {
        if (templatesRoot == null || templateName == null || templateName.isBlank() || presetName == null || presetName.isBlank()) {
            return false;
        }
        if (!isSafeName(templateName) || !isSafeName(presetName)) {
            NullOuroboros.LOGGER.warn("Rejected unsafe terminus template/preset name: {} / {}", templateName, presetName);
            return false;
        }
        Path templateDir = templatesRoot.resolve(templateName);
        try {
            if (Files.exists(templateDir)) {
                deleteRecursively(templateDir);
            }
            Files.createDirectories(templateDir);
            Path presetDir = templateDir.resolve(presetName);
            Files.createDirectories(presetDir);
            writeNodeChildren(fileSystem.getRoot(), presetDir);

            JsonObject root = new JsonObject();
            JsonObject presets = new JsonObject();
            JsonObject preset = new JsonObject();
            preset.addProperty("weight", Math.max(1, weight));
            presets.add(presetName, preset);
            root.add("presets", presets);
            Files.writeString(templateDir.resolve("template.json"), GSON.toJson(root), StandardCharsets.UTF_8);
            TerminusTemplateManager.INSTANCE.reloadWorldSaveTemplates();
            return true;
        } catch (IOException e) {
            NullOuroboros.LOGGER.error("Failed to save terminus template {}", templateName, e);
            return false;
        }
    }

    private static boolean isSafeName(String name) {
        return name.matches("[A-Za-z0-9_./-]+") && !name.contains("..") && !name.startsWith("/") && !name.startsWith("\\");
    }

    private static void writeNodeChildren(TerminusDirectory directory, Path targetDir) throws IOException {
        for (TerminusNode child : directory.getChildren().values()) {
            if (child instanceof TerminusDirectory childDir) {
                Path next = targetDir.resolve(childDir.getName());
                Files.createDirectories(next);
                writeNodeChildren(childDir, next);
            } else if (child instanceof TerminusTextFile textFile) {
                Files.writeString(targetDir.resolve(textFile.getName()), textFile.getContent(), StandardCharsets.UTF_8);
            }
        }
    }

    private static void deleteRecursively(Path root) throws IOException {
        Files.walkFileTree(root, new SimpleFileVisitor<>() {
            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                Files.delete(file);
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
                Files.delete(dir);
                return FileVisitResult.CONTINUE;
            }
        });
    }
}
