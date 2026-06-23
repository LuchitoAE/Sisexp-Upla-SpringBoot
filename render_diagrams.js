const fs = require('fs');
const path = require('path');
const { execSync } = require('child_process');

const MARKDOWN_FILE = path.resolve(__dirname, 'docs', 'SISEXP_DIAGRAMAS_COMPLETO.md');
const IMAGES_DIR = path.resolve(__dirname, 'docs', 'diagramas', 'images');
const OUTPUT_MD = path.resolve(__dirname, 'docs', 'diagramas', 'SISEXP_DIAGRAMAS_CON_IMAGENES.md');

fs.mkdirSync(IMAGES_DIR, { recursive: true });

const content = fs.readFileSync(MARKDOWN_FILE, 'utf-8').replace(/\r\n/g, '\n').replace(/\r/g, '');
const lines = content.split('\n');

// Find all mermaid blocks with line numbers
const blocks = [];
let inMermaid = false;
let startLine = 0;
let blockLines = [];
let endLine = 0;

lines.forEach((line, idx) => {
    if (line.trim() === '```mermaid') {
        inMermaid = true;
        startLine = idx;
        blockLines = [];
    } else if (inMermaid && line.trim() === '```') {
        inMermaid = false;
        endLine = idx;
        blocks.push({
            startLine,
            endLine,
            code: blockLines.join('\n'),
            lines: blockLines
        });
    } else if (inMermaid) {
        blockLines.push(line);
    }
});

console.log(`Found ${blocks.length} mermaid blocks.`);

// Get heading context for naming: look backwards from each block to find the nearest ## or ### heading
function findHeading(lines, blockStartLine) {
    for (let i = blockStartLine - 1; i >= 0; i--) {
        const trimmed = lines[i].trim();
        if (trimmed.startsWith('## ') || trimmed.startsWith('### ')) {
            return trimmed.replace(/^#+\s*/, '').trim();
        }
    }
    return 'Diagram';
}

// Build a map: for each block, find the heading
const blockNames = [];
let ersActoresDone = false;
let ersClasesDone = false;
let bceCount = 0;
let ssdCount = 0;

blocks.forEach((block, idx) => {
    const heading = findHeading(lines, block.startLine);
    
    // Try to categorize
    if (heading.includes('Diagrama de Actores')) {
        blockNames.push({ name: 'ERS - Diagrama de Actores', file: 'ERS-01-Actores' });
        ersActoresDone = true;
    } else if (heading.includes('Diagrama de Clases') && !ersClasesDone && idx < 5) {
        blockNames.push({ name: 'ERS - Diagrama de Clases (Modelo de Dominio)', file: 'ERS-02-Diagrama_de_Clases' });
        ersClasesDone = true;
    } else if (heading.includes('BCE')) {
        // Extract BCE number from heading
        const match = heading.match(/BCE(\d+)/);
        const num = match ? match[1] : String(++bceCount).padStart(2, '0');
        const cleanHeading = heading.replace(/Diagrama Mermaid\s*/i, '').trim();
        blockNames.push({ name: `BCE${num} - ${cleanHeading}`, file: `BCE${num}` });
    } else if (heading.includes('SSD')) {
        const match = heading.match(/SSD-CU(\d+)/);
        const num = match ? match[1].padStart(2, '0') : String(++ssdCount).padStart(2, '0');
        const cleanHeading = heading.replace(/Mermaid\s*/i, '').trim();
        blockNames.push({ name: `SSD-CU${num} - ${cleanHeading}`, file: `SSD-CU${num}` });
    } else {
        blockNames.push({ name: heading || `Diagram-${idx + 1}`, file: `Diagram-${String(idx + 1).padStart(2, '0')}` });
    }
});

// Override specific names based on block index (more reliable)
const diagramNames = [
    { name: 'ERS - Diagrama de Actores', file: 'ERS-01-Actores' },
    { name: 'ERS - Diagrama de Clases (Modelo de Dominio)', file: 'ERS-02-Diagrama_de_Clases' },
    { name: 'BCE01 - Iniciar Sesión', file: 'BCE01' },
    { name: 'BCE02 - Ver Dashboard', file: 'BCE02' },
    { name: 'BCE03 - Crear Expediente', file: 'BCE03' },
    { name: 'BCE04 - Cambiar Estado Expediente', file: 'BCE04' },
    { name: 'BCE05 - Adjuntar Documento', file: 'BCE05' },
    { name: 'BCE06 - Gestionar Techo Presupuestal', file: 'BCE06' },
    { name: 'BCE07 - Gestionar Actividad POI', file: 'BCE07' },
    { name: 'BCE08 - Gestionar Necesidad PAP', file: 'BCE08' },
    { name: 'BCE09 - Gestionar Nota Modificatoria', file: 'BCE09' },
    { name: 'BCE10 - Ver Reportes', file: 'BCE10' },
    { name: 'BCE11 - Gestionar Usuarios', file: 'BCE11' },
    { name: 'BCE12 - Gestionar Notificaciones', file: 'BCE12' },
    { name: 'BCE13 - Rastrear Expediente (Público)', file: 'BCE13' },
    { name: 'BCE14 - Cerrar Sesión', file: 'BCE14' },
    { name: 'SSD-CU01 - Iniciar Sesión', file: 'SSD-CU01' },
    { name: 'SSD-CU02 - Ver Dashboard', file: 'SSD-CU02' },
    { name: 'SSD-CU03 - Crear Expediente', file: 'SSD-CU03' },
    { name: 'SSD-CU04 - Cambiar Estado Expediente', file: 'SSD-CU04' },
    { name: 'SSD-CU05 - Adjuntar Documento', file: 'SSD-CU05' },
    { name: 'SSD-CU06 - Gestionar Techo Presupuestal', file: 'SSD-CU06' },
    { name: 'SSD-CU07 - Gestionar Actividad POI', file: 'SSD-CU07' },
    { name: 'SSD-CU08 - Gestionar Necesidad PAP', file: 'SSD-CU08' },
    { name: 'SSD-CU09 - Gestionar Nota Modificatoria', file: 'SSD-CU09' },
    { name: 'SSD-CU10 - Ver Reportes', file: 'SSD-CU10' },
    { name: 'SSD-CU11 - Gestionar Usuarios', file: 'SSD-CU11' },
    { name: 'SSD-CU12 - Gestionar Notificaciones', file: 'SSD-CU12' },
    { name: 'SSD-CU13 - Rastrear Expediente (Público)', file: 'SSD-CU13' },
    { name: 'SSD-CU14 - Cerrar Sesión', file: 'SSD-CU14' }
];

const errors = [];
let successCount = 0;

const puppeteerConfigPath = path.resolve(__dirname, 'puppeteer-config.json');
const puppeteerConfig = {
    executablePath: 'C:\\Program Files\\Google\\Chrome\\Application\\chrome.exe',
    args: ['--no-sandbox', '--disable-setuid-sandbox']
};
fs.writeFileSync(puppeteerConfigPath, JSON.stringify(puppeteerConfig, null, 2));

blocks.forEach((block, idx) => {
    const info = diagramNames[idx];
    if (!info) {
        errors.push(`No name mapping for block ${idx}`);
        return;
    }
    
    const tmpMmd = path.resolve(__dirname, `temp_${String(idx).padStart(2, '0')}.mmd`);
    const outputPng = path.resolve(IMAGES_DIR, `${info.file}.png`);
    
    // Fix :Sistema syntax (colon prefix not valid in mermaid 11.x)
    let code = block.code.replace(/:Sistema/g, 'Sistema');
    
    try {
        fs.writeFileSync(tmpMmd, code, 'utf-8');
        
        execSync(
            `npx mmdc -i "${tmpMmd}" -o "${outputPng}" -w 1200 -H 900 -b white --backgroundColor white -p "${puppeteerConfigPath}"`,
            { cwd: __dirname, stdio: 'pipe', timeout: 60000 }
        );
        
        successCount++;
        console.log(`[${idx + 1}/${blocks.length}] ✓ ${info.name} → ${info.file}.png`);
    } catch (err) {
        errors.push(`[${idx + 1}] ${info.name}: ${err.message}`);
        console.error(`[${idx + 1}/${blocks.length}] ✗ ${info.name}: ${err.message}`);
    } finally {
        if (fs.existsSync(tmpMmd)) {
            fs.unlinkSync(tmpMmd);
        }
    }
});

// Clean up puppeteer config
if (fs.existsSync(puppeteerConfigPath)) {
    fs.unlinkSync(puppeteerConfigPath);
}

console.log(`\n=== Summary ===`);
console.log(`Total diagrams processed: ${blocks.length}`);
console.log(`Successfully rendered: ${successCount}`);
console.log(`Errors: ${errors.length}`);

if (errors.length > 0) {
    console.log(`\nErrors:`);
    errors.forEach(e => console.log(`  ${e}`));
}

// Generate the output markdown with embedded images
let mdOutput = `# SISEXP-UPLA — Diagramas Completos con Imágenes

Este documento contiene todos los diagramas renderizados como imágenes PNG.

---

## ERS — Especificación de Requisitos de Software

`;

// ERS diagrams
for (let i = 0; i < 2; i++) {
    const info = diagramNames[i];
    const imgRelPath = `images/${info.file}.png`;
    const imgAbsPath = path.resolve(IMAGES_DIR, `${info.file}.png`);
    if (fs.existsSync(imgAbsPath)) {
        mdOutput += `### ${info.name}\n\n![${info.name}](${imgRelPath})\n\n`;
    } else {
        mdOutput += `### ${info.name}\n\n*[Imagen no generada — ${info.file}.png]*\n\n`;
    }
}

mdOutput += `---

## BCE — Diagramas de Robustez (Boundary-Control-Entity)

`;

for (let i = 2; i < 16; i++) {
    const info = diagramNames[i];
    const imgRelPath = `images/${info.file}.png`;
    const imgAbsPath = path.resolve(IMAGES_DIR, `${info.file}.png`);
    if (fs.existsSync(imgAbsPath)) {
        mdOutput += `### ${info.name}\n\n![${info.name}](${imgRelPath})\n\n`;
    } else {
        mdOutput += `### ${info.name}\n\n*[Imagen no generada — ${info.file}.png]*\n\n`;
    }
}

mdOutput += `---

## SSD — Diagramas de Secuencia del Sistema

`;

for (let i = 16; i < 30; i++) {
    const info = diagramNames[i];
    const imgRelPath = `images/${info.file}.png`;
    const imgAbsPath = path.resolve(IMAGES_DIR, `${info.file}.png`);
    if (fs.existsSync(imgAbsPath)) {
        mdOutput += `### ${info.name}\n\n![${info.name}](${imgRelPath})\n\n`;
    } else {
        mdOutput += `### ${info.name}\n\n*[Imagen no generada — ${info.file}.png]*\n\n`;
    }
}

fs.writeFileSync(OUTPUT_MD, mdOutput, 'utf-8');
console.log(`\nMarkdown with embedded images written to: ${OUTPUT_MD}`);
console.log(`Images directory: ${IMAGES_DIR}`);
