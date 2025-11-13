# Anleitung: Benutzerdefinierte Themes erstellen

Diese Anleitung erklärt, wie Sie eigene Themes für die Tetris-App erstellen können.

## Theme-Struktur

Ein Theme in der Tetris-App besteht aus verschiedenen Farbelementen, die das Aussehen der gesamten Anwendung bestimmen. Die Theme-Definitionen befinden sich in:

```
app/src/main/java/com/tetris/ui/theme/Theme.kt
```

## Theme-Komponenten

Ein Theme (`TetrisTheme`) enthält folgende Eigenschaften:

### Allgemeine Farben
- **name**: Der Name des Themes (z.B. "Minimalistic", "Tron")
- **background**: Hintergrundfarbe der gesamten App
- **gridBorder**: Farbe des Spielfeld-Rahmens und der Buttons
- **blockBorder**: Farbe der Rahmen um einzelne Tetris-Blöcke
- **overlay**: Farbe für Overlay-Screens (Pause, Game Over)

### Text-Farben
- **textPrimary**: Haupttextfarbe (z.B. für Score, Level)
- **textSecondary**: Sekundäre Textfarbe (z.B. für Labels wie "SCORE", "LEVEL")
- **textHighlight**: Hervorhebungsfarbe (z.B. für High Score)
- **textDanger**: Warnfarbe (z.B. für "GAME OVER")

### Tetromino-Farben
- **shapeColors**: Eine Map, die jedem Tetromino-Typ eine Farbe zuweist
  - `TetrominoType.I` - I-förmiger Block (4 Blöcke in einer Reihe)
  - `TetrominoType.O` - Quadratischer Block (2x2)
  - `TetrominoType.T` - T-förmiger Block
  - `TetrominoType.S` - S-förmiger Block
  - `TetrominoType.Z` - Z-förmiger Block
  - `TetrominoType.J` - J-förmiger Block
  - `TetrominoType.L` - L-förmiger Block

## Eigenes Theme erstellen

### Schritt 1: Theme definieren

Öffnen Sie die Datei `app/src/main/java/com/tetris/ui/theme/Theme.kt` und fügen Sie Ihr neues Theme hinzu:

```kotlin
/**
 * Mein eigenes Theme
 */
val MeinEigenesTheme = TetrisTheme(
    name = "Mein Theme",
    background = Color(0xFF000000),           // Schwarz
    gridBorder = Color(0xFF333333),           // Dunkelgrau
    blockBorder = Color(0xFFFFFFFF),          // Weiß
    textPrimary = Color(0xFFFFFFFF),          // Weiß
    textSecondary = Color(0xFF999999),        // Hellgrau
    textHighlight = Color(0xFF00FF00),        // Grün
    textDanger = Color(0xFFFF0000),           // Rot
    overlay = Color(0xFF000000),              // Schwarz
    shapeColors = mapOf(
        TetrominoType.I to Color(0xFF00FFFF), // Cyan
        TetrominoType.O to Color(0xFFFFFF00), // Gelb
        TetrominoType.T to Color(0xFFFF00FF), // Magenta
        TetrominoType.S to Color(0xFF00FF00), // Grün
        TetrominoType.Z to Color(0xFFFF0000), // Rot
        TetrominoType.J to Color(0xFF0000FF), // Blau
        TetrominoType.L to Color(0xFFFFA500)  // Orange
    )
)
```

### Schritt 2: Theme zur Liste hinzufügen

Fügen Sie Ihr Theme zur Liste `AllThemes` am Ende der Datei hinzu:

```kotlin
/**
 * Available themes
 */
val AllThemes = listOf(
    MinimalisticTheme,
    TronTheme,
    NewYorkTheme,
    ArtDecoTheme,
    MeinEigenesTheme  // Ihr neues Theme hier hinzufügen
)
```

### Schritt 3: App neu kompilieren

Kompilieren Sie die App neu, damit das neue Theme verfügbar wird:

```bash
./gradlew assembleDebug
```

oder über Android Studio: **Build > Make Project**

### Schritt 4: Theme in der App auswählen

Nach dem Neustart der App können Sie Ihr neues Theme im Optionsmenü auswählen.

## Farben in Kotlin

Farben werden in Kotlin mit dem Format `Color(0xAARRGGBB)` definiert:

- **AA**: Alpha-Kanal (Transparenz) - `FF` = vollständig opak, `00` = vollständig transparent
- **RR**: Rot-Wert (00-FF)
- **GG**: Grün-Wert (00-FF)
- **BB**: Blau-Wert (00-FF)

### Beispiele:
- `Color(0xFF000000)` - Schwarz
- `Color(0xFFFFFFFF)` - Weiß
- `Color(0xFFFF0000)` - Rot
- `Color(0xFF00FF00)` - Grün
- `Color(0xFF0000FF)` - Blau
- `Color(0x80FFFFFF)` - Halbtransparentes Weiß

## Tipps für gute Themes

1. **Kontrast**: Achten Sie auf ausreichenden Kontrast zwischen Hintergrund und Textfarben
2. **Lesbarkeit**: Stellen Sie sicher, dass alle Texte gut lesbar sind
3. **Unterscheidbarkeit**: Die Tetromino-Farben sollten sich deutlich voneinander unterscheiden
4. **Konsistenz**: Verwenden Sie eine harmonische Farbpalette
5. **Testen**: Testen Sie Ihr Theme im Spiel, um sicherzustellen, dass alles gut sichtbar ist

## Vorhandene Themes als Vorlage

Die App enthält bereits folgende Themes, die Sie als Vorlage verwenden können:

- **Minimalistic**: Klassisches schwarzes Theme mit leuchtenden Farben
- **Tron**: Futuristisches Neon-Theme in Blau/Cyan-Tönen
- **New York**: Elegantes Theme mit Gold-Akzenten
- **Art Deco**: Retro-elegantes Theme mit warmen Farbtönen

## Farb-Tools

Hilfreiche Online-Tools zum Erstellen von Farbpaletten:

- [Coolors](https://coolors.co/) - Farbpaletten-Generator
- [Adobe Color](https://color.adobe.com/) - Professionelles Farbwerkzeug
- [Material Design Colors](https://materialui.co/colors/) - Material Design Farbpaletten
- [ColorHunt](https://colorhunt.co/) - Kuratierte Farbpaletten

## Problembehebung

**Theme wird nicht angezeigt?**
- Stellen Sie sicher, dass Sie das Theme zur `AllThemes`-Liste hinzugefügt haben
- Kompilieren Sie die App neu
- Prüfen Sie, ob alle Farben korrekt im `Color(0xAARRGGBB)`-Format definiert sind

**Farben sehen nicht wie erwartet aus?**
- Überprüfen Sie den Alpha-Kanal (erste zwei Ziffern nach `0x`)
- Verwenden Sie `FF` für vollständig opake Farben

**Text ist nicht lesbar?**
- Erhöhen Sie den Kontrast zwischen Text- und Hintergrundfarben
- Testen Sie verschiedene Textfarben für bessere Lesbarkeit
