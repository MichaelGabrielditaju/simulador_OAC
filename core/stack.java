package core;

public class Stack {
    private Memory memory;
    private Register stackTop;
    private Register stackBottom;
    private ULA ula; // Adiciona referência à ULA

    // Construtor atualizado para receber a ULA
    public Stack(Memory memory, Register stackTop, Register stackBottom, ULA ula) {
        this.memory = memory;
        this.stackTop = stackTop;
        this.stackBottom = stackBottom;
        this.ula = ula; // Armazena a instância da ULA
    }

    public void push(int value) {
        // "StackTop é "avançado" (mas, na memória, é recuado) uma posição."
        // Isso significa que primeiro decrementamos StackTop, e então escrevemos.
        // StackTop apontará para o último elemento inserido.

        // Simula a operação stackTop.set(stackTop.get() - 1) via ULA
        // Para isso, a ULA precisaria de um método para fazer decremento/incremento de registradores
        // Ou você pode passar o valor do registrador e a ULA retornar o novo valor.
        // Vou assumir que a ULA terá um método para decremento de um valor.

        int currentTopValue = stackTop.get();
        int newTopValue = ula.performOperation(currentTopValue, 1, "SUB"); // ULA.SUB representa subtração
        stackTop.set(newTopValue); // Atualiza o registrador StackTop

        // "o endereço de retorno deve ser inserido na posição indicada por StackTop."
        memory.write(stackTop.get(), value); // Escreve o valor na nova posição do topo
    }

    public int pop() {
        if (isEmpty()) {
            throw new IllegalStateException("Stack underflow: pilha vazia.");
        }
        // "o conteúdo da posição apontada por ele deve ser enviado para PC."
        int poppedValue = memory.read(stackTop.get());

        // "o registrador SackTop é "recuado" (mas, na memória, é avançado) uma posição"
        // Simula a operação stackTop.set(stackTop.get() + 1) via ULA
        int currentTopValue = stackTop.get();
        int newTopValue = ula.performOperation(currentTopValue, 1, "ADD"); // ULA.ADD representa adição
        stackTop.set(newTopValue); // Atualiza o registrador StackTop
        
        return poppedValue;
    }

    public boolean isEmpty() {
        // Pilha vazia quando StackTop e StackBottom apontam para o mesmo endereço
        return stackTop.get() == stackBottom.get();
    }

    public void reset() {
        stackTop.set(stackBottom.get());
    }
}