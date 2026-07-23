package destiny.null_ouroboros.server.datagen;

public record PluralBlockset(
        String baseName,
        String textureKey,
        String recipeSubPath,
        String fullBlockMaterialItem,
        int miningTier,
        boolean isEmissive,
        boolean includeToStone,
        boolean includeToCobblestone
) implements StoneFamilyBlockset {
    public PluralBlockset {
        if (!baseName.endsWith("s")) {
            throw new IllegalArgumentException("PluralBlockset baseName must end with s: " + baseName);
        }
    }

    @Override
    public String variantStem() {
        return baseName.substring(0, baseName.length() - 1);
    }
}
