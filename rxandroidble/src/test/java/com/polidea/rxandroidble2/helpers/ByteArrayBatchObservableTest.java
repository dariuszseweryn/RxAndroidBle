package com.polidea.rxandroidble2.helpers;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.util.List;

import io.reactivex.subscribers.TestSubscriber;

class ByteArrayBatchObservableTest {

    private ByteArrayBatchObservable objectUnderTest;

    private void prepareObjectUnderTest(byte[] bytes, int maxBatchSize) {
        objectUnderTest = new ByteArrayBatchObservable(bytes, maxBatchSize);
    }

    @ParameterizedTest
    @CsvSource({
            "10, 1, 10",
            "0, 10, 0",
            "3999, 10, 400",
            "32145, 27, 1191"
    })
    void shouldEmitCorrectNumberOfBatchesAndComplete(int byteArraySize, int maxBatchSize, int valuesCount) {
        byte[] byteArray = new byte[byteArraySize];
        prepareObjectUnderTest(byteArray, maxBatchSize);

        TestSubscriber<byte[]> testObserver = objectUnderTest.test();

        assertEquals(valuesCount, testObserver.valueCount());
        testObserver.assertComplete();
    }

    @ParameterizedTest
    @CsvSource({
            "10, 1",
            "3999, 10",
            "32145, 27"
    })
    void shouldEmitAllButLastBatchesOfCorrectSize(int byteArraySize, int maxBatchSize) {
        byte[] byteArray = new byte[byteArraySize];
        prepareObjectUnderTest(byteArray, maxBatchSize);

        TestSubscriber<byte[]> testObserver = objectUnderTest.test();

        List<byte[]> values = testObserver.values();
        assertTrue(values.size() > 0);
        for (int i = 0; i < values.size(); i++) {
            byte[] value = values.get(i);
            assertTrue(
                    value.length <= maxBatchSize,
                    "Batch " + i + " has length=" + value.length + ", should be no more than " + maxBatchSize);
        }
    }

    @ParameterizedTest
    @CsvSource({
            "10, 1, 1",
            "3999, 10, 9",
            "32145, 27, 15"
    })
    void shouldEmitLastBatchesOfCorrectSize(int byteArraySize, int maxBatchSize, int lastBatchSize) {
        byte[] byteArray = new byte[byteArraySize];
        prepareObjectUnderTest(byteArray, maxBatchSize);

        TestSubscriber<byte[]> testObserver = objectUnderTest.test();

        assertEquals(lastBatchSize, testObserver.values().get(testObserver.valueCount() - 1).length);
    }

    @Test
    void shouldEmitProperBatches() {
        prepareObjectUnderTest(arrayFrom(0, 1, 2, 3, 4, 5, 6, 7, 8, 9), 3);

        TestSubscriber<byte[]> testObserver = objectUnderTest.test();

        assertArrayEquals(arrayFrom(0, 1, 2), testObserver.values().get(0));
        assertArrayEquals(arrayFrom(3, 4, 5), testObserver.values().get(1));
        assertArrayEquals(arrayFrom(6, 7, 8), testObserver.values().get(2));
        assertArrayEquals(arrayFrom(9), testObserver.values().get(3));
    }

    @Test
    void shouldEmitValuesFromStateOfByteArrayAtCreation() {
        byte[] expectedBytes = arrayFrom(0);
        prepareObjectUnderTest(expectedBytes, 3);
        expectedBytes[0] = 1;

        TestSubscriber<byte[]> testObserver = objectUnderTest.test();

        assertEquals(1, testObserver.valueCount());
        assertArrayEquals(expectedBytes, testObserver.values().get(0));
    }

    @ParameterizedTest
    @CsvSource({
            "0",
            "-1",
            "-2147483648"
    })
    void shouldThrowIllegalArgumentExceptionWhenCalledWithInvalidMaxBatchSize(int maxBatchSize) {
        assertThrows(IllegalArgumentException.class, () -> prepareObjectUnderTest(new byte[0], maxBatchSize));
    }

    private static byte[] arrayFrom(int... ints) {
        byte[] bytes = new byte[ints.length];
        for (int i = 0; i < ints.length; i++) {
            bytes[i] = (byte) ints[i];
        }
        return bytes;
    }
}
