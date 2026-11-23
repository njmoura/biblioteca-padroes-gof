"""Monta o PDF dos modelos: um diagrama por página, com a página do tamanho do diagrama (nada é reduzido)."""
import glob
import os
import sys

from PIL import Image
from reportlab.pdfgen import canvas

pasta_imagens, saida = sys.argv[1], sys.argv[2]
ESCALA = 0.62          # pixels do Astah -> pontos do PDF
MARGEM = 28

arquivos = sorted(glob.glob(os.path.join(pasta_imagens, "**", "*.png"), recursive=True),
                  key=lambda p: (not os.path.basename(p).startswith("CD"),
                                 not os.path.basename(p).startswith("SM"),
                                 os.path.basename(p)))
pdf = canvas.Canvas(saida)
pdf.setTitle("Trabalho 03 - Modelos UML (padrões GoF)")
pdf.setAuthor("Gabriel Naumann Jerônimo de Moura; Gabriel Pacheco Benin")

# capa com o índice
pdf.setPageSize((842, 595))
pdf.setFont("Helvetica-Bold", 20)
pdf.drawString(50, 530, "Trabalho 03 - Sistema de empréstimo de livros com padrões GoF")
pdf.setFont("Helvetica", 12)
pdf.drawString(50, 506, "Modelos UML (Astah) - Gabriel Naumann Jerônimo de Moura e Gabriel Pacheco Benin")
pdf.drawString(50, 488, "Classes, atributos e operações foram gerados a partir do código compilado; um diagrama por página.")
y = 462
pdf.setFont("Helvetica", 9.5)
for i, arq in enumerate(arquivos, start=2):
    pdf.drawString(60, y, f"{i:>2}   {os.path.splitext(os.path.basename(arq))[0]}")
    y -= 13.4
pdf.showPage()

for arq in arquivos:
    img = Image.open(arq)
    largura, altura = img.size[0] * ESCALA, img.size[1] * ESCALA
    pdf.setPageSize((largura + 2 * MARGEM, altura + 2 * MARGEM + 26))
    pdf.setFont("Helvetica-Bold", 14)
    pdf.drawString(MARGEM, altura + MARGEM + 8, os.path.splitext(os.path.basename(arq))[0])
    pdf.drawImage(arq, MARGEM, MARGEM, width=largura, height=altura)
    pdf.showPage()
pdf.save()
print(f"{len(arquivos) + 1} paginas em {saida}")
