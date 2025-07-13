package core;

public class Memory {
    private int[] data;
    private final int MAX_SIZE; // Tamanho total da memória em "palavras" (inteiros)

    // Endereços de memória reservados e pontos de interesse
    private int imulMicroprogramStartAddress; // Início do microprograma IMUL
    private int imulRegistersSaveAreaStart;   // Início da área para salvar registradores
    private int imulResultAddress;            // Endereço onde o resultado do IMUL será salvo
    private int variablesStartAddress;        // Início da área de variáveis
    private int stackBottomAddress;           // Endereço inicial (base) da pilha (StkBOT)
                                              // A pilha cresce "para baixo" a partir daqui.

    // Construtor: define o tamanho da memória e os endereços de áreas reservadas
    public Memory(int maxSize) {
        if (maxSize <= 0) {
            throw new IllegalArgumentException("Memory size must be positive.");
        }
        this.MAX_SIZE = maxSize;
        this.data = new int[MAX_SIZE];
        
        // Inicializa a memória com zeros
        for (int i = 0; i < MAX_SIZE; i++) {
            this.data[i] = 0;
        }

        // --- Definição dos endereços de áreas reservadas ---
        // Estes valores são arbitrários para começar. Você precisará ajustá-los
        // com base no tamanho do seu microprograma IMUL, quantos registradores salvar, etc.
        // É crucial que estas áreas não se sobreponham.

        // Exemplo de alocação (ajuste conforme necessário):
        // Assumindo que a memória começa do endereço 0
        int currentAddress = 0;

        // 1. Área para o microprograma IMUL
        // Ex: 20 posições para o microprograma
        this.imulMicroprogramStartAddress = currentAddress;
        currentAddress += 20; // +20 para o microprograma IMUL

        // 2. Área para salvar registradores (PC, REG0-3, FLAGS - 5 registradores + flags = 6 palavras)
        // Você pode alocar mais se precisar de espaço para flags separadas ou outros.
        this.imulRegistersSaveAreaStart = currentAddress;
        currentAddress += 6; // +6 para salvar registradores e flags (ex: 5 regs + 1 para flags compactadas ou separadas)

        // 3. Endereço para o resultado do IMUL
        this.imulResultAddress = currentAddress;
        currentAddress += 1; // +1 para o resultado da multiplicação

        // 4. Início da área de variáveis
        // Variáveis começam após todas as áreas reservadas do sistema.
        this.variablesStartAddress = currentAddress;
        // Para este exemplo, não vamos predefinir um tamanho fixo para variáveis aqui.
        // O Loader/CPU precisará gerenciar a alocação de variáveis a partir daqui.

        // 5. Início da pilha (cresce para baixo a partir daqui)
        // A pilha é alocada a partir da primeira posição livre após as variáveis.
        // Para simplificar no construtor da memória, vamos definir o "fundo" da pilha
        // como o final da memória disponível para o usuário, mas antes disso, precisamos
        // considerar o espaço para variáveis.
        // POR ENQUANTO, VOU COLOCAR O STACK_BOTTOM COMO O ENDEREÇO MÁXIMO DA MEMÓRIA - 1.
        // Isso precisará ser ajustado APÓS o carregamento de variáveis pelo Loader.
        // O `currentAddress` neste ponto é o primeiro endereço livre APÓS as variáveis do sistema.
        // A pilha deve ser alocada "logo abaixo das variáveis", ou seja, começando do final
        // da memória e vindo para trás.
        // Então, StackBottom será o MAX_SIZE - 1, e StackTop será inicializado com StackBottom.
        this.stackBottomAddress = MAX_SIZE - 1; // A pilha vai do MAX_SIZE-1 para baixo.
                                                 // Se MAX_SIZE = 1000, StackBottom = 999.
                                                 // O primeiro push iria para 998.

        // Validação inicial para garantir que as áreas reservadas não excedam o MAX_SIZE
        if (stackBottomAddress < variablesStartAddress) {
            System.err.println("Warning: Stack area overlaps with reserved system/variable areas!");
            System.err.println("  IMUL Microprogram Start: " + imulMicroprogramStartAddress);
            System.err.println("  IMUL Regs Save Start: " + imulRegistersSaveAreaStart);
            System.err.println("  IMUL Result Address: " + imulResultAddress);
            System.err.println("  Variables Start Address: " + variablesStartAddress);
            System.err.println("  Calculated Stack Bottom Address: " + stackBottomAddress);
            throw new IllegalArgumentException("Memory allocation overlap detected during initialization.");
        }
    }

    /**
     * Lê um valor da memória em um dado endereço.
     *
     * @param address O endereço de memória a ser lido.
     * @return O valor armazenado no endereço.
     * @throws IndexOutOfBoundsException Se o endereço estiver fora dos limites da memória.
     */
    public int read(int address) {
        if (address < 0 || address >= MAX_SIZE) {
            throw new IndexOutOfBoundsException("Memory read error: Address " + address + " is out of bounds [0, " + (MAX_SIZE - 1) + "]");
        }
        return data[address];
    }

    /**
     * Escreve um valor na memória em um dado endereço.
     *
     * @param address O endereço de memória onde o valor será escrito.
     * @param value   O valor a ser escrito.
     * @throws IndexOutOfBoundsException Se o endereço estiver fora dos limites da memória.
     */
    public void write(int address, int value) {
        if (address < 0 || address >= MAX_SIZE) {
            throw new IndexOutOfBoundsException("Memory write error: Address " + address + " is out of bounds [0, " + (MAX_SIZE - 1) + "]");
        }
        data[address] = value;
    }

    // --- Getters para os endereços das áreas reservadas ---
    public int getImulMicroprogramStartAddress() {
        return imulMicroprogramStartAddress;
    }

    public int getImulRegistersSaveAreaStart() {
        return imulRegistersSaveAreaStart;
    }

    public int getImulResultAddress() {
        return imulResultAddress;
    }

    public int getVariablesStartAddress() {
        return variablesStartAddress;
    }

    public int getStackBottomAddress() {
        return stackBottomAddress;
    }

    public int getMaxSize() {
        return MAX_SIZE;
    }

    // Método para carregar um programa/dados na memória
    public void load(int startAddress, int[] programData) {
        if (startAddress < 0 || startAddress + programData.length > MAX_SIZE) {
            throw new IllegalArgumentException("Program data does not fit in memory at address " + startAddress);
        }
        System.arraycopy(programData, 0, data, startAddress, programData.length);
    }
    
    // Método para depuração
    public void dumpMemory(int start, int end) {
        System.out.println("--- Memory Dump from " + start + " to " + end + " ---");
        for (int i = start; i <= end; i++) {
            if (i >= 0 && i < MAX_SIZE) {
                System.out.printf("MEM[%04d]: %d%n", i, data[i]);
            } else {
                System.out.println("MEM[----]: Out of bounds");
            }
        }
        System.out.println("---------------------------");
    }
}