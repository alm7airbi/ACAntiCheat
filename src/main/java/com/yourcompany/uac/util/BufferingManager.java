package com.yourcompany.uac.util;

import java.util.ArrayDeque;
import java.util.Deque;

/**
 * Simple rolling buffer for events to support burst-tolerant detection.
 */
public class BufferingManager<T> {

    private final int maxEvents;
    private final Deque<T> events;

    public BufferingManager(int maxEvents) {
        this.maxEvents = maxEvents;
        this.events = new ArrayDeque<>(maxEvents);
    }

    public void add(T event) {
        if (events.size() >= maxEvents) {
            events.removeFirst();
        }
        events.addLast(event);
    }

    public int size() {
        return events.size();
    }

    public Deque<T> getEvents() {
        return events;
    }
}
