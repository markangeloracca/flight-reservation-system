package ca.ucalgary.frs.ui.components;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

/** Combo-box item with a display label; a {@code null} value means "any". */
public record Choice<T>(T value, String label) {
    public static <T> Choice<T> any(String label) {
        return new Choice<>(null, label);
    }

    public static <T> List<Choice<T>> list(String anyLabel, List<T> values, Function<T, String> label) {
        List<Choice<T>> out = new ArrayList<>();
        if (anyLabel != null) {
            out.add(any(anyLabel));
        }
        for (T v : values) {
            out.add(new Choice<>(v, label.apply(v)));
        }
        return out;
    }

    @Override
    public String toString() {
        return label;
    }
}
