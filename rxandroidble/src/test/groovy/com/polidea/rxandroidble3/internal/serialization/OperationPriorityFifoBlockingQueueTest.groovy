package com.polidea.rxandroidble2.internal.serialization

import com.polidea.rxandroidble2.MockOperation
import com.polidea.rxandroidble2.internal.Priority
import com.polidea.rxandroidble2.internal.operations.Operation
import spock.lang.Shared
import spock.lang.Specification
import spock.lang.Unroll

public class OperationPriorityFifoBlockingQueueTest extends Specification {

    def static LOW = Priority.LOW
    def static NORMAL = Priority.NORMAL
    def static HIGH = Priority.HIGH
    def static lowPriority0 = MockOperation.mockOperation(LOW)
    def static lowPriority1 = MockOperation.mockOperation(LOW)
    def static lowPriority2 = MockOperation.mockOperation(LOW)
    def static normalPriority0 = MockOperation.mockOperation(NORMAL)
    def static normalPriority1 = MockOperation.mockOperation(NORMAL)
    def static normalPriority2 = MockOperation.mockOperation(NORMAL)
    def static highPriority0 = MockOperation.mockOperation(HIGH)
    def static highPriority1 = MockOperation.mockOperation(HIGH)
    def static highPriority2 = MockOperation.mockOperation(HIGH)

    @Shared def static entryOperation0 = new FIFORunnableEntry(MockOperation.mockOperation(NORMAL), null)
    @Shared def static entryOperation1 = new FIFORunnableEntry(MockOperation.mockOperation(NORMAL), null)
    OperationPriorityFifoBlockingQueue objectUnderTest

    def setup() {
        objectUnderTest = new OperationPriorityFifoBlockingQueue()
    }

    @Unroll
    def "should return operations in proper order #id"() {

        given:
        for (Operation operation : entryOrder) {
            objectUnderTest.add(new FIFORunnableEntry(operation, null))
        }

        expect:
        dumpQueueOperations() == properOrder

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
        dumpQueueEntries() == expectedItems

        where:
        addedItems                         | removedItems                       | expectedItems
        [entryOperation0]                  | [entryOperation0]                  | []
        [entryOperation0]                  | []                                 | [entryOperation0]
        [entryOperation0, entryOperation1] | [entryOperation0]                  | [entryOperation1]
        [entryOperation0, entryOperation1] | [entryOperation1, entryOperation0] | []
    }

    private List<Operation> dumpQueueOperations() {
        def operationsQueueList = new ArrayList<Operation>()

        while (!objectUnderTest.isEmpty()) {
            operationsQueueList.add(objectUnderTest.take().operation)
        }

        return operationsQueueList
    }

    private List<Operation> dumpQueueEntries() {
        def entriesQueueList = new ArrayList<FIFORunnableEntry>()

        while (!objectUnderTest.isEmpty()) {
            entriesQueueList.add(objectUnderTest.take())
        }

        return entriesQueueList
    }
}
