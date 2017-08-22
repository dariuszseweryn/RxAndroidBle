package com.polidea.rxandroidble.internal;


/**
 * The class representing a priority with which an {@link QueueOperation} should be executed.
 * Used in @Override definedPriority()
 */
public class Priority {

    public static final Priority HIGH = new Priority(100);
    public static final Priority NORMAL = new Priority(50);
    public static final Priority LOW = new Priority(0);
    final int priority;

    private Priority(int priority) {

        this.priority = priority;
    }
}
