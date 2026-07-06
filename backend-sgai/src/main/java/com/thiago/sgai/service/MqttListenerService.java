package com.thiago.sgai.service;

import com.thiago.sgai.model.EstadoDispositivo;
import com.thiago.sgai.model.HistoricoSensor;
import com.thiago.sgai.repository.EstadoDispositivoRepository;
import com.thiago.sgai.repository.HistoricoSensorRepository;
import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.MqttCallback;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.springframework.stereotype.Service;
import jakarta.annotation.PostConstruct;
import java.util.Optional;

@Service
public class MqttListenerService implements MqttCallback {

    private final MqttClient mqttClient;
    private final HistoricoSensorRepository historicoRepository;
    private final EstadoDispositivoRepository estadoRepository;
    private final MotorRegrasService motorRegrasService;

    public MqttListenerService(MqttClient mqttClient, 
                               HistoricoSensorRepository historicoRepository, 
                               EstadoDispositivoRepository estadoRepository,
                               MotorRegrasService motorRegrasService) {
        this.mqttClient = mqttClient;
        this.historicoRepository = historicoRepository;
        this.estadoRepository = estadoRepository;
        this.motorRegrasService = motorRegrasService;
    }

    @PostConstruct
    public void init() {
        try {
            mqttClient.setCallback(this);
            mqttClient.subscribe("sgai/#", 1); 
            System.out.println("SGAI Master Listener initialized. Subscribed to 'sgai/#'");
        } catch (MqttException e) {
            System.err.println("Error subscribing to MQTT topics: " + e.getMessage());
        }
    }

    @Override
    public void connectionLost(Throwable cause) {
        System.out.println("MQTT Connection lost! Cause: " + cause.getMessage());
    }

    @Override
    public void messageArrived(String topic, MqttMessage message) throws Exception {
        String payload = new String(message.getPayload()).trim();
        String[] partes = topic.split("/");
        
        if (partes.length >= 3) {
            String salaId = partes[1];
            String tipoDispositivo = partes[2];
            
            System.out.println("[MQTT] Sala: " + salaId + " | Disp: " + tipoDispositivo + " | Valor: " + payload);

            try {
                // 1. Salva no Histórico e Aciona o Motor de Regras (Agora inclui umidade e presenca)
                if (tipoDispositivo.equals("temp") || tipoDispositivo.equals("luz") || 
                    tipoDispositivo.equals("umidade") || tipoDispositivo.equals("presenca")) {
                    try {
                        Double valorNumerico = Double.parseDouble(payload);
                        historicoRepository.save(new HistoricoSensor(salaId, tipoDispositivo, valorNumerico));
                        
                        motorRegrasService.avaliarRegra(salaId, tipoDispositivo, valorNumerico);
                        
                    } catch (NumberFormatException e) {
                        System.err.println("⚠️ Valor numérico inválido para histórico: " + payload);
                    }
                }
                
                // 2. Atualiza a fotografia do Estado Atual (para todos os tópicos)
                Optional<EstadoDispositivo> estadoExistente = estadoRepository.findBySalaAndDispositivo(salaId, tipoDispositivo);
                if (estadoExistente.isPresent()) {
                    EstadoDispositivo dispositivo = estadoExistente.get();
                    dispositivo.setEstado(payload);
                    estadoRepository.save(dispositivo);
                } else {
                    estadoRepository.save(new EstadoDispositivo(salaId, tipoDispositivo, payload));
                }
                
            } catch (NumberFormatException e) {
                System.err.println("❌ Erro de conversão em " + tipoDispositivo + ": " + payload);
            }
        }
    }

    @Override
    public void deliveryComplete(IMqttDeliveryToken token) {
    }
}