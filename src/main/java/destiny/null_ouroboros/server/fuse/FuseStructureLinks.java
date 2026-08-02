package destiny.null_ouroboros.server.fuse;

import destiny.null_ouroboros.NullOuroboros;
import destiny.null_ouroboros.server.item.FuseItem;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.nbt.Tag;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;

public final class FuseStructureLinks {
    private static final String FUSE_ID = NullOuroboros.MODID + ":fuse";
    private static final String ITEMS = "Items";
    private static final String INVENTORY = "Inventory";
    private static final String TAG = "tag";
    private static final String ID = "id";
    private static final String BLOCK_ENTITY_TAG = "BlockEntityTag";

    private FuseStructureLinks() {
    }

    public static void toRelative(CompoundTag beNbt, BlockPos bePos) {
        if (beNbt == null || bePos == null) {
            return;
        }
        processCompound(beNbt, bePos, true, Mirror.NONE, Rotation.NONE);
    }

    public static void toAbsolute(CompoundTag beNbt, BlockPos bePos, Mirror mirror, Rotation rotation) {
        if (beNbt == null || bePos == null) {
            return;
        }
        Mirror safeMirror = mirror != null ? mirror : Mirror.NONE;
        Rotation safeRotation = rotation != null ? rotation : Rotation.NONE;
        processCompound(beNbt, bePos, false, safeMirror, safeRotation);
    }

    private static void processCompound(CompoundTag tag, BlockPos bePos, boolean toRelative, Mirror mirror, Rotation rotation) {
        if (tag.contains(ITEMS, Tag.TAG_LIST)) {
            processItemList(tag.getList(ITEMS, Tag.TAG_COMPOUND), bePos, toRelative, mirror, rotation);
        }
        if (tag.contains(INVENTORY, Tag.TAG_COMPOUND)) {
            CompoundTag inventory = tag.getCompound(INVENTORY);
            if (inventory.contains(ITEMS, Tag.TAG_LIST)) {
                processItemList(inventory.getList(ITEMS, Tag.TAG_COMPOUND), bePos, toRelative, mirror, rotation);
            }
        }
    }

    private static void processItemList(ListTag items, BlockPos bePos, boolean toRelative, Mirror mirror, Rotation rotation) {
        for (int i = 0; i < items.size(); i++) {
            processItem(items.getCompound(i), bePos, toRelative, mirror, rotation);
        }
    }

    private static void processItem(CompoundTag item, BlockPos bePos, boolean toRelative, Mirror mirror, Rotation rotation) {
        if (item.contains(TAG, Tag.TAG_COMPOUND)) {
            CompoundTag stackTag = item.getCompound(TAG);
            if (stackTag.contains(BLOCK_ENTITY_TAG, Tag.TAG_COMPOUND)) {
                processCompound(stackTag.getCompound(BLOCK_ENTITY_TAG), bePos, toRelative, mirror, rotation);
            }
        }

        if (!isFuseItem(item) || !item.contains(TAG, Tag.TAG_COMPOUND)) {
            return;
        }

        CompoundTag stackTag = item.getCompound(TAG);
        if (toRelative) {
            convertToRelative(stackTag, bePos);
        } else {
            convertToAbsolute(stackTag, bePos, mirror, rotation);
        }
    }

    private static boolean isFuseItem(CompoundTag item) {
        return item.contains(ID, Tag.TAG_STRING) && FUSE_ID.equals(item.getString(ID));
    }

    private static void convertToRelative(CompoundTag stackTag, BlockPos bePos) {
        if (!stackTag.contains(FuseItem.LINKED_POS, Tag.TAG_COMPOUND) || stackTag.getBoolean(FuseItem.LINKED_RELATIVE)) {
            return;
        }
        BlockPos absolute = NbtUtils.readBlockPos(stackTag.getCompound(FuseItem.LINKED_POS));
        BlockPos relative = absolute.subtract(bePos);
        stackTag.put(FuseItem.LINKED_POS, NbtUtils.writeBlockPos(relative));
        stackTag.putBoolean(FuseItem.LINKED_RELATIVE, true);
    }

    private static void convertToAbsolute(CompoundTag stackTag, BlockPos bePos, Mirror mirror, Rotation rotation) {
        if (!stackTag.getBoolean(FuseItem.LINKED_RELATIVE)) {
            return;
        }
        if (!stackTag.contains(FuseItem.LINKED_POS, Tag.TAG_COMPOUND)) {
            stackTag.remove(FuseItem.LINKED_RELATIVE);
            return;
        }
        BlockPos relative = NbtUtils.readBlockPos(stackTag.getCompound(FuseItem.LINKED_POS));
        BlockPos transformed = StructureTemplate.transform(relative, mirror, rotation, BlockPos.ZERO);
        BlockPos absolute = bePos.offset(transformed);
        stackTag.put(FuseItem.LINKED_POS, NbtUtils.writeBlockPos(absolute));
        stackTag.remove(FuseItem.LINKED_RELATIVE);
    }
}
