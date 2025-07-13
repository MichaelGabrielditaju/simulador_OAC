package utils;

import core.Opcodes; // Importa a classe Opcodes para usar as constantes
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Loader {

    private Map<String, Integer> labelAddresses; // Mapa para armazenar labels e seus endereços
    private int programStartAddress; // Onde o programa será carregado na memória
    private int currentAddress;      // Endereço atual durante a montagem

    public Loader(int programStartAddress) {
        this.programStartAddress = programStartAddress;
        this.labelAddresses = new HashMap<>();
    }

    /**
     * Carrega um arquivo assembly (.asm), monta-o em código de máquina
     * e retorna o array de inteiros (palavras) pronto para ser carregado na memória.
     *
     * @param filePath O caminho do arquivo assembly.
     * @return Um array de inteiros representando o código de máquina.
     * @throws IOException Se houver um erro de leitura do arquivo.
     * @throws IllegalArgumentException Se houver um erro de sintaxe ou label não encontrado.
     */
    public int[] loadAssembly(String filePath) throws IOException {
        System.out.println("Iniciando montagem do arquivo: " + filePath);
        // Primeira passagem: mapear labels para endereços
        pass1_mapLabels(filePath);
        System.out.println("Labels mapeados: " + labelAddresses);

        // Segunda passagem: gerar código de máquina
        int[] machineCode = pass2_generateMachineCode(filePath);
        System.out.println("Montagem concluída. Tamanho do código: " + machineCode.length + " palavras.");
        return machineCode;
    }

    // --- Passagem 1: Mapear Labels ---
    private void pass1_mapLabels(String filePath) throws IOException {
        try (BufferedReader reader = new BufferedReader(new FileReader(filePath))) {
            String line;
            currentAddress = programStartAddress; // Começa a partir do endereço de carga do programa

            while ((line = reader.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty() || line.startsWith("#")) { // Ignora linhas vazias e comentários
                    continue;
                }

                // Remove comentários inline
                int commentIndex = line.indexOf('#');
                if (commentIndex != -1) {
                    line = line.substring(0, commentIndex).trim();
                }

                // Verifica se é uma label
                if (line.endsWith(":")) {
                    String label = line.substring(0, line.length() - 1).trim();
                    if (labelAddresses.containsKey(label)) {
                        throw new IllegalArgumentException("Erro: Label duplicada encontrada: " + label);
                    }
                    labelAddresses.put(label, currentAddress);
                } else {
                    // Se não é uma label, é uma instrução, então avança o endereço
                    // Assumimos que cada instrução ocupa 1 palavra de memória (1 int)
                    currentAddress++;
                }
            }
        }
    }

    // --- Passagem 2: Gerar Código de Máquina ---
    private int[] pass2_generateMachineCode(String filePath) throws IOException {
        List<Integer> machineCodeList = new ArrayList<>();
        try (BufferedReader reader = new BufferedReader(new FileReader(filePath))) {
            String line;
            // currentAddress é redefinido aqui apenas para controle de depuração, não de alocação.
            // A alocação real é feita pelo tamanho da lista e `programStartAddress`.
            currentAddress = programStartAddress; 

            while ((line = reader.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty() || line.startsWith("#")) {
                    continue;
                }

                // Remove comentários inline
                int commentIndex = line.indexOf('#');
                if (commentIndex != -1) {
                    line = line.substring(0, commentIndex).trim();
                }

                // Se for uma label, apenas a ignoramos (já foi processada na Passagem 1)
                if (line.endsWith(":")) {
                    // currentAddress já foi avançado na pass1 para a próxima instrução
                    // aqui ela apenas confirma que o endereço está no lugar certo
                    // for debugging, can check if currentAddress == labelAddresses.get(label)
                    continue; 
                }

                // Processa a instrução
                int instructionWord = encodeInstruction(line, currentAddress);
                machineCodeList.add(instructionWord);
                currentAddress++; // Avança para a próxima posição de memória
            }
        }
        return machineCodeList.stream().mapToInt(i -> i).toArray();
    }

    // --- Método para codificar uma instrução assembly em um inteiro ---
    private int encodeInstruction(String assemblyLine, int currentInstructionAddress) {
        // Divide a linha em partes: opcode e operandos
        String[] parts = assemblyLine.split("\\s+", 2); // Divide no primeiro espaço
        String opcodeStr = parts[0].toLowerCase();
        String operandsStr = (parts.length > 1) ? parts[1].trim() : "";

        int opcodeValue;
        
        // Trata os opcodes especiais que podem ter sufixos (ex: _reg_reg, _mem_reg)
        // Isso é crucial para distinguir as diferentes formas de ADD, SUB, MOVE, INC
        if (opcodeStr.startsWith("add")) {
            if (operandsStr.matches("%reg[0-3]\\s+%reg[0-3]")) {
                opcodeValue = Opcodes.ADD_REG_REG;
            } else if (operandsStr.matches("\\d+\\s+%reg[0-3]")) { // Ex: add 10 %REG0
                opcodeValue = Opcodes.ADD_MEM_REG; // mem aqui significa um valor (constante ou endereço)
            } else if (operandsStr.matches("%reg[0-3]\\s+\\d+")) { // Ex: add %REG0 10
                opcodeValue = Opcodes.ADD_REG_MEM;
            } else {
                 throw new IllegalArgumentException("Sintaxe inválida para ADD: " + assemblyLine);
            }
        } else if (opcodeStr.startsWith("sub")) {
            if (operandsStr.matches("%reg[0-3]\\s+%reg[0-3]")) {
                opcodeValue = Opcodes.SUB_REG_REG;
            } else if (operandsStr.matches("\\d+\\s+%reg[0-3]")) {
                opcodeValue = Opcodes.SUB_MEM_REG;
            } else if (operandsStr.matches("%reg[0-3]\\s+\\d+")) {
                opcodeValue = Opcodes.SUB_REG_MEM;
            } else {
                throw new IllegalArgumentException("Sintaxe inválida para SUB: " + assemblyLine);
            }
        } else if (opcodeStr.startsWith("move")) {
            if (operandsStr.matches("\\d+\\s+%reg[0-3]")) { // move <mem> %regA (mem pode ser literal)
                opcodeValue = Opcodes.MOVE_MEM_REG;
            } else if (operandsStr.matches("%reg[0-3]\\s+\\d+")) { // move %regA <mem>
                opcodeValue = Opcodes.MOVE_REG_MEM;
            } else if (operandsStr.matches("%reg[0-3]\\s+%reg[0-3]")) { // move %regA %regB
                opcodeValue = Opcodes.MOVE_REG_REG;
            } else if (operandsStr.matches("imm\\s+\\d+\\s+%reg[0-3]")) { // move imm <val> %regA
                opcodeValue = Opcodes.MOVE_IMM_REG;
            } else {
                throw new IllegalArgumentException("Sintaxe inválida para MOVE: " + assemblyLine);
            }
        } else if (opcodeStr.startsWith("inc")) {
             if (operandsStr.matches("%reg[0-3]")) {
                 opcodeValue = Opcodes.INC_REG;
             } else if (operandsStr.matches("\\d+")) { // inc <mem>
                 opcodeValue = Opcodes.INC_MEM;
             } else {
                 throw new IllegalArgumentException("Sintaxe inválida para INC: " + assemblyLine);
             }
        } else {
            // Para opcodes simples sem variações de operando
            opcodeValue = Opcodes.getOpcode(opcodeStr);
            if (opcodeValue == null) {
                throw new IllegalArgumentException("Opcode desconhecido: " + opcodeStr + " na linha: " + assemblyLine);
            }
        }

        // Começa a construir a palavra da instrução com o opcode
        int instructionWord = opcodeValue << (32 - Opcodes.OPCODE_BITS); // Coloca opcode nos bits mais altos

        // Processa os operandos com base no opcode
        String[] ops; // Array para os operandos parseados

        switch (opcodeValue) {
            // --- Instruções com 2 Registradores (RegA, RegB) ---
            case Opcodes.ADD_REG_REG:
            case Opcodes.SUB_REG_REG:
            case Opcodes.MOVE_REG_REG:
                ops = operandsStr.split("\\s+%"); // Divide por espaço e % para pegar "REG0" "REG1"
                int regA_id = parseRegister(ops[0].trim());
                int regB_id = parseRegister(ops[1].trim());
                instructionWord |= (regA_id << 22); // regA nos bits 23-22
                instructionWord |= (regB_id << 20); // regB nos bits 21-20
                break;

            // --- Instruções com Memória/Imediato e 1 Registrador ---
            case Opcodes.ADD_MEM_REG: // add <mem> %<regA>
            case Opcodes.SUB_MEM_REG: // sub <mem> %<regA>
            case Opcodes.MOVE_MEM_REG: // move <mem> %<regA>
                ops = operandsStr.split("\\s+%");
                int memValOrAddr = parseMemOrImmediate(ops[0].trim());
                regA_id = parseRegister(ops[1].trim());
                instructionWord |= (regA_id << 22); // regA nos bits 23-22
                instructionWord |= (memValOrAddr & 0x3FFFFF); // Valor/Endereço nos 22 bits restantes (bits 21-0)
                break;

            case Opcodes.ADD_REG_MEM: // add %<regA> <mem>
            case Opcodes.SUB_REG_MEM: // sub %<regA> <mem>
            case Opcodes.MOVE_REG_MEM: // move %<regA> <mem>
                ops = operandsStr.split("\\s+"); // divide por espaço
                regA_id = parseRegister(ops[0].trim().substring(1)); // Remove '%'
                memValOrAddr = parseMemOrImmediate(ops[1].trim());
                instructionWord |= (regA_id << 22); // regA nos bits 23-22
                instructionWord |= (memValOrAddr & 0x3FFFFF); // Valor/Endereço nos 22 bits restantes (bits 21-0)
                break;

            // --- Instrução com Imediato e 1 Registrador ---
            case Opcodes.MOVE_IMM_REG: // move imm <value> %regA
                ops = operandsStr.split("\\s+"); // Ex: ["imm", "123", "%REG0"]
                int immediateValue = Integer.parseInt(ops[1].trim());
                regA_id = parseRegister(ops[2].trim());
                instructionWord |= (regA_id << 22); // regA nos bits 23-22
                instructionWord |= (immediateValue & 0x3FFFFF); // Imediato nos 22 bits restantes (bits 21-0)
                break;

            // --- Instruções com 1 Registrador ---
            case Opcodes.INC_REG: // inc %<regA>
                regA_id = parseRegister(operandsStr.trim());
                instructionWord |= (regA_id << 22); // regA nos bits 23-22
                break;

            // --- Instruções com 1 Endereço de Memória ---
            case Opcodes.INC_MEM: // inc <mem> (aqui <mem> é um endereço literal ou label)
            case Opcodes.JMP:     // jmp <mem>
            case Opcodes.JN:      // jn <mem>
            case Opcodes.JZ:      // jz <mem>
            case Opcodes.JNZ:     // jnz <mem>
            case Opcodes.CALL:    // call <mem>
                int address = parseMemOrImmediate(operandsStr.trim()); // Pode ser literal ou label
                instructionWord |= (address & 0x3FFFFF); // Endereço nos 24 bits restantes (bits 23-0)
                break;

            // --- Instruções com 3 Operandos (RegA, RegB, Memória) ---
            case Opcodes.JEQ: // jeq %<regA> %<regB> <mem>
            case Opcodes.JGT: // jgt %<regA> %<regB> <mem>
            case Opcodes.JLW: // jlw %<regA> %<regB> <mem>
                // Formato: %regA %regB mem
                Pattern p3 = Pattern.compile("%(reg[0-3])\\s+%(reg[0-3])\\s+([a-zA-Z0-9_]+)");
                Matcher m3 = p3.matcher(operandsStr);
                if (!m3.matches()) {
                    throw new IllegalArgumentException("Sintaxe inválida para instrução de 3 operandos: " + assemblyLine);
                }
                regA_id = parseRegister(m3.group(1));
                regB_id = parseRegister(m3.group(2));
                address = parseMemOrImmediate(m3.group(3)); // Pode ser literal ou label
                
                instructionWord |= (regA_id << 22); // regA nos bits 23-22
                instructionWord |= (regB_id << 20); // regB nos bits 21-20
                instructionWord |= (address & 0xFFFFF); // Endereço nos 20 bits restantes (bits 19-0)
                break;

            // --- Instruções sem Operandos ---
            case Opcodes.RET:
            case Opcodes.HALT:
                // Nenhum operando a ser adicionado
                break;
            
            // --- Instruções Especiais ---
            case Opcodes.IMUL: // imul %<regA> %<regB> - ou outro formato definido para IMUL
                // Por simplicidade, vamos assumir imul %regA %regB por enquanto.
                // O microprograma IMUL será invocado pela CPU.
                // Aqui apenas codificamos os operandos que a instrução IMUL "real" precisa.
                ops = operandsStr.split("\\s+%");
                regA_id = parseRegister(ops[0].trim());
                regB_id = parseRegister(ops[1].trim());
                instructionWord |= (regA_id << 22); // regA nos bits 23-22
                instructionWord |= (regB_id << 20); // regB nos bits 21-20
                break;

            default:
                throw new IllegalArgumentException("Codificação não implementada para opcode: " + Opcodes.getInstructionName(opcodeValue) + " na linha: " + assemblyLine);
        }

        return instructionWord;
    }

    // --- Métodos Auxiliares de Parsing ---

    // Converte nome de registrador (ex: "REG0") para ID numérico (0)
    private int parseRegister(String regName) {
        if (!regName.startsWith("%")) {
            regName = "%" + regName; // Adiciona % se faltar
        }
        if (regName.matches("%reg[0-3]")) {
            return Integer.parseInt(regName.substring(4)); // Pega o número após "reg"
        }
        throw new IllegalArgumentException("Registrador inválido: " + regName);
    }

    // Converte um valor que pode ser um número ou um label em seu endereço/valor
    private int parseMemOrImmediate(String valueStr) {
        try {
            return Integer.parseInt(valueStr); // Tenta converter para número (endereço literal ou imediato)
        } catch (NumberFormatException e) {
            // Se não é número, tenta como label
            if (labelAddresses.containsKey(valueStr)) {
                return labelAddresses.get(valueStr);
            }
            throw new IllegalArgumentException("Operando inválido (não é número nem label): " + valueStr);
        }
    }

    // Para depuração
    public void printLabels() {
        System.out.println("--- Mapeamento de Labels ---");
        labelAddresses.forEach((label, address) -> System.out.println(label + " -> " + address));
        System.out.println("----------------------------");
    }
}