package adris.altoclef.util.csharpisbetter;


import java.util.ArrayList;
import java.util.List;


public class Action<T> {
    
    private final List<ActionListener<T>> consumers = new ArrayList<>();
    private final List<ActionListener<T>> toAdd = new ArrayList<>();
    private final List<ActionListener<T>> toRemove = new ArrayList<>();
    private boolean lock;
    
    public void addListener(ActionListener<T> listener) {
        if (lock) {
            toAdd.add(listener);
        } else {
            consumers.add(listener);
        }
    }
    
    public void removeListener(ActionListener<T> listener) {
        // TODO: Maybe use a linked list with stored nodes?
        if (lock) {
            toRemove.add(listener);
        } else {
            consumers.remove(listener);
            
        }
    }
    
    public void invoke(T value) {
        lock = true;
        consumers.addAll(toAdd);
        toAdd.clear();
        for (ActionListener<T> consumer : consumers) {
            consumer.invoke(value);
        }
        lock = false;
        
        // If we made modifications while iterating, do the thing.
        
        for (ActionListener<T> consumer : toRemove) {
            consumers.remove(consumer);
        }
        toRemove.clear();
    }
    
    public void invoke() {
        invoke(null);
    }
    
}
