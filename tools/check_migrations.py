#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
AUT-22 · Verifica que toda tabla de negocio nueva nace aislada por tenant.

ADR-0002: si una tabla se crea sin tenant_id, anadirlo despues obliga a migrar los datos y
reescribir los repositorios. Y si se crea sin politica RLS, en la Fase 1 queda directamente
expuesta: la app movil habla con Supabase con la clave anon, que es publica.

Este control no depende de que nadie se acuerde. Rompe el despliegue.

Comprueba, para cada tabla creada en supabase/migrations/:
  1. tiene columna tenant_id NOT NULL  (o esta exenta y declarada)
  2. tiene RLS habilitado Y forzado
  3. tiene al menos una politica con WITH CHECK, no solo USING

El punto 3 es el que mas se olvida: una politica solo con USING deja leer bien pero permite
insertar filas en otro tenant, y eso no se nota hasta que ya paso.

Uso:
    python tools/check_migrations.py
"""
import os
import re
import sys

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
MIGRATIONS = os.path.join(ROOT, "supabase", "migrations")

# Tablas que legitimamente no llevan tenant_id, con el motivo.
# Cada exencion es una decision, no un descuido: si algo entra aqui sin razon, se nota.
EXEMPT = {
    "tenant": "es la propia empresa; se filtra por su id, no por una columna tenant_id",
    "app_user": "el usuario consumidor pertenece a la plataforma, no a una empresa (doc 10 §3)",
    "plan": "catalogo global de planes de la plataforma",
    "feature_catalog": "catalogo global de funcionalidades",
    "category": "tenant_id nullable: la taxonomia canonica es compartida",
    "color": "tenant_id nullable: la paleta canonica es compartida",
    "consent": "el consentimiento es del usuario frente a la plataforma, no frente a una empresa",
    "erasure_task": "deriva del consentimiento; mismo ambito",
    "auth_attempt": "identidad de plataforma; ademas no la lee ningun cliente",
}

CREATE_TABLE = re.compile(
    r"create\s+table\s+(?:if\s+not\s+exists\s+)?([a-z_][a-z0-9_]*)\s*\((.*?)\n\);",
    re.IGNORECASE | re.DOTALL,
)


def read_all():
    if not os.path.isdir(MIGRATIONS):
        sys.exit("No existe %s" % MIGRATIONS)
    files = sorted(f for f in os.listdir(MIGRATIONS) if f.endswith(".sql"))
    if not files:
        sys.exit("No hay migraciones en %s" % MIGRATIONS)
    text = ""
    for f in files:
        text += open(os.path.join(MIGRATIONS, f), encoding="utf-8").read() + "\n"
    return files, text


def main():
    files, sql = read_all()
    low = sql.lower()

    # apply_tenant_rls() aplica enable + force + politica con USING y WITH CHECK de una vez.
    helper_tables = set(re.findall(r"app\.apply_tenant_rls\(\s*'([a-z_][a-z0-9_]*)'\s*\)", low))

    problems = []
    tables = []

    for match in CREATE_TABLE.finditer(sql):
        name = match.group(1).lower()
        body = match.group(2).lower()
        tables.append(name)

        # 1. tenant_id
        if name not in EXEMPT:
            if not re.search(r"\btenant_id\s+uuid\b", body):
                problems.append((name, "no tiene columna tenant_id"))
            elif not re.search(r"\btenant_id\s+uuid\s+not\s+null\b", body):
                problems.append((name, "tenant_id existe pero admite NULL"))

        # 2 y 3. RLS
        if name in helper_tables:
            continue  # el helper garantiza enable, force, using y with check

        has_enable = re.search(r"alter\s+table\s+%s\s+enable\s+row\s+level\s+security" % name, low)
        has_force = re.search(r"alter\s+table\s+%s\s+force\s+row\s+level\s+security" % name, low)
        policies = re.findall(
            r"create\s+policy\s+[a-z_0-9]+\s+on\s+%s\b(.*?)(?=create\s|alter\s|select\s+app\.|\Z)" % name,
            low, re.DOTALL)

        if not has_enable:
            problems.append((name, "sin ENABLE ROW LEVEL SECURITY"))
        if not has_force:
            problems.append((name, "sin FORCE ROW LEVEL SECURITY (el rol dueno ignoraria la politica)"))
        if not policies:
            problems.append((name, "sin ninguna politica RLS"))
        elif name not in EXEMPT:
            # WITH CHECK solo tiene sentido donde se escribe. Una politica FOR SELECT no lo
            # admite, asi que exigirselo seria un falso positivo.
            escribibles = [p for p in policies if not re.search(r"^\s*for\s+select", p)]
            if escribibles and not any("with check" in p for p in escribibles):
                problems.append(
                    (name, "politica de escritura sin WITH CHECK: dejaria insertar en otro tenant"))

    print("Aislamiento por tenant en migraciones")
    print("=" * 62)
    print("  archivos  : %d (%s)" % (len(files), ", ".join(files)))
    print("  tablas    : %d" % len(tables))
    print("  exentas   : %d" % len([t for t in tables if t in EXEMPT]))
    print("  problemas : %d" % len(problems))

    if problems:
        print()
        for name, why in problems:
            print("   [FALLA] %-24s %s" % (name, why))
        print()
        print("ADR-0002: toda tabla de negocio nace con tenant_id NOT NULL y politica RLS.")
        print("Si la tabla no debe llevarlo, declarala en EXEMPT de este archivo con su motivo.")
        return 1

    print()
    for t in tables:
        marca = "exenta: %s" % EXEMPT[t] if t in EXEMPT else "aislada por tenant"
        print("   [ok] %-22s %s" % (t, marca))
    print()
    print("Todas las tablas cumplen ADR-0002.")
    return 0


if __name__ == "__main__":
    sys.exit(main())
