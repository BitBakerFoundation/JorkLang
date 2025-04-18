package org.code;

import org.typesystem.Object_T;

import java.util.Vector;

public class ByteCode {
    Vector<Byte> instructions;
    Vector<Object_T> constants;

    public ByteCode(Vector<Byte> instructions, Vector<Object_T> constants) {
        this.instructions = instructions;
        this.constants = constants;
    }

    public Vector<Byte> getInstructions() {
        return this.instructions;
    }

    public Vector<Object_T> getConstants() {
        return this.constants;
    }
}
