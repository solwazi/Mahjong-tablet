# Shift Mahjong Daily — Desktop (Java Swing)

A pure-Java desktop port of the *Daily Slide Mahjong* web game.
No external dependencies; requires **Java 11 or newer**.

## How the game works

- 6×6 board with 24 tiles (12 mahjong symbol pairs, each pair in its own color).
- **Drag a tile** with the mouse: the tile and the contiguous block behind it
  slide in the drag direction. The move only counts if it creates a
  line-of-sight match (two equal symbols with no tiles between them in a
  row or column).
- Matching pairs clear automatically, with chain reactions.
- **Hint** highlights a legal move for 1.5 seconds.
- **New Board** deals a fresh, always-solvable board.
- Clear every tile to win.

## Compile

From this directory (`swing/`):

**Linux / macOS:**

```sh
javac -encoding UTF-8 -d out $(find src -name "*.java")
```

**Windows (PowerShell):**

```powershell
javac -encoding UTF-8 -d out (Get-ChildItem -Recurse -Filter *.java src | ForEach-Object { $_.FullName })
```

## Run

```sh
java -cp out com.solwazi.slidemahjong.SlideMahjong
```

## Mahjong glyphs

The tiles use Unicode Mahjong Tile symbols (🀀–🀤, U+1F000 block).
If your system lacks a font covering that block, tiles still play
correctly but may render as boxes. Install one of these:

- **Windows:** Segoe UI Symbol (ships with Windows)
- **Linux:** Noto Sans Symbols 2 (`fonts-noto-extra` on Debian/Ubuntu)
- **macOS:** usually covered by the system fonts

## Project layout

```
src/com/solwazi/slidemahjong/
  Tile.java          — symbol + color value object
  Board.java         — all game rules, UI-agnostic (Tile[36])
  BoardPanel.java    — Swing painting + mouse drag handling
  SlideMahjong.java  — JFrame assembly + main()
```
