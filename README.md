# Retro Tetris - Android Edition 🎮

Ein klassisches Tetris-Spiel für Android, entwickelt mit **Kotlin** und **Jetpack Compose**.

## 📱 Features

- ✨ **Native Android App** - Optimiert für Android-Smartphones
- 🎨 **4 Themes** - Minimalistic, Tron, New York, Art Deco
- 👆 **Intuitive Touch-Controls** - Swipe-Gesten und Tap-Controls
- 📊 **Scoring System** - Punkte, Levels und Highscore-Tracking
- 💾 **Persistent Storage** - Speichert Highscores und Einstellungen
- 🎯 **Single Player** - Fokussiert auf das klassische Solo-Erlebnis
- 📐 **Modern Architecture** - MVVM mit Kotlin Coroutines

## 🎮 Steuerung

### Swipe-Gesten (Empfohlen)
- **Swipe Links/Rechts** → Tetromino bewegen
- **Swipe Nach Unten** → Schneller fallen (Soft Drop)
- **Swipe Nach Oben** → Rotieren
- **Tap** → Rotieren
- **Double Tap** → Hard Drop (sofort fallen lassen)

### Button-Controls (Alternative)
- Verwende die On-Screen-Buttons für präzise Kontrolle

## 🎨 Themes

Das Spiel enthält 4 eingebaute Themes:

1. **Minimalistic** - Klassisches schwarzes Design
2. **Tron** - Futuristisches Neon-Design
3. **New York** - Elegantes Gold-Design
4. **Art Deco** - Retro-Eleganz

Themes können im OPTIONS-Menü gewechselt werden.

## 🚀 Installation & Build

### Voraussetzungen
- Android Studio Hedgehog (2023.1.1) oder neuer
- JDK 17
- Android SDK 34
- Gradle 8.2+

### Projekt in Android Studio öffnen

1. Clone das Repository oder entpacke das Projekt
2. Öffne Android Studio
3. Wähle "Open" und navigiere zum Projektordner
4. Warte, bis Gradle die Abhängigkeiten heruntergeladen hat

### Build & Run

#### Mit Android Studio
1. Verbinde ein Android-Gerät oder starte einen Emulator
2. Klicke auf den "Run"-Button (▶️) oder drücke `Shift + F10`

#### Mit Gradle (Command Line)
```bash
# Debug Build erstellen
./gradlew assembleDebug

# Release Build erstellen
./gradlew assembleRelease

# App installieren und starten
./gradlew installDebug
```

Die APK findest du dann unter:
- Debug: `app/build/outputs/apk/debug/app-debug.apk`
- Release: `app/build/outputs/apk/release/app-release.apk`

## 📦 Projektstruktur

```
TetrisAndroid/
├── app/
│   └── src/main/java/com/tetris/
│       ├── game/                    # Game-Logik
│       │   ├── Tetromino.kt        # Tetromino-Formen
│       │   ├── Board.kt            # Spielfeld
│       │   ├── GameState.kt        # Spiel-Zustände
│       │   └── TetrisGame.kt       # Haupt-Game-Engine
│       ├── ui/                      # UI-Komponenten
│       │   ├── MenuScreen.kt       # Hauptmenü
│       │   ├── GameScreen.kt       # Spiel-Bildschirm
│       │   ├── theme/              # Theme-System
│       │   └── components/         # UI-Komponenten
│       ├── data/                    # Datenverwaltung
│       │   └── GamePreferences.kt  # Einstellungen
│       ├── GameViewModel.kt        # ViewModel
│       └── MainActivity.kt         # Haupt-Activity
└── build.gradle.kts
```

## 🎯 Gameplay

### Ziel
Vervollständige horizontale Linien, um sie zu löschen und Punkte zu sammeln!

### Scoring
- **1 Linie**: 100 × Level
- **2 Linien**: 300 × Level
- **3 Linien**: 500 × Level
- **4 Linien (Tetris)**: 800 × Level
- **Soft Drop**: +1 Punkt pro Zeile
- **Hard Drop**: +2 Punkte pro Zeile

### Level-System
- Alle 10 gelöschten Linien steigt das Level
- Höheres Level = höhere Geschwindigkeit
- Höheres Level = mehr Punkte pro Linie

## 🛠️ Technologie-Stack

- **Kotlin** - Moderne JVM-Sprache
- **Jetpack Compose** - Deklaratives UI-Framework
- **Material 3** - Modern Design System
- **Coroutines** - Asynchrone Programmierung
- **StateFlow** - Reactive State Management
- **DataStore** - Preferences Storage
- **ViewModel** - MVVM Architecture

## 📝 Entwicklung

### Code-Qualität
Das Projekt folgt modernen Android-Entwicklungsstandards:
- Clean Architecture mit MVVM
- Separation of Concerns
- Reactive Programming mit Flows
- Composable-basierte UI

### Anpassungen

#### Neue Themes hinzufügen
Bearbeite `app/src/main/java/com/tetris/ui/theme/Theme.kt`:

```kotlin
val MyCustomTheme = TetrisTheme(
    name = "My Theme",
    background = Color(0xFF000000),
    // ... weitere Farben
    shapeColors = mapOf(
        TetrominoType.I to Color(0xFF00FFFF),
        // ... weitere Tetromino-Farben
    )
)

// Zu AllThemes hinzufügen
val AllThemes = listOf(
    MinimalisticTheme,
    TronTheme,
    NewYorkTheme,
    ArtDecoTheme,
    MyCustomTheme  // NEU
)
```

#### Gameplay-Parameter ändern
Bearbeite `GameStats` in `app/src/main/java/com/tetris/game/GameState.kt`:

```kotlin
fun dropSpeed(): Long {
    return maxOf(100L, 500L - (level - 1) * 50L)  // Anpassen
}
```

## 📱 Systemanforderungen

- **Minimum**: Android 7.0 (API Level 24)
- **Target**: Android 14 (API Level 34)
- **Orientierung**: Portrait (Hochformat)
- **Empfohlene Bildschirmgröße**: 5.5" oder größer

## 🐛 Bekannte Probleme & Lösungen

### Build-Fehler "SDK not found"
```bash
# Erstelle local.properties mit SDK-Pfad
echo "sdk.dir=/path/to/Android/Sdk" > local.properties
```

### Gradle-Sync-Fehler
```bash
# Gradle Cache löschen
./gradlew clean
./gradlew --stop
# Projekt in Android Studio neu öffnen
```

## 🎓 Vom Python-Original portiert

Dieses Projekt ist eine native Android-Portierung des ursprünglichen Python/Pygame-Tetris.

**Hauptunterschiede:**
- Native Android statt Desktop
- Touch-Controls statt Tastatur/Controller
- Jetpack Compose statt Pygame-Rendering
- Kotlin Coroutines statt Threading
- Single-Player-Fokus (Multiplayer-Modi entfernt)

## 📄 Lizenz

Dieses Projekt ist Open Source und kann frei verwendet werden.

## 🙏 Credits

- Entwickelt mit Kotlin und Jetpack Compose
- Basiert auf dem klassischen Tetris-Gameplay
- Portiert vom Python/Pygame-Original

---

**Viel Spaß beim Spielen! 🎮**
