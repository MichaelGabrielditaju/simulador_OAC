package core;

// Importa a classe Flags para que a ULA possa interagir com ela

public class ULA {
    private Flags flags; // A ULA precisa de uma referência às Flags para atualizá-las

    // Construtor: a ULA precisa receber uma instância de Flags
    public ULA(Flags flags) {
        this.flags = flags;
    }

    /**
     * Realiza uma operação de adição.
     *
     * @param operand1 O primeiro operando.
     * @param operand2 O segundo operando.
     * @return O resultado da adição.
     */
    public int add(int operand1, int operand2) {
        int result = operand1 + operand2;
        flags.updateFlags(result); // Atualiza as flags com base no resultado
        return result;
    }

    /**
     * Realiza uma operação de subtração.
     *
     * @param operand1 O primeiro operando (minuendo).
     * @param operand2 O segundo operando (subtraendo).
     * @return O resultado da subtração.
     */
    public int sub(int operand1, int operand2) {
        int result = operand1 - operand2;
        flags.updateFlags(result); // Atualiza as flags com base no resultado
        return result;
    }

    /**
     * Realiza uma operação de incremento.
     *
     * @param operand O valor a ser incrementado.
     * @return O valor incrementado.
     */
    public int inc(int operand) {
        int result = operand + 1; // Ou você pode usar add(operand, 1)
        flags.updateFlags(result); // Atualiza as flags com base no resultado
        return result;
    }

    /**
     * Realiza uma operação de decremento.
     * Necessário para a manipulação da pilha (StackTop e StackBottom).
     *
     * @param operand O valor a ser decrementado.
     * @return O valor decrementado.
     */
    public int dec(int operand) {
        int result = operand - 1; // Ou você pode usar sub(operand, 1)
        flags.updateFlags(result); // Atualiza as flags com base no resultado
        return result;
    }

    /**
     * Método genérico para realizar operações aritméticas com base em uma string de operação.
     * Este método é útil para a classe Stack, como discutimos anteriormente.
     * Você pode expandir este método para incluir mais operações se precisar.
     *
     * @param operand1 O primeiro operando.
     * @param operand2 O segundo operando (para operações binárias) ou um valor para unárias.
     * @param operation A string que representa a operação (ex: "ADD", "SUB", "INC", "DEC").
     * @return O resultado da operação.
     * @throws IllegalArgumentException Se a operação não for reconhecida.
     */
    public int performOperation(int operand1, int operand2, String operation) {
        int result;
        switch (operation.toUpperCase()) {
            case "ADD":
                result = add(operand1, operand2);
                break;
            case "SUB":
                result = sub(operand1, operand2);
                break;
            case "INC": // Para operações unárias, o operand2 pode ser ignorado ou usado como 1
                result = inc(operand1);
                break;
            case "DEC": // Para operações unárias, o operand2 pode ser ignorado ou usado como 1
                result = dec(operand1);
                break;
            // TODO: Adicionar outras operações lógicas se houver (AND, OR, NOT, XOR, etc.)
            default:
                throw new IllegalArgumentException("ULA: Operação desconhecida: " + operation);
        }
        // As flags já são atualizadas dentro dos métodos específicos (add, sub, inc, dec)
        return result;
    }

    /**
     * Método para comparar dois valores e atualizar as flags Z e N.
     * Útil para instruções de comparação como JE, JG, JL.
     *
     * @param value1 O primeiro valor a ser comparado.
     * @param value2 O segundo valor a ser comparado.
     */
    public void compare(int value1, int value2) {
        int result = value1 - value2; // A comparação é feita implicitamente por uma subtração
        flags.updateFlags(result); // Atualiza as flags com base no resultado da "subtração"
    }
}