# Daily Slide Mahjong — Android App

A Java port of the **Daily Slide Mahjong** web game (originally a single HTML file).
Swipe tiles to slide them; matching mahjong pairs in line of sight clear automatically.

- Package: `com.solwazi.slidemahjong`
- minSdk 24 · targetSdk 34 · Java 11
- Android Gradle Plugin 8.5.2 · Gradle 8.7 (via the included wrapper)
- No third-party dependencies — pure Android framework + Java.

## Project layout

```
android/
├── settings.gradle
├── build.gradle                 # project-level
├── gradlew / gradlew.bat        # Gradle wrapper
├── gradle/wrapper/
└── app/
    ├── build.gradle             # applicationId com.solwazi.slidemahjong
    └── src/main/
        ├── AndroidManifest.xml
        ├── java/com/solwazi/slidemahjong/
        │   ├── Tile.java        # symbol glyph + face color (immutable)
        │   ├── Board.java       # all game rules; no Android dependencies
        │   ├── BoardView.java   # custom View: draws the board, handles drags
        │   └── MainActivity.java# layout wiring, tile counter, win dialog
        └── res/
            ├── layout/activity_main.xml
            └── values/strings.xml, colors.xml, themes.xml
```

`Board.java` is deliberately UI-agnostic (only `java.util`), so the rules can be
unit-tested on a plain JVM. `BoardView` only renders and forwards input.

## How to play

- **Drag a tile** up/down/left/right: the contiguous block of tiles ahead of it
  slides along. The move only counts if it creates a line-of-sight match —
  two equal symbols with no other tile between them in a row or column.
- **Matching pairs clear automatically**, including chain reactions.
- **Hint** highlights one legal move for 1.5 seconds (amber outlines).
- **New Board** deals a fresh solvable board.
- The board is reshuffled automatically whenever no legal move remains.

## Open in Android Studio

1. Android Studio → **File → Open…** → select this `android/` folder.
2. Let Gradle sync (first run downloads the Gradle 8.7 distribution and the
   Android Gradle Plugin — needs internet).
3. Press **Run ▶** on an emulator or a physical device with USB debugging on.

Requires Android Studio Hedgehog (2023.1.1) or newer; AGP 8.5 needs JDK 17,
which Android Studio bundles by default.

## Build the APK

**Build → Build Bundle(s) / APK(s) → Build APK(s)**.

The debug APK lands at:

```
app/build/outputs/apk/debug/app-debug.apk
```

Install it on a device with `adb install app-debug.apk`.

## Notes

- **Mahjong glyphs:** tiles use the Unicode Mahjong Tiles block (🀀–🀤).
  Most modern Android devices ship a font covering it; very old devices may
  show tofu boxes for some tiles.
- **Dark theme** (`#1E293B` background, `#0F172A` board) matches the web game.
- If the Gradle wrapper jar is missing in your checkout, Android Studio will
  offer to generate/download it on first sync.
