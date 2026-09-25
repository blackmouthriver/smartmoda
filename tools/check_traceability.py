#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
AUT-18 · Verifica la trazabilidad regla de negocio -> prueba automatizada.

Toda RN-xxx documentada en docs/06-reglas-de-negocio.md debe tener al menos una prueba
cuyo nombre la referencie (RN_001, RN-001 o RN001). Sin este control, la trazabilidad se
degrada en tres sprints y nadie se entera.

Uso:
    python tools/check_traceability.py            # falla si hay reglas sin prueba
    python tools/check_traceability.py --report   # solo informa, no falla

Las reglas que aun no toca implementar se declaran en tools/traceability_waivers.txt,
una por linea, con motivo despues de #. Un waiver es una deuda explicita, no una excusa.
"""
import argparse
import os
import re
import sys

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
RULES_DOC = os.path.join(ROOT, "docs", "06-reglas-de-negocio.md")
WAIVERS = os.path.join(ROOT, "tools", "traceability_waivers.txt")
TEST_DIRS = ["shared-domain", "android", "web", "backend"]
TEST_EXT = (".kt", ".kts", ".java", ".ts", ".tsx", ".py")

# Declaracion de prueba en los lenguajes del proyecto: Kotlin/Java, Jest/Vitest, pytest.
TEST_DECL = re.compile(r"(?:^|[^A-Za-z_])(?:fun|void|it|test|def)[\s(]")


def documented_rules():
    """IDs de regla declarados en la documentacion."""
    if not os.path.exists(RULES_DOC):
        sys.exit("No se encontro %s" % RULES_DOC)
    text = open(RULES_DOC, encoding="utf-8").read()
    return sorted(set(re.findall(r"\*\*(RN-\d{3})\*\*", text)))


def rules_covered_by_tests():
    """IDs de regla referenciados desde algun archivo de pruebas."""
    found = {}
    for d in TEST_DIRS:
        base = os.path.join(ROOT, d)
        if not os.path.isdir(base):
            continue
        for dirpath, _, files in os.walk(base):
            if "test" not in dirpath.lower().replace("\\", "/"):
                continue
            for f in files:
                if not f.endswith(TEST_EXT):
                    continue
                path = os.path.join(dirpath, f)
                try:
                    text = open(path, encoding="utf-8").read()
                except OSError:
                    continue
                # Solo cuenta si el ID esta en el NOMBRE de una prueba, no en cualquier
                # comentario del archivo. Un comentario que dice "RN-009 se verifica en otro
                # sitio" no cubre RN-009, y contarlo daria una falsa sensacion de cobertura.
                for line in text.splitlines():
                    if not TEST_DECL.search(line):
                        continue
                    for rid in re.findall(r"RN[_-]?(\d{3})", line):
                        found.setdefault("RN-" + rid, set()).add(
                            os.path.relpath(path, ROOT).replace("\\", "/"))
    return found


def waived():
    if not os.path.exists(WAIVERS):
        return {}
    out = {}
    for line in open(WAIVERS, encoding="utf-8"):
        line = line.strip()
        if not line or line.startswith("#"):
            continue
        rid, _, reason = line.partition("#")
        out[rid.strip()] = reason.strip() or "sin motivo declarado"
    return out


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("--report", action="store_true", help="informar sin fallar")
    args = ap.parse_args()

    rules = documented_rules()
    covered = rules_covered_by_tests()
    exempt = waived()

    missing = [r for r in rules if r not in covered and r not in exempt]
    stale = [r for r in exempt if r in covered]

    print("Trazabilidad de reglas de negocio")
    print("=" * 62)
    print("  documentadas : %d" % len(rules))
    print("  con prueba   : %d" % len([r for r in rules if r in covered]))
    print("  con waiver   : %d" % len([r for r in rules if r in exempt]))
    print("  SIN cubrir   : %d" % len(missing))

    if stale:
        print()
        print("Waivers caducos: la regla YA tiene prueba y sobra la excepcion.")
        print("Retiralos de tools/traceability_waivers.txt; si no, la lista deja de ser cierta.")
        for r in stale:
            print("   %s -> %s" % (r, ", ".join(sorted(covered[r]))))

    if missing:
        print()
        print("Reglas sin ninguna prueba que las nombre:")
        for r in missing:
            print("   %s" % r)
        print()
        print("Anade una prueba cuyo nombre empiece por el ID, por ejemplo:")
        print('   fun `RN_003 el stock se revalida antes de reservar`() { ... }')
        print()
        print("O declara la deuda en tools/traceability_waivers.txt si aun no toca implementarla.")

    if (missing or stale) and not args.report:
        return 1
    if not missing and not stale:
        print()
        print("Todas las reglas documentadas tienen prueba o waiver, y ningun waiver sobra.")
    return 0


if __name__ == "__main__":
    sys.exit(main())
