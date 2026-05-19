package com.thiago.sgai.config;

import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class MqttConfig {

    @Value("${mqtt.broker.url}")
    private String brokerUrl;

    @Value("${mqtt.client.id}")
    private String clientId;

    @Bean
    public MqttClient mqttClient() throws MqttException {
        // MemoryPersistence faz o cliente guardar mensagens temporárias na RAM em vez de criar arquivos no disco
        MqttClient client = new MqttClient(brokerUrl, clientId, new MemoryPersistence());
        
        MqttConnectOptions options = new MqttConnectOptions();
        options.setCleanSession(true);
        options.setAutomaticReconnect(true); // Se a internet cair, o Spring reconecta sozinho!
        options.setConnectionTimeout(10);
        
        System.out.println("Connecting to MQTT Broker: " + brokerUrl);
        client.connect(options);
        System.out.println("Successfully connected to MQTT Broker!");
        
        return client;
    }
}