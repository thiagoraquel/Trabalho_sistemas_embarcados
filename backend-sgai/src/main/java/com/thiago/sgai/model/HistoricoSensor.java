package com.thiago.sgai.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "historico_sensores")
public class HistoricoSensor {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String sala; // ex: "sala1"

    @Column(nullable = false)
    private String tipoSensor; // ex: "temp" ou "luz"

    @Column(nullable = false)
    private Double valor; // ex: 29.30 ou 1001.0

    @Column(nullable = false)
    private LocalDateTime dataHora;

    // Construtor padrão obrigatório pelo JPA
    public HistoricoSensor() {}

    // Construtor facilitador para usarmos no Service
    public HistoricoSensor(String sala, String tipoSensor, Double valor) {
        this.sala = sala;
        this.tipoSensor = tipoSensor;
        this.valor = valor;
        this.dataHora = LocalDateTime.now();
    }

    // Getters e Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getSala() { return sala; }
    public void setSala(String sala) { this.sala = sala; }
    public String getTipoSensor() { return tipoSensor; }
    public void setTipoSensor(String tipoSensor) { this.tipoSensor = tipoSensor; }
    public Double getValor() { return valor; }
    public void setValor(Double valor) { this.valor = valor; }
    public LocalDateTime getDataHora() { return dataHora; }
    public void setDataHora(LocalDateTime dataHora) { this.dataHora = dataHora; }
}