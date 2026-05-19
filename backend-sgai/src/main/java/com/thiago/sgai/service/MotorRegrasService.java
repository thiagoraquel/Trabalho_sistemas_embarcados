package com.thiago.sgai.service;

import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.springframework.stereotype.Service;

@Service
public class MotorRegrasService {

    private final MqttClient mqttClient;

    public MotorRegrasService(MqttClient mqttClient) {
        this.mqttClient = mqttClient;
    }

    /**
     * Avalia as regras de automação com base nas telemetrias recebidas
     */
    public void avaliarRegra(String salaId, String tipoSensor, Double valor) {
        try {
            // 🛑 REGRA 1: Controle Inteligente do Ar-Condicionado
            if (tipoSensor.equals("temp")) {
                if (valor > 28.0) {
                    enviarComando(salaId, "LIGAR_AR");
                } else if (valor < 23.0) {
                    enviarComando(salaId, "DESLIGAR_AR");
                }
            }

            // 🛑 REGRA 2: Controle Inteligente da Cortina/Janela por Luminosidade
            if (tipoSensor.equals("luz")) {
                if (valor < 800) { // Muito sol batendo na janela
                    enviarComando(salaId, "FECHAR_CORTINA");
                } else if (valor > 3000) { // Anoiteceu / Ambiente muito escuro
                    enviarComando(salaId, "ABRIR_CORTINA");
                }
            }

        } catch (Exception e) {
            System.err.println("❌ Erro ao processar motor de regras: " + e.getMessage());
        }
    }

    /**
     * Envia um comando MQTT de volta para a placa ESP32 específica
     */
    public void enviarComando(String salaId, String comando) throws Exception {
        String topicoComando = "sgai/" + salaId + "/comandos";
        
        MqttMessage message = new MqttMessage(comando.getBytes());
        message.setQos(1); // Garante que a ordem chegue à placa
        
        mqttClient.publish(topicoComando, message);
        System.out.println("🤖 [MOTOR DE REGRAS] Comando enviado para " + topicoComando + ": " + comando);
    }
}