package org.typesystem;

import org.typesystem.utils.*;

import java.util.Vector;

/** Compiled_Function_T */
public class Compiled_Block_T implements Object_T {

    Vector<Byte> instructions;
    int numLocals;

    public Compiled_Block_T(Vector<Byte> ins) {
        this.instructions = ins;
    }

    public Compiled_Block_T(Vector<Byte> ins, int numLocals) {
        this(ins);
        this.numLocals = numLocals;
    }

    @Override
    public String type() {
        return TypeList.COMPILED_BLOCK_OBJECT;
    }

    @Override
    public String inspect() {
        return "Compiled Block : " + this;
    }

    public Vector<Byte> getInstructions() {
        return this.instructions;
    }

    public int getNumLocals() {
        return this.numLocals;
    }
}
