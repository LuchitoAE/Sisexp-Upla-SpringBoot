import React, { createContext, useContext, useState, useCallback, useEffect } from 'react';
import { authApi } from '../api';

const AuthContext = createContext(null);

export function AuthProvider({ children }) {
  const [user, setUser] = useState(null);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    authApi.me()
      .then(data => setUser(data))
      .catch(() => setUser(null))
      .finally(() => setLoading(false));
  }, []);

  const login = useCallback(async (email, password) => {
    const data = await authApi.login(email, password);
    setUser(data.usuario);
    return data;
  }, []);

  const logout = useCallback(async () => {
    try { await authApi.logout(); } catch { /* ignore */ }
    setUser(null);
  }, []);

  return (
    <AuthContext.Provider value={{ user, login, logout, loading, isAuth: !!user }}>
      {children}
    </AuthContext.Provider>
  );
}

export function useAuth() {
  const ctx = useContext(AuthContext);
  if (!ctx) throw new Error('useAuth must be used within AuthProvider');
  return ctx;
}
