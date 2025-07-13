package core;

public class Register {
    private String name;
    private int value;

    public Register(String name) {
        this.name = name;
        this.value = 0;
    }

    public String getName() {
        return name;
    }

    public int get() {
        return value;
    }

    public void set(int value) {
        this.value = value;
    }

    public void inc() {
        this.value++;
    }

    public void dec() {
        this.value--;
    }

    @Override
    public String toString() {
        return name + ": " + value;
    }
} 