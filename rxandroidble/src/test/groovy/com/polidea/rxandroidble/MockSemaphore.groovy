package com.polidea.rxandroidble

import com.polidea.rxandroidble.internal.RadioAwaitReleaseInterface
import com.polidea.rxandroidble.internal.RadioReleaseInterface

class MockSemaphore implements RadioReleaseInterface, RadioAwaitReleaseInterface {
    int permits = 0;

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
        permits <= 0;
    }
}
