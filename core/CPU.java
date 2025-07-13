package core;

import java.util.Arrays;

public class CPU {
    private final Memory memory;
    private final Register[] regs; // REG0 a REG3
    private final Register PC, IR, FLAGS, StackTop, StackBottom;
    private final ULA ula;
    private final Stack stack;

    // Endereços reservados (ajuste conforme necessário)
    private final int ADDR_IMUL = 100;
    private final int ADDR_RESULT_IMUL = 120;
    private final int ADDR_SAVE_REGS = 130;
    private final int ADDR_VARS = 150;
    private final int ADDR_STACK_BOTTOM = 200;

    public CPU(int memSize) {
        this.memory = new Memory(memSize);
        this.regs = new Register[4];
        for (int i = 0; i < 4; i++) {
            regs[i] = new Register("REG" + i);
        }
        this.PC = new Register("PC");
        this.IR = new Register("IR");
        this.FLAGS = new Register("FLAGS");
        this.StackBottom = new Register("StackBottom");
        this.StackTop = new Register("StackTop");
        this.ula = new ULA();

        StackBottom.set(ADDR_STACK_BOTTOM);
        StackTop.set(ADDR_STACK_BOTTOM);
        this.stack = new Stack(memory, StackTop, StackBottom);
    }

    public void loadProgram(String[] lines) {
        for (int i = 0; i < lines.length; i++) {
            memory.set(i, parseInstruction(lines[i]));
        }
    }

    public void run() {
        while (true) {
            IR.set(memory.get(PC.get()));
            int instr = IR.get();
            PC.set(PC.get() + 1);

            if (!execute(instr)) break;
        }
    }

    private boolean execute(int instr) {
        switch (instr) {
            case -1: // HALT
                return false;
            default:
                System.out.println("Instrução não implementada: " + instr);
                return false;
        }
    }

    public int parseInstruction(String line) {
        // Exemplo simplificado - cada instrução deve ter seu opcode definido
        line = line.trim().toLowerCase();
        switch (line) {
            case "halt": return -1;
            // mapear outros comandos para seus opcodes
            default: return -999; // instrução inválida
        }
    }

    // Getters auxiliares para debug
    public Register[] getRegs() { return regs; }
    public Memory getMemory() { return memory; }
    public Register getPC() { return PC; }
    public Register getFLAGS() { return FLAGS; }
    public Stack getStack() { return stack; }
} 
