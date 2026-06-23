# SISEXP-UPLA

Sistema de Seguimiento y Control de Expedientes — Universidad Peruana Los Andes

Spring Boot 3.4.1 + React 19 SPA + PostgreSQL

## Stack

| Capa | Tecnología |
|---|---|
| Backend | Spring Boot 3.4.1, Java 17, Spring Security, JPA |
| Frontend | React 19, React Router 6, CSS nativo |
| BD | PostgreSQL (prod Railway), H2 (dev local) |
| Deploy | Railway + Docker multi-stage |

## Estructura

```
sisexp/
├── src/main/java/com/upla/sisexp/
│   ├── api/           # REST controllers (/api/**)
│   ├── config/        # Security, CORS, DataInitializer
│   ├── security/      # JWT, CustomUserDetails, HorarioLaboralFilter
│   ├── model/         # 11 entidades JPA
│   ├── enums/         # Estados, roles, tipos
│   ├── repository/    # Spring Data JPA
│   ├── dto/           # DTOs
│   └── service/       # Lógica de negocio
├── frontend/          # React SPA (embebido en JAR)
└── Dockerfile         # 3-stage: Node → Maven → JRE alpine
```

## URLs

| Entorno | URL |
|---|---|
| Producción | https://sisexp-web-production.up.railway.app |
| Login | https://sisexp-web-production.up.railway.app/login |

## Credenciales seed

| Usuario | Contraseña | Rol |
|---|---|---|
| jefe@upla.edu.pe | jefe123 | Administrador |
| coord@upla.edu.pe | coord123 | Coordinacion |
| secretaria@upla.edu.pe | secretaria123 | Secretaria |
| director@upla.edu.pe | director123 | Director |
| lab@upla.edu.pe | lab123 | Laboratorio |
| decanato@upla.edu.pe | decanato123 | Decanato |

## Guía para colaboradores

### Requisitos

- Git instalado ([git-scm.com](https://git-scm.com))
- Cuenta en GitHub
- Java 17, Node 18+ y pnpm (solo si se trabaja local)

### 1. Clonar el repositorio

```bash
git clone https://github.com/LuchitoAE/Sisexp-Upla-SpringBoot.git
cd Sisexp-Upla-SpringBoot
```

### 2. Hacer cambios

Editar los archivos necesarios. El código está en `sisexp/`:
- Backend: `sisexp/src/main/java/com/upla/sisexp/`
- Frontend: `sisexp/frontend/src/`

### 3. Enviar cambios a GitHub

```bash
# Ver qué archivos se modificaron
git status

# Agregar archivos al stage
git add sisexp/src/.../Archivo.java

# Guardar en historial local con mensaje descriptivo
git commit -m "fix: descripcion corta de lo que se arreglo"

# Subir a GitHub
git push origin master
```

Al hacer `push`, Railway detecta el cambio y redeploya automáticamente.

### ¿Y si hay conflictos?

Si otra persona subió cambios mientras trabajabas:

```bash
# Bajar cambios remotos
git pull origin master

# Si hay conflicto, Git lo marca en el archivo con <<<<<<< ======= >>>>>>>
# Editar el archivo para resolverlo, luego:
git add archivo-con-conflicto.java
git commit -m "fix: resolver conflicto"
git push origin master
```

### Convención de mensajes

| Prefijo | Uso |
|---|---|
| `fix:` | Corrección de bug |
| `feat:` | Nueva funcionalidad |
| `docs:` | Documentación |

### Pull Request

Si prefieres que alguien revise antes de hacer merge:

1. Crear rama: `git checkout -b mi-cambio`
2. Push: `git push origin mi-cambio`
3. Ir a https://github.com/LuchitoAE/Sisexp-Upla-SpringBoot/pulls
4. **New pull request** → `mi-cambio` → `master`
5. Escribir descripción de los cambios
6. El dueño revisa, aprueba y hace merge

## Variables de entorno

| Variable | Descripción |
|---|---|
| `SPRING_DATASOURCE_URL` | JDBC URL PostgreSQL |
| `SPRING_DATASOURCE_USERNAME` | Usuario BD |
| `SPRING_DATASOURCE_PASSWORD` | Password BD |
| `JWT_SECRET` | Llave secreta JWT |
