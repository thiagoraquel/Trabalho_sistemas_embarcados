package com.thiago.sgai.repository;

import com.thiago.sgai.model.HistoricoSensor;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.stereotype.Repository;
import org.springframework.data.jpa.repository.Query;
import jakarta.transaction.Transactional;
import java.time.LocalDateTime;

@Repository
public interface HistoricoSensorRepository extends JpaRepository<HistoricoSensor, Long> {
    @Modifying // Indica que a query vai alterar/deletar dados
    @Transactional // Obrigatorio para operações de escrita customizadas
    @Query("DELETE FROM HistoricoSensor h WHERE h.dataHora >= :inicio AND h.dataHora <= :fim")
    void deletarHistoricoNoIntervalo(LocalDateTime inicio, LocalDateTime fim);
}