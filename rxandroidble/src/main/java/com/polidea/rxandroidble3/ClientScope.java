package com.polidea.rxandroidble2;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

import bleshadow.javax.inject.Scope;

@Scope
@Retention(RetentionPolicy.RUNTIME)
public @interface ClientScope {

}