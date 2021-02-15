package com.polidea.rxandroidble3.helpers

import io.reactivex.rxjava3.annotations.NonNull
import io.reactivex.rxjava3.functions.Predicate
import spock.lang.Specification
import spock.lang.Unroll

class ByteArrayBatchObservableTest extends Specification {

    ByteArrayBatchObservable objectUnderTest;

    private void prepareObjectUnderTest(byte[] bytes, int maxBatchSize) {
        objectUnderTest = new ByteArrayBatchObservable(bytes, maxBatchSize)
    }

    @Unroll
    def "should emit correct number of batches and complete"() {

        given:
        def byteArray = new byte[byteArraySize];
        prepareObjectUnderTest(byteArray, maxBatchSize)

        when:
        def testSubscriber = objectUnderTest.test()

        then:
        testSubscriber.assertValueCount(valuesCount)

        and:
        testSubscriber.assertComplete()

        where:
        byteArraySize | maxBatchSize | valuesCount
        10            | 1            | 10
        0             | 10           | 0
        3999          | 10           | 400
        32145         | 27           | 1191
    }

    @Unroll
    def "should emit all but last batches of correct size"() {

        given:
        def byteArray = new byte[byteArraySize];
        prepareObjectUnderTest(byteArray, maxBatchSize)

        when:
        def testSubscriber = objectUnderTest.test()

        then:
        testSubscriber.assertAllBatchesSmaller(maxBatchSize)

        where:
        byteArraySize | maxBatchSize
        10            | 1
        3999          | 10
        32145         | 27
    }

    @Unroll
    def "should emit last batches of correct size"() {

        given:
        def byteArray = new byte[byteArraySize];
        prepareObjectUnderTest(byteArray, maxBatchSize)

        when:
        def testSubscriber = objectUnderTest.test()

        then:
        testSubscriber.assertLastBatchesSize(lastBatchSize)

        where:
        byteArraySize | maxBatchSize | lastBatchSize
        10            | 1            | 1
        3999          | 10           | 9
        32145         | 27           | 15
    }

    def "should emit proper batches"() {

        given:
        prepareObjectUnderTest(arrayFrom(0, 1, 2, 3, 4, 5, 6, 7, 8, 9), 3)

        when:
        def testSubscriber = objectUnderTest.test()

        then:
        testSubscriber.assertValuesEquals(arrayFrom(0, 1, 2), arrayFrom(3, 4, 5), arrayFrom(6, 7, 8), arrayFrom(9))
    }

    def "should emit values from the state of the byte array at the time of creation"() {

        given:
        def expectedBytes = arrayFrom(0)
        prepareObjectUnderTest(expectedBytes, 3)
        expectedBytes[0] = 1

        when:
        def testSubscriber = objectUnderTest.test()

        then:
        testSubscriber.assertValueCount(1)
        testSubscriber.assertValue(new Predicate<byte[]>() {
            @Override
            boolean test(@NonNull byte[] bytes) throws Exception {
                return bytes == expectedBytes
            }
        })
    }

    @Unroll
    def "should throw IllegalArgumentException when called with maxBatchSize <= 0"() {

        when:
        prepareObjectUnderTest(new byte[0], maxBatchSize)

        then:
        thrown IllegalArgumentException

        where:
        maxBatchSize << [0, -1, Integer.MIN_VALUE]
    }

    private static byte[] arrayFrom(int... ints) {
        def bytes = new byte[ints.length]
        for (int i = 0; i < ints.length; i++) {
            bytes[i] = (byte) ints[i]
        }
        return bytes
    }
}