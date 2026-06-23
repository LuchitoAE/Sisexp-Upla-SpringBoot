const fs = require('fs');
const content = fs.readFileSync('E:\\proyecto\\UPLA - Clases\\octavo ciclo\\arquitectura de software\\semana 10\\Proyecto-spring boot\\docs\\SISEXP_DIAGRAMAS_COMPLETO.md', 'utf-8').replace(/\r\n/g, '\n').replace(/\r/g, '');
const lines = content.split('\n');

// Find all mermaid blocks
const blocks = [];
let inMermaid = false;
let blockLines = [];

lines.forEach((line, idx) => {
    const trimmed = line.trim();
    if (trimmed === '```mermaid') {
        inMermaid = true;
        blockLines = [];
    } else if (inMermaid && trimmed === '```') {
        inMermaid = false;
        blocks.push(blockLines.join('\n'));
    } else if (inMermaid) {
        blockLines.push(line);
    }
});

// Write block 16 (SSD-CU01) to temp file
const fname = 'debug2_block_16.mmd';
const code = blocks[16];
fs.writeFileSync(fname, code, 'utf-8');
console.log('Block 16 content:');
console.log(code);
console.log('\nHex dump of first 200 chars:');
const buf = Buffer.from(code, 'utf-8');
for (let i = 0; i < Math.min(code.length, 200); i++) {
    process.stdout.write(buf[i].toString(16).padStart(2, '0') + ' ');
    if ((i + 1) % 16 === 0) process.stdout.write('\n');
}
console.log('\n');
