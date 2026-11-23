#!/usr/bin/env bash
# Regenera docs/Trabalho03_GoF.asta e docs/Trabalho03_GoF_diagramas.pdf a partir do codigo compilado.
# Requisitos: Astah UML instalado (usa o Java e a API que vem com ele), Python com Pillow e reportlab,
# e o projeto ja compilado (./mvnw package). Uso: bash docs/gerador/gerar.sh   (a partir da raiz do repositorio)
set -e
ASTAH="${ASTAH_HOME:-C:/Program Files/astah-UML}"
RAIZ="$(pwd -W 2>/dev/null || pwd)"
TMP="$RAIZ/target/modelos"
rm -rf "$TMP" && mkdir -p "$TMP/classes" "$TMP/img"
JAVA="$ASTAH/jre/bin/java.exe"
CP="$ASTAH/astah-api.jar;$ASTAH/astah-uml.jar"

"$JAVA" -m jdk.compiler/com.sun.tools.javac.Main -encoding UTF-8 -cp "$CP" -d "$TMP/classes" docs/gerador/GerarModelos.java
rm -f docs/Trabalho03_GoF.asta
(cd "$ASTAH" && "$JAVA" -Xmx1024m "-Djava.library.path=lib\\rlm" -cp "$TMP/classes;$RAIZ/target/classes;$CP" \
    GerarModelos "$RAIZ/target/classes" "$RAIZ/src/main/java" "$RAIZ/docs/Trabalho03_GoF.asta")
(cd "$ASTAH" && "$JAVA" -Xmx1024m "-Djava.library.path=lib\\rlm" -cp "$ASTAH/astah-uml.jar" \
    com.change_vision.jude.cmdline.JudeCommandRunner -image all -f "$RAIZ/docs/Trabalho03_GoF.asta" -t png -o "$TMP/img")
python docs/gerador/montar_pdf.py "$TMP/img" docs/Trabalho03_GoF_diagramas.pdf
