package com.polidea.rxandroidble3.helpers;


import androidx.annotation.NonNull;

import org.reactivestreams.Subscriber;

import java.nio.ByteBuffer;

import io.reactivex.rxjava3.core.Emitter;
import io.reactivex.rxjava3.core.Flowable;
import io.reactivex.rxjava3.functions.Consumer;

/**
 * TODO It may be possible to introduce backpressure here.
 *
 * A helper class for reactive batching of long byte arrays.
 */
public class ByteArrayBatchObservable extends Flowable<byte[]> {

    @NonNull
    final ByteBuffer byteBuffer;
    final int maxBatchSize;

    /**
     * Constructor
     *
     * @param bytes        the byte array that is needed to be split - must not be null
     * @param maxBatchSize maximum size of an emitted byte[] batch - must be bigger than 0
     */
    public ByteArrayBatchObservable(@NonNull final byte[] bytes, final int maxBatchSize) {
        if (maxBatchSize <= 0) {
            throw new IllegalArgumentException("maxBatchSize must be > 0 but found: " + maxBatchSize);
        }

        this.byteBuffer = ByteBuffer.wrap(bytes);
        this.maxBatchSize = maxBatchSize;
    }

    @Override
    protected void subscribeActual(Subscriber<? super byte[]> subscriber) {
        Flowable.generate(new Consumer<Emitter<byte[]>>() {

            @Override
            public void accept(Emitter<byte[]> emitter) {
                int nextBatchSize = Math.min(byteBuffer.remaining(), maxBatchSize);
                if (nextBatchSize == 0) {
                    emitter.onComplete();
                    return;
                }
                final byte[] nextBatch = new byte[nextBatchSize];
                byteBuffer.get(nextBatch);
                emitter.onNext(nextBatch);
            }
        }).subscribe(subscriber);
    }
}
