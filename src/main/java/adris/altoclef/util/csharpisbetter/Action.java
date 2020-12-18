package adris.altoclef.util.csharpisbetter;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public class Action<T> {

    private List<Consumer<T>> _consumers = new ArrayList<>();

    private boolean _lock = false;

    private List<Consumer<T>> _toAdd = new ArrayList<>();
    private List<Consumer<T>> _toRemove = new ArrayList<>();

    public void addListener(Consumer<T> listener) {
        if (_lock) {
            _toAdd.add(listener);
        } else {
            _consumers.add(listener);
        }
    }

    public void removeListener(Consumer<T> listener) {
        // TODO: Maybe use a linked list with stored nodes?
        if (_lock) {
            _toRemove.add(listener);
        } else {
            _consumers.remove(listener);
        }
    }

    public void invoke(T value) {
        _lock = true;
        for(Consumer<T> consumer : _consumers) {
            consumer.accept(value);
        }

        // If we made modifications while iterating, do the thing.

        _consumers.addAll(_toAdd);
        for (Consumer<T> consumer : _toRemove) {
            _consumers.remove(consumer);
        }

        _lock = false;
    }
}
