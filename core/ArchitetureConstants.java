package core;

public class ArchitectureConstants {

    // Endereços reservados para a arquitetura e microprograma
    public static final int IMUL_MICROPROGRAM_START = 100;     // Início do código do microprograma imul
    public static final int IMUL_OP1_TEMP_ADDR = 110;          // Endereço temporário do operando 1 (multiplicando)
    public static final int IMUL_OP2_TEMP_ADDR = 111;          // Endereço temporário do operando 2 (multiplicador)
    public static final int IMUL_RESULT_ADDRESS = 120;         // Onde o resultado da imul será armazenado
    public static final int SAVE_REGISTERS_START = 130;        // Onde o microprograma pode salvar REG0..REG3 temporariamente
    public static final int VARIABLE_START = 150;              // Onde o programa principal pode alocar variáveis
    public static final int STACK_BOTTOM = 200;                // Endereço onde a pilha começa (crescendo para baixo)

    // StackTop será inicializado com STACK_BOTTOM
    // A pilha é manipulada com push/pop em ordem decrescente
}
