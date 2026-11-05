# katana-dl

A command-line tool for downloading manga chapters from Manga Katana as CBZ files.

Katana-dl scrapes a manga page to list its chapters, lets you pick which chapters to download, and packages the chapter images into a CBZ (Comic Book ZIP) archive ready for use in comic reader apps.

## Features

- Scrapes the manga title and chapter list from a Manga Katana URL
- Interactive chapter selection with support for:
  - Single chapters: `1`
  - Ranges: `1-5`
  - Mixed lists: `1, 3, 5-8`
  - Everything: `all`
- Automatically skips chapters that have already been downloaded
- Cleans up incomplete CBZ files if a download fails
- Downloads chapters into `~/Downloads/Manga/<Manga Title>/`

## Requirements

- [JDK 21](https://adoptium.net/)
- [Gradle](https://gradle.org/) (via the included wrapper: `./gradlew`)

## Installation

### From source (recommended)

```sh
git clone https://github.com/Enoch02/katana-dl.git
cd katana-dl
./install.sh
```

This builds a fat JAR with all dependencies and installs it as `katana-dl` in your local bin directory (`~/.local/bin`), adding that directory to your shell `PATH` if it isn't already there.

### Manual

```sh
./gradlew fatJar
cp build/libs/katana-dl-1.0-SNAPSHOT-all.jar ~/.local/bin/katana-dl.jar
```

Then create a wrapper script at `~/.local/bin/katana-dl`:

```sh
#!/bin/sh
exec java -jar "$HOME/.local/bin/katana-dl.jar"
```

and make it executable with `chmod +x ~/.local/bin/katana-dl`.

### Uninstall

```sh
./uninstall.sh
```

## Usage

```sh
katana-dl
```

1. Enter the link to a manga to continue:

   ```
   Enter the link to a manga to continue:
   ```

2. Provide a valid Manga Katana URL.

3. The chapter list is displayed. For series with more than 10 chapters, the newest and oldest chapters are shown first, with an option to list all chapters.

4. Enter the chapter indices/ranges to download (e.g., `1-5, 8, 12-15` or `all`).

5. Each selected chapter is downloaded and saved as a `.cbz` file in your downloads folder (default: `~/Downloads/Manga/`).

Already downloaded chapters are skipped automatically, and failed downloads are cleaned up.
