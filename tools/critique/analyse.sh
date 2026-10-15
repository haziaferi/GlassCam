#!/usr/bin/env bash
# analyse.sh -- rendered-side measurements over the walk's screenshots.
# craftkit analyse: value distribution, focal point, detail, legibility per PNG
# designkit extract: k-means palette actually on screen (rendered vs declared drift)
CK="${CRAFTKIT:?set CRAFTKIT to craftkit_cli.py from the creative-design-suite plugin}"
DK="${DESIGNKIT:-$HOME/.claude/plugins/cache/gold-standard-local/design-optimization/2.0.6/engine/designkit_cli.py}"
cd "$(dirname "$0")/screenshots"
for f in "$@"; do
  echo "== $f"; python "$CK" --json analyse "$f" | python -c "import json,sys; d=json.load(sys.stdin); print({k:d[k] for k in d if k in ('value','focal','legibility','detail','mean_luminance','contrast')})" 2>/dev/null || python "$CK" analyse "$f" | head -12
  python "$DK" extract "$f" --count 5 2>/dev/null | head -7
done
