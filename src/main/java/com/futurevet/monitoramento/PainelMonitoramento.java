package com.futurevet.monitoramento;

/**
 * Indicadores exibidos na tela de monitoramento da clínica.
 *
 * @param memoriaUsadaMb   memória da JVM em uso (MB)
 * @param memoriaMaximaMb  memória máxima disponível para a JVM (MB)
 * @param cpuProcessoPct   uso de CPU pela aplicação (%)
 * @param cpuSistemaPct    uso de CPU da máquina (%)
 * @param minutosAtivo     tempo desde a inicialização (minutos)
 * @param requisicoesHttp  requisições HTTP atendidas desde a inicialização
 */
public record PainelMonitoramento(double memoriaUsadaMb, double memoriaMaximaMb, double cpuProcessoPct,
        double cpuSistemaPct, double minutosAtivo, long requisicoesHttp, String statusAplicacao,
        String statusBanco, String statusIa, boolean rabbitAtivo, String statusRabbit,
        double notificacoesRabbit, double notificacoesLocais) {

    /** Percentual da memória máxima em uso, limitado a 0–100 para a barra de progresso. */
    public long memoriaPct() {
        if (memoriaMaximaMb <= 0)
            return 0;
        return Math.round(Math.min(100, memoriaUsadaMb * 100 / memoriaMaximaMb));
    }
}
