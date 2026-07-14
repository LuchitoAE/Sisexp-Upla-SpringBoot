import { readFileSync, writeFileSync, mkdirSync, existsSync } from 'fs';
import { resolve, dirname } from 'path';
import { fileURLToPath } from 'url';
import { deflateSync } from 'zlib';

const __dirname = dirname(fileURLToPath(import.meta.url));
const PROJECT = resolve(__dirname, '..');
const DIAGRAMS_FILE = resolve(PROJECT, 'frontend/src/pages/monitor/diagrams.js');
const OUT_DIR = resolve(PROJECT, 'docs/diagramas');

if (!existsSync(OUT_DIR)) mkdirSync(OUT_DIR, { recursive: true });

const src = readFileSync(DIAGRAMS_FILE, 'utf-8');

const mermaidPattern = /mermaid:\s*`([^`]+)`/gs;
const idPattern = /id:\s*'([^']+)'/g;
const titlePattern = /title:\s*'([^']+)'/g;

const allIds = [...src.matchAll(idPattern)].map(m => m[1]);
const allTitles = [...src.matchAll(titlePattern)].map(m => m[1]);
const allMermaid = [...src.matchAll(mermaidPattern)].map(m => m[1]);

console.log(`Found ${allMermaid.length} diagrams`);

for (let i = 0; i < allMermaid.length; i++) {
  const id = allIds[i] || `diagram-${i}`;
  const title = allTitles[i] || `Diagrama ${i + 1}`;
  const code = allMermaid[i].trim();
  const safeName = id.replace(/[^a-z0-9-]/g, '-');

  console.log(`[${i + 1}/${allMermaid.length}] Rendering: ${title}`);

  try {
    const state = JSON.stringify({
      code,
      mermaid: { theme: 'dark' },
      updateEditor: false,
      autoSync: true,
    });
    const deflated = deflateSync(state);
    const b64url = deflated.toString('base64url');
    const url = `https://mermaid.ink/img/pako:${b64url}`;

    const resp = await fetch(url);
    if (!resp.ok) {
      console.log(`  FAIL: HTTP ${resp.status}`);
      continue;
    }

    const buf = Buffer.from(await resp.arrayBuffer());
    const pngPath = resolve(OUT_DIR, `${safeName}.png`);
    writeFileSync(pngPath, buf);
    console.log(`  OK -> ${safeName}.png (${buf.length} bytes)`);
  } catch (e) {
    console.log(`  ERROR: ${e.message}`);
  }

  await new Promise(r => setTimeout(r, 300));
}

console.log(`\nDone. Images saved to ${OUT_DIR}`);
