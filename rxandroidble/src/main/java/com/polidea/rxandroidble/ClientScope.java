package com.polidea.rxandroidble;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

import bleshadow.javax.inject.Scope;

@Scope
@Retention(RetentionPolicy.RUNTIME)
public @interface ClientScope {

}