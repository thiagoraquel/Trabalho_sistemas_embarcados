package com.thiago.sgai.controller;

import com.thiago.sgai.model.EstadoDispositivo;
import com.thiago.sgai.model.HistoricoSensor;
import com.thiago.sgai.repository.EstadoDispositivoRepository;
import com.thiago.sgai.repository.HistoricoSensorRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import com.thiago.sgai.service.MotorRegrasService;
import com.thiago.sgai.repository.RegraAutomacaoRepository;

import java.util.List;

@RestController
@RequestMapping("/api/salas")
@CrossOrigin(origins = "http://localhost:3000") // <-- ESSA É A LINHA MÁGICA!
public class SalaController {

    private final EstadoDispositivoRepository estadoRepository;
    private final HistoricoSensorRepository historicoRepository;
    private final MotorRegrasService motorRegrasService;
    private final RegraAutomacaoRepository regraRepository;

    // Injeção automática dos repositórios
    public SalaController(EstadoDispositivoRepository estadoRepository, 
                          HistoricoSensorRepository historicoRepository,
                          MotorRegrasService motorRegrasService,
                          RegraAutomacaoRepository regraRepository  ) {
        this.estadoRepository = estadoRepository;
        this.historicoRepository = historicoRepository;
        this.motorRegrasService = motorRegrasService;
        this. regraRepository = regraRepository;
    }

    /**
     * ROTA 1: Retorna o estado atual de TODOS os dispositivos de uma sala específica.
     * Exemplo de uso: GET http://localhost:8080/api/salas/sala1/status
     */
    @GetMapping("/{salaId}/status")
    public ResponseEntity<List<EstadoDispositivo>> getStatusAtual(@PathVariable String salaId) {
        // Como o método findBySala não existe por padrão, vamos buscar tudo e filtrar por simplicidade,
        // ou você verá abaixo como adicionar essa busca limpa no repositório.
        List<EstadoDispositivo> estados = estadoRepository.findAll().stream()
                .filter(e -> e.getSala().equalsIgnoreCase(salaId))
                .toList();
                
        return ResponseEntity.ok(estados);
    }

    /**
     * ROTA 2: Retorna todo o histórico de sensores (temp e luz) para alimentar os gráficos.
     * Exemplo de uso: GET http://localhost:8080/api/salas/sala1/historico
     */
    @GetMapping("/{salaId}/historico")
    public ResponseEntity<List<HistoricoSensor>> getHistoricoSensores(@PathVariable String salaId) {
        List<HistoricoSensor> historico = historicoRepository.findAll().stream()
                .filter(h -> h.getSala().equalsIgnoreCase(salaId))
                .toList();
                
        return ResponseEntity.ok(historico);
    }

    /**
     * ROTA 3: Envia um comando manual do Dashboard diretamente para a placa ESP32.
     * Exemplo de uso: POST http://localhost:8080/api/salas/sala1/comando
     * Corpo da requisição (Texto puro): LIGAR_LUZ ou DESLIGAR_AR, etc.
     */
    @PostMapping("/{salaId}/comando")
    public ResponseEntity<String> enviarComandoManual(@PathVariable String salaId, @RequestBody String comando) {
        try {
            // Usamos o método enviarComando que já criamos dentro do MotorRegrasService!
            // Para podermos chamá-lo aqui, mude o modificador dele no MotorRegrasService de 'private' para 'public'.
            motorRegrasService.enviarComando(salaId, comando.trim());
            return ResponseEntity.ok("Comando '" + comando + "' enviado com sucesso para a " + salaId);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body("Erro ao enviar comando: " + e.getMessage());
        }
    }

    /**
     * ROTA 4: Busca todas as regras automáticas de uma sala
     * GET /api/salas/{salaId}/regras
     */
    @GetMapping("/{salaId}/regras")
    public ResponseEntity<List<com.thiago.sgai.model.RegraAutomacao>> getRegras(@PathVariable String salaId) {
        return ResponseEntity.ok(regraRepository.findBySalaIdAndAtivaTrue(salaId));
    }

    /**
     * ROTA 5: Cria uma nova regra customizada vinda do Dashboard
     * POST /api/salas/{salaId}/regras
     */
    @PostMapping("/{salaId}/regras")
    public ResponseEntity<com.thiago.sgai.model.RegraAutomacao> criarRegra(@PathVariable String salaId, 
                                                                           @RequestBody com.thiago.sgai.model.RegraAutomacao novaRegra) {
        novaRegra.setSalaId(salaId); // Garante que a regra pertence a esta sala
        com.thiago.sgai.model.RegraAutomacao regraSalva = regraRepository.save(novaRegra);
        return ResponseEntity.ok(regraSalva);
    }

    /**
     * ROTA 6: Busca a última leitura dos sensores (Temperatura, Luz, etc)
     * GET /api/salas/{salaId}/telemetria
     */
    @GetMapping("/{salaId}/telemetria")
    public ResponseEntity<com.thiago.sgai.model.HistoricoSensor> getUltimaTelemetria(@PathVariable String salaId) {
        // Atualizamos o nome do método aqui também:
        return historicoRepository.findTopBySalaOrderByDataHoraDesc(salaId)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * ROTA 7: Remove uma regra de automação pelo ID
     * DELETE /api/salas/{salaId}/regras/{id}
     */
    @DeleteMapping("/{salaId}/regras/{id}")
    public ResponseEntity<Void> deletarRegra(@PathVariable String salaId, @PathVariable Long id) {
        try {
            if (regraRepository.existsById(id)) {
                regraRepository.deleteById(id);
                System.out.println("🗑️ [SALA CONTROLLER] Regra " + id + " removida do banco.");
                return ResponseEntity.noContent().build(); // Retorna Status 204 (Sucesso, sem conteúdo)
            }
            return ResponseEntity.notFound().build();
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }
}