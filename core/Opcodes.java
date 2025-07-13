package core; // Colocando Opcodes no pacote core, junto com CPU

public final class Opcodes {
    // Definindo a largura do opcode em bits para facilitar o shift e máscara no decode
    public static final int OPCODE_BITS = 8;
    public static final int OPCODE_MASK = 0xFF; // Máscara para pegar os 8 bits do opcode

    // --- Aritméticas e Lógicas (ADD, SUB, INC) ---
    // Grupo 0x1X - Operações com registradores
    public static final int ADD_REG_REG = 0x10; // add %<regA> %<regB>   || RegB <- RegA + RegB
    public static final int SUB_REG_REG = 0x11; // sub %<regA> %<regB>   || RegB <- RegA - RegB
    public static final int INC_REG     = 0x12; // inc %<regA>           || RegA ++

    // Grupo 0x2X - Operações com memória e registrador
    public static final int ADD_MEM_REG = 0x20; // add <mem> %<regA>     || RegA <- memória[mem] + RegA
    public static final int ADD_REG_MEM = 0x21; // add %<regA> <mem>     || Memória[mem] <- RegA + memória[mem]
    public static final int SUB_MEM_REG = 0x22; // sub <mem> %<regA>     || RegA <- memória[mem] - RegA
    public static final int SUB_REG_MEM = 0x23; // sub %<regA> <mem>     || memória[mem] <- RegA - memória[mem]
    public static final int INC_MEM     = 0x24; // inc <mem>             || memória[mem] ++

    // --- Movimentação de Dados (MOVE) ---
    // Grupo 0x3X - Operações de movimentação
    public static final int MOVE_MEM_REG = 0x30; // move <mem> %<regA>   || RegA <- memória[mem]
    public static final int MOVE_REG_MEM = 0x31; // move %<regA> <mem>   || memória[mem] <- RegA
    public static final int MOVE_REG_REG = 0x32; // move %<regA> %<regB> || RegB <- RegA
    public static final int MOVE_IMM_REG = 0x33; // move imm %<regA>     || RegA <- immediate

    // --- Desvios (Jumps) ---
    // Grupo 0x4X - Desvios incondicionais e condicionais (baseados em flags)
    public static final int JMP  = 0x40; // jmp <mem>   || PC <- mem (desvio incondicional)
    public static final int JN   = 0x41; // jn <mem>    || se última operação<0 então PC <- mem
    public static final int JZ   = 0x42; // jz <mem>    || se última operação=0 então PC <- mem
    public static final int JNZ  = 0x43; // jnz <mem>   || se última operação|=0 então PC <- mem

    // Grupo 0x5X - Desvios condicionais (com comparação de registradores)
    public static final int JEQ = 0x50; // jeq %<regA> %<regB> <mem> || se RegA==RegB então PC <- mem
    public static final int JGT = 0x51; // jgt %<regA> %<regB> <mem> || se RegA>RegB então PC <- mem
    public static final int JLW = 0x52; // jlw %<regA> %<regB> <mem> || se RegA<RegB então PC <- mem

    // --- Controle de Pilha e Sub-rotinas (CALL, RET) ---
    // Grupo 0x6X - Operações de sub-rotina
    public static final int CALL = 0x60; // call <mem> || PC <- mem (push(PC++) )
    public static final int RET  = 0x61; // ret        || PC <- pop()

    // --- Instruções Especiais ---
    // Grupo 0x7X
    public static final int IMUL = 0x70; // imul %<regA> %<regB> (ou outros formatos, dependendo da sua definição)
                                        // A instrução IMUL no assembly pode ter diferentes modos de endereçamento.
                                        // Para o simulador, podemos ter um opcode genérico IMUL,
                                        // e o decode/execute da CPU determina como os operandos serão usados
                                        // para o microprograma.
    
    // Grupo 0xFF - Instruções de Sistema/Controle
    public static final int HALT = 0xFF; // Uma instrução para parar a execução da CPU.
                                         // Não está na sua lista, mas é essencial para testar.


    // --- Mapeamento para nomes de instrução (útil para o Loader/Assembler e depuração) ---
    // Este mapa é opcional, mas ajuda muito na conversão de strings para opcodes.
    private static final Map<String, Integer> OPCODE_MAP = new HashMap<>();
    private static final Map<Integer, String> INSTRUCTION_NAMES = new HashMap<>();

    static {
        // Inicializa os mapas estaticamente
        // Aritméticas e Lógicas
        OPCODE_MAP.put("add_reg_reg", ADD_REG_REG);
        INSTRUCTION_NAMES.put(ADD_REG_REG, "add %reg %reg");
        OPCODE_MAP.put("sub_reg_reg", SUB_REG_REG);
        INSTRUCTION_NAMES.put(SUB_REG_REG, "sub %reg %reg");
        OPCODE_MAP.put("inc_reg", INC_REG);
        INSTRUCTION_NAMES.put(INC_REG, "inc %reg");

        OPCODE_MAP.put("add_mem_reg", ADD_MEM_REG);
        INSTRUCTION_NAMES.put(ADD_MEM_REG, "add mem %reg");
        OPCODE_MAP.put("add_reg_mem", ADD_REG_MEM);
        INSTRUCTION_NAMES.put(ADD_REG_MEM, "add %reg mem");
        OPCODE_MAP.put("sub_mem_reg", SUB_MEM_REG);
        INSTRUCTION_NAMES.put(SUB_MEM_REG, "sub mem %reg");
        OPCODE_MAP.put("sub_reg_mem", SUB_REG_MEM);
        INSTRUCTION_NAMES.put(SUB_REG_MEM, "sub %reg mem");
        OPCODE_MAP.put("inc_mem", INC_MEM);
        INSTRUCTION_NAMES.put(INC_MEM, "inc mem");

        // Movimentação
        OPCODE_MAP.put("move_mem_reg", MOVE_MEM_REG);
        INSTRUCTION_NAMES.put(MOVE_MEM_REG, "move mem %reg");
        OPCODE_MAP.put("move_reg_mem", MOVE_REG_MEM);
        INSTRUCTION_NAMES.put(MOVE_REG_MEM, "move %reg mem");
        OPCODE_MAP.put("move_reg_reg", MOVE_REG_REG);
        INSTRUCTION_NAMES.put(MOVE_REG_REG, "move %reg %reg");
        OPCODE_MAP.put("move_imm_reg", MOVE_IMM_REG);
        INSTRUCTION_NAMES.put(MOVE_IMM_REG, "move imm %reg");

        // Desvios
        OPCODE_MAP.put("jmp", JMP);
        INSTRUCTION_NAMES.put(JMP, "jmp mem");
        OPCODE_MAP.put("jn", JN);
        INSTRUCTION_NAMES.put(JN, "jn mem");
        OPCODE_MAP.put("jz", JZ);
        INSTRUCTION_NAMES.put(JZ, "jz mem");
        OPCODE_MAP.put("jnz", JNZ);
        INSTRUCTION_NAMES.put(JNZ, "jnz mem");

        OPCODE_MAP.put("jeq", JEQ);
        INSTRUCTION_NAMES.put(JEQ, "jeq %reg %reg mem");
        OPCODE_MAP.put("jgt", JGT);
        INSTRUCTION_NAMES.put(JGT, "jgt %reg %reg mem");
        OPCODE_MAP.put("jlw", JLW);
        INSTRUCTION_NAMES.put(JLW, "jlw %reg %reg mem");

        // Pilha/Sub-rotinas
        OPCODE_MAP.put("call", CALL);
        INSTRUCTION_NAMES.put(CALL, "call mem");
        OPCODE_MAP.put("ret", RET);
        INSTRUCTION_NAMES.put(RET, "ret");

        // Especiais
        OPCODE_MAP.put("imul", IMUL);
        INSTRUCTION_NAMES.put(IMUL, "imul %reg %reg"); // Simplificado, pode ter outros formatos
        OPCODE_MAP.put("halt", HALT);
        INSTRUCTION_NAMES.put(HALT, "halt");
    }

    /**
     * Retorna o opcode numérico para um dado nome de instrução assembly (lower case).
     * Útil para o Assembler/Loader.
     * @param instructionName O nome da instrução (ex: "add_reg_reg", "jmp").
     * @return O opcode correspondente, ou null se não encontrado.
     */
    public static Integer getOpcode(String instructionName) {
        return OPCODE_MAP.get(instructionName.toLowerCase());
    }

    /**
     * Retorna o nome da instrução assembly para um dado opcode numérico.
     * Útil para depuração.
     * @param opcode O valor numérico do opcode.
     * @return O nome da instrução, ou "UNKNOWN" se não encontrado.
     */
    public static String getInstructionName(int opcode) {
        return INSTRUCTION_NAMES.getOrDefault(opcode, "UNKNOWN");
    }
}