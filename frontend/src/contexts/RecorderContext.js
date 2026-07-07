import React, { createContext, useContext, useState, useCallback, useRef } from 'react';

const STORAGE_KEY = 'sisexp_recordings';

function loadRecordings() {
  try {
    const raw = localStorage.getItem(STORAGE_KEY);
    return raw ? JSON.parse(raw) : [];
  } catch { return []; }
}

function saveRecordings(recs) {
  localStorage.setItem(STORAGE_KEY, JSON.stringify(recs.slice(0, 20)));
}

const RecorderContext = createContext(null);

export function RecorderProvider({ children }) {
  const [isRecording, setIsRecording] = useState(false);
  const [elapsed, setElapsed] = useState(0);
  const [recordings, setRecordings] = useState(loadRecordings);
  const bufferRef = useRef([]);
  const timerRef = useRef(null);
  const startTimeRef = useRef(null);

  const startRecording = useCallback(() => {
    bufferRef.current = [];
    startTimeRef.current = Date.now();
    setIsRecording(true);
    setElapsed(0);
    window.__SISEXP_RECORDING__ = true;
    window.__SISEXP_RECORDING_BUFFER__ = bufferRef.current;
    timerRef.current = setInterval(() => {
      setElapsed(Math.floor((Date.now() - startTimeRef.current) / 1000));
    }, 1000);
  }, []);

  const stopRecording = useCallback(() => {
    if (timerRef.current) { clearInterval(timerRef.current); timerRef.current = null; }
    window.__SISEXP_RECORDING__ = false;
    window.__SISEXP_RECORDING_BUFFER__ = null;
    setIsRecording(false);
    setElapsed(0);
    const actions = [...bufferRef.current];
    if (actions.length === 0) return;
    const now = new Date();
    const rec = {
      id: 'rec_' + Date.now(),
      name: 'Grabacion ' + now.toLocaleDateString('es-PE') + ' ' + now.toLocaleTimeString('es-PE', { hour: '2-digit', minute: '2-digit' }),
      date: now.toISOString(),
      totalActions: actions.length,
      duration: Math.floor((Date.now() - startTimeRef.current) / 1000),
      actions
    };
    const updated = [rec, ...loadRecordings()].slice(0, 20);
    saveRecordings(updated);
    setRecordings(updated);
    bufferRef.current = [];
  }, []);

  const deleteRecording = useCallback((id) => {
    const updated = loadRecordings().filter(r => r.id !== id);
    saveRecordings(updated);
    setRecordings(updated);
  }, []);

  return (
    <RecorderContext.Provider value={{
      isRecording, elapsed, recordings,
      startRecording, stopRecording, deleteRecording
    }}>
      {children}
    </RecorderContext.Provider>
  );
}

export function useRecorder() {
  return useContext(RecorderContext);
}

export function getRecordings() {
  return loadRecordings();
}
