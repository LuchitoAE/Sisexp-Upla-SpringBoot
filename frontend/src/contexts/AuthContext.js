import React, { createContext, useContext, useState, useCallback, useEffect } from 'react';
import { authApi } from '../api';

const AuthContext = createContext(null);

export function AuthProvider({ children }) {
  const [user, setUser] = useState(null);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    const token = localStorage.getItem('jwt_token');
    if (token) {
      authApi.me()
        .then(data => setUser(data))
        .catch(() => { localStorage.removeItem('jwt_token'); setUser(null); })
        .finally(() => setLoading(false));
    } else {
      setLoading(false);
    }
  }, []);

  const login = useCallback(async (email, password) => {
    const data = await authApi.login(email, password);
    if (data.token) localStorage.setItem('jwt_token', data.token);
    setUser(data.usuario);
    return data;
  }, []);

  const logout = useCallback(async () => {
    localStorage.removeItem('jwt_token');
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
