package com.polidea.rxandroidble.helpers;


import android.support.annotation.NonNull;
import java.nio.ByteBuffer;
import rx.Observable;
import rx.Observer;
import rx.functions.Action2;
import rx.functions.Func0;
import rx.observables.SyncOnSubscribe;

/**
 * A helper class for reactive batching of long byte arrays.
 */
public class ByteArrayBatchObservable extends Observable<byte[]> {

    /**
     * Constructor
     *
     * @param bytes        the byte array that is needed to be split - must not be null
     * @param maxBatchSize maximum size of an emitted byte[] batch - must be bigger than 0
     */
    public ByteArrayBatchObservable(@NonNull final byte[] bytes, final int maxBatchSize) {
        super(createSyncOnSubscribe(copy(bytes), maxBatchSize));
        if (maxBatchSize <= 0) {
            throw new IllegalArgumentException("maxBatchSize must be >0 but found: " + maxBatchSize);
        }
    }

    @NonNull
    private static SyncOnSubscribe<ByteBuffer, byte[]> createSyncOnSubscribe(final byte[] bytes, final int maxBatchSize) {
        return SyncOnSubscribe.createSingleState(
                new Func0<ByteBuffer>() {
                    @Override
                    public ByteBuffer call() {
                        return ByteBuffer.wrap(bytes);
                    }
                },
                new Action2<ByteBuffer, Observer<? super byte[]>>() {
                    @Override
                    public void call(ByteBuffer byteBuffer, Observer<? super byte[]> observer) {
                        int nextBatchSize = Math.min(byteBuffer.remaining(), maxBatchSize);
                        if (nextBatchSize == 0) {
                            observer.onCompleted();
                            return;
                        }
                        final byte[] nextBatch = new byte[nextBatchSize];
                        byteBuffer.get(nextBatch);
                        observer.onNext(nextBatch);
                    }
                }
        );
    }

    @NonNull
    private static byte[] copy(@NonNull final byte[] bytes) {
        final int length = bytes.length;
        final byte[] bytesCopy = new byte[length];
        System.arraycopy(bytes, 0, bytesCopy, 0, length);
        return bytesCopy;
    }
}
