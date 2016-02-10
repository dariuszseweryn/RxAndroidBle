package com.polidea.rxandroidble.internal

import spock.lang.Specification
import spock.lang.Unroll

public class OperationPriorityFifoBlockingQueueTest extends Specification {

    def static LOW = RxBleRadioOperation.Priority.LOW

    def static NORMAL = RxBleRadioOperation.Priority.NORMAL

    def static HIGH = RxBleRadioOperation.Priority.HIGH

    def static lowPriority0 = mockOperation(LOW)

    def static lowPriority1 = mockOperation(LOW)

    def static lowPriority2 = mockOperation(LOW)

    def static normalPriority0 = mockOperation(NORMAL)

    def static normalPriority1 = mockOperation(NORMAL)

    def static normalPriority2 = mockOperation(NORMAL)

    def static highPriority0 = mockOperation(HIGH)

    def static highPriority1 = mockOperation(HIGH)

    def static highPriority2 = mockOperation(HIGH)

    def queue

    def setup() {
        queue = new OperationPriorityFifoBlockingQueue()
    }

    @Unroll
    def "should return operations in proper order #id"() {

        given:
        for (RxBleRadioOperation operation : entryOrder) {
            queue.add(operation)
        }

        when:
        def operationsQueueList = new ArrayList<RxBleRadioOperation>()
        while (!queue.isEmpty()) {
            operationsQueueList.add(queue.take())
        }

        then:
        for (int i = 0; i < entryOrder.size(); i++) {
            def operation = entryOrder.get(i)
            assert properOrder.indexOf(operation) == operationsQueueList.indexOf(operation)
        }

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

    private static def mockOperation(RxBleRadioOperation.Priority priority) {
        return new RxBleRadioOperation<Void>() {

            protected RxBleRadioOperation.Priority definedPriority() {
                return priority
            }

            @Override
            void run() {

            }
        }
    }
}
