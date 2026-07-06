# Trabalho de sistemas embarcados: SGAI - Sistema de Gerenciamento Acadêmico Integrado

O trabalho tem como proposta solucionar um problema de economia de energia em prédios comerciais e acadêmicos, ao criar regras de automação remota para poupar energia em momentos oportunos.

O codigo_final.ino é o código utilizado na placa.

### Componentes

Thiago Raquel

Marcos Fontes

Eduardo Marinho

### Comandos usados

SELECT * FROM historico_sensores;

SELECT * FROM estado_dispositivos;

cd backend-sgai
mvnw spring-boot:run

cd sgai-frontend
npm run dev