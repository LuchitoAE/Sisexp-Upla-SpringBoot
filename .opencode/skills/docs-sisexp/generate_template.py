#!/usr/bin/env python3
"""
Genera un reference.docx profesional usando el default de pandoc como base
y parcheando sus estilos con python-docx.

Uso: python generate_template.py
Output: reference.docx (mismo directorio)
"""

import os
import io
import zipfile
import tempfile
import shutil
from docx import Document
from docx.shared import Pt, Cm, Inches, RGBColor, Emu
from docx.enum.text import WD_ALIGN_PARAGRAPH
from docx.enum.style import WD_STYLE_TYPE
from docx.oxml.ns import qn, nsdecls
from docx.oxml import parse_xml

AZUL_OSCURO  = "1F4E79"
AZUL_MEDIO   = "2E75B6"
GRIS_OSCURO  = "333333"
GRIS_MEDIO   = "666666"


def patch_styles_xml(zip_bytes):
    """Parchea el styles.xml dentro del docx (zip) con estilos profesionales."""
    with zipfile.ZipFile(io.BytesIO(zip_bytes), "r") as zin:
        all_files = zin.namelist()
        styles_xml = zin.read("word/styles.xml").decode("utf-8")

    # ── Reemplazar colores de headings en el XML directamente ──
    replacements = {
        # Heading1: bold + azul oscuro + 16pt
        'w:style w:type="paragraph" w:styleId="Heading1"': 'w:style w:type="paragraph" w:styleId="Heading1"',
        # Heading2: bold + azul oscuro + 14pt
        # Heading3: bold + gris oscuro + 12pt
        # Heading4: bold + gris medio + 11pt
    }

    # Enfoque: vamos a inyectar propiedades de estilo completas
    # para Heading 1-4, Table y Normal usando XPath / XML

    # Esto es complejo. Mejor enfoque: usar python-docx para modificar
    # el documento y guardarlo.

    # Estrategia B: Generar un docx desde cero con python-docx, con todos
    # los estilos necesarios, como base para pandoc
    return styles_xml


def create_from_scratch():
    """Crea un reference.docx desde cero con todos los estilos que pandoc necesita."""

    doc = Document()

    # ── Seccion A4 ──
    section = doc.sections[0]
    section.page_width  = Cm(21.0)
    section.page_height = Cm(29.7)
    section.top_margin    = Cm(2.0)
    section.bottom_margin = Cm(2.0)
    section.left_margin   = Cm(2.5)
    section.right_margin  = Cm(2.5)

    styles = doc.styles

    # ============================
    #  NORMAL
    # ============================
    normal = styles["Normal"]
    normal.font.name = "Calibri"
    normal.font.size = Pt(11)
    normal.font.color.rgb = RGBColor(0x00, 0x00, 0x00)
    npf = normal.paragraph_format
    npf.line_spacing = 1.15
    npf.space_before = Pt(0)
    npf.space_after = Pt(6)

    # ============================
    #  HEADING 1  (pandoc: # heading)
    # ============================
    h1 = styles["Heading 1"]
    h1.font.name = "Calibri"
    h1.font.size = Pt(16)
    h1.font.bold = True
    h1.font.color.rgb = RGBColor(0x1F, 0x4E, 0x79)
    h1pf = h1.paragraph_format
    h1pf.space_before = Pt(24)
    h1pf.space_after  = Pt(8)
    h1pf.keep_with_next = True
    h1pf.line_spacing = 1.15

    # ============================
    #  HEADING 2  (pandoc: ## heading)
    # ============================
    h2 = styles["Heading 2"]
    h2.font.name = "Calibri"
    h2.font.size = Pt(14)
    h2.font.bold = True
    h2.font.color.rgb = RGBColor(0x1F, 0x4E, 0x79)
    h2pf = h2.paragraph_format
    h2pf.space_before = Pt(18)
    h2pf.space_after  = Pt(6)
    h2pf.keep_with_next = True
    h2pf.line_spacing = 1.15

    # ============================
    #  HEADING 3  (pandoc: ### heading)
    # ============================
    h3 = styles["Heading 3"]
    h3.font.name = "Calibri"
    h3.font.size = Pt(12)
    h3.font.bold = True
    h3.font.color.rgb = RGBColor(0x33, 0x33, 0x33)
    h3pf = h3.paragraph_format
    h3pf.space_before = Pt(12)
    h3pf.space_after  = Pt(4)
    h3pf.keep_with_next = True
    h3pf.line_spacing = 1.15

    # ============================
    #  HEADING 4  (pandoc: #### heading)
    # ============================
    h4 = styles["Heading 4"]
    h4.font.name = "Calibri"
    h4.font.size = Pt(11)
    h4.font.bold = True
    h4.font.color.rgb = RGBColor(0x66, 0x66, 0x66)
    h4pf = h4.paragraph_format
    h4pf.space_before = Pt(8)
    h4pf.space_after  = Pt(4)
    h4pf.keep_with_next = True
    h4pf.line_spacing = 1.15

    # ============================
    #  TITLE  (metadata title)
    # ============================
    title = styles["Title"]
    title.font.name = "Calibri"
    title.font.size = Pt(22)
    title.font.bold = True
    title.font.color.rgb = RGBColor(0x1F, 0x4E, 0x79)
    tpf = title.paragraph_format
    tpf.alignment = WD_ALIGN_PARAGRAPH.CENTER
    tpf.space_before = Pt(60)
    tpf.space_after  = Pt(12)

    # ============================
    #  SUBTITLE
    # ============================
    subtitle = styles["Subtitle"]
    subtitle.font.name = "Calibri"
    subtitle.font.size = Pt(13)
    subtitle.font.color.rgb = RGBColor(0x2E, 0x75, 0xB6)
    spf = subtitle.paragraph_format
    spf.alignment = WD_ALIGN_PARAGRAPH.CENTER
    spf.space_after = Pt(50)

    # ============================
    #  TABLE STYLE
    # ============================
    # Pandoc usa el estilo "Table" para tablas. Lo creamos si no existe.
    try:
        table_style = styles["Table"]
    except KeyError:
        table_style = styles.add_style("Table", WD_STYLE_TYPE.TABLE)
    tblPr = table_style.element.find(qn("w:tblPr"))
    if tblPr is None:
        tblPr = parse_xml(f'<w:tblPr {nsdecls("w")}></w:tblPr>')
        table_style.element.append(tblPr)

    borders = parse_xml(
        f'<w:tblBorders {nsdecls("w")}>'
        f'  <w:top    w:val="single" w:sz="4" w:space="0" w:color="{AZUL_MEDIO}"/>'
        f'  <w:left   w:val="single" w:sz="4" w:space="0" w:color="{AZUL_MEDIO}"/>'
        f'  <w:bottom w:val="single" w:sz="4" w:space="0" w:color="{AZUL_MEDIO}"/>'
        f'  <w:right  w:val="single" w:sz="4" w:space="0" w:color="{AZUL_MEDIO}"/>'
        f'  <w:insideH w:val="single" w:sz="4" w:space="0" w:color="999999"/>'
        f'  <w:insideV w:val="single" w:sz="4" w:space="0" w:color="999999"/>'
        f'</w:tblBorders>'
    )
    tblPr.append(borders)

    # Sombreado alternado (banded rows)
    tblStylePr_first = parse_xml(
        f'<w:tblStylePr {nsdecls("w")} w:type="firstRow">'
        f'  <w:rPr>'
        f'    <w:b/>'
        f'    <w:color w:val="FFFFFF"/>'
        f'    <w:rFonts w:ascii="Calibri" w:hAnsi="Calibri"/>'
        f'    <w:sz w:val="22"/>'
        f'  </w:rPr>'
        f'  <w:tcPr>'
        f'    <w:shd w:val="clear" w:color="auto" w:fill="{AZUL_MEDIO}"/>'
        f'  </w:tcPr>'
        f'</w:tblStylePr>'
    )
    table_style.element.append(tblStylePr_first)

    # ── Table Header paragraph style ──
    try:
        th = styles["Table Header"]
    except KeyError:
        th = styles.add_style("Table Header", WD_STYLE_TYPE.PARAGRAPH)
    th.font.name = "Calibri"
    th.font.size = Pt(10)
    th.font.bold = True
    th.font.color.rgb = RGBColor(0xFF, 0xFF, 0xFF)

    # ── Compact  (celdas de tabla normales) ──
    try:
        compact = styles["Compact"]
    except KeyError:
        compact = styles.add_style("Compact", WD_STYLE_TYPE.PARAGRAPH)
    compact.font.name = "Calibri"
    compact.font.size = Pt(10)
    cpf = compact.paragraph_format
    cpf.space_before = Pt(2)
    cpf.space_after  = Pt(2)

    # ── Hyperlink ──
    try:
        link = styles["Hyperlink"]
        link.font.color.rgb = RGBColor(0x2E, 0x75, 0xB6)
        link.font.underline = True
    except KeyError:
        pass

    # ── Code blocks ──
    try:
        code = styles["Verbatim Char"]
        code.font.name = "Consolas"
        code.font.size = Pt(9)
    except KeyError:
        pass

    # ==== Guardar ====
    out_dir = os.path.dirname(os.path.abspath(__file__))
    out_path = os.path.join(out_dir, "reference.docx")
    doc.save(out_path)
    print(f"[OK] reference.docx generado en: {out_path}")
    print(f"     Size: {os.path.getsize(out_path):,} bytes")
    return out_path


if __name__ == "__main__":
    create_from_scratch()
