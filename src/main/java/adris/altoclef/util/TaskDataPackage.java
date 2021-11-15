package adris.altoclef.util;

import net.minecraft.util.math.BlockPos;

public class TaskDataPackage {
    public enum ExtraData {
        REMAINING_TO_MINE,
        PROGRESS_DATA,
        LOCKED_CHEST
    }

    public enum Feedback {
        SUCCESS,
        CONTAINER_FULL,
        NOT_ENOUGH_ITEMS,
        NONE
    }

    private boolean finished;
    private boolean paused;
    private Store store;
    private Feedback feedback;

    public TaskDataPackage() {
        finished = false;
        paused = false;
        feedback = Feedback.NONE;
        store = new Store();
    }

    public void setFeedback(final Feedback reason) {
        this.feedback = reason;
    }

    public void setFinished(boolean finished) {
        this.finished = finished;
    }

    public Feedback getFeedback() {
        return this.feedback;
    }

    public boolean isFinished() {
        return finished;
    }

    public void putExtra(final ExtraData extra, final Object obj) {
        store.setAttribute(extra.name(), obj);
    }

    public final <T> T getExtra(final ExtraData key, final Class<T> type) {
        return store.fromStorage(key.name(), type);
    }

    public boolean isPaused() {
        return paused;
    }

    public void setPaused(final boolean paused) {this.paused = paused;}
}
