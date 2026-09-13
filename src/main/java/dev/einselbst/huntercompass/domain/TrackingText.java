package dev.einselbst.huntercompass.domain;

public final class TrackingText {
    private TrackingText() {
    }

    public static String format(
            String template,
            String target,
            BlockPosition position,
            String dimension,
            String direction
    ) {
        return template
                .replace("{target}", target)
                .replace("{x}", Integer.toString(position.x()))
                .replace("{y}", Integer.toString(position.y()))
                .replace("{z}", Integer.toString(position.z()))
                .replace("{dimension}", dimension)
                .replace("{direction}", direction);
    }

    public static String formatOffline(String template, String target, String offline) {
        return template.replace("{target}", target).replace("{offline}", offline);
    }
}
