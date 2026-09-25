#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
Ejecuta las migraciones y las pruebas SQL contra un PostgreSQL real en Docker.

Por que contra Postgres de verdad y no simulado: lo que se prueba aqui son politicas RLS,
reglas, funciones SECURITY DEFINER y aritmetica de intervalos. Nada de eso se comporta igual
en otro motor, y el Auth Hook es un control de seguridad: escribirlo sin ejecutarlo seria
confiar en que esta bien.

Uso:
    python tools/run_sql_tests.py            # arranca, prueba y limpia
    python tools/run_sql_tests.py --keep     # deja el contenedor vivo para depurar
"""
import argparse
import os
import subprocess
import sys
import time
import uuid

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
MIGRATIONS = os.path.join(ROOT, "supabase", "migrations")
TESTS = os.path.join(ROOT, "supabase", "tests")
IMAGE = "postgres:16-alpine"
PASSWORD = "pruebas-locales"

# Supabase trae estos roles y esquemas de serie. Al probar contra un Postgres desnudo hay que
# crearlos, o las migraciones fallan por algo que en produccion si existe.
BOOTSTRAP = """
create schema if not exists auth;
create extension if not exists pgcrypto;

do $$ begin
    if not exists (select 1 from pg_roles where rolname = 'supabase_auth_admin') then
        create role supabase_auth_admin noinherit;
    end if;
    if not exists (select 1 from pg_roles where rolname = 'authenticated') then
        create role authenticated noinherit;
    end if;
    if not exists (select 1 from pg_roles where rolname = 'anon') then
        create role anon noinherit;
    end if;
end $$;

create table if not exists auth.users (
    id uuid primary key default gen_random_uuid(),
    email text
);

-- En Supabase devuelve el usuario del JWT. Aqui basta con que exista para que las politicas
-- compilen: lo que se prueba en este archivo es el hook, no el aislamiento por usuario.
create or replace function auth.uid() returns uuid
language sql stable as $$ select nullif(current_setting('request.jwt.claim.sub', true), '')::uuid $$;
"""


def run(cmd, **kw):
    return subprocess.run(cmd, shell=True, capture_output=True, text=True, **kw)


def psql(container, sql=None, path=None, quiet=False):
    base = "docker exec -i %s psql -v ON_ERROR_STOP=1 -U postgres -d postgres" % container
    if quiet:
        base += " -q"
    if path:
        with open(path, encoding="utf-8") as f:
            data = f.read()
    else:
        data = sql
    p = subprocess.run(base, shell=True, input=data, capture_output=True, text=True)
    return p


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("--keep", action="store_true", help="no borrar el contenedor al terminar")
    args = ap.parse_args()

    if run("docker info").returncode != 0:
        sys.exit("Docker no esta disponible o no esta arrancado.")

    container = "smartmoda-sqltest-%s" % uuid.uuid4().hex[:8]
    print("Levantando %s (%s)" % (container, IMAGE))
    p = run('docker run -d --name %s -e POSTGRES_PASSWORD=%s %s' % (container, PASSWORD, IMAGE))
    if p.returncode != 0:
        sys.exit("No se pudo arrancar el contenedor:\n%s" % p.stderr)

    try:
        # Esperar a que acepte conexiones
        for _ in range(60):
            if run("docker exec %s pg_isready -U postgres" % container).returncode == 0:
                break
            time.sleep(1)
        else:
            sys.exit("Postgres no acepto conexiones a tiempo")
        print("Postgres listo\n")

        print("Preparando roles y esquema auth de Supabase")
        r = psql(container, sql=BOOTSTRAP, quiet=True)
        if r.returncode != 0:
            print(r.stderr)
            return 1

        print("\nAplicando migraciones")
        for name in sorted(os.listdir(MIGRATIONS)):
            if not name.endswith(".sql"):
                continue
            r = psql(container, path=os.path.join(MIGRATIONS, name), quiet=True)
            if r.returncode != 0:
                print("  [FALLA] %s" % name)
                print(r.stderr.strip()[:1500])
                return 1
            print("  [ok]    %s" % name)

        print("\nEjecutando pruebas")
        failed = False
        for name in sorted(os.listdir(TESTS)):
            if not name.endswith(".sql"):
                continue
            r = psql(container, path=os.path.join(TESTS, name))
            salida = (r.stdout or "") + (r.stderr or "")
            for line in salida.splitlines():
                line = line.strip()
                if line.startswith("NOTICE:"):
                    print("  " + line.replace("NOTICE:  ", ""))
                elif "FALLA" in line or "ERROR" in line:
                    print("  " + line)
            if r.returncode != 0:
                failed = True

        print()
        if failed:
            print("=" * 62)
            print("PRUEBAS SQL FALLIDAS")
            return 1
        print("=" * 62)
        print("PRUEBAS SQL EN VERDE")
        return 0

    finally:
        if args.keep:
            print("\nContenedor conservado: %s" % container)
            print("  docker exec -it %s psql -U postgres" % container)
        else:
            run("docker rm -f %s" % container)


if __name__ == "__main__":
    sys.exit(main())
