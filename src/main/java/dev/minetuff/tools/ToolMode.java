package dev.minetuff.tools;

public enum ToolMode {
    PICKAXE(1, "Pickaxe"),
    HAMMER(25, "Hammer"),
    DRILL(50, "Drill"),
    LASER(100, "Laser");

    private final int requiredLevel;
    private final String displayName;

    ToolMode(int requiredLevel, String displayName) {
        this.requiredLevel = requiredLevel;
        this.displayName = displayName;
    }

    public int requiredLevel() { return requiredLevel; }
    public String displayName() { return displayName; }
}
