package core;

public class Stack {
    private Memory memory;
    private Register stackTop;
    private Register stackBottom;

    public Stack(Memory memory, Register stackTop, Register stackBottom) {
        this.memory = memory;
        this.stackTop = stackTop;
        this.stackBottom = stackBottom;
    }

    public void push(int value) {
        int addr = stackTop.get();
        memory.set(addr, value);
        stackTop.set(addr - 1); // decresce na memória
    }

    public int pop() {
        if (isEmpty()) {
            throw new IllegalStateException("Stack underflow: pilha vazia.");
        }
        stackTop.set(stackTop.get() + 1);
        return memory.get(stackTop.get());
    }

    public boolean isEmpty() {
        return stackTop.get() == stackBottom.get();
    }

    public void reset() {
        stackTop.set(stackBottom.get());
    }
} 
