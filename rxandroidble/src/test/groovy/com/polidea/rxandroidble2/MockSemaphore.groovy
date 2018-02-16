package com.polidea.rxandroidble2

import com.polidea.rxandroidble2.internal.serialization.QueueAwaitReleaseInterface
import com.polidea.rxandroidble2.internal.serialization.QueueReleaseInterface

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
