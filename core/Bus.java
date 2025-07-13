package core;

// A classe Bus pode ser mais uma abstração de transferência do que um componente com estado
// Ela pode ser usada pela CPU para "modelar" o fluxo de dados entre os componentes.
public class Bus {

    // Nomes simbólicos para os barramentos, se quisermos referenciá-los.
    public static final String INTTBUS1 = "Inttbus1";
    public static final String INTTBUS2 = "Inttbus2";
    public static final String EXTBUS = "Extbus";

    // Métodos para simular a "transferência" de dados através dos barramentos.
    // Na prática, estes métodos não fazem muito além de retornar o valor,
    // pois a lógica de "quem lê de quem e quem escreve em quem"
    // estará no controle da CPU.

    /**
     * Simula a leitura de um valor para o Inttbus1.
     * Geralmente, um registrador ou memória envia seu valor para este barramento.
     * @param value O valor a ser "colocado" no barramento.
     * @return O valor transferido.
     */
    public int transferToInttbus1(int value) {
        // System.out.println("Transferindo " + value + " para Inttbus1");
        return value;
    }

    /**
     * Simula a leitura de um valor para o Inttbus2.
     * Geralmente, um registrador ou memória envia seu valor para este barramento.
     * @param value O valor a ser "colocado" no barramento.
     * @return O valor transferido.
     */
    public int transferToInttbus2(int value) {
        // System.out.println("Transferindo " + value + " para Inttbus2");
        return value;
    }

    /**
     * Simula a leitura de um valor para o Extbus.
     * Geralmente, um registrador ou ULA envia seu valor para este barramento,
     * que por sua vez pode ir para a memória.
     * @param value O valor a ser "colocado" no barramento.
     * @return O valor transferido.
     */
    public int transferToExtbus(int value) {
        // System.out.println("Transferindo " + value + " para Extbus");
        return value;
    }

    // Métodos que a CPU pode usar para "mover" dados
    // Estes métodos não são estáticos para permitir que uma instância de Bus
    // possa ser passada para a CPU, caso queira simular múltiplos barramentos
    // ou uma hierarquia de barramentos no futuro.
    // Por enquanto, eles apenas retornam o valor, delegando a ação real à CPU.

    /**
     * Simula o movimento de dados de uma origem para um destino.
     * Esta é uma representação simplificada. A CPU será responsável por
     * chamar os métodos get/set dos componentes reais (registradores, memória).
     * @param sourceValue O valor lido da origem.
     * @return O valor transferido.
     */
    public int moveData(int sourceValue) {
        return sourceValue;
    }
}