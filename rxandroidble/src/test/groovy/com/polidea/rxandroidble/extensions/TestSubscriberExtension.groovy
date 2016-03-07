package com.polidea.rxandroidble.extensions

import rx.observers.TestSubscriber

class TestSubscriberExtension {

    static boolean assertAnyOnNext(final TestSubscriber subscriber, Closure<Boolean>  closure) {
        subscriber.onNextEvents.any closure
    }

    static boolean assertOneMatches(final TestSubscriber subscriber, Closure<Boolean>  closure) {
        subscriber.onNextEvents.count(closure) == 1
    }

    static boolean assertErrorClosure(final TestSubscriber subscriber, Closure<Boolean> closure) {
        subscriber.onErrorEvents?.any closure
    }
}
