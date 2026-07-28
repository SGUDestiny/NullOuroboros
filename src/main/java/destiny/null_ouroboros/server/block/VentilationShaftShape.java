package destiny.null_ouroboros.server.block;

import net.minecraft.core.Direction;
import net.minecraft.util.StringRepresentable;

import java.util.Locale;

public enum VentilationShaftShape implements StringRepresentable {
    X(Direction.EAST, Direction.WEST),
    Y(Direction.UP, Direction.DOWN),
    Z(Direction.NORTH, Direction.SOUTH),
    NORTH_EAST(Direction.NORTH, Direction.EAST),
    NORTH_WEST(Direction.NORTH, Direction.WEST),
    NORTH_UP(Direction.NORTH, Direction.UP),
    NORTH_DOWN(Direction.NORTH, Direction.DOWN),
    SOUTH_EAST(Direction.SOUTH, Direction.EAST),
    SOUTH_WEST(Direction.SOUTH, Direction.WEST),
    SOUTH_UP(Direction.SOUTH, Direction.UP),
    SOUTH_DOWN(Direction.SOUTH, Direction.DOWN),
    EAST_UP(Direction.EAST, Direction.UP),
    EAST_DOWN(Direction.EAST, Direction.DOWN),
    WEST_UP(Direction.WEST, Direction.UP),
    WEST_DOWN(Direction.WEST, Direction.DOWN);

    private final Direction first;
    private final Direction second;

    VentilationShaftShape(Direction first, Direction second) {
        this.first = first;
        this.second = second;
    }

    public Direction first() {
        return first;
    }

    public Direction second() {
        return second;
    }

    public boolean connects(Direction direction) {
        return direction == first || direction == second;
    }

    public boolean isStraight() {
        return first.getAxis() == second.getAxis();
    }

    public static VentilationShaftShape from(Direction a, Direction b) {
        if (a == b) {
            return fromAxis(a.getAxis());
        }
        Direction first = a;
        Direction second = b;
        if (first.get3DDataValue() > second.get3DDataValue()) {
            Direction tmp = first;
            first = second;
            second = tmp;
        }
        for (VentilationShaftShape shape : values()) {
            if ((shape.first == a && shape.second == b) || (shape.first == b && shape.second == a)) {
                return shape;
            }
        }
        return fromAxis(a.getAxis());
    }

    public static VentilationShaftShape fromAxis(Direction.Axis axis) {
        return switch (axis) {
            case X -> X;
            case Y -> Y;
            case Z -> Z;
        };
    }

    @Override
    public String getSerializedName() {
        return name().toLowerCase(Locale.ROOT);
    }
}
