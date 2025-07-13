package simulador;

import core.CPU;
import utils.Loader;
import core.Opcodes; // Importar Opcodes para a codificação do microprograma IMUL no Main
import java.io.IOException;

public class Main {

    public static void main(String[] args) {
        // --- Configurações do Simulador ---
        int memorySize = 1024; // Tamanho total da memória em "palavras" (inteiros)
                               // Ajuste conforme a necessidade dos seus programas.
                               // Certifique-se de que é grande o suficiente para:
                               //  - Microprograma IMUL
                               //  - Área de salvamento de contexto IMUL
                               //  - Área de resultado IMUL
                               //  - Variáveis (se existirem no seu programa)
                               //  - O programa principal
                               //  - A pilha (que cresce para baixo)

        // O endereço onde o programa assembly principal será carregado.
        // Ele deve começar APÓS as áreas reservadas para o sistema (IMUL microprograma, etc.).
        // A classe Memory já tem a lógica para essas áreas, então podemos usar.
        int programLoadAddress; // Será definido após a CPU ser instanciada.

        String assemblyFilePath = "programs/test_program.asm"; // Caminho padrão para o arquivo assembly

        // Se um caminho de arquivo for passado como argumento de linha de comando, use-o.
        if (args.length > 0) {
            assemblyFilePath = args[0];
        }

        System.out.println("----- Iniciando Simulador da Arquitetura C -----");
        System.out.println("Tamanho da Memória: " + memorySize + " palavras.");
        System.out.println("Arquivo Assembly a ser carregado: " + assemblyFilePath);

        try {
            // 1. Inicializa a CPU (que por sua vez inicializa Memory, ULA, Flags, Stack, Bus)
            CPU cpu = new CPU(memorySize);
            System.out.println("\nCPU inicializada com componentes.");

            // Pega o endereço de onde o programa principal deve começar, após as áreas reservadas.
            // O Loader precisará deste endereço.
            // Para simplificar, vamos carregar o microprograma IMUL logo após as áreas de salvamento,
            // e o programa principal logo após o microprograma IMUL.
            programLoadAddress = cpu.getMemory().getImulMicroprogramStartAddress() + generateImulMicroprogram().length;


            // 2. Carrega o Microprograma IMUL na memória da CPU
            // Este microprograma é a sua implementação de multiplicação por software.
            // Ele deve ser escrito usando as instruções da sua arquitetura assembly.
            // O exemplo abaixo é codificado diretamente em int[], mas idealmente você
            // usaria o Loader para montar um arquivo .asm para o IMUL também.
            int[] imulMicroprogramCode = generateImulMicroprogram();
            cpu.loadImulMicroprogram(imulMicroprogramCode);
            // Atualiza o endereço de carga do programa principal para ser após o microprograma IMUL
            programLoadAddress = cpu.getMemory().getImulMicroprogramStartAddress() + imulMicroprogramCode.length;
            
            System.out.println("Endereço de carregamento do programa principal: " + programLoadAddress);

            // 3. Inicializa o Loader com o endereço de início do programa
            // O Loader usará este endereço como base para os offsets de labels.
            Loader loader = new Loader(programLoadAddress);

            // 4. Monta o arquivo assembly principal
            System.out.println("\nMontando programa assembly principal...");
            int[] programMachineCode = loader.loadAssembly(assemblyFilePath);
            loader.printLabels(); // Imprime o mapeamento de labels para depuração
            
            // 5. Carrega o código de máquina do programa principal na memória da CPU
            cpu.loadProgram(programMachineCode, programLoadAddress);

            System.out.println("\nEstado inicial dos registradores:");
            cpu.printRegisters();
            System.out.println("\nConteúdo da memória (primeiras posições e área do microprograma IMUL):");
            cpu.getMemory().dumpMemory(0, cpu.getMemory().getImulMicroprogramStartAddress() + imulMicroprogramCode.length + programMachineCode.length + 5);


            // 6. Inicia a execução da CPU
            System.out.println("\n----- Iniciando Execução da CPU -----");
            cpu.start();
            System.out.println("----- Execução da CPU Finalizada -----");

            // 7. Imprime o estado final para depuração
            System.out.println("\nEstado final dos registradores:");
            cpu.printRegisters();
            System.out.println("\nConteúdo da memória após execução (primeiras posições e área de resultados/pilha):");
            cpu.getMemory().dumpMemory(0, cpu.getMemory().getMaxSize() - 1); // Dump da memória completa ou parte relevante
            
            // Exemplo de verificação de resultado de IMUL (se o programa usou IMUL)
            System.out.println("Valor do resultado IMUL em MEM[" + cpu.getMemory().getImulResultAddress() + "]: " + 
                               cpu.getMemory().read(cpu.getMemory().getImulResultAddress()));


        } catch (IOException e) {
            System.err.println("Erro de E/S ao carregar o arquivo assembly: " + e.getMessage());
            e.printStackTrace();
        } catch (IllegalArgumentException e) {
            System.err.println("Erro de montagem ou configuração: " + e.getMessage());
            e.printStackTrace();
        } catch (IllegalStateException e) {
            System.err.println("Erro de execução da CPU: " + e.getMessage());
            e.printStackTrace();
        } catch (Exception e) {
            System.err.println("Ocorreu um erro inesperado: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Gera o código de máquina para o microprograma IMUL.
     * Este é um exemplo simplificado. O microprograma REAL precisa ser testado
     * para garantir a correção da multiplicação e o comportamento de retorno.
     *
     * Pseudocódigo do microprograma IMUL (REG0 * REG1 -> REG2, via soma repetida):
     * imul_microprogram:
     * ; Salva REG0 (multiplicando), REG1 (multiplicador) em REG0, REG1 da CPU
     * ; (A CPU já copiou os operandos da instrução IMUL para IMUL_OP1_TEMP_ADDR e IMUL_OP2_TEMP_ADDR)
     * move <IMUL_OP1_TEMP_ADDR> %REG0   ; REG0 = multiplicando
     * move <IMUL_OP2_TEMP_ADDR> %REG1   ; REG1 = multiplicador (contador de loops)
     * move imm 0 %REG2                 ; REG2 = resultado = 0
     * * jz %REG1 end_imul_micro          ; Se multiplicador (REG1) == 0, salta para o fim
     * * imul_loop:
     * add %REG0 %REG2                  ; REG2 = REG0 + REG2 (acumula multiplicando no resultado)
     * dec %REG1                        ; Decrementa o multiplicador (contador)
     * jnz imul_loop                    ; Se REG1 != 0, continua o loop
     *
     * end_imul_micro:
     * move %REG2 <IMUL_RESULT_ADDRESS> ; Salva o resultado em IMUL_RESULT_ADDRESS
     * halt                             ; Sinaliza o fim do microprograma. CPU restaurará contexto.
     *
     * NOTA: Os endereços de salto JZ e JNZ aqui são literais calculados baseados na posição
     * das instruções dentro do próprio microprograma. Em um assembler mais avançado,
     * você usaria labels dentro do microprograma e o assembler os resolveria.
     */
    private static int[] generateImulMicroprogram() {
        // Obter uma instância temporária da CPU para acessar os endereços reservados.
        // Isso é um hack; idealmente, esses endereços seriam passados de forma mais limpa
        // ou definidos como constantes em um local acessível.
        // CUIDADO: Este CPU temporário não tem a mesma memória que o CPU real.
        // Apenas para pegar os endereços fixos.
        CPU tempCpuForAddresses = new CPU(1024); // Tamanho não importa muito aqui, só para init.
        int imulOp1TempAddr = tempCpuForAddresses.IMUL_OP1_TEMP_ADDR;
        int imulOp2TempAddr = tempCpuForAddresses.IMUL_OP2_TEMP_ADDR;
        int imulResultAddr = tempCpuForAddresses.IMUL_RESULT_ADDRESS;
        int imulMicroprogramStart = tempCpuForAddresses.IMUL_MICROPROGRAM_START;


        // Array que representa o código de máquina do microprograma IMUL.
        // Os jumps precisam de offsets relativos ao imulMicroprogramStart
        // Ou, mais fácil, offsets absolutos se o microprograma for fixo.
        // VOU USAR OFFSETS ABSOLUTOS (IMUL_MICROPROGRAM_START + offset)
        // O Loader espera que JZ/JNZ usem o endereço final para o operando.

        // Estrutura do microprograma e seus endereços relativos ao IMUL_MICROPROGRAM_START:
        // 0: move <IMUL_OP1_TEMP_ADDR> %REG0
        // 1: move <IMUL_OP2_TEMP_ADDR> %REG1
        // 2: move imm 0 %REG2
        // 3: jz %REG1 (para a instrução 7: end_imul_micro)
        // 4: imul_loop: add %REG0 %REG2
        // 5: dec %REG1
        // 6: jnz imul_loop (para a instrução 4)
        // 7: end_imul_micro: move %REG2 <IMUL_RESULT_ADDRESS>
        // 8: halt

        // Cálculo dos endereços de salto para o microprograma:
        int imulLoopAddress = imulMicroprogramStart + 4; // Endereço absoluto do "imul_loop"
        int endImulMicroAddress = imulMicroprogramStart + 7; // Endereço absoluto do "end_imul_micro"

        return new int[]{
            (Opcodes.MOVE_MEM_REG << 24) | (0 << 22) | imulOp1TempAddr,   // 0: move imulOp1TempAddr %REG0
            (Opcodes.MOVE_MEM_REG << 24) | (1 << 22) | imulOp2TempAddr,   // 1: move imulOp2TempAddr %REG1
            (Opcodes.MOVE_IMM_REG << 24) | (2 << 22) | 0,                 // 2: move imm 0 %REG2 (resultado)

            (Opcodes.JZ << 24) | endImulMicroAddress,                    // 3: jz %REG1 (para end_imul_micro)
            
            // imul_loop (endereço: 4)
            (Opcodes.ADD_REG_REG << 24) | (0 << 22) | (2 << 20),           // 4: add %REG0 %REG2
            (Opcodes.INC_REG << 24) | (1 << 22),                        // 5: dec %REG1 (nossa ULA tem INC e DEC, não dec)
            // Cuidado: O INC_REG vai incrementar. Para decrementar, precisamos de DEC_REG.
            // Se não tiver DEC_REG, use SUB_IMM_REG (sub imm 1 %reg).
            // Supondo que a ULA.dec(int operand) pode ser chamada via ULA.performOperation(operand, 1, "DEC")
            // Ou o opcode INC_REG poderia ter sido usado como um DEC_REG se a ULA.inc() fosse a mesma para dec.
            // No meu ULA.java, eu já adicionei um `dec(int operand)`.
            // Preciso de um opcode DEC_REG:
            // return (Opcodes.DEC_REG << 24) | (1 << 22); // Assume DEC_REG 0x13
            // Vamos usar o ADD_IMM_REG onde IMM é -1 para simular DEC, se não tiver DEC_REG opcode.
            // Para simplicidade, vou usar a instrução INC_REG no microprograma com o ID do registrador 1.
            // Mas para DEC, é melhor usar SUB_IMM_REG. Se não tem no seu assembly, você tem que criar.
            // Se o seu assembly só tem INC, então você terá que usar:
            // move imm -1 %REG3 ; add %REG3 %REG1
            // Por simplicidade, vou usar SUB_IMM_REG, mas este opcode não está na lista original.
            // Precisa ser `sub imm 1 %REG1`. Isso teria que ser um opcode novo ou usar MOVE_IMM_REG + SUB_REG_REG.
            // Simplificando o microprograma:
            // move imm 1 %REG3 ; sub %REG3 %REG1 (para REG1 <- REG1 - 1)
            (Opcodes.MOVE_IMM_REG << 24) | (3 << 22) | 1,                  // 5: move imm 1 %REG3 (REG3=1)
            (Opcodes.SUB_REG_REG << 24) | (3 << 22) | (1 << 20),           // 6: sub %REG3 %REG1 (REG1 <- REG1 - REG3, ou seja, REG1--)
            (Opcodes.JNZ << 24) | imulLoopAddress,                        // 7: jnz imul_loop (para imul_loopAddress)

            // end_imul_micro (endereço: 8)
            (Opcodes.MOVE_REG_MEM << 24) | (2 << 22) | imulResultAddr,     // 8: move %REG2 imulResultAddr
            (Opcodes.HALT << 24)                                          // 9: halt (sinaliza para CPU restaurar)
        };
    }
}