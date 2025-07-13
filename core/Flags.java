package core;

public class Flags {
    private boolean zero;     // Z flag: true if the result of the last operation was zero
    private boolean negative; // N flag: true if the result of the last operation was negative

    public Flags() {
        // Inicializa todas as flags como false (estado padrão)
        this.zero = false;
        this.negative = false;
    }

    /**
     * Atualiza as flags Z (Zero) e N (Negative) com base em um resultado de operação.
     *
     * @param result O resultado da última operação aritmética ou lógica.
     */
    public void updateFlags(int result) {
        this.zero = (result == 0);
        this.negative = (result < 0);
    }

    // --- Getters para as flags ---
    public boolean isZero() {
        return zero;
    }

    public boolean isNegative() {
        return negative;
    }

    // --- Setters (podem ser usados para resetar ou forçar um estado, se necessário) ---
    public void setZero(boolean zero) {
        this.zero = zero;
    }

    public void setNegative(boolean negative) {
        this.negative = negative;
    }

    @Override
    public String toString() {
        return "Flags [Z=" + zero + ", N=" + negative + "]";
    }
}