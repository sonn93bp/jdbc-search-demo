package com.example.demo.function;

@FunctionalInterface
public interface QuadFunction<T, U, V, W, R> {
    /**
     * Applies this function to the given arguments.
     *
     * @param t the first function argument
     * @param u the second function argument
     * @param v the tree function argument
     * @param w the four function argument
     * @return the function result
     */
    R apply(T t, U u, V v, W w);
}
