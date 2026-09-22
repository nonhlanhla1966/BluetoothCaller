import React, { useState, useEffect, useRef } from "react";

const DEVICES = [
  { id: "d1", name: "JBL Tune 520BT", type: "Headphones", battery: 78 },
  { id: "d2", name: "Toyota Corolla 2019", type: "Car Audio", battery: null },
  { id: "d3", name: "Samsung Galaxy Buds", type: "Headphones", battery: 45 },
  { id: "d4", name: "Anker Soundcore", type: "Speaker", battery: 92 },
];

const CONTACTS = [
  { id: "c1", name: "Thandi Mkhize", number: "+268 7612 3456", initials: "TM" },
  { id: "c2", name: "Bongani Dlamini", number: "+268 7701 9876", initials: "BD" },
  { id: "c3", name: "Nomsa Nkambule", number: "+268 5502 1122", initials: "NN" },
  { id: "c4", name: "Sibusiso Ngwenya", number: "+268 7420 8890", initials: "SN" },
  { id: "c5", name: "Zanele Mabuza", number: "+268 7833 4455", initials: "ZM" },
];

function fmt(sec) {
  const m = String(Math.floor(sec / 60)).padStart(2, "0");
  const s = String(sec % 60).padStart(2, "0");
  return `${m}:${s}`;
}

export default function App() {
  const [screen, setScreen] = useState("devices"); // devices | dialer | contacts | calling | incoming
  const [paired, setPaired] = useState(null);
  const [scanning, setScanning] = useState(false);
  const [scanProgress, setScanProgress] = useState(0);
  const [found, setFound] = useState([]);
  const [number, setNumber] = useState("");
  const [call, setCall] = useState({ contact: null, state: "dialing", sec: 0 });
  const [muted, setMuted] = useState(false);
  const timerRef = useRef(null);

  // Bluetooth scanning simulation
  useEffect(() => {
    if (!scanning) return;
    setScanProgress(0);
    setFound([]);
    const int = setInterval(() => {
      setScanProgress((p) => {
        const next = p + 4;
        if (next >= 100) {
          clearInterval(int);
          setScanning(false);
          return 100;
        }
        if (next > 25 && found.length === 0) setFound([DEVICES[0]]);
        if (next > 55 && found.length === 1) setFound((f) => [...f, DEVICES[2]]);
        if (next > 80 && found.length === 2) setFound((f) => [...f, DEVICES[3]]);
        return next;
      });
    }, 90);
    return () => clearInterval(int);
  }, [scanning]);

  // Call timer
  useEffect(() => {
    if (call.state === "active") {
      timerRef.current = setInterval(() => {
        setCall((c) => ({ ...c, sec: c.sec + 1 }));
      }, 1000);
    }
    return () => clearInterval(timerRef.current);
  }, [call.state]);

  const pair = (device) => {
    setPaired(device);
    setScreen("dialer");
  };

  const startCall = (contact) => {
    setCall({ contact, state: "dialing", sec: 0 });
    setScreen("calling");
    setTimeout(() => {
      setCall((c) => (c.state === "dialing" ? { ...c, state: "active" } : c));
    }, 2500);
  };

  const simulateIncoming = () => {
    const contact = CONTACTS[Math.floor(Math.random() * CONTACTS.length)];
    setCall({ contact, state: "incoming", sec: 0 });
    setScreen("incoming");
  };

  const hangUp = () => {
    setScreen(paired ? "dialer" : "devices");
    setCall({ contact: null, state: "dialing", sec: 0 });
  };

  const answer = () => {
    setCall((c) => ({ ...c, state: "active" }));
    setScreen("calling");
  };

  const pressKey = (k) => {
    if (k === "del") {
      setNumber((n) => n.slice(0, -1));
      return;
    }
    setNumber((n) => (n.length < 15 ? n + k : n));
  };

  // ---------- Screens ----------

  if (screen === "calling" || screen === "incoming") {
    const incoming = call.state === "incoming";
    const active = call.state === "active";
    return (
      <div className="min-h-screen bg-gradient-to-b from-slate-900 to-slate-950 text-white flex items-center justify-center p-4">
        <div className="w-full max-w-sm">
          <div className="rounded-3xl bg-slate-900/70 border border-slate-700 p-8 text-center shadow-2xl">
            {paired && (
              <div className="text-xs text-sky-400 mb-4 flex items-center justify-center gap-2">
                <span>🔊</span> Audio via {paired.name}
              </div>
            )}
            <div className="mx-auto w-24 h-24 rounded-full bg-gradient-to-br from-sky-500 to-indigo-600 flex items-center justify-center text-3xl font-bold mb-4">
              {call.contact?.initials}
            </div>
            <h1 className="text-2xl font-semibold">{call.contact?.name}</h1>
            <p className="text-slate-400 text-sm mt-1">{call.contact?.number}</p>
            <p className="mt-4 font-mono text-lg">
              {incoming ? (
                <span className="animate-pulse text-emerald-400">Incoming call…</span>
              ) : active ? (
                fmt(call.sec)
              ) : (
                <span className="animate-pulse text-sky-300">Calling…</span>
              )}
            </p>

            {active && (
              <div className="grid grid-cols-3 gap-3 mt-8">
                <button
                  onClick={() => setMuted((m) => !m)}
                  className={`py-3 rounded-xl text-sm ${muted ? "bg-amber-500 text-slate-900" : "bg-slate-800"}`}
                >
                  {muted ? "🔇 Muted" : "🎙 Mute"}
                </button>
                <button className="py-3 rounded-xl text-sm bg-slate-800">⌨️ Keypad</button>
                <button className="py-3 rounded-xl text-sm bg-slate-800">🔊 Speaker</button>
              </div>
            )}

            <div className="flex justify-center gap-6 mt-8">
              {incoming ? (
                <>
                  <button
                    onClick={hangUp}
                    className="w-16 h-16 rounded-full bg-red-600 text-2xl shadow-lg hover:bg-red-700"
                  >
                    ✕
                  </button>
                  <button
                    onClick={answer}
                    className="w-16 h-16 rounded-full bg-emerald-600 text-2xl shadow-lg hover:bg-emerald-700 animate-bounce"
                  >
                    ✓
                  </button>
                </>
              ) : (
                <button
                  onClick={hangUp}
                  className="w-16 h-16 rounded-full bg-red-600 text-2xl shadow-lg hover:bg-red-700"
                >
                  ✕
                </button>
              )}
            </div>
          </div>
          <p className="text-center text-slate-500 text-xs mt-4">BluetoothCaller — hands-free demo</p>
        </div>
      </div>
    );
  }

  return (
    <div className="min-h-screen bg-slate-950 text-white p-4">
      <div className="max-w-md mx-auto">
        {/* Header */}
        <div className="flex items-center justify-between py-4">
          <div>
            <h1 className="text-xl font-bold">BluetoothCaller</h1>
            <p className="text-xs text-slate-400">
              {paired ? `Connected: ${paired.name}` : "No device connected"}
            </p>
          </div>
          <div
            className={`w-3 h-3 rounded-full ${paired ? "bg-emerald-400 animate-pulse" : "bg-slate-600"}`}
          />
        </div>

        {/* Tabs */}
        <div className="flex gap-2 mb-4">
          {["devices", "dialer", "contacts"].map((s) => (
            <button
              key={s}
              onClick={() => setScreen(s)}
              className={`flex-1 py-2 rounded-xl text-sm capitalize transition ${
                screen === s
                  ? "bg-sky-600 text-white"
                  : "bg-slate-800 text-slate-300 hover:bg-slate-700"
              }`}
            >
              {s === "devices" ? "📡 Devices" : s === "dialer" ? "⌨️ Dialer" : "👥 Contacts"}
            </button>
          ))}
        </div>

        {/* Devices screen */}
        {screen === "devices" && (
          <div className="space-y-3">
            {paired && (
              <div className="rounded-2xl border border-emerald-700 bg-emerald-950/50 p-4 flex items-center justify-between">
                <div>
                  <p className="font-medium">{paired.name}</p>
                  <p className="text-xs text-emerald-400">Connected · {paired.type}</p>
                </div>
                <button
                  onClick={() => setPaired(null)}
                  className="text-xs bg-slate-800 px-3 py-1.5 rounded-lg hover:bg-slate-700"
                >
                  Disconnect
                </button>
              </div>
            )}

            <button
              onClick={() => setScanning(true)}
              disabled={scanning}
              className="w-full py-3 rounded-xl bg-sky-600 hover:bg-sky-700 disabled:bg-slate-700 font-medium"
            >
              {scanning ? "Scanning…" : "Scan for devices"}
            </button>

            {scanning && (
              <div className="h-2 bg-slate-800 rounded-full overflow-hidden">
                <div
                  className="h-full bg-sky-500 transition-all"
                  style={{ width: `${scanProgress}%` }}
                />
              </div>
            )}

            {(scanning ? found : DEVICES).map((d) => (
              <div
                key={d.id}
                className="rounded-2xl border border-slate-700 bg-slate-900 p-4 flex items-center justify-between"
              >
                <div>
                  <p className="font-medium">{d.name}</p>
                  <p className="text-xs text-slate-400">
                    {d.type}
                    {d.battery !== null && ` · 🔋 ${d.battery}%`}
                  </p>
                </div>
                <button
                  onClick={() => pair(d)}
                  className="text-xs bg-sky-600 px-3 py-1.5 rounded-lg hover:bg-sky-700"
                >
                  Pair
                </button>
              </div>
            ))}

            {paired && (
              <button
                onClick={simulateIncoming}
                className="w-full py-3 rounded-xl bg-amber-600 hover:bg-amber-700 font-medium"
              >
                📲 Simulate incoming call
              </button>
            )}
          </div>
        )}

        {/* Dialer screen */}
        {screen === "dialer" && (
          <div className="rounded-2xl border border-slate-700 bg-slate-900 p-6">
            {!paired && (
              <p className="text-amber-400 text-xs mb-4 text-center">
                ⚠️ No Bluetooth device paired — pair one in Devices first.
              </p>
            )}
            <div className="h-14 flex items-center justify-center text-3xl font-mono tracking-wider mb-2">
              {number || <span className="text-slate-600 text-xl">Enter number</span>}
            </div>
            <p className="text-center text-xs text-slate-500 h-4 mb-4">
              {number.length > 0 && "Press call to dial hands-free"}
            </p>
            <div className="grid grid-cols-3 gap-3">
              {["1", "2", "3", "4", "5", "6", "7", "8", "9", "*", "0", "#"].map((k) => (
                <button
                  key={k}
                  onClick={() => pressKey(k)}
                  className="h-14 rounded-full bg-slate-800 hover:bg-slate-700 text-xl font-medium"
                >
                  {k}
                </button>
              ))}
              <button
                onClick={() => setNumber("")}
                className="h-14 rounded-full bg-slate-800 hover:bg-slate-700 text-xs"
              >
                Clear
              </button>
              <button
                onClick={() =>
                  startCall({
                    name: number,
                    number,
                    initials: number.slice(0, 2) || "??",
                  })
                }
                disabled={number.length === 0}
                className="h-14 rounded-full bg-emerald-600 hover:bg-emerald-700 disabled:bg-slate-700 text-2xl"
              >
                📞
              </button>
              <button
                onClick={() => pressKey("del")}
                className="h-14 rounded-full bg-slate-800 hover:bg-slate-700 text-lg"
              >
                ⌫
              </button>
            </div>
          </div>
        )}

        {/* Contacts screen */}
        {screen === "contacts" && (
          <div className="space-y-3">
            {CONTACTS.map((c) => (
              <div
                key={c.id}
                className="rounded-2xl border border-slate-700 bg-slate-900 p-4 flex items-center gap-4"
              >
                <div className="w-11 h-11 rounded-full bg-gradient-to-br from-sky-500 to-indigo-600 flex items-center justify-center text-sm font-bold">
                  {c.initials}
                </div>
                <div className="flex-1">
                  <p className="font-medium">{c.name}</p>
                  <p className="text-xs text-slate-400">{c.number}</p>
                </div>
                <button
                  onClick={() => startCall(c)}
                  className="bg-emerald-600 w-10 h-10 rounded-full hover:bg-emerald-700"
                >
                  📞
                </button>
              </div>
            ))}
          </div>
        )}
      </div>
    </div>
  );
}
