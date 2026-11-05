#!/bin/sh

local_bin="$HOME/.local/bin"

rm -f "$local_bin/katana-dl"
rm -f "$local_bin/katana-dl.jar"

rmdir "$local_bin" 2>/dev/null

case "$SHELL" in
    *zsh) profile="$HOME/.zshrc" ;;
    *bash) profile="$HOME/.bashrc" ;;
    *) profile="$HOME/.profile" ;;
esac

if [ -n "$profile" ] && [ -f "$profile" ]; then
    if grep -q "export PATH.*\.local/bin" "$profile" 2>/dev/null; then
        sed -i '' '/export PATH.*\.local\/bin/d' "$profile" 2>/dev/null || \
        sed -i '/export PATH.*\.local\/bin/d' "$profile" 2>/dev/null
    fi
fi

echo "Uninstallation complete!"