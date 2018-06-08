package com.polidea.rxandroidble.internal.eventlog;

public class OperationAttribute {

    public final String name;
    public final String value;

    public OperationAttribute(String name, String value) {
        this.name = name;
        this.value = value;
    }

    @Override
    public String toString() {
        return "OperationAttribute{name='" + name + "\', value='" + value + "\'}";
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        OperationAttribute operationAttribute = (OperationAttribute) o;

        if (name != null ? !name.equals(operationAttribute.name) : operationAttribute.name != null) return false;
        return value != null ? value.equals(operationAttribute.value) : operationAttribute.value == null;
    }

    @Override
    public int hashCode() {
        int result = name != null ? name.hashCode() : 0;
        result = 31 * result + (value != null ? value.hashCode() : 0);
        return result;
    }
}