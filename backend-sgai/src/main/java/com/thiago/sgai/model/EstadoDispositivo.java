package com.thiago.sgai.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "estado_dispositivos", 
       uniqueConstraints = {@UniqueConstraint(columnNames = {"sala", "dispositivo"})})
public class EstadoDispositivo {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String sala; // ex: "sala1"

    @Column(nullable = false)
    private String dispositivo; // ex: "led", "ar", "cortina", "presenca"

    @Column(nullable = false)
    private String estado; // ex: "ON", "OFF", "90" (ângulo do servo), "1" (movimento)

    @Column(nullable = false)
    private LocalDateTime ultimaAtualizacao;

    public EstadoDispositivo() {}

    public EstadoDispositivo(String sala, String dispositivo, String estado) {
        this.sala = sala;
        this.dispositivo = dispositivo;
        this.estado = estado;
        this.ultimaAtualizacao = LocalDateTime.now();
    }

    // Getters e Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getSala() { return sala; }
    public void setSala(String sala) { this.sala = sala; }
    public String getDispositivo() { return dispositivo; }
    public void setDispositivo(String dispositivo) { this.dispositivo = dispositivo; }
    public String getEstado() { return estado; }
    public void setEstado(String estado) { this.estado = estado; this.ultimaAtualizacao = LocalDateTime.now(); }
    public LocalDateTime getUltimaAtualizacao() { return ultimaAtualizacao; }
    public void setUltimaAtualizacao(LocalDateTime ultimaAtualizacao) { this.ultimaAtualizacao = ultimaAtualizacao; }
}