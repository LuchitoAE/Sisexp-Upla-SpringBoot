---
name: docs-sisexp
description: Use when generating project documentation, structure docs, entity diagrams, flow charts, README updates, or any technical documentation for SISEXP-UPLA. Produces formatted .md files ready for pandoc + reference.docx → professional .docx with styled headings, bordered tables, and A4 layout.
---

# Skill: Docs SISEXP-UPLA — Documentacion Tecnica Profesional

---

## 1. WORKFLOW COMPLETO (MD → DOCX profesional)

```
1. Escribir documento .md con el formato correcto (secciones abajo)
2. Generar/reusar reference.docx con estilos institucionales
3. pandoc --reference-doc=reference.docx ... -o out.docx
4. (Opcional) Abrir en Word y ajustar salto de pagina inicial
```

### Comando de conversion
```bash
pandoc docs/DOCUMENTO.md -o docs/DOCUMENTO.docx \
  --reference-doc="docs/reference.docx" \
  --from=markdown --to=docx \
  --toc --toc-depth=3 \
  -V lang=es
```

---

## 2. ESTILOS DEL DOCUMENTO (controlados por reference.docx)

El template `reference.docx` define:

| Estilo Word | Aplicado a | Formato |
|:------------|:-----------|:--------|
| **Title** | metadata `title` o `#` centrado | Calibri 22pt, bold, azul #1F4E79, centrado |
| **Heading 1** | `# Titulo seccion` | Calibri 16pt, bold, azul #1F4E79 |
| **Heading 2** | `## Subtitulo` | Calibri 14pt, bold, azul #1F4E79 |
| **Heading 3** | `### Sub-seccion` | Calibri 12pt, bold, gris #333333 |
| **Heading 4** | `#### Detalle` | Calibri 11pt, bold, gris #666666 |
| **Table** | Tablas markdown | Bordes azules, header azul + texto blanco |
| **Compact** | Celdas de tabla | Calibri 10pt, padding reducido |
| **Table Header** | `|:----|` encabezados | Calibri 10pt, bold, blanco sobre azul |
| **Normal** | Parrafos | Calibri 11pt, interlineado 1.15 |
| **Verbatim Char** | Codigo inline | Consolas 9pt |
| **Hyperlink** | Links | Azul #2E75B6, subrayado |

### Regenerar el template
```bash
python .opencode/skills/docs-sisexp/generate_template.py
# Output: .opencode/skills/docs-sisexp/reference.docx
# Tambien copiado a docs/reference.docx y %APPDATA%/pandoc/reference.docx
```

---

## 3. FORMATO DEL MARKDOWN

### 3.1 Encabezado YAML (metadata)
```yaml
---
title: "SISEXP-UPLA — Titulo del Documento"
subtitle: "Universidad Peruana Los Andes"
author: "Arquitectura de Software — VIII Ciclo — 2026"
lang: es
---
```

### 3.2 Headings — Reglas estrictas

- **`#` (H1)**: SOLO el titulo principal del documento. NUNCA repetir H1 en secciones.
  ```markdown
  # **SISEXP-UPLA — Arquitectura de Microservicios**
  ```
- **`##` (H2)**: Titulos de secciones principales. Usar numeracion.
  ```markdown
  ## **1. Introduccion**
  ```
- **`###` (H3)**: Sub-secciones dentro de un H2.
  ```markdown
  ### 1.1 Contexto del Proyecto
  ```
- **`####` (H4)**: Detalles o tercer nivel. Usar con moderacion.
  ```markdown
  #### Opcion A: Con Railway
  ```

**SIEMPRE** usar `**bold**` dentro de los headings para que Word los muestre con negrita.

### 3.3 Tablas — Profesionales con bordes

Formato obligatorio para tablas en SISEXP:

```markdown
| **Columna A** | **Columna B** | **Columna C** |
|:--------------|:-------------:|--------------:|
| izquierda     | centro        | derecha       |
```

Reglas:
1. **TODAS las celdas del header en bold** `**texto**` (para que Word las marque como header)
2. Separador con alineacion: `|:----|:----:|----:|`
3. Alineacion izquierda: `|:------|`, centro: `|:-----:|`, derecha: `|------:|`
4. NO usar pipes internos dentro de celdas (rompe el parseo)
5. Mantener ancho de columna consistente (misma cantidad de guiones)
6. Usar `<br>` para saltos de linea en celdas largas

**Tablas sin bordes / planas = RECHAZADAS.** Siempre deben tener todas las columnas delimitadas.

### 3.4 Bloques de codigo

```markdown
    ```bash
    docker compose up -d
    ```
```

Usar triple backtick CON especificador de lenguaje (`bash`, `java`, `json`, `yaml`, `xml`, `properties`).

### 3.5 Listas

```markdown
- Item nivel 1
  - Sub-item (2 espacios de indentacion)
    - Sub-sub-item (4 espacios)
```

Numeradas:
```markdown
1. Primer paso
2. Segundo paso
3. Tercer paso
```

### 3.6 Notas y warnings (usando blockquote)

```markdown
> **NOTA**: Informacion complementaria importante.

> **IMPORTANTE**: Algo que el lector no debe pasar por alto.

> **PRECAUCION**: Riesgo o problema potencial.
```

### 3.7 Imagenes y diagramas

```markdown
![Descripcion alternativa](docs/diagramas/img/nombre.png)
```

NO usar referencias relativas tipo `../../img/`. Usar path desde raiz del proyecto.

---

## 4. ESTRUCTURA DE DOCUMENTO ESTANDAR

Todo documento SISEXP debe seguir este orden:

```markdown
---
title: "SISEXP-UPLA — Titulo"
subtitle: "Universidad Peruana Los Andes"
author: "Arquitectura de Software — 2026"
---

(Cuando se usa metadata YAML, NO usar H1 manual. Pandoc lo genera del title.)

# **1. Introduccion**

## **1.1 Proposito**

## **1.2 Alcance**

# **2. Stack Tecnologico**

| **Tecnologia** | **Version** | **Proposito** |
|:---------------|:-----------:|:--------------|
| ... | ... | ... |

# **3. Arquitectura / Diseno**

(Usar H2 y H3 con numeracion)

# **4. Implementacion**

# **5. API Reference**

| **Metodo** | **Endpoint** | **Auth** | **Descripcion** |
|:-----------|:------------|:--------:|:----------------|
| ... | ... | ... | ... |

# **6. Despliegue**

# **7. Verificacion**
```

---

## 5. EXPORT A DOCX (checklist)

- [ ] `--reference-doc` apunta a un template valido
- [ ] `--toc --toc-depth=3` genera tabla de contenido
- [ ] Metadata YAML incluye `title`, `subtitle`, `lang: es`
- [ ] 0 warnings de pandoc (o minimos, revisados)
- [ ] Tablas tienen TODAS las celdas con pipes de apertura y cierre
- [ ] Headers de tabla en bold `**...**`
- [ ] NO hay H1 repetidos (solo titulo principal)

### Comando final
```bash
pandoc docs/DOCUMENTO.md -o docs/DOCUMENTO.docx \
  --reference-doc="docs/reference.docx" \
  --from=markdown --to=docx \
  --toc --toc-depth=3 \
  -V lang=es
```

---

## 6. TEMPLATE: reference.docx

Archivo: `.opencode/skills/docs-sisexp/reference.docx`

Se genera con:
```bash
python .opencode/skills/docs-sisexp/generate_template.py
```

Este script crea un DOCX con todos los estilos institucionales (Headings 1-4, Table, Normal, Compact, etc.) usando python-docx. El archivo resultante se copia automaticamente a:
- `.opencode/skills/docs-sisexp/reference.docx` (fuente)
- `docs/reference.docx` (proyecto)
- `%APPDATA%/pandoc/reference.docx` (global, para todos los docs)

---

## 7. REFERENCIA RAPIDA

| Elemento | Markdown | DOCX resultante |
|:---------|:---------|:----------------|
| Titulo doc | metadata `title` | Title style (22pt, bold, azul) |
| H1 | `# **Seccion**` | Heading 1 (16pt, bold, azul) |
| H2 | `## **Seccion**` | Heading 2 (14pt, bold, azul) |
| H3 | `### **Sub**` | Heading 3 (12pt, bold, gris) |
| H4 | `#### **Detalle**` | Heading 4 (11pt, bold, gris) |
| Tabla | `| **H1** | **H2** |` | Table (bordes azules, header azul+blanco) |
| Codigo | ` ```bash ` | Consolas 9pt |
| Link | `[texto](url)` | Hyperlink azul subrayado |
| Parrafo | texto normal | Normal (Calibri 11pt) |
| Negrita | `**texto**` | Bold |
| Italica | `*texto*` | Italic |
