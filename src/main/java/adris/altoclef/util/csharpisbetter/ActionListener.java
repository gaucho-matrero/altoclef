package adris.altoclef.util.csharpisbetter;

import java.util.function.Consumer;

public class ActionListener<T> {

    private final Consumer<T> _onInvoke;

    public ActionListener(Consumer<T> onInvoke) {
        _onInvoke = onInvoke;
    }

    void invoke(T value) {
        _onInvoke.accept(value);
    }
}
