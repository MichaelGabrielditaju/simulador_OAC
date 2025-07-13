package core;

import model.Instruction;
import java.util.HashMap;
import java.util.Map;
// Opcodes.java deve estar no mesmo pacote 'core' ou ser importado corretamente
import core.Opcodes; 

public class CPU {
    // Componentes da CPU
    private Memory memory;
    private ULA ula;
    private Flags flags;
    private Stack stack;
    private Bus bus; // Instância do barramento, embora seu uso seja mais conceitual

    // Registradores
    private Register IR;    // Instruction Register
    private Register PC;    // Program Counter
    private Register REG0;
    private Register REG1;
    private Register REG2;
    private Register REG3;
    private Register StkTOP; // Stack Top Pointer
    private Register StkBOT; // Stack Bottom Pointer

    // Mapa para acesso fácil aos registradores por ID (ex: 0 para REG0, 1 para REG1)
    private Map<Integer, Register> generalPurposeRegisters;

    // Constantes para endereços de memória reservados (do Memory.java)
    private final int IMUL_MICROPROGRAM_START;
    private final int IMUL_REGS_SAVE_AREA_START;
    private final int IMUL_RESULT_ADDRESS;
    // Endereço de retorno do microprograma IMUL (onde a CPU deve voltar após a multiplicação)
    private final int IMUL_RETURN_PC_SAVE_ADDR;
    // Endereços temporários para os operandos do IMUL no microprograma (se necessário)
    private final int IMUL_OP1_TEMP_ADDR;
    private final int IMUL_OP2_TEMP_ADDR;


    // Variáveis para depuração e controle do ciclo
    private boolean running;

    // Construtor da CPU
    public CPU(int memorySize) {
        // 1. Inicializa os componentes auxiliares
        this.flags = new Flags();
        this.ula = new ULA(this.flags); // ULA precisa de acesso às Flags
        this.memory = new Memory(memorySize); // Memória é criada com um tamanho específico
        this.bus = new Bus(); // Instância do barramento

        // 2. Inicializa os Registradores
        this.IR = new Register("IR");
        this.PC = new Register("PC"); // Program Counter começa em 0 (ou onde o programa será carregado)
        this.REG0 = new Register("REG0");
        this.REG1 = new Register("REG1");
        this.REG2 = new Register("REG2");
        this.REG3 = new Register("REG3");

        // Inicializa StkTOP e StkBOT com o endereço de base da pilha fornecido pela memória
        // Eles devem começar apontando para o mesmo lugar (o "fundo" da pilha)
        this.StkBOT = new Register("StkBOT");
        this.StkTOP = new Register("StkTOP");
        // O valor inicial de StkBOT é o endereço mais alto da área de pilha na memória.
        // StackBottomAddress da memória já considera a pilha crescendo para baixo.
        this.StkBOT.set(memory.getStackBottomAddress());
        this.StkTOP.set(memory.getStackBottomAddress()); // No início, Top e Bottom são iguais

        // A Stack precisa de Memory, StkTOP, StkBOT e ULA para funcionar corretamente
        this.stack = new Stack(this.memory, this.StkTOP, this.StkBOT, this.ula);

        // Mapeia registradores de uso geral para fácil acesso
        this.generalPurposeRegisters = new HashMap<>();
        generalPurposeRegisters.put(0, REG0);
        generalPurposeRegisters.put(1, REG1);
        generalPurposeRegisters.put(2, REG2);
        generalPurposeRegisters.put(3, REG3);

        // Armazena os endereços das áreas reservadas da memória
        this.IMUL_MICROPROGRAM_START = memory.getImulMicroprogramStartAddress();
        this.IMUL_REGS_SAVE_AREA_START = memory.getImulRegistersSaveAreaStart();
        this.IMUL_RESULT_ADDRESS = memory.getImulResultAddress();
        // Endereços dentro da área de salvamento para maior clareza
        this.IMUL_RETURN_PC_SAVE_ADDR = IMUL_REGS_SAVE_AREA_START + 4; // PC salvo na 5ª posição (0-indexed)
        this.IMUL_OP1_TEMP_ADDR = IMUL_REGS_SAVE_AREA_START + 6; // Onde o microprograma pode ler o primeiro operando
        this.IMUL_OP2_TEMP_ADDR = IMUL_REGS_SAVE_AREA_START + 7; // Onde o microprograma pode ler o segundo operando

        this.running = false; // CPU não está rodando por padrão
    }

    // --- Métodos de Controle da CPU ---

    public void loadProgram(int[] programCode, int startAddress) {
        memory.load(startAddress, programCode);
        PC.set(startAddress); // Define o PC para o início do programa
        System.out.println("Programa carregado na memória a partir do endereço: " + startAddress);
    }
    
    /**
     * Carrega o microprograma IMUL na memória.
     * Este microprograma deve ser escrito em assembly e montado separadamente.
     * @param imulMicroprogramCode O código de máquina do microprograma IMUL.
     */
    public void loadImulMicroprogram(int[] imulMicroprogramCode) {
        memory.load(IMUL_MICROPROGRAM_START, imulMicroprogramCode);
        System.out.println("Microprograma IMUL carregado a partir do endereço: " + IMUL_MICROPROGRAM_START);
    }


    public void start() {
        running = true;
        System.out.println("CPU Iniciada.");
        runCycle();
    }

    public void stop() {
        running = false;
        System.out.println("CPU Parada.");
    }

    // Ciclo de execução principal
    private void runCycle() {
        int instructionCount = 0; // Para evitar loops infinitos em programas com erro
        while (running) {
            instructionCount++;
            if (instructionCount > 100000) { // Limite de instruções para evitar loops
                System.err.println("Limite de instruções excedido. Parando CPU.");
                stop();
                break;
            }

            // 1. Fetch (Busca da Instrução)
            if (PC.get() < 0 || PC.get() >= memory.getMaxSize()) {
                System.err.println("Erro: PC fora dos limites da memória: " + PC.get());
                stop();
                break;
            }
            int instructionWord = bus.moveData(memory.read(PC.get())); // Lê a instrução da memória
            IR.set(instructionWord); // Coloca a instrução no Instruction Register
            
            // Incrementa o PC para apontar para a próxima instrução (default)
            // Desvios condicionais ou incondicionais alterarão o PC depois.
            PC.inc(); 

            // 2. Decode (Decodificação da Instrução)
            Instruction decodedInstruction = decode(instructionWord);
            if (decodedInstruction == null) {
                System.err.println("Erro: Instrução inválida ou não implementada em PC: " + (PC.get() - 1) + ", Raw: 0x" + Integer.toHexString(instructionWord));
                stop();
                break;
            }

            // 3. Execute (Execução da Instrução)
            execute(decodedInstruction);
            
            // Condição de parada (ex: HALT instruction ou fim do programa)
            // Se o PC ultrapassar o limite do programa, pode ser considerado fim.
            if (decodedInstruction.getOpcode() == Opcodes.HALT) { 
                 System.out.println("Instrução HALT executada. Fim do programa.");
                 stop();
            }
            // Para depuração:
            // printRegisters();
            // memory.dumpMemory(0, 20); // Dumps the first 20 memory locations for debugging
            // try { Thread.sleep(50); } catch (InterruptedException e) { e.printStackTrace(); } // Pequeno delay
        }
    }

    // --- Métodos de Decodificação e Execução ---

    /**
     * Decodifica uma palavra de instrução em um objeto Instruction.
     * Baseia-se no formato: Opcode (8 bits mais significativos) + Operandos (24 bits restantes).
     * O layout dos operandos varia de acordo com o opcode.
     *
     * @param instructionWord O inteiro que representa a instrução.
     * @return Um objeto Instruction decodificado.
     */
    private Instruction decode(int instructionWord) {
        // Extrai o opcode (8 bits mais significativos)
        int opcode = (instructionWord >>> (32 - Opcodes.OPCODE_BITS)) & Opcodes.OPCODE_MASK;
        
        // System.out.println("Decodificando: 0x" + Integer.toHexString(instructionWord) + 
        //                    " -> Opcode: 0x" + Integer.toHexString(opcode) + 
        //                    " (" + Opcodes.getInstructionName(opcode) + ")");

        int operand1 = 0;
        int operand2 = 0;
        int operand3 = 0;

        // Máscara para 22 bits de valor/endereço: 0x3FFFFF (2^22 - 1)
        // Máscara para 20 bits de endereço (no JEQ/JGT/JLW): 0xFFFFF (2^20 - 1)
        // Máscara para 2 bits de registrador: 0x3

        switch (opcode) {
            // --- Instruções com 2 Registradores (RegA, RegB) ---
            // Formato: [OPCODE (8b)] [REG_A (2b)] [REG_B (2b)] [0s (20b)]
            // REG_A nos bits 23-22
            // REG_B nos bits 21-20
            case Opcodes.ADD_REG_REG:
            case Opcodes.SUB_REG_REG:
            case Opcodes.MOVE_REG_REG:
            case Opcodes.IMUL: // IMUL também usa 2 regs (para os operandos)
                operand1 = (instructionWord >> 22) & 0x3; // RegA ID
                operand2 = (instructionWord >> 20) & 0x3; // RegB ID
                break;

            // --- Instruções com Memória/Imediato (22 bits) e 1 Registrador (2 bits) ---
            // Formato: [OPCODE (8b)] [REG (2b)] [VALUE (22b)]
            // REG nos bits 23-22
            // VALUE nos bits 21-0
            case Opcodes.ADD_MEM_REG:   // add <mem> %<regA> -> operand1 = mem_addr, operand2 = regA_id
            case Opcodes.SUB_MEM_REG:   // sub <mem> %<regA> -> operand1 = mem_addr, operand2 = regA_id
            case Opcodes.MOVE_MEM_REG:  // move <mem> %<regA> -> operand1 = mem_addr, operand2 = regA_id
            case Opcodes.MOVE_IMM_REG:  // move imm <val> %<regA> -> operand1 = immediate, operand2 = regA_id
                operand1 = instructionWord & 0x3FFFFF; // Value/Address (22 bits)
                operand2 = (instructionWord >> 22) & 0x3; // Reg ID (2 bits)
                break;
            
            case Opcodes.ADD_REG_MEM:   // add %<regA> <mem> -> operand1 = regA_id, operand2 = mem_addr
            case Opcodes.SUB_REG_MEM:   // sub %<regA> <mem> -> operand1 = regA_id, operand2 = mem_addr
            case Opcodes.MOVE_REG_MEM:  // move %<regA> <mem> -> operand1 = regA_id, operand2 = mem_addr
                operand1 = (instructionWord >> 22) & 0x3; // RegA ID (2 bits)
                operand2 = instructionWord & 0x3FFFFF; // Mem Address (22 bits)
                break;

            // --- Instruções com 1 Registrador ---
            // Formato: [OPCODE (8b)] [REG (2b)] [0s (22b)]
            // REG nos bits 23-22
            case Opcodes.INC_REG: // inc %<regA> -> operand1 = regA_id
                operand1 = (instructionWord >> 22) & 0x3; // RegA ID
                break;

            // --- Instruções com 1 Endereço de Memória (24 bits) ---
            // Formato: [OPCODE (8b)] [ADDRESS (24b)]
            // ADDRESS nos bits 23-0
            case Opcodes.INC_MEM: // inc <mem> -> operand1 = mem_addr
            case Opcodes.JMP:     // jmp <mem> -> operand1 = mem_addr
            case Opcodes.JN:      // jn <mem>  -> operand1 = mem_addr
            case Opcodes.JZ:      // jz <mem>  -> operand1 = mem_addr
            case Opcodes.JNZ:     // jnz <mem> -> operand1 = mem_addr
            case Opcodes.CALL:    // call <mem> -> operand1 = mem_addr
                operand1 = instructionWord & 0xFFFFFF; // Endereço (24 bits)
                break;

            // --- Instruções com 3 Operandos (RegA, RegB, Endereço - 20 bits) ---
            // Formato: [OPCODE (8b)] [REG_A (2b)] [REG_B (2b)] [ADDRESS (20b)]
            // REG_A nos bits 23-22
            // REG_B nos bits 21-20
            // ADDRESS nos bits 19-0
            case Opcodes.JEQ: // jeq %<regA> %<regB> <mem>
            case Opcodes.JGT: // jgt %<regA> %<regB> <mem>
            case Opcodes.JLW: // jlw %<regA> %<regB> <mem>
                operand1 = (instructionWord >> 22) & 0x3; // RegA ID
                operand2 = (instructionWord >> 20) & 0x3; // RegB ID
                operand3 = instructionWord & 0xFFFFF; // Address (20 bits)
                break;

            // --- Instruções sem Operandos ---
            case Opcodes.RET:
            case Opcodes.HALT:
                // Nenhum operando a ser extraído
                break;

            default:
                // Instrução não reconhecida
                System.err.println("DEBUG: Opcode desconhecido durante decodificação: 0x" + Integer.toHexString(opcode));
                return null; // Retorna null para indicar erro ou instrução desconhecida
        }
        return new Instruction(opcode, operand1, operand2, operand3, instructionWord);
    }

    /**
     * Executa a instrução decodificada.
     * @param instruction O objeto Instruction contendo o opcode e os operandos.
     */
    private void execute(Instruction instruction) {
        // System.out.println("Executando: " + Opcodes.getInstructionName(instruction.getOpcode()) + 
        //                    " Op1: " + instruction.getOperand1() + " Op2: " + instruction.getOperand2() + 
        //                    " Op3: " + instruction.getOperand3());

        int opcode = instruction.getOpcode();
        Register regA, regB; // Variáveis temporárias para registradores
        int memAddr, memVal; // Variáveis temporárias para endereços e valores de memória
        int immediate;

        switch (opcode) {
            case Opcodes.ADD_REG_REG: // add %<regA> %<regB> || RegB <- RegA + RegB
                regA = getRegisterById(instruction.getOperand1());
                regB = getRegisterById(instruction.getOperand2());
                regB.set(ula.add(bus.transferToInttbus1(regA.get()), bus.transferToInttbus2(regB.get())));
                break;
            case Opcodes.ADD_MEM_REG: // add <mem> %<regA> || RegA <- memória[mem] + RegA
                memAddr = instruction.getOperand1(); // operand1 é o endereço de memória
                regA = getRegisterById(instruction.getOperand2()); // operand2 é o ID do registrador
                memVal = bus.moveData(memory.read(memAddr));
                regA.set(ula.add(bus.transferToInttbus1(memVal), bus.transferToInttbus2(regA.get())));
                break;
            case Opcodes.ADD_REG_MEM: // add %<regA> <mem> || Memória[mem] <- RegA + memória[mem]
                regA = getRegisterById(instruction.getOperand1()); // operand1 é o ID do registrador
                memAddr = instruction.getOperand2(); // operand2 é o endereço de memória
                memVal = bus.moveData(memory.read(memAddr));
                memory.write(memAddr, ula.add(bus.transferToInttbus1(regA.get()), bus.transferToInttbus2(memVal)));
                break;
            case Opcodes.SUB_REG_REG: // sub %<regA> %<regB> || RegB <- RegA - RegB
                regA = getRegisterById(instruction.getOperand1());
                regB = getRegisterById(instruction.getOperand2());
                regB.set(ula.sub(bus.transferToInttbus1(regA.get()), bus.transferToInttbus2(regB.get())));
                break;
            case Opcodes.SUB_MEM_REG: // sub <mem> %<regA> || RegA <- memória[mem] - RegA
                memAddr = instruction.getOperand1();
                regA = getRegisterById(instruction.getOperand2());
                memVal = bus.moveData(memory.read(memAddr));
                regA.set(ula.sub(bus.transferToInttbus1(memVal), bus.transferToInttbus2(regA.get())));
                break;
            case Opcodes.SUB_REG_MEM: // sub %<regA> <mem> || memória[mem] <- RegA - memória[mem]
                regA = getRegisterById(instruction.getOperand1());
                memAddr = instruction.getOperand2();
                memVal = bus.moveData(memory.read(memAddr));
                memory.write(memAddr, ula.sub(bus.transferToInttbus1(regA.get()), bus.transferToInttbus2(memVal)));
                break;
            case Opcodes.MOVE_MEM_REG: // move <mem> %<regA> || RegA <- memória[mem]
                memAddr = instruction.getOperand1();
                regA = getRegisterById(instruction.getOperand2());
                regA.set(bus.moveData(memory.read(memAddr)));
                break;
            case Opcodes.MOVE_REG_MEM: // move %<regA> <mem> || memória[mem] <- RegA
                regA = getRegisterById(instruction.getOperand1());
                memAddr = instruction.getOperand2();
                memory.write(memAddr, bus.moveData(regA.get()));
                break;
            case Opcodes.MOVE_REG_REG: // move %<regA> %<regB> || RegB <- RegA
                regA = getRegisterById(instruction.getOperand1());
                regB = getRegisterById(instruction.getOperand2());
                regB.set(bus.moveData(regA.get()));
                break;
            case Opcodes.MOVE_IMM_REG: // move imm <val> %<regA> || RegA <- immediate
                immediate = instruction.getOperand1(); // O valor imediato
                regA = getRegisterById(instruction.getOperand2()); // O registrador de destino
                regA.set(bus.moveData(immediate));
                break;
            case Opcodes.INC_REG: // inc %<regA> || RegA ++
                regA = getRegisterById(instruction.getOperand1());
                regA.set(ula.inc(bus.transferToInttbus1(regA.get())));
                break;
            case Opcodes.INC_MEM: // inc <mem> || memória[mem] ++
                memAddr = instruction.getOperand1();
                memVal = bus.moveData(memory.read(memAddr));
                memory.write(memAddr, ula.inc(bus.transferToInttbus1(memVal)));
                break;
            case Opcodes.JMP: // jmp <mem> || PC <- mem (desvio incondicional)
                PC.set(bus.moveData(instruction.getOperand1()));
                break;
            case Opcodes.JN: // jn <mem> || se última operação<0 então PC <- mem (desvio condicional)
                if (flags.isNegative()) {
                    PC.set(bus.moveData(instruction.getOperand1()));
                }
                break;
            case Opcodes.JZ: // jz <mem> || se última operação=0 então PC <- mem (desvio condicional)
                if (flags.isZero()) {
                    PC.set(bus.moveData(instruction.getOperand1()));
                }
                break;
            case Opcodes.JNZ: // jnz <mem> || se última operação|=0 então PC <- mem (desvio condicional)
                if (!flags.isZero()) {
                    PC.set(bus.moveData(instruction.getOperand1()));
                }
                break;
            case Opcodes.JEQ: // jeq %<regA> %<regB> <mem> || se RegA==RegB então PC <- mem (desvio condicional)
                regA = getRegisterById(instruction.getOperand1());
                regB = getRegisterById(instruction.getOperand2());
                ula.compare(regA.get(), regB.get()); // Compara e atualiza flags (result = RegA - RegB)
                if (flags.isZero()) { // Se RegA - RegB == 0, então RegA == RegB
                    PC.set(bus.moveData(instruction.getOperand3()));
                }
                break;
            case Opcodes.JGT: // jgt %<regA> %<regB> <mem> || se RegA>RegB então PC <- mem (desvio condicional)
                regA = getRegisterById(instruction.getOperand1());
                regB = getRegisterById(instruction.getOperand2());
                ula.compare(regA.get(), regB.get()); // Compara e atualiza flags (result = RegA - RegB)
                // Se RegA > RegB, então RegA - RegB > 0. Nem zero, nem negativo.
                if (!flags.isZero() && !flags.isNegative()) {
                    PC.set(bus.moveData(instruction.getOperand3()));
                }
                break;
            case Opcodes.JLW: // jlw %<regA> %<regB> <mem> || se RegA<RegB então PC <- mem (desvio condicional)
                regA = getRegisterById(instruction.getOperand1());
                regB = getRegisterById(instruction.getOperand2());
                ula.compare(regA.get(), regB.get()); // Compara e atualiza flags (result = RegA - RegB)
                // Se RegA < RegB, então RegA - RegB < 0. Negativo.
                if (flags.isNegative()) {
                    PC.set(bus.moveData(instruction.getOperand3()));
                }
                break;
            case Opcodes.CALL: // call <mem> || PC <- mem (push(PC++) )
                // PC já foi incrementado no fetch, então é o endereço da *próxima* instrução
                stack.push(bus.moveData(PC.get())); 
                PC.set(bus.moveData(instruction.getOperand1())); // Desvia para o endereço da chamada
                break;
            case Opcodes.RET: // ret || PC <- pop()
                PC.set(bus.moveData(stack.pop()));
                break;
            case Opcodes.IMUL: // imul %<regA> %<regB>
                System.out.println("Executando IMUL...");
                
                // 1. Salvar contexto atual (REG0-3, PC, FLAGS)
                saveContext(IMUL_REGS_SAVE_AREA_START);

                // 2. Passar operandos para o microprograma.
                // A instrução IMUL assembly define os registradores de origem.
                // Vamos copiá-los para locais de memória fixos onde o microprograma possa acessá-los.
                // (Isso simula o que aconteceria se o microprograma usasse registradores internos
                // ou precisasse mover os valores para locais conhecidos).
                // Exemplo: IMUL %REG0 %REG1 -> multiplicaria REG0 * REG1
                memory.write(IMUL_OP1_TEMP_ADDR, bus.moveData(getRegisterById(instruction.getOperand1()).get()));
                memory.write(IMUL_OP2_TEMP_ADDR, bus.moveData(getRegisterById(instruction.getOperand2()).get()));

                // 3. Armazenar o endereço de retorno para depois que o microprograma terminar.
                // O PC já está apontando para a instrução APÓS o IMUL.
                memory.write(IMUL_RETURN_PC_SAVE_ADDR, PC.get()); 
                
                // 4. Desviar o PC para o início do microprograma IMUL.
                PC.set(bus.moveData(IMUL_MICROPROGRAM_START));
                
                // O runCycle() continuará executando as instruções do microprograma.
                // O microprograma deve, ao final, ler o PC de retorno de IMUL_RETURN_PC_SAVE_ADDR
                // e fazer um JMP para ele, além de escrever o resultado em IMUL_RESULT_ADDRESS.
                break;
            case Opcodes.HALT: // Instrução HALT para parar a CPU
                stop();
                break;
            default:
                System.err.println("Instrução não reconhecida ou não implementada durante execução: 0x" + Integer.toHexString(opcode) + " (Raw: 0x" + Integer.toHexString(instruction.getRawInstruction()) + ")");
                stop();
                break;
        }
    }

    // --- Métodos Auxiliares para IMUL e Context Switching ---
    /**
     * Salva o estado dos registradores REG0-REG3, PC e FLAGS em uma área da memória.
     * @param startAddress O endereço inicial da área de salvamento.
     */
    private void saveContext(int startAddress) {
        memory.write(startAddress, REG0.get());
        memory.write(startAddress + 1, REG1.get());
        memory.write(startAddress + 2, REG2.get());
        memory.write(startAddress + 3, REG3.get());
        memory.write(startAddress + 4, PC.get());
        // Salvar flags: compacta Z e N em um int. (N << 1) | Z
        int packedFlags = (flags.isNegative() ? 1 : 0) << 1 | (flags.isZero() ? 1 : 0);
        memory.write(startAddress + 5, packedFlags);
        System.out.println("Contexto salvo em: " + startAddress + " (REGs, PC, Flags)");
    }

    /**
     * Restaura o estado dos registradores REG0-REG3, PC e FLAGS de uma área da memória.
     * @param startAddress O endereço inicial da área de salvamento.
     */
    private void restoreContext(int startAddress) {
        REG0.set(memory.read(startAddress));
        REG1.set(memory.read(startAddress + 1));
        REG2.set(memory.read(startAddress + 2));
        REG3.set(memory.read(startAddress + 3));
        PC.set(memory.read(startAddress + 4)); // PC restaurado para o ponto de retorno
        int packedFlags = memory.read(startAddress + 5);
        flags.setNegative(((packedFlags >> 1) & 1) == 1);
        flags.setZero((packedFlags & 1) == 1);
        System.out.println("Contexto restaurado de: " + startAddress + " (REGs, PC, Flags)");
    }

    // --- Getters para componentes (útil para depuração ou acesso externo) ---
    public Memory getMemory() {
        return memory;
    }

    public Register getPC() {
        return PC;
    }

    public Register getRegisterById(int id) {
        if (id < 0 || id > 3) {
            throw new IllegalArgumentException("ID de registrador inválido: " + id + ". Esperado 0-3.");
        }
        return generalPurposeRegisters.get(id);
    }

    public Register getStkTOP() {
        return StkTOP;
    }

    public Register getStkBOT() {
        return StkBOT;
    }
    
    // Método para imprimir o estado dos registradores (útil para depuração)
    public void printRegisters() {
        System.out.println("--- Registers State (PC: " + PC.get() + ") ---");
        System.out.println("IR:  " + String.format("0x%08X", IR.get()));
        System.out.println("PC:  " + PC.get());
        System.out.println("REG0: " + REG0.get());
        System.out.println("REG1: " + REG1.get());
        System.out.println("REG2: " + REG2.get());
        System.out.println("REG3: " + REG3.get());
        System.out.println("StkTOP: " + StkTOP.get());
        System.out.println("StkBOT: " + StkBOT.get());
        System.out.println(flags);
        System.out.println("-----------------");
    }

    // --- Main para Teste Rápido da CPU (Remover ou mover para Main.java) ---
    public static void main(String[] args) {
        System.out.println("--- Teste Básico da CPU ---");
        int memorySize = 256; // 256 palavras de memória
        CPU cpu = new CPU(memorySize);
        cpu.printRegisters();

        // 1. Carregar um microprograma IMUL (muito simples, apenas um retorno)
        // Você precisará de um microprograma IMUL REAL que faça a multiplicação!
        // Exemplo de microprograma IMUL que apenas retorna:
        // load_return_pc: move <IMUL_RETURN_PC_SAVE_ADDR> %REG0
        // jmp %REG0
        //
        // Para simular, vamos fazer um microprograma dummy que apenas salta de volta.
        // Na prática, ele faria a multiplicação, salvaria em IMUL_RESULT_ADDRESS
        // e então retornaria.
        int[] dummyImulMicroprogram = {
            (Opcodes.MOVE_MEM_REG << 24) | (0 << 22) | cpu.IMUL_RETURN_PC_SAVE_ADDR, // move IMUL_RETURN_PC_SAVE_ADDR %REG0
            (Opcodes.JMP << 24) | (0 << 22) // jmp %REG0 (o endereço de salto estaria em REG0)
            // CUIDADO: Este JMP precisa ser para o valor em REG0.
            // O formato JMP <mem> usa o operando1 como o endereço literal.
            // Para pular para um valor em um registrador, você precisaria de uma instrução JMP_REG.
            // Ou o microprograma IMUL leria o PC de retorno, colocaria no PC, e então um RET.
            // Vamos mudar para que o microprograma "popule" o PC de retorno diretamente.
            //
            // Microprograma IMUL mais realista (Exemplo: REG0 * REG1 -> REG2)
            // Supondo: operandos em IMUL_OP1_TEMP_ADDR, IMUL_OP2_TEMP_ADDR
            //           resultado vai para IMUL_RESULT_ADDRESS
            //           PC de retorno em IMUL_RETURN_PC_SAVE_ADDR
            //
            // Pseudo-Assembly do Microprograma IMUL:
            // imul_start:
            //   move <IMUL_OP1_TEMP_ADDR> %REG2  ; REG2 = Operando1
            //   move <IMUL_OP2_TEMP_ADDR> %REG3  ; REG3 = Operando2 (contador/multiplicador)
            //   move imm 0 %REG1                 ; REG1 = 0 (acumulador para o resultado)
            //   jz end_imul                      ; Se Operando2 for 0, pula direto para o fim
            // imul_loop:
            //   add %REG2 %REG1                  ; REG1 = REG2 + REG1 (acumula Operando1)
            //   dec %REG3                        ; Decrementa o contador (Operando2)
            //   jnz imul_loop                    ; Se contador != 0, continua o loop
            // end_imul:
            //   move %REG1 <IMUL_RESULT_ADDRESS> ; Salva o resultado em IMUL_RESULT_ADDRESS
            //   move <IMUL_RETURN_PC_SAVE_ADDR> %REG0 ; REG0 = PC de retorno
            //   jmp %REG0                        ; Salta de volta para o programa principal
            //
            // Codificação desse microprograma (simplificado, você terá que montar o seu):
            // Assumindo que IMUL_RESULT_ADDRESS e IMUL_RETURN_PC_SAVE_ADDR são endereços válidos
        };

        // Vamos criar um microprograma IMUL placeholder para teste
        // Ele apenas copia o primeiro operando para o resultado e retorna.
        // Você PRECISARÁ substituir isso pelo microprograma de multiplicação real.
        // O microprograma de verdade terá o laço de soma.
        int[] placeholderImul = {
            (Opcodes.MOVE_MEM_REG << 24) | (0 << 22) | cpu.IMUL_OP1_TEMP_ADDR, // move IMUL_OP1_TEMP_ADDR %REG0
            (Opcodes.MOVE_REG_MEM << 24) | (0 << 22) | cpu.IMUL_RESULT_ADDRESS, // move %REG0 IMUL_RESULT_ADDRESS
            (Opcodes.MOVE_MEM_REG << 24) | (1 << 22) | cpu.IMUL_RETURN_PC_SAVE_ADDR, // move IMUL_RETURN_PC_SAVE_ADDR %REG1 (use REG1 temporariamente)
            (Opcodes.JMP << 24) | (1) // JMP para o endereço em REG1. PRECISA DE JMP_REG ou similar para isso.
                                    // OU FAZER O MICROPROGRAMA ESCREVER DIRETO NO PC!
                                    // A alternativa mais fácil para o microprograma é:
                                    // move <IMUL_RETURN_PC_SAVE_ADDR> <PC_REGISTER_ADDRESS_IN_MEMORY_MAP>
                                    // Mas nosso PC é um objeto Register, não um endereço de memória.
                                    // Então, o microprograma deve usar um JMP para o endereço LIDO.
                                    // O JMP na nossa arquitetura é JMP <mem>, onde <mem> é um literal.
                                    // Vamos fazer o IMUL_RETURN_PC_SAVE_ADDR conter o valor, e o microprograma
                                    // precisa carregá-lo e então pular para ele.
                                    // Isso requer JMP_INDIRECT ou JMP para um registrador.
                                    // Como não temos JMP_REG, o microprograma precisa de uma forma de "voltar".
                                    // A maneira mais simples é a última instrução do microprograma
                                    // ser `JMP <valor_do_PC_de_retorno_salvo_anteriormente>`
                                    // Mas o microprograma NÃO CONHECE o PC de retorno até que a CPU o salve.
                                    // O microprograma precisa ler IMUL_RETURN_PC_SAVE_ADDR e JMP para ele.
                                    // Como a CPU.execute() para JMP espera um literal, o microprograma não pode fazer JMP %REG.
                                    // SOLUÇÃO: O microprograma imul vai para um "ponto de encontro" final,
                                    // onde a CPU detecta que o microprograma terminou e faz o restoreContext.
                                    // Ou a instrução final do microprograma IMUL pode ser um tipo de 'RET_IMUL'
                                    // que a CPU detecta. Vamos adicionar uma instrução especial para isso.
                                    // Ou podemos simplesmente ter um JMP para um endereço fixo.
        };

        // ALTERNATIVA: Para o microprograma IMUL, vamos forçar uma instrução "IMUL_END" (0x7F)
        // que a CPU detecta para restaurar o contexto.
        // Isso é mais simples do que fazer o microprograma manipular o PC de retorno diretamente
        // com as instruções de JUMP que temos.
        
        // Microprograma IMUL REALISTA (usando REG0 e REG1 para entrada, REG2 para resultado)
        // Assume que REG0 e REG1 já estão copiados para IMUL_OP1_TEMP_ADDR e IMUL_OP2_TEMP_ADDR.
        // Resultado em IMUL_RESULT_ADDRESS.
        // O microprograma opera em REG0 (Multiplicando), REG1 (Multiplicador/Contador), REG2 (Acumulador)
        // REG0 <--- IMUL_OP1_TEMP_ADDR (Multiplicando)
        // REG1 <--- IMUL_OP2_TEMP_ADDR (Multiplicador)
        // REG2 <--- 0 (Acumulador)
        // REG3 <--- 0 (Constante Zero para comparações)
        //
        // imul_microprogram:
        //  move <IMUL_OP1_TEMP_ADDR> %REG0   ; REG0 = multiplicando
        //  move <IMUL_OP2_TEMP_ADDR> %REG1   ; REG1 = multiplicador (contador)
        //  move imm 0 %REG2                 ; REG2 = resultado = 0
        //  move imm 0 %REG3                 ; REG3 = constante 0
        //  
        //  jeq %REG1 %REG3 end_imul_micro  ; se multiplicador (REG1) == 0, salta para o fim
        //  
        // imul_loop:
        //  add %REG0 %REG2                  ; REG2 = REG0 + REG2 (acumula multiplicando)
        //  inc %REG3                        ; REG3 = REG3 + 1 (contador de loops)
        //  sub %REG3 %REG1                  ; REG1 <- REG3 - REG1 (subtrai contador do multiplicador original)
        //  jnz imul_loop                    ; se REG1 != 0, continua (mas isso está errado, deve ser reg3 vs reg1)
        //
        // Correção da lógica do loop (usando REG1 como contador e decrementando ele):
        // imul_microprogram:
        //  move <IMUL_OP1_TEMP_ADDR> %REG0   ; REG0 = multiplicando (multiplicador a ser somado)
        //  move <IMUL_OP2_TEMP_ADDR> %REG1   ; REG1 = multiplicador (contador)
        //  move imm 0 %REG2                 ; REG2 = resultado = 0
        //  
        //  jz %REG1 end_imul_micro          ; se multiplicador (REG1) == 0, salta para o fim
        //  
        // imul_loop:
        //  add %REG0 %REG2                  ; REG2 = REG0 + REG2 (acumula multiplicando)
        //  dec %REG1                        ; Decrementa o multiplicador (contador)
        //  jnz imul_loop                    ; Se REG1 != 0, continua o loop
        //
        // end_imul_micro:
        //  move %REG2 <IMUL_RESULT_ADDRESS> ; Salva o resultado em IMUL_RESULT_ADDRESS
        //  halt_imul                        ; Nova instrução para indicar o fim do microprograma IMUL
        //
        // Adicionando um HALT_IMUL (0x7F) para que a CPU saiba quando restaurar.
        // Isso é uma extensão da arquitetura, mas simplifica o fluxo.

        // Opcodes e valores precisam ser os que o LOADER geraria!
        int[] actualImulMicroprogram = {
            (Opcodes.MOVE_MEM_REG << 24) | (0 << 22) | cpu.IMUL_OP1_TEMP_ADDR,   // move IMUL_OP1_TEMP_ADDR %REG0
            (Opcodes.MOVE_MEM_REG << 24) | (1 << 22) | cpu.IMUL_OP2_TEMP_ADDR,   // move IMUL_OP2_TEMP_ADDR %REG1
            (Opcodes.MOVE_IMM_REG << 24) | (2 << 22) | 0,                       // move imm 0 %REG2 (resultado)

            // Loop para multiplicação (laço de somas)
            // JZ %REG1 imul_end_address (pula se REG1 for 0)
            (Opcodes.JZ << 24) | (cpu.IMUL_MICROPROGRAM_START + 7), // JZ REG1 para end_imul_micro (PC+7)
                                                                    // Hardcoded jump target, idealmente seria via label do assembler

            // imul_loop_start: (PC atual + 4)
            (Opcodes.ADD_REG_REG << 24) | (0 << 22) | (2 << 20), // add %REG0 %REG2 (REG2 += REG0)
            (Opcodes.INC_REG << 24) | (1 << 22),              // dec %REG1 (decrementa contador)
            (Opcodes.JNZ << 24) | (cpu.IMUL_MICROPROGRAM_START + 4), // jnz imul_loop_start (volta se REG1 != 0)

            // imul_end_micro: (PC atual + 7)
            (Opcodes.MOVE_REG_MEM << 24) | (2 << 22) | cpu.IMUL_RESULT_ADDRESS, // move %REG2 IMUL_RESULT_ADDRESS
            Opcodes.HALT // Use HALT como instrução para retornar do microprograma, a CPU vai restaurar o contexto
        };
        cpu.loadImulMicroprogram(actualImulMicroprogram);


        // 2. Carregar um programa principal de teste
        // move imm 5 %REG0
        // move imm 3 %REG1
        // imul %REG0 %REG1 ; REG0 = 5, REG1 = 3 -> Result = 15 (em IMUL_RESULT_ADDRESS)
        // move <IMUL_RESULT_ADDRESS> %REG2 ; REG2 = 15
        // halt
        int[] program = {
            (Opcodes.MOVE_IMM_REG << 24) | (0 << 22) | 5,  // move imm 5 %REG0
            (Opcodes.MOVE_IMM_REG << 24) | (1 << 22) | 3,  // move imm 3 %REG1
            (Opcodes.IMUL << 24) | (0 << 22) | (1 << 20),  // imul %REG0 %REG1
            (Opcodes.MOVE_MEM_REG << 24) | (2 << 22) | cpu.IMUL_RESULT_ADDRESS, // move IMUL_RESULT_ADDRESS %REG2
            Opcodes.HALT                                 // halt
        };

        // O programa principal começa após o microprograma IMUL
        int programStart = cpu.IMUL_MICROPROGRAM_START + actualImulMicroprogram.length;
        cpu.loadProgram(program, programStart);

        cpu.printRegisters();
        System.out.println("\nIniciando execução do programa principal...");
        cpu.start();
        cpu.printRegisters();
        
        System.out.println("Verificando resultado IMUL em REG2: " + cpu.getRegisterById(2).get());
        System.out.println("Valor da Memória em IMUL_RESULT_ADDRESS: " + cpu.getMemory().read(cpu.IMUL_RESULT_ADDRESS));
    }
}