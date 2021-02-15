package com.polidea.rxandroidble3

import com.polidea.rxandroidble3.internal.serialization.QueueAwaitReleaseInterface
import com.polidea.rxandroidble3.internal.serialization.QueueReleaseInterface

class MockSemaphore implements QueueReleaseInterface, QueueAwaitReleaseInterface {
    int permits = 0

    MockSemaphore() {
    }

    @Override
    void awaitRelease() throws InterruptedException {
        permits++
    }

    @Override
    void release() {
        permits--
    }

    boolean isReleased() {
        permits <= 0
    }
}
