SELECT * FROM historico_sensores;

SELECT * FROM estado_dispositivos;

cd backend-sgai
mvnw spring-boot:run

cd sgai-frontend
npm run dev