package model;

// Esta classe representará uma instrução decodificada.
// O CPU.decode() irá preencher e o CPU.execute() irá consumir uma instância desta classe.
public class Instruction {
    private int opcode; // Código da operação (ex: Opcodes.ADD_REG_REG)
    private int operand1; // Primeiro operando (ID de registrador, endereço de memória, ou valor imediato)
    private int operand2; // Segundo operando
    private int operand3; // Terceiro operando (usado principalmente para desvios condicionais com 3 argumentos)
    private int rawInstruction; // O valor inteiro original da instrução lido da memória

    // Construtor para instruções com 0 a 3 operandos.
    // O significado dos operandos (reg ID, address, immediate) depende do opcode.
    public Instruction(int opcode, int operand1, int operand2, int operand3, int rawInstruction) {
        this.opcode = opcode;
        this.operand1 = operand1;
        this.operand2 = operand2;
        this.operand3 = operand3;
        this.rawInstruction = rawInstruction;
    }

    // Sobrecarga para instruções com 2 operandos
    public Instruction(int opcode, int operand1, int operand2, int rawInstruction) {
        this(opcode, operand1, operand2, 0, rawInstruction); // operand3 default para 0
    }

    // Sobrecarga para instruções com 1 operando
    public Instruction(int opcode, int operand1, int rawInstruction) {
        this(opcode, operand1, 0, 0, rawInstruction); // operand2 e operand3 default para 0
    }

    // Sobrecarga para instruções sem operandos (ex: RET, HALT)
    public Instruction(int opcode, int rawInstruction) {
        this(opcode, 0, 0, 0, rawInstruction); // Todos os operandos default para 0
    }

    // --- Getters ---
    public int getOpcode() {
        return opcode;
    }

    public int getOperand1() {
        return operand1;
    }

    public int getOperand2() {
        return operand2;
    }

    public int getOperand3() {
        return operand3;
    }

    public int getRawInstruction() {
        return rawInstruction;
    }

    @Override
    public String toString() {
        // Para depuração: mostra o opcode e os operandos
        return String.format("Instruction [Opcode=0x%X, Op1=%d, Op2=%d, Op3=%d, Raw=0x%X]",
                             opcode, operand1, operand2, operand3, rawInstruction);
    }
}