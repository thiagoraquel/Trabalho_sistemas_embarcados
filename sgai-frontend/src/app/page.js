"use client";

import { useState, useEffect } from "react";

export default function Dashboard() {
  // Agora TUDO é um estado do React, para poder mudar na tela!
  const [ledStatus, setLedStatus] = useState("OFF");
  const [arStatus, setArStatus] = useState("OFF");
  const [cortinaStatus, setCortinaStatus] = useState("FECHADA");
  const [sensorTemp, setSensorTemp] = useState(0.0);
  const [sensorLuz, setSensorLuz] = useState(0);

  // Função para buscar o estado atual lá do Spring Boot
  const carregarDadosDaSala = async () => {
    try {
      // Puxa a fotografia instantânea completa (atuadores e sensores) de uma só vez!
      const resposta = await fetch("http://localhost:8080/api/salas/sala1/status");
      if (resposta.ok) {
        const dados = await resposta.json();
        
        dados.forEach((disp) => {
          // Ajusta os Atuadores
          if (disp.dispositivo === "led") setLedStatus(disp.estado);
          if (disp.dispositivo === "ar") setArStatus(disp.estado);
          if (disp.dispositivo === "cortina") setCortinaStatus(disp.estado);
          
          // Ajusta os Sensores em Tempo Real (Mapeando direto do payload do MQTT salvo no banco)
          if (disp.dispositivo === "temp") setSensorTemp(parseFloat(disp.estado));
          if (disp.dispositivo === "luz") setSensorLuz(parseInt(disp.estado));
        });
      }
    } catch (erro) {
      console.error("Erro ao buscar dados do backend:", erro);
    }
  };
  
  // O useEffect roda sozinho quando a página abre e cria o "loop" de atualização
  useEffect(() => {
    carregarDadosDaSala(); // Busca imediatamente ao abrir a tela
    
    const intervalo = setInterval(() => {
      carregarDadosDaSala(); // Atualiza a tela a cada 5 segundos
    }, 5000);

    return () => clearInterval(intervalo); // Limpa o timer se fecharmos a tela
  }, []);

  // Função para enviar a ordem real para o Backend quando o usuário clica
  const alternarDispositivo = async (dispositivo, estadoAtual, setEstado) => {
    let comandoStr = "";

    if (dispositivo === "cortina") {
      comandoStr = estadoAtual === "ABERTA" ? "FECHAR_CORTINA" : "ABRIR_CORTINA";
    } else if (dispositivo === "led") {
      comandoStr = estadoAtual === "ON" ? "DESLIGAR_LUZ" : "LIGAR_LUZ";
    } else if (dispositivo === "ar") {
      comandoStr = estadoAtual === "ON" ? "DESLIGAR_AR" : "LIGAR_AR";
    }

    try {
      const resposta = await fetch("http://localhost:8080/api/salas/sala1/comando", {
        method: "POST",
        headers: {
          "Content-Type": "text/plain",
        },
        body: comandoStr,
      });

      if (resposta.ok) {
        const novoEstado = (dispositivo === "cortina") 
          ? (comandoStr === "ABRIR_CORTINA" ? "ABERTA" : "FECHADA")
          : (comandoStr.startsWith("LIGAR") ? "ON" : "OFF"); 
          
        setEstado(novoEstado);
        console.log(`✅ Comando real enviado com sucesso: ${comandoStr}`);
      } else {
        console.error("❌ Erro no backend ao processar o comando.");
      }
    } catch (erro) {
      console.error("❌ O Dashboard não conseguiu achar o servidor Java.", erro);
    }
  };

  return (
    <div className="min-h-screen bg-slate-900 text-slate-100 p-6 font-sans">
      
      {/* HEADER DO DASHBOARD */}
      <header className="max-w-6xl mx-auto mb-10 flex flex-col md:flex-row justify-between items-start md:items-center border-b border-slate-800 pb-6 gap-4">
        <div>
          <h1 className="text-3xl font-extrabold tracking-tight bg-gradient-to-r from-emerald-400 to-teal-500 bg-clip-text text-transparent">
            SGAI · Sistema de Gestão Acadêmica Inteligente
          </h1>
          <p className="text-slate-400 mt-1">Monitoramento e Automação Ambiental · Sala 1</p>
        </div>
        <div className="flex items-center gap-2 bg-emerald-500/10 text-emerald-400 px-3 py-1.5 rounded-full text-sm font-semibold border border-emerald-500/20">
          <span className="w-2.5 h-2.5 bg-emerald-400 rounded-full animate-pulse"></span>
          Conectado ao Backend
        </div>
      </header>

      <main className="max-w-6xl mx-auto space-y-8">
        
        {/* SEÇÃO 1: CARDS DOS SENSORES (TELEMETRIA) */}
        <section>
          <h2 className="text-xl font-bold mb-4 text-slate-300">Sensores em Tempo Real</h2>
          <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
            
            {/* Card Temperatura */}
            <div className="bg-gradient-to-br from-slate-800 to-slate-850 p-6 rounded-2xl border border-slate-750 shadow-xl flex justify-between items-center">
              <div>
                <p className="text-sm font-medium text-slate-400 uppercase tracking-wider">Temperatura</p>
                <p className="text-4xl font-black mt-2 text-white">{sensorTemp.toFixed(1)}°C</p>
              </div>
              <div className="text-4xl p-4 bg-orange-500/10 rounded-2xl text-orange-400">🌡️</div>
            </div>

            {/* Card Luminosidade */}
            <div className="bg-gradient-to-br from-slate-800 to-slate-850 p-6 rounded-2xl border border-slate-750 shadow-xl flex justify-between items-center">
              <div>
                <p className="text-sm font-medium text-slate-400 uppercase tracking-wider">Luminosidade (LDR)</p>
                <p className="text-4xl font-black mt-2 text-white">{sensorLuz}</p>
              </div>
              <div className="text-4xl p-4 bg-yellow-500/10 rounded-2xl text-yellow-400">☀️</div>
            </div>

          </div>
        </section>

        {/* SEÇÃO 2: INTERRUPTORES (ATUADORES) */}
        <section>
          <h2 className="text-xl font-bold mb-4 text-slate-300">Controle de Atuadores</h2>
          <div className="grid grid-cols-1 sm:grid-cols-3 gap-6">
            
            {/* Botão Lâmpada LED */}
            <button 
              onClick={() => alternarDispositivo("led", ledStatus, setLedStatus)}
              className={`p-6 rounded-2xl border transition-all duration-200 text-left shadow-lg flex flex-col justify-between h-40 ${
                ledStatus === "ON" 
                  ? "bg-yellow-500/10 border-yellow-500/40 text-yellow-400 shadow-yellow-500/5 hover:bg-yellow-500/15" 
                  : "bg-slate-800 border-slate-750 text-slate-400 hover:border-slate-700 hover:bg-slate-750"
              }`}
            >
              <span className="text-3xl">💡</span>
              <div>
                <p className="font-bold text-lg text-white">Iluminação LED</p>
                <p className="text-sm mt-1 font-medium">{ledStatus === "ON" ? "Ligado" : "Desligado"}</p>
              </div>
            </button>

            {/* Botão Ar-Condicionado */}
            <button 
              onClick={() => alternarDispositivo("ar", arStatus, setArStatus)}
              className={`p-6 rounded-2xl border transition-all duration-200 text-left shadow-lg flex flex-col justify-between h-40 ${
                arStatus === "ON" 
                  ? "bg-cyan-500/10 border-cyan-500/40 text-cyan-400 shadow-cyan-500/5 hover:bg-cyan-500/15" 
                  : "bg-slate-800 border-slate-750 text-slate-400 hover:border-slate-700 hover:bg-slate-750"
              }`}
            >
              <span className="text-3xl">❄️</span>
              <div>
                <p className="font-bold text-lg text-white">Ar-Condicionado</p>
                <p className="text-sm mt-1 font-medium">{arStatus === "ON" ? "Ligado" : "Desligado"}</p>
              </div>
            </button>

            {/* Botão Cortina */}
            <button 
              onClick={() => alternarDispositivo("cortina", cortinaStatus, setCortinaStatus)}
              className={`p-6 rounded-2xl border transition-all duration-200 text-left shadow-lg flex flex-col justify-between h-40 ${
                cortinaStatus === "ABERTA" 
                  ? "bg-emerald-500/10 border-emerald-500/40 text-emerald-400 shadow-emerald-500/5 hover:bg-emerald-500/15" 
                  : "bg-slate-800 border-slate-750 text-slate-400 hover:border-slate-700 hover:bg-slate-750"
              }`}
            >
              <span className="text-3xl">🪟</span>
              <div>
                <p className="font-bold text-lg text-white">Cortina (Servo)</p>
                <p className="text-sm mt-1 font-medium">{cortinaStatus === "ABERTA" ? "Aberta (180°)" : "Fechada (0°)"}</p>
              </div>
            </button>

          </div>
        </section>

      </main>
    </div>
  );
}