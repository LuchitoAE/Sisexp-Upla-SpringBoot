# SISEXP-UPLA v2.0 — Guía de despliegue

## Despliegue de tres componentes

- **Base de datos**: PostgreSQL en Supabase (producción) / SQLite (desarrollo local)
- **Backend API**: Railway → servicio `SISEXP-UPLA-api`
- **Frontend**: Vercel → proyecto `SISEXP-UPLA`

---

## 1. Supabase — BD en producción

### Datos del proyecto
- URL: `https://vwsemldiehnjhspsdroi.supabase.co`
- Proyecto: `bdSISEXP-UPLA` (ref: `vwsemldiehnjhspsdroi`)
- Región: South America (São Paulo) — sa-east-1
- **IMPORTANTE**: La BD solo tiene IPv6. Usar siempre el **Session Pooler** (IPv4):
  ```
  postgresql://postgres.vwsemldiehnjhspsdroi:[PASSWORD]@aws-1-sa-east-1.pooler.supabase.com:5432/postgres
  ```

---

## 2. Railway — Backend API

### Login
```bash
railway login
```

### Linkear proyecto
```bash
cd backend-as
railway link
```

### Variables de entorno
```bash
railway variables --set "DATABASE_URL=postgresql://postgres.vwsemldiehnjhspsdroi:[PASSWORD]@aws-1-sa-east-1.pooler.supabase.com:5432/postgres"
railway variables --set "JWT_SECRET=super-secret-key-sisexp-2026"
railway variables --set "PORT=3000"
railway variables --set "SUPABASE_URL=https://vwsemldiehnjhspsdroi.supabase.co"
railway variables --set "SUPABASE_SERVICE_KEY=eyJ..."
railway variables --set "SUPABASE_BUCKET=documentos"
railway variables --set "CORS_ORIGINS=https://sisexp-upla.vercel.app,http://localhost:3001"
```

**Nota**: Cuando `DATABASE_URL` está presente, el sistema automáticamente usa PostgreSQL (Supabase). Cuando no está, usa SQLite local.

### Desplegar
```bash
railway up
```

Verificar: `curl https://backend-as-production.up.railway.app/api/health`

---

## 3. Vercel — Frontend

### Login
```bash
vercel login
```

### Linkear y desplegar
```bash
cd frontend-as
vercel link

vercel env add REACT_APP_API_URL production
# valor: https://backend-as-production.up.railway.app/api

vercel --prod
```

---

## 4. Docker — Desarrollo local

```bash
docker compose up --build -d     # levantar
docker compose down              # apagar
docker compose down -v           # destruir datos
```

- Backend: `http://localhost:3000`
- Frontend: `http://localhost:80`
- BD: SQLite (archivo `data/sisexp.sqlite`, persistido en volumen)

El seed crea 6 usuarios, 2 techos presupuestales, 20 actividades POI, 13 necesidades PAP y 5 expedientes de ejemplo.

---

## 5. Flujo completo

```
1. docker compose up --build -d     ← probar localmente
2. Verificar backend + frontend
3. docker compose down              ← apagar
4. git add -A; git commit; git push ← subir a GitHub
5. cd backend-as; railway up        ← Railway
6. cd frontend-as; vercel --prod    ← Vercel
```

**Regla**: Si falla en Docker, no se despliega. Corregir primero localmente.

---

## Credenciales de prueba (seed v2.0)

| Rol | Email | Password |
|---|---|---|
| Administrador | jefe@upla.edu.pe | jefe123 |
| Coordinación | coord@upla.edu.pe | coord123 |
| Secretaria | secretaria@upla.edu.pe | secretaria123 |
| Director | director@upla.edu.pe | director123 |
| Laboratorio | lab@upla.edu.pe | lab123 |
| Decanato | decanato@upla.edu.pe | decanato123 |
