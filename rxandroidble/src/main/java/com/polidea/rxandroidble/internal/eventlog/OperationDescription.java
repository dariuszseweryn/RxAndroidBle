package com.polidea.rxandroidble.internal.eventlog;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class OperationDescription {

    public final List<OperationAttribute> attributes;

    public OperationDescription(OperationAttribute... attributes) {
        this.attributes = new ArrayList<>(Arrays.asList(attributes));
    }

    @Override
    public String toString() {
        return "OperationDescription{attributes=" + attributes + '}';
    }
}