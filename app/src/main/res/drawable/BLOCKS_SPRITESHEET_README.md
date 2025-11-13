# Blocks Spritesheet

## Benötigte Datei
Legen Sie eine Datei namens **`blocks_spritesheet.png`** in diesem Ordner ab.

## Spritesheet-Spezifikationen

### Layout
Das Spritesheet sollte alle 7 Tetromino-Blöcke in einer **horizontalen Reihe** enthalten:

```
+-----+-----+-----+-----+-----+-----+-----+
|  I  |  O  |  T  |  S  |  Z  |  J  |  L  |
+-----+-----+-----+-----+-----+-----+-----+
```

### Reihenfolge der Blöcke (von links nach rechts)
1. **I-Block** (Cyan/Türkis)
2. **O-Block** (Gelb)
3. **T-Block** (Lila/Magenta)
4. **S-Block** (Grün)
5. **Z-Block** (Rot)
6. **J-Block** (Blau)
7. **L-Block** (Orange)

### Empfohlene Abmessungen
- **Gesamt:** 448 x 64 px (7 Blöcke × 64px Breite × 64px Höhe)
- **Pro Block:** 64 x 64 px
- **Alternative Größen:** 896 x 128 px, 1344 x 192 px, etc. (beliebig skalierbar)

### Format
- **PNG** (empfohlen) - unterstützt Transparenz
- **WebP** - moderne Alternative mit kleinerer Dateigröße
- **JPG** - nur wenn keine Transparenz benötigt wird

### Beispiel-Layout

```
Gesamtgröße: 448px Breite × 64px Höhe

Position:  0px   64px  128px  192px  256px  320px  384px  448px
           |     |     |     |     |     |     |     |
           +-----+-----+-----+-----+-----+-----+-----+
           |  I  |  O  |  T  |  S  |  Z  |  J  |  L  |
           +-----+-----+-----+-----+-----+-----+-----+
```

### Hinweise
- Jeder Block sollte gleich groß sein (quadratisch)
- Keine Abstände zwischen den Blöcken
- Der Code extrahiert automatisch den richtigen Bereich für jeden Block-Typ
- Die Blöcke werden dynamisch auf die Spielfeldgröße skaliert

### Fallback
Wenn keine `blocks_spritesheet.png` Datei gefunden wird, verwendet die App automatisch farbige Rechtecke als Fallback.
