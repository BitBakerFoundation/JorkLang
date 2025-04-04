package org.compiler;

import org.code.*;
import org.code.utils.*;
import org.debugger.*;
import org.error.*;
import org.parser.*;
import org.parser.expressions.*;
import org.parser.literals.*;
import org.parser.statements.*;
import org.typesystem.*;

import java.util.*;

public class Compiler {

    Vector<Object_T> constants;

    SymbolTable symbolTable;

    Vector<CompilationScope> scopes;
    int scopeIndex;

    public Compiler() {
        this.constants = new Vector<Object_T>();
        this.symbolTable = new SymbolTable();
        this.scopeIndex = 0;
        this.scopes = new Vector<CompilationScope>();
        this.scopes.add(new CompilationScope());
    }

    public Compiler(SymbolTable st, Vector<Object_T> constants) {
        this();
        this.symbolTable = st;
        this.constants = constants;
    }

    int addConstant(Object_T obj) {
        this.constants.add(obj);
        return this.constants.size() - 1;
    }

    public void printIns() {
        System.out.println(Code.toString(this.currentInstructions()));
    }

    public CompilerError compile(Node node) {
        CompilerError err = null;
        switch (node) {
            // Program
            // Statements
            // Expressions
            // Literals
            case Program prg:
                for (Statement s : prg.getStatements()) {
                    err = this.compile(s);
                    if (err != null) {
                        return err;
                    }
                }
                return null;

            case ExpressionStatement es:
                System.out.println(
                        "Are we here ? EXPRESSION STATEMETN" + es.getExpression().print(""));
                err = this.compile(es.getExpression());
                if (err != null) {
                    return err;
                }
                this.emit(OpCodes.OpPop);
                return null;
            case BlockStatement bs:
                for (Statement s : bs.getStatements()) {
                    err = this.compile(s);
                    if (err != null) {
                        return err;
                    }
                }
                return null;
            case LetStatement ls:
                err = this.compile(ls.getValue());
                if (err != null) {
                    return err;
                }
                Symbol symbol = this.symbolTable.resolve(ls.getName().getValue());
                if (symbol != null) {
                    return new CompilerError(
                            "", "Variable already exists : " + ls.getName().getValue());
                }

                symbol = this.symbolTable.define(ls.getName().getValue());
                this.emit(OpCodes.OpSetGlobal, symbol.index);
                return null;
            case AssignmentStatement as:
                err = this.compile(as.getExpression());
                if (err != null) {
                    return err;
                }
                symbol = this.symbolTable.resolve(as.getName().getValue());
                if (symbol == null) {
                    return new CompilerError(
                            "", "Variable does not exist : " + as.getName().getValue());
                }
                this.emit(OpCodes.OpSetGlobal, symbol.index);
                return err;
            case ReturnStatement rs:
                err = this.compile(rs.getReturnValue());
                if (err != null) {
                    return err;
                }
                this.emit(OpCodes.OpReturnValue);
                return null;
            case IfExpression ie:
                err = this.compile(ie.getCondition());

                if (err != null) {
                    return err;
                }
                int jumpNotTruthyPos = this.emit(OpCodes.OpJumpNotTruthy, 9999);
                err = this.compile(ie.getConsequence());
                if (err != null) {
                    return err;
                }
                if (this.lastInstructionIs(OpCodes.OpPop)) {
                    this.removeLastPop();
                }
                Vector<Integer> jumpPoses = new Vector<>();

                jumpPoses.add(
                        this.emit(OpCodes.OpJump, 9999)); // Should go out of the if else block
                int afterConsequencePos = this.currentInstructions().size();
                this.changeOperand(jumpNotTruthyPos, afterConsequencePos);

                for (ElifExpression elf : ie.getElifExpression()) {
                    err = this.compile(elf.getCondition());

                    if (err != null) {
                        return err;
                    }
                    jumpNotTruthyPos = this.emit(OpCodes.OpJumpNotTruthy, 9999);
                    err = this.compile(elf.getConsequence());
                    if (err != null) {
                        return err;
                    }
                    if (this.lastInstructionIs(OpCodes.OpPop)) {
                        this.removeLastPop();
                    }

                    jumpPoses.add(this.emit(OpCodes.OpJump, 9999));
                    afterConsequencePos = this.currentInstructions().size();

                    this.changeOperand(jumpNotTruthyPos, afterConsequencePos);
                }

                if (ie.getAlternative() == null) {
                    this.emit(OpCodes.OpNull);
                } else {

                    err = this.compile(ie.getAlternative());
                    if (err != null) {
                        return err;
                    }
                    if (this.lastInstructionIs(OpCodes.OpPop)) {
                        this.removeLastPop();
                    }
                }

                int afterAlternativePos = this.currentInstructions().size();
                for (int jumpPos : jumpPoses) this.changeOperand(jumpPos, afterAlternativePos);
                return null;
            case InfixExpression ie
            when !(ie instanceof DotExpression):
                if (ie.getOperator().equals("<") || ie.getOperator().equals("<=")) {
                    err = this.compile(ie.getRight());
                    if (err != null) {
                        return err;
                    }

                    err = this.compile(ie.getLeft());
                    if (err != null) {
                        return err;
                    }

                    if (ie.getOperator().equals("<")) this.emit(OpCodes.OpGreaterThan);
                    else this.emit(OpCodes.OpGreaterThanEqualTo);
                    return null;
                }
                err = this.compile(ie.getLeft());
                if (err != null) {
                    return err;
                }

                err = this.compile(ie.getRight());
                if (err != null) {
                    return err;
                }

                switch (ie.getOperator()) {
                    case "+":
                        this.emit(OpCodes.OpAdd);
                        break;
                    case "-":
                        this.emit(OpCodes.OpSub);
                        break;
                    case "*":
                        this.emit(OpCodes.OpMul);
                        break;
                    case "/":
                        this.emit(OpCodes.OpDiv);
                        break;
                    case "^":
                        this.emit(OpCodes.OpPow);
                        break;
                    case ">":
                        this.emit(OpCodes.OpGreaterThan);
                        break;
                    case ">=":
                        this.emit(OpCodes.OpGreaterThanEqualTo);
                        break;
                    case "==":
                        this.emit(OpCodes.OpEqual);
                        break;
                    case "!=":
                        this.emit(OpCodes.OpNotEqual);
                        break;
                    default:
                        return new CompilerError("", "Unknown Operator : " + ie.getOperator());
                }
                return null;
            case PrefixExpression pe:
                err = this.compile(pe.getRight());
                if (err != null) {
                    return err;
                }

                switch (pe.getOperator()) {
                    case "!":
                        this.emit(OpCodes.OpBang);
                        break;
                    case "-":
                        this.emit(OpCodes.OpMinus);
                        break;
                    default:
                        return new CompilerError("", "Unsupported operator : " + pe.getOperator());
                }
                return null;
            case IndexExpression ie:
                err = this.compile(ie.getLeft());
                if (err != null) {
                    return err;
                }
                err = this.compile(ie.getIndex());
                if (err != null) {
                    return err;
                }
                this.emit(OpCodes.OpIndex);
                return null;
            case CallExpression ce:
                System.out.println("calling");
                System.out.println(ce.getFunction().print("FUCNTON : "));
                err = this.compile(ce.getFunction());
                if (err != null) {
                    return err;
                }
                this.emit(OpCodes.OpCall);
                return null;
            case DotExpression de:
                if (de.getRight() instanceof CallExpression) {

                    CallExpression c = (CallExpression) de.getRight();
                    c.addArgument(de.getLeft());
                    err = this.compile(c.getFunction());
                    if (err != null) {
                        return err;
                    }
                    this.emit(OpCodes.OpCall);
                    return null;

                } else {
                    return new CompilerError("", "Wrong Type : " + de.getRight().getClass());
                }

            case Identifier id:
                symbol = this.symbolTable.resolve(id.getValue());
                if (symbol == null) {
                    return new CompilerError("", "Variable does not exist : " + id.getValue());
                }
                this.emit(OpCodes.OpGetGlobal, symbol.index);
                return null;
            case IntegerLiteral il:
                Integer_T integer = new Integer_T(il.getValue());
                this.emit(OpCodes.OpConstant, this.addConstant(integer));
                return null;
            case FloatLiteral fl:
                Float_T f = new Float_T(fl.getValue());
                this.emit(OpCodes.OpConstant, this.addConstant(f));
                return null;
            case BooleanLiteral bl:
                if (bl.getValue()) this.emit(OpCodes.OpTrue);
                else this.emit(OpCodes.OpFalse);
                return null;
            case StringLiteral s:
                String_T str = new String_T(s.getValue());
                this.emit(OpCodes.OpConstant, this.addConstant(str));
                return null;
            case ArrayLiteral al:
                for (Expression ex : al.getElements()) {
                    err = this.compile(ex);
                    if (err != null) {
                        return err;
                    }
                }
                this.emit(OpCodes.OpArray, al.getElements().size());
                return null;
            case HashLiteral hl:
                for (Expression key : hl.getElements().keySet()) {
                    err = this.compile(key);
                    if (err != null) {
                        return err;
                    }
                    err = this.compile(hl.getElement(key));
                    if (err != null) {
                        return err;
                    }
                }
                this.emit(OpCodes.OpHash, hl.getElements().size() * 2);

                return null;
            case FunctionLiteral fl:
                System.out.println("AAAAAAAfg");

                symbol = this.symbolTable.resolve(fl.getName().getValue());
                if (symbol != null) {
                    return new CompilerError(
                            "", "Function already exists : " + fl.getName().getValue());
                }

                this.enterScope();
                err = this.compile(fl.getBody());
                if (err != null) {
                    return err;
                }
                if (this.lastInstructionIs(OpCodes.OpPop)) {
                    this.replaceLastPopWithReturn();
                }
                if (!this.lastInstructionIs(OpCodes.OpReturnValue)) {
                    this.emit(OpCodes.OpReturn);
                }
                Vector<Byte> instructions = this.leaveScope();
                Compiled_Function_T function = new Compiled_Function_T(instructions);
                this.emit(OpCodes.OpConstant, this.addConstant(function));

                symbol = this.symbolTable.define(fl.getName().getValue());
                this.emit(OpCodes.OpSetGlobal, symbol.index);

                return null;
            default:
                return new CompilerError("", "Unsupported Type : " + node.getClass());
        }
    }

    void replaceLastPopWithReturn() {
        int lastPos = this.scopes.get(this.scopeIndex).lastInstruction.position;
        this.replaceInstruction(lastPos, Code.make(OpCodes.OpReturnValue));
        this.scopes.get(this.scopeIndex).lastInstruction.opCode = OpCodes.OpReturnValue;
    }

    boolean lastInstructionIs(byte op) {
        if (this.currentInstructions().size() == 0) {
            return false;
        }
        return this.scopes.get(this.scopeIndex).lastInstruction.opCode == op;
    }

    void removeLastPop() {
        EmittedInstruction last = this.scopes.get(this.scopeIndex).lastInstruction;
        EmittedInstruction previous = this.scopes.get(this.scopeIndex).previousInstruction;

        Vector<Byte> oldIns = this.currentInstructions();
        Vector<Byte> newIns = Helper.slice(oldIns, 0, last.position);

        this.scopes.get(this.scopeIndex).instructions = newIns;
        this.scopes.get(this.scopeIndex).lastInstruction = previous;
    }

    int emit(byte op, int... operands) {
        byte[] ins = Code.make(op, operands);
        int pos = this.addInstuctions(ins);

        this.setLastInstruction(op, pos);
        return pos;
    }

    int addInstuctions(byte[] ins) {
        int posNewInstruction = this.currentInstructions().size();
        for (byte in : ins) this.currentInstructions().add(in);
        return posNewInstruction;
    }

    void replaceInstruction(int pos, byte[] newInstruction) {
        for (int i = 0; i < newInstruction.length; i++) {
            this.currentInstructions().set(pos + i, newInstruction[i]);
        }
    }

    void changeOperand(int opPos, int operand) {
        byte op = this.currentInstructions().get(opPos);
        byte[] newInstruction = Code.make(op, operand);

        this.replaceInstruction(opPos, newInstruction);
    }

    void setLastInstruction(byte op, int pos) {
        EmittedInstruction previous = this.scopes.get(this.scopeIndex).lastInstruction;
        EmittedInstruction last = new EmittedInstruction(op, pos);

        this.scopes.get(this.scopeIndex).previousInstruction = previous;
        this.scopes.get(this.scopeIndex).lastInstruction = last;
    }

    public ByteCode bytecode() {
        return new ByteCode(this.currentInstructions(), this.constants);
    }

    Vector<Byte> currentInstructions() {
        return this.scopes.get(this.scopeIndex).instructions;
    }

    void enterScope() {
        this.scopes.add(new CompilationScope());
        this.scopeIndex++;
    }

    Vector<Byte> leaveScope() {
        Vector<Byte> ins = this.currentInstructions();

        this.scopes.removeLast();
        this.scopeIndex--;
        return ins;
    }
}

class EmittedInstruction {
    byte opCode;
    int position;

    EmittedInstruction() {
        this.opCode = 0;
        this.position = 0;
    }

    EmittedInstruction(byte op, int pos) {
        this.opCode = op;
        this.position = pos;
    }
}
