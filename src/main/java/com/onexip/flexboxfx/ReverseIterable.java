/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.onexip.flexboxfx;

import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;

/**
 *
 * @author adrian
 */
class ReverseIterable<T> implements Iterable<T> {

    private final List<T> original;

    public ReverseIterable(List<T> original) {
        this.original = original;
    }

    @Override
    public Iterator<T> iterator() {
        final ListIterator<T> i = original.listIterator(original.size());
        return new Iterator<T>() {
            @Override
            public boolean hasNext() {
                return i.hasPrevious();
            }
            @Override
            public T next() {
                return i.previous();
            }
            @Override
            public void remove() {
                i.remove();
            }
        };
    }

    public static <T> ReverseIterable<T> reverse(List<T> original) {
        return new ReverseIterable<>(original);
    }
}
