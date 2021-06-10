package com.oracle.graalvm.codeonline;

import java.util.AbstractList;
import java.util.function.Function;
import net.java.html.lib.dom.NodeListOf;

/**
 * Wraps a {@link NodeListOf} in a standard Java list interface.
 * @param <T> the type of list elements
 */
public final class NodeListWrapper<T> extends AbstractList<T> {
    private final NodeListOf<?> wrapped;
    private final Function<Object, T> castToT;

    /**
     * Constructs a new wrapper backed by the given node list.
     * @param wrapped the list to be wrapped
     * @param $as should be {@code T::$as}
     */
    public NodeListWrapper(NodeListOf<T> wrapped, Function<Object, T> $as) {
        this.wrapped = wrapped;
        this.castToT = $as;
    }

    @Override
    public T get(int index) {
        // Due to type erasure, the compiler will insert a `(T) ...` cast to call sites of this method.
        // We cannot cast JavaScript objects using Java casts. Use `T.$as(...)` instead.
        return castToT.apply(wrapped.item(index));
    }

    @Override
    public int size() {
        return wrapped.length().intValue();
    }
}
