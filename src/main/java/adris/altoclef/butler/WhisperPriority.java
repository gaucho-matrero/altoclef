package adris.altoclef.butler;

public enum WhisperPriority {
    ASAP(3),
    TIMELY(2),
    OPTIONAL(1),
    UNAUTHORIZED(0);

    private final int _importance;
    WhisperPriority(int importance) {
        _importance = importance;
    }

    public int getImportance() {
        return _importance;
    }
}
