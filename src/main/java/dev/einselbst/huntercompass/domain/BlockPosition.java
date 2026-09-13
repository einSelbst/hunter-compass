package dev.einselbst.huntercompass.domain;

public record BlockPosition(int x, int y, int z) {
    public static BlockPosition from(double x, double y, double z) {
        return new BlockPosition(floor(x), floor(y), floor(z));
    }

    private static int floor(double value) {
        return (int) Math.floor(value);
    }
}
