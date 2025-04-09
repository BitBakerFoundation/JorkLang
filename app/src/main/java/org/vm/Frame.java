package org.vm;

import org.typesystem.Closure_T;
import org.typesystem.Compiled_Block_T;
import org.typesystem.Compiled_Function_T;
import org.typesystem.utils.TypeList;

import java.util.*;

/** Frame */
public class Frame {

    int insPointer;
    int basePointer;
    Closure_T closure;

    public Frame(Closure_T closure, int basePointer) {
        this.insPointer = -1;
        this.basePointer = basePointer;
        this.closure = closure;
    }

    public Vector<Byte> instructions() {
        switch (this.closure.getClosureType()) {
            case TypeList.COMPILED_BLOCK_OBJECT:
                return ((Compiled_Block_T) this.closure.getObject()).getInstructions();
            case TypeList.COMPILED_FUNCTION_OBJECT:
                return ((Compiled_Function_T) this.closure.getObject()).getInstructions();
            default:
                return null;
        }
    }
}
