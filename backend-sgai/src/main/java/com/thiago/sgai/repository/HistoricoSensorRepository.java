package com.thiago.sgai.repository;

import com.thiago.sgai.model.HistoricoSensor;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface HistoricoSensorRepository extends JpaRepository<HistoricoSensor, Long> {
}