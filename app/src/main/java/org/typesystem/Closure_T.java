package org.typesystem;

import org.typesystem.utils.*;

import java.util.Vector;

/** Compiled_Function_T */
public class Closure_T implements Object_T {

    Object_T obj;
    Vector<Object_T> free;
    String closureType;

    public Closure_T(Object_T obj, String type) {

        this.obj = obj;
        this.closureType = type;
        this.free = new Vector<Object_T>();
    }

    public Closure_T(Object_T obj, Vector<Object_T> free, String type) {
        this(obj, type);
        this.free = free;
    }

    @Override
    public String type() {
        return TypeList.CLOSURE_OBJECT;
    }

    @Override
    public String inspect() {
        return "Closure : " + this;
    }

    public Vector<Object_T> getFree() {
        return this.free;
    }

    public Object_T getObject() {
        return this.obj;
    }

    public String getClosureType() {
        return this.closureType;
    }
}
