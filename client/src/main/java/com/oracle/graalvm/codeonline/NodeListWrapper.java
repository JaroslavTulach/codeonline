/*
 * Copyright 2021 Oracle and/or its affiliates
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

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
