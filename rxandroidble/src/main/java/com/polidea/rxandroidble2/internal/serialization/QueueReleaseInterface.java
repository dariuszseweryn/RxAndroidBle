package com.polidea.rxandroidble2.internal.serialization;


/**
 * Interface used for releasing the Operation Queue so it may proceed with execution of the next operation
 *
 * @see QueueAwaitReleaseInterface
 * @see QueueSemaphore
 * @see ClientOperationQueue
 * @see ConnectionOperationQueue
 */
public interface QueueReleaseInterface {

    void release();
}
