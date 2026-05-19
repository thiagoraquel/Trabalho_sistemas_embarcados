package com.thiago.sgai.repository;

import com.thiago.sgai.model.EstadoDispositivo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface EstadoDispositivoRepository extends JpaRepository<EstadoDispositivo, Long> {
    // Busca um dispositivo específico de uma sala específica para podermos atualizar seu estado
    Optional<EstadoDispositivo> findBySalaAndDispositivo(String sala, String dispositivo);

    // Adicione esta linha: O Spring Boot vai criar o "SELECT * FROM estado_dispositivos WHERE sala = ?" sozinho!
    List<EstadoDispositivo> findBySala(String sala);
}