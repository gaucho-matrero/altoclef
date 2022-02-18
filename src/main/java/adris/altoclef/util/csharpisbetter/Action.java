package adris.altoclef.util.csharpisbetter;

import java.util.ArrayList;
import java.util.List;

public class Action<T> {

    private final List<ActionListener<T>> _consumers = new ArrayList<>();
    private final List<ActionListener<T>> _toAdd = new ArrayList<>();
    private final List<ActionListener<T>> _toRemove = new ArrayList<>();
    private boolean _lock = false;

    public void addListener(ActionListener<T> listener) {
        if (_lock) {
            _toAdd.add(listener);
        } else {
            _consumers.add(listener);
        }
    }

    public void removeListener(ActionListener<T> listener) {
        // TODO: Maybe use a linked list with stored nodes?
        if (_lock) {
            _toRemove.add(listener);
        } else {
            _consumers.remove(listener);

        }
    }

    public void invoke(T value) {
        _lock = true;
        _consumers.addAll(_toAdd);
        _toAdd.clear();
        for (ActionListener<T> consumer : _consumers) {
            consumer.invoke(value);
        }
        _lock = false;

        // If we made modifications while iterating, do the thing.

        for (ActionListener<T> consumer : _toRemove) {
            _consumers.remove(consumer);
        }
        _toRemove.clear();
    }

    public void invoke() {
        invoke(null);
    }

}
