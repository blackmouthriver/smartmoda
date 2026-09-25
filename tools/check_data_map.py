#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
EN-1501 · Verifica que el mapa de datos corresponda con lo que el sistema guarda de verdad.

Un mapa de datos en prosa esta desactualizado en tres sprints y nadie se entera hasta una
auditoria. Este control lo mantiene vivo:

  1. Toda tabla creada en supabase/migrations/ esta declarada en el mapa
  2. Toda columna de esas tablas tiene clasificacion
  3. Toda entidad de Room esta declarada, con sus campos
  4. NINGUN dato CRITICO vive en un almacen del servidor

El punto 4 es el que convierte ADR-0003 de promesa en garantia comprobable. Si alguien anade
una columna `chest_mm` a una tabla de Supabase y la clasifica como CRITICO, el build se
detiene. Y si la clasifica como BAJO para esquivar el control, el nombre queda en el mapa
firmado por alguien: la decision es visible, que es lo que se puede auditar.

Uso:
    python tools/check_data_map.py
"""
import os
import re
import sys

try:
    import yaml
except ImportError:
    sys.exit("Falta PyYAML. Instala con: pip install pyyaml")

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
DATA_MAP = os.path.join(ROOT, "contracts", "data-map.yaml")
MIGRATIONS = os.path.join(ROOT, "supabase", "migrations")
ANDROID = os.path.join(ROOT, "android")

CREATE_TABLE = re.compile(
    r"create\s+table\s+(?:if\s+not\s+exists\s+)?([a-z_][a-z0-9_]*)\s*\((.*?)\n\);",
    re.IGNORECASE | re.DOTALL,
)
ROOM_ENTITY = re.compile(r"@Entity\b.*?data\s+class\s+(\w+)\s*\((.*?)\n\)", re.DOTALL)
KOTLIN_FIELD = re.compile(r"\bva[lr]\s+(\w+)\s*:", re.MULTILINE)

# Palabras que no son columnas dentro del cuerpo de un CREATE TABLE.
NOT_A_COLUMN = {
    "primary", "unique", "check", "foreign", "constraint", "references", "exclude",
}


def load_map():
    if not os.path.exists(DATA_MAP):
        sys.exit("No existe %s" % DATA_MAP)
    with open(DATA_MAP, encoding="utf-8") as f:
        return yaml.safe_load(f)


def sql_tables():
    """{tabla: [columnas]} a partir de las migraciones."""
    out = {}
    if not os.path.isdir(MIGRATIONS):
        return out
    for name in sorted(os.listdir(MIGRATIONS)):
        if not name.endswith(".sql"):
            continue
        sql = open(os.path.join(MIGRATIONS, name), encoding="utf-8").read()
        for m in CREATE_TABLE.finditer(sql):
            table = m.group(1).lower()
            cols = []
            for raw in m.group(2).split("\n"):
                line = raw.strip().lstrip(",").strip()
                if not line or line.startswith("--"):
                    continue
                first = line.split()[0].lower().strip('"')
                if first in NOT_A_COLUMN or not re.match(r"^[a-z_][a-z0-9_]*$", first):
                    continue
                cols.append(first)
            out[table] = cols
    return out


def room_entities():
    """{Entidad: [campos]} a partir de las clases anotadas con @Entity."""
    out = {}
    for dirpath, _, files in os.walk(ANDROID):
        if "build" in dirpath.replace("\\", "/").split("/"):
            continue
        for f in files:
            if not f.endswith(".kt"):
                continue
            text = open(os.path.join(dirpath, f), encoding="utf-8").read()
            for m in ROOM_ENTITY.finditer(text):
                out[m.group(1)] = KOTLIN_FIELD.findall(m.group(2))
    return out


def main():
    data = load_map()
    classifications = data.get("classifications", {})
    stores = data.get("stores", {})
    datasets = data.get("datasets", [])

    declared_tables = {}
    declared_entities = {}
    problems = []

    for d in datasets:
        cols = d.get("columns") or {}
        if "table" in d:
            declared_tables[d["table"]] = (d, cols)
        elif "entity" in d:
            declared_entities[d["entity"]] = (d, cols)

    # --- 1 y 2. tablas y columnas de SQL
    actual_tables = sql_tables()
    for table, cols in actual_tables.items():
        if table not in declared_tables:
            problems.append(("tabla sin declarar", table,
                             "existe en las migraciones pero no en el mapa de datos"))
            continue
        _, declared_cols = declared_tables[table]
        for c in cols:
            if c not in declared_cols:
                problems.append(("columna sin clasificar", "%s.%s" % (table, c),
                                 "anade su clasificacion al mapa"))

    for table in declared_tables:
        if table not in actual_tables:
            problems.append(("tabla fantasma", table,
                             "esta en el mapa pero ya no existe en las migraciones"))

    # --- 3. entidades de Room
    actual_entities = room_entities()
    for entity, fields in actual_entities.items():
        if entity not in declared_entities:
            problems.append(("entidad sin declarar", entity,
                             "es una @Entity de Room que no esta en el mapa"))
            continue
        _, declared_fields = declared_entities[entity]
        for f in fields:
            if f not in declared_fields:
                problems.append(("campo sin clasificar", "%s.%s" % (entity, f),
                                 "anade su clasificacion al mapa"))

    # --- 4. ADR-0003: nada CRITICO en el servidor
    for d in datasets:
        name = d.get("table") or d.get("entity") or d.get("dataset") or "?"
        store_id = d.get("store")
        cls = d.get("classification")

        store = stores.get(store_id)
        if store is None:
            problems.append(("almacen desconocido", name,
                             "'%s' no esta declarado en stores" % store_id))
            continue

        allowed = classifications.get(cls, {}).get("allowed_stores", [])
        if allowed and store_id not in allowed:
            problems.append(("ADR-0003", name,
                             "clasificado %s pero vive en '%s'; permitidos: %s"
                             % (cls, store_id, ", ".join(allowed))))

        # Una columna CRITICA dentro de una tabla del servidor es el caso peligroso:
        # la tabla puede estar clasificada BAJO y colarse un campo corporal dentro.
        if store.get("side") == "server":
            for col, col_cls in (d.get("columns") or {}).items():
                if col_cls == "CRITICO":
                    problems.append(("ADR-0003", "%s.%s" % (name, col),
                                     "dato CRITICO en un almacen del servidor"))

    # --- informe
    print("Mapa de datos")
    print("=" * 66)
    print("  version          : %s (actualizado %s)" % (data.get("version"), data.get("updated")))
    print("  tablas SQL       : %d declaradas / %d en migraciones"
          % (len(declared_tables), len(actual_tables)))
    print("  entidades Room   : %d declaradas / %d en el codigo"
          % (len(declared_entities), len(actual_entities)))
    print("  conjuntos totales: %d" % len(datasets))
    por_clase = {}
    for d in datasets:
        por_clase[d.get("classification")] = por_clase.get(d.get("classification"), 0) + 1
    print("  por clasificacion: %s"
          % ", ".join("%s=%d" % (k, v) for k, v in sorted(por_clase.items())))
    print("  problemas        : %d" % len(problems))

    if problems:
        print()
        for kind, what, why in problems:
            print("   [FALLA] %-22s %-28s %s" % (kind, what, why))
        print()
        print("El mapa de datos es un contrato, no documentacion: contracts/data-map.yaml")
        return 1

    print()
    criticos = [d.get("table") or d.get("entity") or d.get("dataset")
                for d in datasets if d.get("classification") == "CRITICO"]
    print("   Datos CRITICOS, todos fuera del servidor:")
    for c in criticos:
        print("     - %s" % c)
    print()
    print("Mapa de datos coherente con el esquema y el codigo.")
    return 0


if __name__ == "__main__":
    sys.exit(main())
