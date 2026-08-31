#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
Verifica que ningun enlace relativo de la documentacion apunte a un archivo inexistente.

Barato de mantener y evita el deterioro silencioso: la documentacion se rompe enlace a enlace,
no de golpe, y cuando se nota ya nadie confia en ella.
"""
import os
import re
import sys

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
SKIP_DIRS = {".git", "node_modules", "build", ".gradle", "dist"}
LINK = re.compile(r"\[[^\]]*\]\(([^)\s]+)\)")


def main():
    broken = []
    checked = 0
    for dirpath, dirnames, filenames in os.walk(ROOT):
        dirnames[:] = [d for d in dirnames if d not in SKIP_DIRS]
        for name in filenames:
            if not name.endswith(".md"):
                continue
            path = os.path.join(dirpath, name)
            rel = os.path.relpath(path, ROOT).replace("\\", "/")
            try:
                text = open(path, encoding="utf-8").read()
            except OSError:
                continue
            for target in LINK.findall(text):
                if target.startswith(("http://", "https://", "mailto:", "#")):
                    continue
                checked += 1
                clean = target.split("#")[0]
                if not clean:
                    continue
                dest = os.path.normpath(os.path.join(dirpath, clean))
                if not os.path.exists(dest):
                    broken.append((rel, target))

    print("Enlaces de documentacion")
    print("=" * 62)
    print("  comprobados : %d" % checked)
    print("  rotos       : %d" % len(broken))
    if broken:
        print()
        for src, target in broken:
            print("   %s -> %s" % (src, target))
        return 1
    print()
    print("Sin enlaces rotos.")
    return 0


if __name__ == "__main__":
    sys.exit(main())
