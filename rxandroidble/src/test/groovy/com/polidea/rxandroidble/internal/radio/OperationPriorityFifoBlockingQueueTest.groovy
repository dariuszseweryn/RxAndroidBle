package com.polidea.rxandroidble.internal.radio

import com.polidea.rxandroidble.MockOperation
import com.polidea.rxandroidble.internal.RxBleRadioOperation
import spock.lang.Specification
import spock.lang.Unroll

public class OperationPriorityFifoBlockingQueueTest extends Specification {

    def static LOW = RxBleRadioOperation.Priority.LOW
    def static NORMAL = RxBleRadioOperation.Priority.NORMAL
    def static HIGH = RxBleRadioOperation.Priority.HIGH
    def static lowPriority0 = MockOperation.mockOperation(LOW)
    def static lowPriority1 = MockOperation.mockOperation(LOW)
    def static lowPriority2 = MockOperation.mockOperation(LOW)
    def static normalPriority0 = MockOperation.mockOperation(NORMAL)
    def static normalPriority1 = MockOperation.mockOperation(NORMAL)
    def static normalPriority2 = MockOperation.mockOperation(NORMAL)
    def static highPriority0 = MockOperation.mockOperation(HIGH)
    def static highPriority1 = MockOperation.mockOperation(HIGH)
    def static highPriority2 = MockOperation.mockOperation(HIGH)
    OperationPriorityFifoBlockingQueue objectUnderTest

    def setup() {
        objectUnderTest = new OperationPriorityFifoBlockingQueue()
    }

    @Unroll
    def "should return operations in proper order #id"() {

        given:
        for (RxBleRadioOperation operation : entryOrder) {
            objectUnderTest.add(operation)
        }

        expect:
        dumpQueue() == properOrder

        where:
        id | entryOrder                                                                                  | properOrder
        0  | [highPriority2, highPriority0, highPriority1]                                               | [highPriority2, highPriority0, highPriority1]
        1  | [lowPriority2, lowPriority0, lowPriority1]                                                  | [lowPriority2, lowPriority0, lowPriority1]
        2  | [normalPriority2, normalPriority0, normalPriority1]                                         | [normalPriority2, normalPriority0, normalPriority1]
        3  | [highPriority0, normalPriority0, lowPriority0]                                              | [highPriority0, normalPriority0, lowPriority0]
        4  | [highPriority0, lowPriority0, normalPriority0]                                              | [highPriority0, normalPriority0, lowPriority0]
        5  | [normalPriority0, highPriority0, lowPriority0]                                              | [highPriority0, normalPriority0, lowPriority0]
        6  | [normalPriority0, lowPriority0, highPriority0]                                              | [highPriority0, normalPriority0, lowPriority0]
        7  | [lowPriority0, highPriority0, normalPriority0]                                              | [highPriority0, normalPriority0, lowPriority0]
        8  | [lowPriority0, normalPriority0, highPriority0]                                              | [highPriority0, normalPriority0, lowPriority0]
        9  | [lowPriority0, lowPriority1, lowPriority2, highPriority0, normalPriority0, normalPriority1] | [highPriority0, normalPriority0, normalPriority1, lowPriority0, lowPriority1, lowPriority2]
        10 | [lowPriority0, highPriority0, normalPriority0, lowPriority1, lowPriority2, normalPriority1] | [highPriority0, normalPriority0, normalPriority1, lowPriority0, lowPriority1, lowPriority2]
        11 | [highPriority0, lowPriority0, lowPriority1, lowPriority2, normalPriority0, normalPriority1] | [highPriority0, normalPriority0, normalPriority1, lowPriority0, lowPriority1, lowPriority2]
        12 | [lowPriority0, normalPriority0, lowPriority1, lowPriority2, highPriority0, normalPriority1] | [highPriority0, normalPriority0, normalPriority1, lowPriority0, lowPriority1, lowPriority2]
        13 | [lowPriority0, normalPriority0, lowPriority1, lowPriority2, highPriority0, normalPriority1] | [highPriority0, normalPriority0, normalPriority1, lowPriority0, lowPriority1, lowPriority2]
    }

    @Unroll
    def "should not return item if it was removed"() {
        given:
        addedItems.each {
            objectUnderTest.add(it)
        }

        when:
        removedItems.each {
            objectUnderTest.remove(it)
        }

        then:
        dumpQueue() == expectedItems

        where:
        addedItems                         | removedItems                       | expectedItems
        [normalPriority0]                  | [normalPriority0]                  | []
        [normalPriority0]                  | []                                 | [normalPriority0]
        [normalPriority0, normalPriority1] | [normalPriority0]                  | [normalPriority1]
        [normalPriority0, normalPriority1] | [normalPriority1, normalPriority0] | []
    }

    private List<RxBleRadioOperation> dumpQueue() {
        def operationsQueueList = new ArrayList<RxBleRadioOperation>()

        while (!objectUnderTest.isEmpty()) {
            operationsQueueList.add(objectUnderTest.take())
        }

        return operationsQueueList
    }
}
