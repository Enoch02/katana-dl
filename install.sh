#!/bin/sh

./gradlew fatJar

FAT_JAR="build/libs/katana-dl-1.0-SNAPSHOT-all.jar"

mkdir -p "$HOME/.local/bin"
cp "$FAT_JAR" "$HOME/.local/bin/katana-dl.jar"

cat > "$HOME/.local/bin/katana-dl" << 'EOF'
#!/bin/sh
exec java -jar "$HOME/.local/bin/katana-dl.jar"
EOF

chmod +x "$HOME/.local/bin/katana-dl"

local_bin="$HOME/.local/bin"
if ! echo "$PATH" | tr ':' '\n' | grep -qx "$local_bin"; then
    case "$SHELL" in
        *zsh) profile="$HOME/.zshrc" ;;
        *bash) profile="$HOME/.bashrc" ;;
        *) profile="$HOME/.profile" ;;
    esac
    if ! grep -q "local_bin" "$profile" 2>/dev/null; then
        echo "export PATH=\"$HOME/.local/bin:$PATH\"" >> "$profile"
    fi
fi

echo "Installation complete!"