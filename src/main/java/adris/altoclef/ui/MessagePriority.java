package adris.altoclef.ui;


public enum MessagePriority {
    ASAP(3),
    TIMELY(2),
    OPTIONAL(1),
    UNAUTHORIZED(0);

    private final int importance;

    MessagePriority(int importance) {
        this.importance = importance;
    }

    public int getImportance() {
        return importance;
    }
}
