#include <WiFi.h>
#include <PubSubClient.h>
#include <ArduinoJson.h>
#include <DHT.h>
#include <ESP32Servo.h>

// ==========================================
// 1. CREDENCIAIS WIFI E MQTT
// ==========================================
const char* ssid = "THIAGORAQUEL 0468"; 
const char* password = "2^Xs3063";
const char* mqtt_server = "broker.emqx.io";

WiFiClient espClient;
PubSubClient client(espClient);

// ==========================================
// 2. MAPEAMENTO DE PINOS (Físicos - SYB-118)
// ==========================================
// Lado Esquerdo (Atuadores e Botões)
#define PINO_LED_AZUL 15
#define PINO_RELE 4
#define PINO_SERVO 5
#define PINO_BOTAO_LED 19
#define PINO_BOTAO_RELE 21
#define PINO_BOTAO_SERVO 23

// Lado Direito (Sensores)
#define PINO_LED_VERDE 13
#define PINO_DHT 14
#define PINO_PIR 32
#define PINO_LDR 34

// ==========================================
// 3. OBJETOS E VARIÁVEIS DE ESTADO
// ==========================================
DHT dht(PINO_DHT, DHT22);
Servo meuServo;

// Estados atuais dos atuadores
bool estadoLedAzul = false;
bool estadoRele = false;
bool estadoServo = false; // false = Fechada (0º), true = Aberta (180º)

// MEMÓRIA DO PIR: Guarda se houve movimento dentro do intervalo de 5s
bool houveMovimentoNoIntervalo = false; 

// Variáveis de Debounce (40ms)
const unsigned long delayDebounce = 40;
bool estadoEstavelB1 = HIGH, ultimoFisicoB1 = HIGH; unsigned long tempoRepiqueB1 = 0;
bool estadoEstavelB2 = HIGH, ultimoFisicoB2 = HIGH; unsigned long tempoRepiqueB2 = 0;
bool estadoEstavelB3 = HIGH, ultimoFisicoB3 = HIGH; unsigned long tempoRepiqueB3 = 0;

// Temporizadores (Sem usar delay!)
unsigned long tempoAnteriorSensores = 0;
const unsigned long intervaloSensores = 1000; // 1 seg para Monitor Serial local

unsigned long ultimoTempoDeEnvioMQTT = 0;
const unsigned long intervaloMQTT = 5000;     // 5 seg para Nuvem

// ==========================================
// 4. FUNÇÕES DE REDE (WIFI E MQTT)
// ==========================================
void setup_wifi() {
  delay(10);
  Serial.println();
  Serial.print("Conectando na rede: ");
  Serial.println(ssid);

  WiFi.begin(ssid, password);

  while (WiFi.status() != WL_CONNECTED) {
    delay(500);
    Serial.print(".");
  }
  Serial.println("\nWiFi conectado com Sucesso!");
}

void reconnect() {
  while (!client.connected()) {
    Serial.print("Tentando conexao MQTT...");
    String clientId = "SGAI_ESP32_Mestre_";
    clientId += String(random(0xffff), HEX);
    
    if (client.connect(clientId.c_str())) {
      Serial.println("Conectado ao Broker MQTT!");
      client.subscribe("sgai/sala1/comandos"); 
    } else {
      Serial.print("Falhou, rc=");
      Serial.print(client.state());
      Serial.println(" tentando em 5 segundos...");
      delay(5000);
    }
  }
}

// Callback: Recebe mensagens do Dashboard (Nuvem -> Placa)
void callback_mqtt(char* topic, byte* payload, unsigned int length) {
  String mensagem = "";
  for (int i = 0; i < length; i++) {
    mensagem += (char)payload[i];
  }
  
  if (mensagem.startsWith("{")) {
    StaticJsonDocument<256> doc;
    DeserializationError error = deserializeJson(doc, mensagem);
    
    if (!error) {
      Serial.println("📦 [JSON] Comando Recebido...");
      
      if (doc.containsKey("led")) {
        String cmdLed = doc["led"];
        if (cmdLed == "ON" && !estadoLedAzul) {
          estadoLedAzul = true; digitalWrite(PINO_LED_AZUL, HIGH);
        } else if (cmdLed == "OFF" && estadoLedAzul) {
          estadoLedAzul = false; digitalWrite(PINO_LED_AZUL, LOW);
        }
      }

      if (doc.containsKey("ar")) {
        String cmdAr = doc["ar"];
        if (cmdAr == "ON" && !estadoRele) {
          estadoRele = true; digitalWrite(PINO_RELE, HIGH);
        } else if (cmdAr == "OFF" && estadoRele) {
          estadoRele = false; digitalWrite(PINO_RELE, LOW);
        }
      }

      if (doc.containsKey("cortina")) {
        String cmdCortina = doc["cortina"];
        if (cmdCortina == "ABERTA" && !estadoServo) {
          estadoServo = true; meuServo.write(180);
        } else if (cmdCortina == "FECHADA" && estadoServo) {
          estadoServo = false; meuServo.write(0);
        }
      }
    }
  } 
  else {
    if (mensagem == "LIGAR_LUZ") {
      estadoLedAzul = true; digitalWrite(PINO_LED_AZUL, HIGH);
    } else if (mensagem == "DESLIGAR_LUZ") {
      estadoLedAzul = false; digitalWrite(PINO_LED_AZUL, LOW);
    }
    else if (mensagem == "LIGAR_AR") {
      estadoRele = true; digitalWrite(PINO_RELE, HIGH);
    }
    else if (mensagem == "DESLIGAR_AR") {
      estadoRele = false; digitalWrite(PINO_RELE, LOW);
    }
    else if (mensagem == "ABRIR_CORTINA") {
      estadoServo = true; meuServo.write(180); 
    }
    else if (mensagem == "FECHAR_CORTINA") {
      estadoServo = false; meuServo.write(0);  
    }
  }
}

// ==========================================
// 5. SETUP E LOOP PRINCIPAL
// ==========================================
void setup() {
  Serial.begin(115200);
  
  dht.begin();
  meuServo.attach(PINO_SERVO);
  meuServo.write(0); 

  pinMode(PINO_LED_AZUL, OUTPUT);
  pinMode(PINO_RELE, OUTPUT);
  pinMode(PINO_LED_VERDE, OUTPUT);
  
  pinMode(PINO_PIR, INPUT);
  pinMode(PINO_LDR, INPUT); 

  pinMode(PINO_BOTAO_LED, INPUT_PULLUP);
  pinMode(PINO_BOTAO_RELE, INPUT_PULLUP);
  pinMode(PINO_BOTAO_SERVO, INPUT_PULLUP);
  
  digitalWrite(PINO_LED_AZUL, LOW);
  digitalWrite(PINO_RELE, LOW);
  digitalWrite(PINO_LED_VERDE, LOW);
  
  setup_wifi();
  client.setServer(mqtt_server, 1883);
  client.setCallback(callback_mqtt);
}

void loop() {
  if (!client.connected()) reconnect();
  client.loop(); 

  unsigned long tempoAtual = millis();

  // --- LÓGICA DO PIR EM TEMPO REAL ---
  bool leituraPIR = digitalRead(PINO_PIR);
  digitalWrite(PINO_LED_VERDE, leituraPIR); 
  
  // Se detectou movimento neste exato instante, trava a memória como VERDADEIRA
  if (leituraPIR == HIGH) {
    houveMovimentoNoIntervalo = true;
  }

  // --- LÓGICA LOCAL DO MONITOR SERIAL (A cada 1s) ---
  if (tempoAtual - tempoAnteriorSensores >= intervaloSensores) {
    tempoAnteriorSensores = tempoAtual;
    
    int valorLuz = analogRead(PINO_LDR);
    float temperatura = dht.readTemperature();
    float umidade = dht.readHumidity();
    
    Serial.println("--- LEITURA LOCAL ---");
    Serial.print("Luz: "); Serial.print(valorLuz);
    Serial.print(" | Movimento: "); Serial.print(leituraPIR ? "Sim" : "Nao");
    
    if (!isnan(temperatura) && !isnan(umidade)) {
      Serial.print(" | Temp: "); Serial.print(temperatura); Serial.print(" °C");
      Serial.print(" | Umi: "); Serial.print(umidade); Serial.println(" %");
    } else {
      Serial.println();
    }
    Serial.println("---------------------");
  }

  // --- LÓGICA DE ENVIO MQTT (A cada 5s) ---
  if (tempoAtual - ultimoTempoDeEnvioMQTT > intervaloMQTT) {
    ultimoTempoDeEnvioMQTT = tempoAtual;
    
    int valorLDR = analogRead(PINO_LDR);
    float temperatura = dht.readTemperature();
    float umidade = dht.readHumidity();

    // 1. Envia a flag travada de movimento
    client.publish("sgai/sala1/presenca", houveMovimentoNoIntervalo ? "1" : "0"); 
    
    // 2. Imediatamente após enviar, limpa a memória para os próximos 5 segundos
    houveMovimentoNoIntervalo = false;

    // 3. Envia Telemetria (Luz, Temp, Umidade)
    client.publish("sgai/sala1/luz", String(valorLDR).c_str());
    if (!isnan(temperatura)) {
      client.publish("sgai/sala1/temp", String(temperatura).c_str());
    }
    if (!isnan(umidade)) {
      client.publish("sgai/sala1/umidade", String(umidade).c_str());
    }

    // 4. Envia Estado dos Atuadores
    client.publish("sgai/sala1/led", estadoLedAzul ? "ON" : "OFF");
    client.publish("sgai/sala1/ar", estadoRele ? "ON" : "OFF");
    client.publish("sgai/sala1/cortina", estadoServo ? "ABERTA" : "FECHADA");
  }

  // --- LÓGICA DE DEBOUNCE DOS BOTÕES FÍSICOS (40ms) ---
  
  // BOTÃO 1: LED
  bool leituraFisicaB1 = digitalRead(PINO_BOTAO_LED);
  if (leituraFisicaB1 != ultimoFisicoB1) tempoRepiqueB1 = tempoAtual;
  if ((tempoAtual - tempoRepiqueB1) > delayDebounce) {
    if (leituraFisicaB1 != estadoEstavelB1) {
      estadoEstavelB1 = leituraFisicaB1;
      if (estadoEstavelB1 == LOW) {
        estadoLedAzul = !estadoLedAzul;
        digitalWrite(PINO_LED_AZUL, estadoLedAzul);
      }
    }
  }
  ultimoFisicoB1 = leituraFisicaB1;

  // BOTÃO 2: Relé
  bool leituraFisicaB2 = digitalRead(PINO_BOTAO_RELE);
  if (leituraFisicaB2 != ultimoFisicoB2) tempoRepiqueB2 = tempoAtual;
  if ((tempoAtual - tempoRepiqueB2) > delayDebounce) {
    if (leituraFisicaB2 != estadoEstavelB2) {
      estadoEstavelB2 = leituraFisicaB2;
      if (estadoEstavelB2 == LOW) {
        estadoRele = !estadoRele;
        digitalWrite(PINO_RELE, estadoRele); 
      }
    }
  }
  ultimoFisicoB2 = leituraFisicaB2;

  // BOTÃO 3: Servo Motor
  bool leituraFisicaB3 = digitalRead(PINO_BOTAO_SERVO);
  if (leituraFisicaB3 != ultimoFisicoB3) tempoRepiqueB3 = tempoAtual;
  if ((tempoAtual - tempoRepiqueB3) > delayDebounce) {
    if (leituraFisicaB3 != estadoEstavelB3) {
      estadoEstavelB3 = leituraFisicaB3;
      if (estadoEstavelB3 == LOW) {
        estadoServo = !estadoServo;
        meuServo.write(estadoServo ? 180 : 0); 
      }
    }
  }
  ultimoFisicoB3 = leituraFisicaB3;
}