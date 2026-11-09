package dk.worktrail.app;

/** Pure Java work-balance heatmap palette, usable in light and dark themes. */
public final class HeatmapColors {
    public static final int GREEN = 0xFF188C68;
    public static final int BLUE = 0xFF285FB8;
    public static final int RED = 0xFFBE4054;
    private HeatmapColors() {}

    public static int tileColor(int workedMinutes, int targetMinutes, boolean configured,
                                boolean future, boolean dark) {
        if (future || !configured || (targetMinutes == 0 && workedMinutes == 0))
            return dark ? 0xFF26364B : 0xFFE9EFF3;
        int difference = workedMinutes - targetMinutes;
        if (Math.abs(difference) <= 15) return GREEN;
        float intensity = Math.min(1f, (Math.abs(difference) - 15f) /
                Math.max(90f, targetMinutes * 0.25f));
        return blend(GREEN, difference < 0 ? BLUE : RED, intensity);
    }

    public static int blend(int from, int to, float fraction) {
        float p = Math.max(0f, Math.min(1f, fraction));
        int r = Math.round(((from >>> 16) & 255) * (1 - p) + ((to >>> 16) & 255) * p);
        int g = Math.round(((from >>> 8) & 255) * (1 - p) + ((to >>> 8) & 255) * p);
        int b = Math.round((from & 255) * (1 - p) + (to & 255) * p);
        return 0xFF000000 | (r << 16) | (g << 8) | b;
    }

    public static int readableText(int background) {
        int r = (background >>> 16) & 255;
        int g = (background >>> 8) & 255;
        int b = background & 255;
        return (r * 299 + g * 587 + b * 114) / 1000 >= 150 ? 0xFF142235 : 0xFFFFFFFF;
    }
}
