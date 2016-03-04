package com.polidea.rxandroidble

import java.util.concurrent.Semaphore

class MockSemaphore extends Semaphore {
    int permits = 0;

    MockSemaphore() {
        super(0)
    }

    @Override
    void acquire() throws InterruptedException {
        permits++
    }

    @Override
    void release() {
        permits--
    }

    boolean isReleased() {
        permits == 0;
    }
}
