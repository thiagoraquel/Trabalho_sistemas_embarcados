package com.thiago.sgai.service;

import com.thiago.sgai.model.RegraAutomacao;
import com.thiago.sgai.repository.RegraAutomacaoRepository;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalTime;
import java.time.ZonedDateTime;
import java.time.ZoneId;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
public class MotorRegrasService {

    private final RegraAutomacaoRepository regraRepository;
    private final MqttClient mqttClient;
    
    // Guarda os IDs das regras disparadas para garantir o Edge-Trigger (Disparo Único)
    private final Set<Long> regrasDisparadasFiltradas = new HashSet<>();

    public MotorRegrasService(RegraAutomacaoRepository regraRepository, MqttClient mqttClient) {
        this.regraRepository = regraRepository;
        this.mqttClient = mqttClient;
    }

    /**
     * CENÁRIO A: Avaliação baseada em Sensores Dinâmicos (Chamado pelo MQTT Listener)
     */
    public void avaliarRegra(String salaId, String tipoSensor, Double valorAtual) {
        List<RegraAutomacao> regras = regraRepository.findBySalaIdAndAtivaTrue(salaId);
        LocalTime agora = ZonedDateTime.now(ZoneId.of("America/Fortaleza")).toLocalTime();

        for (RegraAutomacao regra : regras) {
            if (!"CONDICAO_SENSOR".equals(regra.getTipoRegra())) continue;
            if (!regra.getSensorAlvo().equalsIgnoreCase(tipoSensor)) continue;

            // Valida se a leitura está dentro da janela de horário permitida
            boolean dentroDoHorario = true;
            if (regra.getHoraInicio() != null && regra.getHoraFim() != null) {
                dentroDoHorario = !agora.isBefore(regra.getHoraInicio()) && !agora.isAfter(regra.getHoraFim());
            }

            if (dentroDoHorario) {
                boolean condicaoAtingida = avaliarCondicaoMatematica(valorAtual, regra.getOperador(), regra.getValorGatilho());

                if (condicaoAtingida) {
                    // DISPARO ÚNICO: Só envia se não tiver sido disparada antes neste ciclo
                    if (!regrasDisparadasFiltradas.contains(regra.getId())) {
                        enviarConjuntoComandosJSON(regra.getSalaId(), regra.getComandosJson());
                        regrasDisparadasFiltradas.add(regra.getId());
                        System.out.println("⚡ [MOTOR] Regra de Sensor '" + regra.getNome() + "' executada com sucesso!");
                    }
                } else {
                    // Quando a leitura sai da zona de perigo, o gatilho é destravado para o próximo evento
                    regrasDisparadasFiltradas.remove(regra.getId());
                }
            } else {
                regrasDisparadasFiltradas.remove(regra.getId());
            }
        }
    }

    /**
     * CENÁRIO B: Avaliação baseada em Horários Pontuais Fixos (Roda a cada 30 segundos)
     */
    @Scheduled(fixedRate = 30000)
    public void verificarRegrasPorHorario() {
        ZonedDateTime agoraLocal = ZonedDateTime.now(ZoneId.of("America/Fortaleza"));
        LocalTime agora = agoraLocal.toLocalTime();
        
        List<RegraAutomacao> regras = regraRepository.findAll();

        for (RegraAutomacao regra : regras) {
            if (!regra.isAtiva() || !"HORARIO_PONTUAL".equals(regra.getTipoRegra())) continue;

            LocalTime horaAlvo = regra.getHoraInicio();
            
            // Bate o minuto exato do fuso do seu computador com o banco
            if (agora.getHour() == horaAlvo.getHour() && agora.getMinute() == horaAlvo.getMinute()) {
                if (!regrasDisparadasFiltradas.contains(regra.getId())) {
                    enviarConjuntoComandosJSON(regra.getSalaId(), regra.getComandosJson());
                    regrasDisparadasFiltradas.add(regra.getId());
                    System.out.println("⏰ [MOTOR] Regra Horária '" + regra.getNome() + "' disparada no minuto programado.");
                }
            } else {
                // Fora daquele minuto específico, limpa a flag para o dia seguinte
                regrasDisparadasFiltradas.remove(regra.getId());
            }
        }
    }

    private boolean avaliarCondicaoMatematica(Double valorAtual, String operador, Double valorGatilho) {
        if (operador == null) return false;
        return switch (operador) {
            case ">" -> valorAtual > valorGatilho;
            case "<" -> valorAtual < valorGatilho;
            case "==" -> valorAtual.equals(valorGatilho);
            default -> false;
        };
    }

    private void enviarConjuntoComandosJSON(String salaId, String comandosJson) {
        try {
            // Envia o payload JSON contendo o grupo de ordens no tópico mestre de comandos
            String topicoComando = "sgai/" + salaId + "/comandos";
            MqttMessage message = new MqttMessage(comandosJson.getBytes());
            message.setQos(1); 
            
            mqttClient.publish(topicoComando, message);
            System.out.println("📤 [MOTOR DE REGRAS] Carga útil JSON despachada via MQTT: " + comandosJson);
        } catch (Exception e) {
            System.err.println("❌ Falha ao publicar conjunto JSON no broker: " + e.getMessage());
        }
    }

    /**
     * ROTA DE ACESSO MESTRE: Envia um comando manual simples (Texto Puro) vindo do Dashboard.
     * Mantido público para o SalaController poder repassar os cliques dos botões.
     */
    public void enviarComando(String salaId, String comando) throws Exception {
        String topicoComando = "sgai/" + salaId + "/comandos";
        
        MqttMessage message = new MqttMessage(comando.getBytes());
        message.setQos(1); // Garante que a ordem chegue com segurança
        
        mqttClient.publish(topicoComando, message);
        System.out.println("🤖 [CLIQUE MANUAL] Comando mestre enviado para " + topicoComando + ": " + comando);
    }
}