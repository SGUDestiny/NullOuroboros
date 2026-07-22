package destiny.null_ouroboros.server.datagen;

public record StoneBlockset(
        String baseName,
        String textureKey,
        String recipeSubPath,
        String fullBlockMaterialItem,
        int miningTier,
        boolean isEmissive,
        boolean includeToStone,
        boolean includeToCobblestone
) implements StoneFamilyBlockset {
    @Override
    public String variantStem() {
        return baseName;
    }
}
