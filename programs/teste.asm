# Programa de teste com multiplicação (imul), somas e desvios

# Inicializa valores
move imm 4 %reg0        # multiplicando = 4
move imm 3 %reg1        # multiplicador = 3

# Copia para área temporária do IMUL
move %reg0 110          # copia reg0 para IMUL_OP1_TEMP_ADDR
move %reg1 111          # copia reg1 para IMUL_OP2_TEMP_ADDR

# Chama o microprograma imul
call 100                # endereço do microprograma IMUL

# Após retorno, pega o resultado da multiplicação
move 120 %reg2          # move resultado (MEM[120]) para REG2

# Soma +1 ao resultado (REG2++)
inc %reg2

# Salva resultado final em memória
move %reg2 300

# Loop: decrementa REG2 até zero
label_loop:
dec %reg2
jnz label_loop

halt
