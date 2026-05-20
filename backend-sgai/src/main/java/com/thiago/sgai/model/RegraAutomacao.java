package com.thiago.sgai.model;

import jakarta.persistence.*;
import java.time.LocalTime;

@Entity
@Table(name = "regras_automacao")
public class RegraAutomacao {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String salaId;
    private String nome; // Ex: "LATE NIGHT", "HOT DAY"

    // Pode ser: "HORARIO_PONTUAL" ou "CONDICAO_SENSOR"
    private String tipoRegra; 

    // Horários (Usados tanto para hora exata quanto para definir o intervalo)
    private LocalTime horaInicio;
    private LocalTime horaFim; // Null se for horário pontual

    // Condições do Sensor (Null se a regra for apenas de horário)
    private String sensorAlvo; // Ex: "temp", "presenca"
    private String operador; // Ex: ">", "<", "=="
    private Double valorGatilho; // Ex: 30.0

    // O conjunto de comandos em formato JSON que você pediu!
    // Ex: {"led": "OFF", "ar": "ON", "cortina": "FECHADA"}
    @Column(columnDefinition = "TEXT")
    private String comandosJson;

    private boolean ativa = true;

    // Construtor vazio obrigatório do JPA
    public RegraAutomacao() {}

    // Getters e Setters (pode gerar no VS Code ou colar estes)
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    
    public String getSalaId() { return salaId; }
    public void setSalaId(String salaId) { this.salaId = salaId; }

    public String getNome() { return nome; }
    public void setNome(String nome) { this.nome = nome; }

    public String getTipoRegra() { return tipoRegra; }
    public void setTipoRegra(String tipoRegra) { this.tipoRegra = tipoRegra; }

    public LocalTime getHoraInicio() { return horaInicio; }
    public void setHoraInicio(LocalTime horaInicio) { this.horaInicio = horaInicio; }

    public LocalTime getHoraFim() { return horaFim; }
    public void setHoraFim(LocalTime horaFim) { this.horaFim = horaFim; }

    public String getSensorAlvo() { return sensorAlvo; }
    public void setSensorAlvo(String sensorAlvo) { this.sensorAlvo = sensorAlvo; }

    public String getOperador() { return operador; }
    public void setOperador(String operador) { this.operador = operador; }

    public Double getValorGatilho() { return valorGatilho; }
    public void setValorGatilho(Double valorGatilho) { this.valorGatilho = valorGatilho; }

    public String getComandosJson() { return comandosJson; }
    public void setComandosJson(String comandosJson) { this.comandosJson = comandosJson; }

    public boolean isAtiva() { return ativa; }
    public void setAtiva(boolean ativa) { this.ativa = ativa; }
}