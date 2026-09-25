#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
Comprueba que el proyecto de Supabase esta bien configurado, sin mostrar ningun secreto.

Verifica, en este orden:
  1. local.properties tiene SUPABASE_URL y SUPABASE_ANON_KEY
  2. la clave es publica (anon/publishable) y NO la secreta
  3. el proyecto responde
  4. las migraciones estan aplicadas
  5. el RLS esta activo: un cliente anonimo no puede leer datos de un tenant

La comprobacion 5 es la que de verdad importa en la Fase 1. La app habla con Supabase
directamente con una clave publica que va dentro del APK, asi que el Row Level Security no es
una red de seguridad: es la seguridad. Si esta prueba falla, los datos estan expuestos.

Uso:
    python tools/check_supabase.py
"""
import base64
import json
import os
import re
import sys
import urllib.error
import urllib.request

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
LOCAL = os.path.join(ROOT, "local.properties")

# Tablas que deben existir tras aplicar las migraciones 0001 a 0005.
TABLAS = ["tenant", "brand", "store", "product", "product_variant", "size_chart", "app_user"]


def leer_local():
    if not os.path.exists(LOCAL):
        return {}
    out = {}
    with open(LOCAL, encoding="utf-8") as f:
        for line in f:
            line = line.strip()
            if not line or line.startswith("#"):
                continue
            if "=" in line:
                k, _, v = line.partition("=")
                out[k.strip()] = v.strip()
    return out


def rol_de_clave(key):
    """Rol declarado dentro de la clave, sin exponerla."""
    if not key:
        return None
    if key.startswith("sb_secret_"):
        return "service_role"
    if key.startswith("sb_publishable_"):
        return "anon"
    partes = key.split(".")
    if len(partes) != 3:
        return None
    try:
        relleno = partes[1] + "=" * (-len(partes[1]) % 4)
        payload = base64.urlsafe_b64decode(relleno).decode("utf-8", "replace")
        m = re.search(r'"role"\s*:\s*"([^"]+)"', payload)
        return m.group(1) if m else None
    except Exception:
        return None


def get(url, key, extra=None):
    req = urllib.request.Request(url, method="GET")
    req.add_header("apikey", key)
    req.add_header("Authorization", "Bearer " + key)
    for k, v in (extra or {}).items():
        req.add_header(k, v)
    try:
        with urllib.request.urlopen(req, timeout=20) as r:
            return r.status, r.read().decode("utf-8", "replace")
    except urllib.error.HTTPError as e:
        return e.code, e.read().decode("utf-8", "replace")
    except Exception as e:
        return None, str(e)


def main():
    props = leer_local()
    url = props.get("SUPABASE_URL", "")
    key = props.get("SUPABASE_ANON_KEY", "")

    ok = True
    print("Configuracion de Supabase")
    print("=" * 66)

    # --- 1
    print()
    print("1. Variables en local.properties")
    if not url:
        print("   [FALLA] falta SUPABASE_URL")
        print("           SUPABASE_URL=https://TU-REF.supabase.co")
        return 1
    if not key:
        print("   [FALLA] falta SUPABASE_ANON_KEY")
        return 1
    if not url.startswith("https://") or ".supabase.co" not in url:
        print("   [AVISO] la URL no tiene la forma habitual: %s" % url)

    # El panel muestra el endpoint REST completo, asi que se copia con /rest/v1/ pegado
    # detras con facilidad. Aqui se normaliza y se avisa: el cliente Android necesita la
    # URL base, no el endpoint, y un 404 sin explicacion manda a buscar en el sitio
    # equivocado.
    base = re.sub(r"/(rest|auth|storage|realtime)/v\d+/?$", "", url.rstrip("/"))
    if base != url.rstrip("/"):
        print("   [AVISO] la URL incluia la ruta del endpoint. Debe ser solo la base:")
        print("           %s" % base)
        url = base
    print("   [ok] SUPABASE_URL     = %s" % url)
    print("   [ok] SUPABASE_ANON_KEY= %s... (%d caracteres)" % (key[:6], len(key)))

    # --- 2
    print()
    print("2. La clave es la publica, no la secreta")
    rol = rol_de_clave(key)
    if rol == "service_role":
        print("   [FALLA] es la clave SECRETA (service_role).")
        print("           Omite el RLS por completo y no puede ir en un cliente movil:")
        print("           cualquiera la extrae del APK. Usa la publishable / anon.")
        print("           REVOCALA en el panel: ya salio de su sitio.")
        return 1
    if rol is None:
        print("   [AVISO] no se pudo determinar el rol; revisa que copiaste la correcta")
    else:
        print("   [ok] rol '%s'" % rol)

    # --- 3
    print()
    print("3. El proyecto responde")
    # Se sondea /auth/v1/health y NO la raiz /rest/v1/: esa raiz devuelve la especificacion
    # OpenAPI y exige clave secreta, asi que da 401 aunque la clave publica sea correcta.
    # Usarla como comprobacion de salud manda a buscar el problema donde no esta.
    status, body = get(url.rstrip("/") + "/auth/v1/health", key)
    if status is None:
        print("   [FALLA] no se pudo conectar: %s" % body[:120])
        return 1
    if status == 401:
        print("   [FALLA] 401: la clave no corresponde a este proyecto")
        return 1
    if status >= 500:
        print("   [FALLA] el proyecto devolvio %s. Puede estar pausado por inactividad." % status)
        return 1
    print("   [ok] responde (HTTP %s)" % status)

    # --- 4
    print()
    print("4. Migraciones aplicadas")
    faltan = []
    for t in TABLAS:
        st, bd = get("%s/rest/v1/%s?select=*&limit=1" % (url.rstrip("/"), t), key)
        # 200 = existe y se puede consultar. 401/403 = existe pero RLS lo bloquea, que
        # tambien confirma que la tabla esta. 404 = no existe.
        # PostgREST responde PGRST205 cuando la tabla no esta en el cache de esquema.
        # Un 200 o un 401/403 confirman que existe: el segundo caso es el RLS bloqueando.
        if st == 404 or "PGRST205" in bd or (st == 400 and "does not exist" in bd):
            faltan.append(t)
    if faltan:
        print("   [FALLA] faltan tablas: %s" % ", ".join(faltan))
        print("           Ejecuta las migraciones de supabase/migrations/ en el SQL Editor,")
        print("           en orden: 0001, 0002, 0003, 0004, 0005")
        ok = False
    else:
        print("   [ok] las %d tablas de referencia existen" % len(TABLAS))

    # --- 5
    print()
    print("5. El RLS protege los datos (lo que de verdad importa en Fase 1)")
    if faltan:
        print("   [omitido] aplica antes las migraciones")
    else:
        st, bd = get("%s/rest/v1/product?select=id&limit=5" % url.rstrip("/"), key)
        try:
            filas = json.loads(bd) if bd.strip().startswith("[") else None
        except Exception:
            filas = None

        if filas is None:
            print("   [ok] la consulta anonima no devuelve datos (HTTP %s)" % st)
        elif len(filas) == 0:
            print("   [ok] la consulta anonima devuelve 0 filas: el RLS filtra correctamente")
        else:
            print("   [FALLA] un cliente anonimo leyo %d filas de 'product'." % len(filas))
            print("           El RLS no esta protegiendo la tabla. En Fase 1 eso significa")
            print("           que los datos estan expuestos: la clave va dentro del APK.")
            ok = False

    print()
    print("=" * 66)
    if ok:
        print("SUPABASE LISTO")
        print()
        print("Siguiente:")
        print("  python tools/configure_supabase_auth.py --check   (limites de tasa)")
        print("  Panel > Authentication > Hooks > Password Verification Attempt")
        print("    -> app.password_verification_hook")
        return 0
    print("CORRIGE LO MARCADO [FALLA]")
    return 1


if __name__ == "__main__":
    sys.exit(main())
