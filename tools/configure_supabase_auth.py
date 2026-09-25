#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
EN-1510 · Configura los limites de tasa de Supabase Auth via Management API.

Por que un script y no clics en el panel: la configuracion de seguridad que solo existe en
una interfaz no se puede revisar, ni versionar, ni reponer si alguien la cambia. Aqui queda
en git, con el motivo de cada valor al lado.

Uso:
    set SUPABASE_ACCESS_TOKEN=sbp_...     (Account > Access Tokens)
    set SUPABASE_PROJECT_REF=abcdefghij   (el subdominio del proyecto)

    python tools/configure_supabase_auth.py --check    # muestra lo actual, no cambia nada
    python tools/configure_supabase_auth.py            # aplica

El token de acceso personal NO es la clave anon ni la service_role: es de tu cuenta y sirve
para administrar proyectos. No debe acabar en ningun archivo del repositorio.
"""
import argparse
import json
import os
import sys
import urllib.error
import urllib.request

TOKEN = os.environ.get("SUPABASE_ACCESS_TOKEN", "").strip()
PROJECT = os.environ.get("SUPABASE_PROJECT_REF", "").strip()
API = "https://api.supabase.com/v1"

# Cada valor con su motivo. Un numero sin justificacion es un numero que nadie se atreve a
# cambiar despues porque no sabe de donde salio.
CONFIG = {
    # Alta de cuentas por hora y por IP. Frena el registro masivo para sondear correos.
    "rate_limit_anonymous_users": 30,

    # Verificaciones de OTP por hora. El OTP es el vector barato de fuerza bruta: seis digitos
    # son un millon de combinaciones, y sin limite se agotan en minutos.
    "rate_limit_verify": 30,

    # rate_limit_email_sent NO se toca a proposito.
    # El valor por defecto del plan gratuito es 2/hora, mas estricto que cualquier objetivo
    # razonable. Subirlo seria aflojar un limite en un script que endurece, y ademas no
    # funcionaria: ese tope viene del SMTP compartido de Supabase. Si algun dia hace falta
    # mas volumen, la solucion es SMTP propio, no un numero mayor aqui.

    # Recuperaciones de contrasena por hora.
    "rate_limit_token_refresh": 150,

    # Duracion del enlace de recuperacion. Una hora es de sobra: cuanto mas vive, mas tiempo
    # esta expuesto en el buzon de correo.
    "mailer_otp_exp": 3600,

    # Longitud minima de contrasena. Por debajo de 12 el espacio de busqueda es abordable.
    "password_min_length": 12,

    # password_required_characters se deja SIN configurar, tambien a proposito.
    # Las reglas de composicion (una mayuscula, un digito, un simbolo) producen contrasenas
    # predecibles del tipo Password1! y empeoran la usabilidad sin subir la entropia real.
    # NIST SP 800-63B recomienda explicitamente no usarlas y apoyarse en longitud minima mas
    # lista de contrasenas filtradas, que es justo lo que hacen las dos lineas de arriba.

    # Rechaza contrasenas que aparecen en filtraciones conocidas. Supabase lo consulta con
    # k-anonimato: envia un prefijo del hash, nunca la contrasena.
    "password_hibp_enabled": True,

    # Sesion: refresh rotativo y deteccion de reuso. Si un refresh ya rotado se vuelve a usar,
    # es senal de robo de token y se invalida toda la familia.
    "refresh_token_rotation_enabled": True,
    "security_refresh_token_reuse_interval": 10,

    # Confirmar el correo antes de conceder sesion. Sin esto, cualquiera registra cuentas con
    # correos ajenos.
    "mailer_autoconfirm": False,
}


# Ajustes que se informan pero NO se aplican. Se configuran en el panel porque llevan
# secretos del proveedor de CAPTCHA, y un PATCH generico los dejaria a medias.
REPORT_ONLY = {
    "security_captcha_enabled": (True, "CAPTCHA activo. En plan gratuito es la mejor defensa "
                                       "disponible: corre en el endpoint de Supabase"),
    "security_captcha_provider": (None, "hcaptcha o turnstile"),
    "rate_limit_otp": (None, "solicitudes de OTP por hora"),
    "rate_limit_sms_sent": (None, "SMS por hora"),
    "security_manual_linking_enabled": (False, "vinculacion manual de identidades"),
    "security_update_password_require_reauthentication": (
        True, "cambiar contrasena exige volver a autenticarse"),
}


def request(method, path, payload=None):
    url = API + path
    data = json.dumps(payload).encode() if payload is not None else None
    req = urllib.request.Request(url, data=data, method=method)
    req.add_header("Authorization", "Bearer " + TOKEN)
    req.add_header("Content-Type", "application/json")
    try:
        with urllib.request.urlopen(req) as r:
            return json.loads(r.read().decode())
    except urllib.error.HTTPError as e:
        body = e.read().decode("utf-8", "replace")[:500]
        sys.exit("HTTP %s en %s\n%s" % (e.code, path, body))


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("--check", action="store_true", help="mostrar lo actual sin cambiar nada")
    args = ap.parse_args()

    if not TOKEN or not PROJECT:
        sys.exit(
            "Faltan SUPABASE_ACCESS_TOKEN o SUPABASE_PROJECT_REF.\n"
            "  $env:SUPABASE_ACCESS_TOKEN = \"sbp_...\"\n"
            "  $env:SUPABASE_PROJECT_REF  = \"abcdefghij\"")

    print("Proyecto: %s" % PROJECT)
    print("=" * 66)

    actual = request("GET", "/projects/%s/config/auth" % PROJECT)

    print()
    print("%-42s %-14s %s" % ("AJUSTE", "ACTUAL", "OBJETIVO"))
    print("-" * 66)
    cambios = {}
    for key, objetivo in CONFIG.items():
        ahora = actual.get(key)
        igual = str(ahora) == str(objetivo)
        if not igual:
            cambios[key] = objetivo
        marca = "" if igual else "  <-- cambia"
        print("%-42s %-14s %s%s" % (key, str(ahora)[:14], str(objetivo)[:20], marca))

    print()
    print("VERIFICACIONES (se informan, no se aplican desde aqui)")
    print("-" * 66)
    avisos = []
    for key, (esperado, nota) in REPORT_ONLY.items():
        ahora = actual.get(key)
        if esperado is None:
            marca = "  "
        elif str(ahora) == str(esperado):
            marca = "ok"
        else:
            marca = "!!"
            avisos.append((key, ahora, esperado, nota))
        print("  [%s] %-46s %s" % (marca, key, str(ahora)[:18]))
    if avisos:
        print()
        for key, ahora, esperado, nota in avisos:
            print("  !! %s = %s, deberia ser %s" % (key, ahora, esperado))
            print("     %s" % nota)
            print("     Se configura en el panel: Authentication > Attack Protection")

    print()
    if not cambios:
        print("Todo lo aplicable ya esta como debe.")
        return 0

    if args.check:
        print("%d ajustes por cambiar. Ejecuta sin --check para aplicarlos." % len(cambios))
        return 0

    print("Aplicando %d cambios..." % len(cambios))
    ruta = "/projects/%s/config/auth" % PROJECT
    r = request("PATCH", ruta, cambios, tolerar_error=True)

    bloqueados = []
    if "__error__" in r:
        # Un solo ajuste que el plan no permite tumba el PATCH entero. Se reintenta uno a
        # uno para aplicar lo que si se puede: abortar dejaria sin aplicar ajustes
        # perfectamente validos por culpa de otro.
        print("  El lote fallo (HTTP %s). Reintentando ajuste por ajuste." % r["__error__"])
        for key, valor in cambios.items():
            ri = request("PATCH", ruta, {key: valor}, tolerar_error=True)
            if "__error__" in ri:
                codigo = ri["__error__"]
                try:
                    motivo = json.loads(ri["__body__"]).get("message", ri["__body__"])
                except Exception:
                    motivo = ri["__body__"]
                # 402 Payment Required: la funcion existe pero el plan no la incluye.
                etiqueta = "PLAN" if codigo == 402 else "ERROR"
                print("  [%-5s] %-34s %s" % (etiqueta, key, motivo[:64]))
                bloqueados.append((key, motivo))
            else:
                print("  [ok   ] %s" % key)
    else:
        print("  [ok   ] los %d cambios se aplicaron" % len(cambios))

    if bloqueados:
        print()
        print("=" * 66)
        print("AJUSTES QUE EL PLAN NO PERMITE")
        for key, motivo in bloqueados:
            print("  %s" % key)
            print("    %s" % motivo[:90])
        print()
        print("No es un fallo de configuracion: la funcion existe pero requiere otro plan.")
        print("Las compensaciones acordadas estan en docs/sprints/sprint-01.md.")

    print()
    print("Lo que esto NO configura:")
    print("  Auth Hooks > Password Verification Attempt  (requiere plan Team, R-33)")
    print("  Attack Protection > CAPTCHA                 (lleva secretos del proveedor)")
    return 0


if __name__ == "__main__":
    sys.exit(main())
