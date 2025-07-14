package core;

import java.util.HashMap;
import java.util.Map;

public final class Opcodes {
    public static final int OPCODE_BITS = 8;
    public static final int OPCODE_MASK = 0xFF;

    // --- Aritméticas e Lógicas ---
    public static final int ADD_REG_REG = 0x10;
    public static final int SUB_REG_REG = 0x11;
    public static final int INC_REG     = 0x12;

    public static final int ADD_MEM_REG = 0x20;
    public static final int ADD_REG_MEM = 0x21;
    public static final int SUB_MEM_REG = 0x22;
    public static final int SUB_REG_MEM = 0x23;
    public static final int INC_MEM     = 0x24;

    // --- Movimentação de Dados ---
    public static final int MOVE_MEM_REG = 0x30;
    public static final int MOVE_REG_MEM = 0x31;
    public static final int MOVE_REG_REG = 0x32;
    public static final int MOVE_IMM_REG = 0x33;

    // --- Desvios ---
    public static final int JMP  = 0x40;
    public static final int JN   = 0x41;
    public static final int JZ   = 0x42;
    public static final int JNZ  = 0x43;

    public static final int JEQ = 0x50;
    public static final int JGT = 0x51;
    public static final int JLW = 0x52;

    // --- Sub-rotinas ---
    public static final int CALL = 0x60;
    public static final int RET  = 0x61;

    // --- Especiais ---
    public static final int IMUL = 0x70;
    public static final int HALT = 0xFF;

    private static final Map<String, Integer> OPCODE_MAP = new HashMap<>();
    private static final Map<Integer, String> INSTRUCTION_NAMES = new HashMap<>();

    static {
        // Formatos específicos
        OPCODE_MAP.put("add_reg_reg", ADD_REG_REG);
        OPCODE_MAP.put("sub_reg_reg", SUB_REG_REG);
        OPCODE_MAP.put("inc_reg", INC_REG);

        OPCODE_MAP.put("add_mem_reg", ADD_MEM_REG);
        OPCODE_MAP.put("add_reg_mem", ADD_REG_MEM);
        OPCODE_MAP.put("sub_mem_reg", SUB_MEM_REG);
        OPCODE_MAP.put("sub_reg_mem", SUB_REG_MEM);
        OPCODE_MAP.put("inc_mem", INC_MEM);

        OPCODE_MAP.put("move_mem_reg", MOVE_MEM_REG);
        OPCODE_MAP.put("move_reg_mem", MOVE_REG_MEM);
        OPCODE_MAP.put("move_reg_reg", MOVE_REG_REG);
        OPCODE_MAP.put("move_imm_reg", MOVE_IMM_REG);

        OPCODE_MAP.put("jmp", JMP);
        OPCODE_MAP.put("jn", JN);
        OPCODE_MAP.put("jz", JZ);
        OPCODE_MAP.put("jnz", JNZ);

        OPCODE_MAP.put("jeq", JEQ);
        OPCODE_MAP.put("jgt", JGT);
        OPCODE_MAP.put("jlw", JLW);

        OPCODE_MAP.put("call", CALL);
        OPCODE_MAP.put("ret", RET);

        OPCODE_MAP.put("imul", IMUL);
        OPCODE_MAP.put("halt", HALT);

        // Mapeamentos simples diretos (sem sufixo)
        OPCODE_MAP.put("add", ADD_REG_REG);
        OPCODE_MAP.put("sub", SUB_REG_REG);
        OPCODE_MAP.put("inc", INC_REG);
        OPCODE_MAP.put("move", MOVE_REG_REG);

        INSTRUCTION_NAMES.put(ADD_REG_REG, "add %reg %reg");
        INSTRUCTION_NAMES.put(SUB_REG_REG, "sub %reg %reg");
        INSTRUCTION_NAMES.put(INC_REG, "inc %reg");
        INSTRUCTION_NAMES.put(ADD_MEM_REG, "add mem %reg");
        INSTRUCTION_NAMES.put(ADD_REG_MEM, "add %reg mem");
        INSTRUCTION_NAMES.put(SUB_MEM_REG, "sub mem %reg");
        INSTRUCTION_NAMES.put(SUB_REG_MEM, "sub %reg mem");
        INSTRUCTION_NAMES.put(INC_MEM, "inc mem");
        INSTRUCTION_NAMES.put(MOVE_MEM_REG, "move mem %reg");
        INSTRUCTION_NAMES.put(MOVE_REG_MEM, "move %reg mem");
        INSTRUCTION_NAMES.put(MOVE_REG_REG, "move %reg %reg");
        INSTRUCTION_NAMES.put(MOVE_IMM_REG, "move imm %reg");
        INSTRUCTION_NAMES.put(JMP, "jmp mem");
        INSTRUCTION_NAMES.put(JN, "jn mem");
        INSTRUCTION_NAMES.put(JZ, "jz mem");
        INSTRUCTION_NAMES.put(JNZ, "jnz mem");
        INSTRUCTION_NAMES.put(JEQ, "jeq %reg %reg mem");
        INSTRUCTION_NAMES.put(JGT, "jgt %reg %reg mem");
        INSTRUCTION_NAMES.put(JLW, "jlw %reg %reg mem");
        INSTRUCTION_NAMES.put(CALL, "call mem");
        INSTRUCTION_NAMES.put(RET, "ret");
        INSTRUCTION_NAMES.put(IMUL, "imul %reg %reg");
        INSTRUCTION_NAMES.put(HALT, "halt");
    }

    public static Integer getOpcode(String instructionName) {
        return OPCODE_MAP.get(instructionName.toLowerCase());
    }

    public static String getInstructionName(int opcode) {
        return INSTRUCTION_NAMES.getOrDefault(opcode, "UNKNOWN");
    }
}
