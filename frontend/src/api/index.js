import { client } from './client';

export const authApi = {
  login: (email, password) => client.post('/auth/login', { email, password }),
  me: () => client.get('/auth/me')
};

export const usuarioApi = {
  list: () => client.get('/usuarios'),
  create: (data) => client.post('/usuarios', data),
  update: (id, data) => client.put(`/usuarios/${id}`, data),
  toggleActivo: (id) => client.patch(`/usuarios/${id}/toggle-activo`, {})
};
