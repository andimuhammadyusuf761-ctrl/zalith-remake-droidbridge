package ca.dnamobile.javalauncher.skin;

/** Minecraft skin model types. */
public enum SkinModelType {
    CLASSIC("default"),
    SLIM("slim");

    public final String id;

    SkinModelType(String id) { this.id = id; }

    public static SkinModelType fromId(String id) {
        if ("slim".equalsIgnoreCase(id)) return SLIM;
        return CLASSIC;
    }
}
