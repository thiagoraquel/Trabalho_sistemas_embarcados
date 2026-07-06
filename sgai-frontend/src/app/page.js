"use client";

import { useState, useEffect, useRef } from "react";

export default function Dashboard() {
  // Estados de Telemetria e Atuadores
  const [ledStatus, setLedStatus] = useState("OFF");
  const [arStatus, setArStatus] = useState("OFF");
  const [cortinaStatus, setCortinaStatus] = useState("FECHADA");
  const [sensorTemp, setSensorTemp] = useState(0.0);
  const [sensorLuz, setSensorLuz] = useState(0);
  // NOVOS ESTADOS
  const [sensorUmidade, setSensorUmidade] = useState(0.0);
  const [sensorPresenca, setSensorPresenca] = useState("0"); 

  const bloqueioUI = useRef({ led: 0, ar: 0, cortina: 0 });

  const [regras, setRegras] = useState([]);
  const [nomeRegra, setNomeRegra] = useState("");
  const [tipoRegra, setTipoRegra] = useState("HORARIO_PONTUAL");
  const [horaInicio, setHoraInicio] = useState("");
  const [horaFim, setHoraFim] = useState("");
  const [sensorAlvo, setSensorAlvo] = useState("temp");
  const [operador, setOperador] = useState(">");
  const [valorGatilho, setValorGatilho] = useState("");
  
  const [cmdLed, setCmdLed] = useState("MANTER");
  const [cmdAr, setCmdAr] = useState("MANTER");
  const [cmdCortina, setCmdCortina] = useState("MANTER");

  const carregarDadosDaSala = async () => {
    try {
      const resposta = await fetch("http://localhost:8080/api/salas/sala1/status");
      if (resposta.ok) {
        const dados = await resposta.json();
        const agora = Date.now();

        dados.forEach((disp) => {
          if (disp.dispositivo === "led" && (agora - bloqueioUI.current.led > 7000)) setLedStatus(disp.estado);
          if (disp.dispositivo === "ar" && (agora - bloqueioUI.current.ar > 7000)) setArStatus(disp.estado);
          if (disp.dispositivo === "cortina" && (agora - bloqueioUI.current.cortina > 7000)) setCortinaStatus(disp.estado);
          
          if (disp.dispositivo === "temp") setSensorTemp(parseFloat(disp.estado));
          if (disp.dispositivo === "luz") setSensorLuz(parseInt(disp.estado));
          if (disp.dispositivo === "umidade") setSensorUmidade(parseFloat(disp.estado));
          if (disp.dispositivo === "presenca") setSensorPresenca(disp.estado);
        });
      }
    } catch (erro) {
      console.error("Erro ao carregar status:", erro);
    }
  };

  const carregarRegras = async () => {
    try {
      const resp = await fetch("http://localhost:8080/api/salas/sala1/regras");
      if (resp.ok) {
        const listaRegras = await resp.json();
        setRegras(listaRegras);
      }
    } catch (erro) {
      console.error("Erro ao carregar regras:", erro);
    }
  };

  useEffect(() => {
    carregarDadosDaSala();
    carregarRegras();
    
    const intervalo = setInterval(() => {
      carregarDadosDaSala();
    }, 5000);

    return () => clearInterval(intervalo);
  }, []);

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
        headers: { "Content-Type": "text/plain" },
        body: comandoStr,
      });
      if (resposta.ok) {
        bloqueioUI.current[dispositivo] = Date.now();
        const novoEstado = (dispositivo === "cortina") 
          ? (comandoStr === "ABRIR_CORTINA" ? "ABERTA" : "FECHADA")
          : (comandoStr.startsWith("LIGAR") ? "ON" : "OFF"); 
        
        setEstado(novoEstado);
      }
    } catch (erro) {
      console.error("Erro no fetch de comando:", erro);
    }
  };

  const salvarNovaRegra = async (e) => {
    e.preventDefault();

    const comandosObj = {};
    if (cmdLed !== "MANTER") comandosObj.led = cmdLed;
    if (cmdAr !== "MANTER") comandosObj.ar = cmdAr;
    if (cmdCortina !== "MANTER") comandosObj.cortina = cmdCortina;

    const payloadRegra = {
      nome: nomeRegra,
      tipoRegra: tipoRegra,
      horaInicio: horaInicio ? `${horaInicio}:00` : null,
      horaFim: tipoRegra === "CONDICAO_SENSOR" && horaFim ? `${horaFim}:00` : null,
      sensorAlvo: tipoRegra === "CONDICAO_SENSOR" ? sensorAlvo : null,
      operador: tipoRegra === "CONDICAO_SENSOR" ? operador : null,
      valorGatilho: tipoRegra === "CONDICAO_SENSOR" ? parseFloat(valorGatilho) : null,
      comandosJson: JSON.stringify(comandosObj),
      ativa: true
    };

    try {
      const resposta = await fetch("http://localhost:8080/api/salas/sala1/regras", {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify(payloadRegra),
      });

      if (resposta.ok) {
        setNomeRegra("");
        setHoraInicio("");
        setHoraFim("");
        setValorGatilho("");
        setCmdLed("MANTER");
        setCmdAr("MANTER");
        setCmdCortina("MANTER");
        carregarRegras();
        alert("Regra de automação salva com sucesso!");
      }
    } catch (erro) {
      console.error("Erro ao salvar regra:", erro);
    }
  };

  const excluirRegra = async (id) => {
    if (!confirm("Tem certeza que deseja remover esta regra de automação?")) return;

    try {
      const resp = await fetch(`http://localhost:8080/api/salas/sala1/regras/${id}`, {
        method: "DELETE",
      });

      if (resp.ok) {
        carregarRegras();
      } else {
        alert("Erro ao tentar excluir a regra no servidor.");
      }
    } catch (erro) {
      console.error("Erro ao conectar com o backend para exclusão:", erro);
    }
  };

  return (
    <div className="min-h-screen bg-slate-900 text-slate-100 p-6 font-sans">
      
      <header className="max-w-6xl mx-auto mb-10 flex flex-col md:flex-row justify-between items-start md:items-center border-b border-slate-800 pb-6 gap-4">
        <div>
          <h1 className="text-3xl font-extrabold tracking-tight bg-gradient-to-r from-emerald-400 to-teal-500 bg-clip-text text-transparent">
            SGAI · Sistema de Gestão Acadêmica Inteligente
          </h1>
          <p className="text-slate-400 mt-1">Painel Mestre de Controle e Automação Customizada</p>
        </div>
      </header>

      <main className="max-w-6xl mx-auto space-y-10">
        
        {/* TELEMETRIA E ATUADORES (LADO A LADO) */}
        <div className="grid grid-cols-1 lg:grid-cols-3 gap-8">
          
          {/* Cards de Sensores */}
          <div className="lg:col-span-1 space-y-4">
            <h2 className="text-xl font-bold text-slate-300">Telemetria Atual</h2>
            
            {/* Temperatura */}
            <div className="bg-slate-850 p-5 rounded-2xl border border-slate-750 flex justify-between items-center shadow-md">
              <div>
                <p className="text-xs text-slate-400 uppercase tracking-wider font-semibold">Temperatura</p>
                <p className="text-3xl font-black mt-1 text-white">{sensorTemp.toFixed(1)}°C</p>
              </div>
              <span className="text-3xl p-3 bg-orange-500/10 rounded-xl text-orange-400">🌡️</span>
            </div>

            {/* Umidade */}
            <div className="bg-slate-850 p-5 rounded-2xl border border-slate-750 flex justify-between items-center shadow-md">
              <div>
                <p className="text-xs text-slate-400 uppercase tracking-wider font-semibold">Umidade</p>
                <p className="text-3xl font-black mt-1 text-white">{sensorUmidade.toFixed(1)}%</p>
              </div>
              <span className="text-3xl p-3 bg-blue-500/10 rounded-xl text-blue-400">💧</span>
            </div>

            {/* Luminosidade */}
            <div className="bg-slate-850 p-5 rounded-2xl border border-slate-750 flex justify-between items-center shadow-md">
              <div>
                <p className="text-xs text-slate-400 uppercase tracking-wider font-semibold">Luminosidade</p>
                <p className="text-3xl font-black mt-1 text-white">{sensorLuz} lx</p>
              </div>
              <span className="text-3xl p-3 bg-yellow-500/10 rounded-xl text-yellow-400">☀️</span>
            </div>

            {/* Presença (PIR) */}
            <div className={`p-5 rounded-2xl border flex justify-between items-center shadow-md transition-all ${sensorPresenca === "1" ? "bg-red-500/10 border-red-500/40" : "bg-slate-850 border-slate-750"}`}>
              <div>
                <p className={`text-xs uppercase tracking-wider font-semibold ${sensorPresenca === "1" ? "text-red-400" : "text-slate-400"}`}>Movimento</p>
                <p className={`text-2xl font-black mt-1 ${sensorPresenca === "1" ? "text-red-400" : "text-white"}`}>
                  {sensorPresenca === "1" ? "DETECTADO" : "Calmo"}
                </p>
              </div>
              <span className={`text-3xl p-3 rounded-xl ${sensorPresenca === "1" ? "bg-red-500/20 text-red-400" : "bg-slate-700/50 text-slate-400"}`}>
                {sensorPresenca === "1" ? "🏃" : "🧘"}
              </span>
            </div>
          </div>

          {/* Botões de Override Manual */}
          <div className="lg:col-span-2 space-y-4">
            <h2 className="text-xl font-bold text-slate-300">Controle Remoto (Mestre)</h2>
            <div className="grid grid-cols-1 sm:grid-cols-3 gap-4">
              <button onClick={() => alternarDispositivo("led", ledStatus, setLedStatus)} className={`p-5 rounded-2xl border text-left transition-all h-32 flex flex-col justify-between ${ledStatus === "ON" ? "bg-yellow-500/10 border-yellow-500/40 text-yellow-400" : "bg-slate-800 border-slate-750 text-slate-400"}`}><span className="text-2xl">💡</span><div><p className="font-bold text-white">Iluminação</p><p className="text-xs">{ledStatus === "ON" ? "Ligado" : "Desligado"}</p></div></button>
              <button onClick={() => alternarDispositivo("ar", arStatus, setArStatus)} className={`p-5 rounded-2xl border text-left transition-all h-32 flex flex-col justify-between ${arStatus === "ON" ? "bg-cyan-500/10 border-cyan-500/40 text-cyan-400" : "bg-slate-800 border-slate-750 text-slate-400"}`}><span className="text-2xl">❄️</span><div><p className="font-bold text-white">Climatização</p><p className="text-xs">{arStatus === "ON" ? "Ligado" : "Desligado"}</p></div></button>
              <button onClick={() => alternarDispositivo("cortina", cortinaStatus, setCortinaStatus)} className={`p-5 rounded-2xl border text-left transition-all h-32 flex flex-col justify-between ${cortinaStatus === "ABERTA" ? "bg-emerald-500/10 border-emerald-500/40 text-emerald-400" : "bg-slate-800 border-slate-750 text-slate-400"}`}><span className="text-2xl">🪟</span><div><p className="font-bold text-white">Cortina</p><p className="text-xs">{cortinaStatus === "ABERTA" ? "Aberta (180°)" : "Fechada (0°)"}</p></div></button>
            </div>
          </div>
        </div>

        {/* REGRAS AUTOMÁTICAS: GERENCIADOR DINÂMICO */}
        <div className="grid grid-cols-1 lg:grid-cols-3 gap-8 pt-4 border-t border-slate-800">
          
          {/* Formulário de Criação */}
          <div className="lg:col-span-2 bg-slate-850 p-6 rounded-2xl border border-slate-750 shadow-xl space-y-4">
            <h2 className="text-xl font-bold text-emerald-400">Nova Regra de Automação</h2>
            <form onSubmit={salvarNovaRegra} className="space-y-4">
              
              <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
                <div>
                  <label className="text-xs font-semibold text-slate-400 block mb-1">Nome da Regra</label>
                  <input type="text" value={nomeRegra} onChange={(e) => setNomeRegra(e.target.value)} placeholder="Ex: LATE NIGHT ou HOT DAY" className="w-full bg-slate-800 border border-slate-700 rounded-xl p-2.5 text-white focus:outline-none focus:border-emerald-500" required />
                </div>
                <div>
                  <label className="text-xs font-semibold text-slate-400 block mb-1">Gatilho de Disparo</label>
                  <select value={tipoRegra} onChange={(e) => setTipoRegra(e.target.value)} className="w-full bg-slate-800 border border-slate-700 rounded-xl p-2.5 text-white focus:outline-none focus:border-emerald-500">
                    <option value="HORARIO_PONTUAL">Horário Pontual Fixo</option>
                    <option value="CONDICAO_SENSOR">Intervalo de Horário + Sensor</option>
                  </select>
                </div>
              </div>

              {/* Parâmetros Condicionais (Renderiza apenas se for regra de sensor) */}
              {tipoRegra === "CONDICAO_SENSOR" && (
                <div className="p-4 bg-slate-800/50 rounded-xl border border-slate-700/50 space-y-3">
                  <p className="text-xs font-bold text-slate-400 uppercase tracking-wider">Configuração de Gatilho</p>
                  <div className="grid grid-cols-1 sm:grid-cols-3 gap-3">
                    <div>
                      <label className="text-xs text-slate-400 block mb-1">Hora de Início</label>
                      <input type="time" value={horaInicio} onChange={(e) => setHoraInicio(e.target.value)} className="w-full bg-slate-800 border border-slate-700 rounded-xl p-2 text-white" required />
                    </div>
                    <div>
                      <label className="text-xs text-slate-400 block mb-1">Hora de Término</label>
                      <input type="time" value={horaFim} onChange={(e) => setHoraFim(e.target.value)} className="w-full bg-slate-800 border border-slate-700 rounded-xl p-2 text-white" required />
                    </div>
                    <div>
                      <label className="text-xs text-slate-400 block mb-1">Sensor</label>
                      <select value={sensorAlvo} onChange={(e) => setSensorAlvo(e.target.value)} className="w-full bg-slate-800 border border-slate-700 rounded-xl p-2 text-white">
                        <option value="temp">Temperatura</option>
                        <option value="luz">Luminosidade</option>
                        <option value="umidade">Umidade</option>
                        <option value="presenca">Presença (1=Sim, 0=Não)</option>
                      </select>
                    </div>
                  </div>

                  <div className="grid grid-cols-1 sm:grid-cols-2 gap-4 pt-2">
                    <div>
                      <label className="text-xs text-slate-400 block mb-1">Operador</label>
                      <select value={operador} onChange={(e) => setOperador(e.target.value)} className="w-full bg-slate-800 border border-slate-700 rounded-xl p-2 text-white">
                        <option value=">">Maior que (&gt;)</option>
                        <option value="<">Menor que (&lt;)</option>
                        <option value="==">Igual a (==)</option>
                      </select>
                    </div>
                    <div>
                      <label className="text-xs text-slate-400 block mb-1">Valor Limite (Gatilho)</label>
                      <input type="number" step="0.1" value={valorGatilho} onChange={(e) => setValorGatilho(e.target.value)} placeholder="Ex: 30.0, 800 ou 1" className="w-full bg-slate-800 border border-slate-700 rounded-xl p-2 text-white" required />
                    </div>
                  </div>
                </div>
              )}

              {/* CONJUNTO DE COMANDOS JSON */}
              <div className="p-4 bg-emerald-500/5 rounded-xl border border-emerald-500/10 space-y-3">
                <p className="text-xs font-bold text-emerald-400 uppercase tracking-wider">Ações Executadas (Carga Útil JSON)</p>
                <div className="grid grid-cols-1 sm:grid-cols-3 gap-3">
                  <div>
                    <label className="text-xs text-slate-400 block mb-1">Lâmpada LED</label>
                    <select value={cmdLed} onChange={(e) => setCmdLed(e.target.value)} className="w-full bg-slate-800 border border-slate-700 rounded-xl p-2 text-white">
                      <option value="MANTER">Manter como está</option>
                      <option value="ON">LIGAR</option>
                      <option value="OFF">DESLIGAR</option>
                    </select>
                  </div>
                  <div>
                    <label className="text-xs text-slate-400 block mb-1">Ar-Condicionado</label>
                    <select value={cmdAr} onChange={(e) => setCmdAr(e.target.value)} className="w-full bg-slate-800 border border-slate-700 rounded-xl p-2 text-white">
                      <option value="MANTER">Manter como está</option>
                      <option value="ON">LIGAR</option>
                      <option value="OFF">DESLIGAR</option>
                    </select>
                  </div>
                  <div>
                    <label className="text-xs text-slate-400 block mb-1">Cortina</label>
                    <select value={cmdCortina} onChange={(e) => setCmdCortina(e.target.value)} className="w-full bg-slate-800 border border-slate-700 rounded-xl p-2 text-white">
                      <option value="MANTER">Manter como está</option>
                      <option value="ABERTA">ABRIR</option>
                      <option value="FECHADA">FECHAR</option>
                    </select>
                  </div>
                </div>
              </div>

              <button type="submit" className="w-full bg-gradient-to-r from-emerald-500 to-teal-600 hover:from-emerald-600 hover:to-teal-700 text-white font-bold py-3 rounded-xl transition-all shadow-md shadow-emerald-950/20">
                Salvar Regra de Automação
              </button>
            </form>
          </div>

          {/* Banco de Regras Ativas */}
          <div className="lg:col-span-1 space-y-4">
            <h2 className="text-xl font-bold text-slate-300">Regras Ativas no Banco</h2>
            <div className="space-y-3 max-h-[480px] overflow-y-auto pr-2">
              {regras.length === 0 ? (
                <p className="text-sm text-slate-500 italic p-4 bg-slate-850 rounded-xl border border-slate-800">Nenhuma regra cadastrada na sala1.</p>
              ) : (
                regras.map((reg) => (
                  <div key={reg.id} className="bg-slate-850 p-4 rounded-xl border border-slate-750 space-y-2 shadow-sm relative group">
                    
                    <div className="flex justify-between items-start">
                      <div className="pr-6">
                        <h3 className="font-bold text-white tracking-wide">{reg.nome}</h3>
                        <span className={`text-[10px] font-bold px-2 py-0.5 rounded-full inline-block mt-1 ${reg.tipoRegra === "HORARIO_PONTUAL" ? "bg-purple-500/10 text-purple-400 border border-purple-500/20" : "bg-blue-500/10 text-blue-400 border border-blue-500/20"}`}>
                          {reg.tipoRegra === "HORARIO_PONTUAL" ? "Fixo" : "Sensor"}
                        </span>
                      </div>
                      
                      <button 
                        onClick={() => excluirRegra(reg.id)}
                        className="text-slate-500 hover:text-red-400 p-1.5 rounded-lg hover:bg-red-500/10 transition-colors absolute top-3 right-3"
                        title="Excluir Regra"
                      >
                        🗑️
                      </button>
                    </div>

                    <p className="text-xs text-slate-400">
                      Horário: <span className="text-slate-300 font-medium">
                        {reg.horaInicio ? reg.horaInicio.substring(0,5) : "Tempo Integral"}
                      </span>
                      {reg.horaFim ? <> até <span className="text-slate-300 font-medium">{reg.horaFim.substring(0,5)}</span></> : ""}
                    </p>
                    {reg.sensorAlvo && (
                      <p className="text-xs text-slate-400">
                        Condição: <span className="text-emerald-400 font-semibold">{reg.sensorAlvo} {reg.operador} {reg.valorGatilho}</span>
                      </p>
                    )}
                    <div className="pt-2 border-t border-slate-800/60">
                      <p className="text-[11px] text-slate-500 block mb-1 font-mono font-bold uppercase tracking-wider">Payload JSON Emitido:</p>
                      <code className="text-[11px] block bg-slate-900 p-2 rounded border border-slate-800 text-teal-400 overflow-x-auto font-mono">
                        {reg.comandosJson}
                      </code>
                    </div>
                  </div>
                ))
              )}
            </div>
          </div>
        </div>
      </main>
    </div>
  );
}