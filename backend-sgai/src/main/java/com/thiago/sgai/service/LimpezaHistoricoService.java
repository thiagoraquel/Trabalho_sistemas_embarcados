package com.thiago.sgai.service;

import com.thiago.sgai.repository.HistoricoSensorRepository;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

@Service
public class LimpezaHistoricoService {

    private final HistoricoSensorRepository historicoRepository;

    public LimpezaHistoricoService(HistoricoSensorRepository historicoRepository) {
        this.historicoRepository = historicoRepository;
    }

    /**
     * Método central que executa a limpeza do exato dia anterior
     */
    public void executarLimpezaDiaAnterior() {
        // Pega a data de ontem com base no dia atual do servidor
        LocalDate ontem = LocalDate.now().minusDays(1);
        
        // Define o intervalo exato: de 00:00:00 até 23:59:59.999999 de ontem
        LocalDateTime inicioOntem = ontem.atStartOfDay();
        LocalDateTime fimOntem = ontem.atTime(LocalTime.MAX);

        System.out.println("🧹 [FAXINA DO BANCO] Iniciando limpeza automatizada...");
        System.out.println("🧹 [FAXINA DO BANCO] Apagando dados entre: " + inicioOntem + " e " + fimOntem);

        try {
            historicoRepository.deletarHistoricoNoIntervalo(inicioOntem, fimOntem);
            System.out.println("✅ [FAXINA DO BANCO] Histórico de ontem limpo com sucesso!");
        } catch (Exception e) {
            System.err.println("❌ [FAXINA DO BANCO] Erro ao limpar histórico: " + e.getMessage());
        }
    }

    /**
     * GATILHO 1: Executa automaticamente todo dia exatamente às 01:00 da madrugada
     * Formato CRON: (segundos minutos horas dia_do_mês mês dia_da_semana)
     */
    @Scheduled(cron = "0 0 1 * * *")
    public void agendamentoMadrugada() {
        System.out.println("⏰ [AGENDAMENTO] Batendo 1h da manhã. Hora da faxina!");
        executarLimpezaDiaAnterior();
    }

    /**
     * GATILHO 2: Executa assim que o sistema terminar de ligar/inicializar completamente
     */
    @EventListener(ApplicationReadyEvent.class)
    public void limparAoIniciar() {
        System.out.println("🚀 [INICIALIZAÇÃO] Sistema SGAI ativo. Rodando faxina preventiva de segurança.");
        executarLimpezaDiaAnterior();
    }
}