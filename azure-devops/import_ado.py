# -*- coding: utf-8 -*-
"""
Importa el backlog de SmartModa en Azure DevOps Boards CON jerarquía completa
(Epic -> Feature -> User Story), que es lo que la importación por CSV no hace.

No necesita instalar nada: solo librería estándar de Python.

Uso (PowerShell, que es la terminal por defecto de VS Code en Windows):
    $env:ADO_ORG     = "https://dev.azure.com/proyectoOutfit"
    $env:ADO_PROJECT = "Outfit"
    $env:ADO_PAT     = "el-token-que-copiaste"

    python import_ado.py --check              # verifica todo, sin crear nada
    python import_ado.py --setup-iterations   # crea Sprint 0..23
    python import_ado.py --dry-run            # muestra qué crearía
    python import_ado.py                      # crea de verdad

REQUISITOS
  - El PAT necesita alcance "Work Items (Read, write & manage)".
  - El proyecto debe usar el proceso AGILE. Con Basic no existe el tipo
    User Story ni los campos Acceptance Criteria y Story Points.
"""
import os, csv, io, json, argparse, base64
import urllib.request
import urllib.error
import urllib.parse

ORG = os.environ.get("ADO_ORG", "").rstrip("/")
PROJECT = os.environ.get("ADO_PROJECT", "Outfit")
PAT = os.environ.get("ADO_PAT", "")
API = "7.0"
HERE = os.path.dirname(os.path.abspath(__file__))
CSV_DIR = os.path.join(HERE, "csv")
PLACEHOLDERS = ("TU-ORGANIZACION", "tu-organizacion", "pega-aqui", "xxxx", "tu-token")


def auth_header():
    tok = base64.b64encode((":" + PAT).encode()).decode()
    return {"Authorization": "Basic " + tok}


def request(method, url, payload=None, ctype="application/json"):
    data = json.dumps(payload).encode("utf-8") if payload is not None else None
    req = urllib.request.Request(url, data=data, method=method)
    for k, v in auth_header().items():
        req.add_header(k, v)
    if data:
        req.add_header("Content-Type", ctype)
    try:
        with urllib.request.urlopen(req) as r:
            return json.loads(r.read().decode("utf-8"))
    except urllib.error.HTTPError as e:
        body = e.read().decode("utf-8", "replace")[:600]
        raise SystemExit("HTTP %s en %s\n%s" % (e.code, url, body))


def get(url):
    """GET tolerante: devuelve {'__error__': ...} en vez de abortar."""
    req = urllib.request.Request(url, method="GET")
    for k, v in auth_header().items():
        req.add_header(k, v)
    try:
        with urllib.request.urlopen(req) as r:
            return json.loads(r.read().decode("utf-8"))
    except urllib.error.HTTPError as e:
        return {"__error__": e.code}
    except Exception as e:
        return {"__error__": str(e)}


def read_csv(name):
    with io.open(os.path.join(CSV_DIR, name), encoding="utf-8-sig") as f:
        return list(csv.DictReader(f))


def key_of(title):
    """'US-1701 - Crear...' -> 'US-1701'"""
    return title.split(" ")[0].strip()


def has_placeholder(v):
    return any(p in (v or "") for p in PLACEHOLDERS)


def epic_tag(row):
    return next((t.strip() for t in row["Tags"].split(";") if t.strip().startswith("EP-")), None)


# ------------------------------------------------------------------ preflight
def preflight():
    """Comprueba todo lo que puede romper la importacion, sin crear nada."""
    ok = True
    line = "=" * 62
    print("VERIFICACION PREVIA")
    print(line)

    print()
    print("1. Variables de entorno")
    if not ORG or has_placeholder(ORG):
        print("   [FALLA] ADO_ORG vacia o con el texto de ejemplo")
        print('           $env:ADO_ORG = "https://dev.azure.com/tu-org-real"')
        return 1
    if not PAT or has_placeholder(PAT):
        print("   [FALLA] ADO_PAT vacio o con el texto de ejemplo")
        print('           $env:ADO_PAT = "el-token-que-copiaste-de-azure"')
        return 1
    print("   [ok] ADO_ORG     =", ORG)
    print("   [ok] ADO_PROJECT =", PROJECT)
    print("   [ok] ADO_PAT     = %s... (%d caracteres)" % (PAT[:4], len(PAT)))

    print()
    print("2. Proyecto y plantilla de proceso")
    pr = get("%s/_apis/projects/%s?includeCapabilities=true&api-version=%s"
             % (ORG, urllib.parse.quote(PROJECT), API))
    if "__error__" in pr:
        code = pr["__error__"]
        if code == 401:
            print("   [FALLA] 401 - el PAT no es valido, caduco, o le falta el alcance Work Items")
        elif code == 404:
            print("   [FALLA] 404 - no existe el proyecto '%s' en esa organizacion" % PROJECT)
            print("           Revisa el nombre exacto y que ADO_ORG apunte a tu organizacion")
        else:
            print("   [FALLA] error", code)
        return 1
    proc = (pr.get("capabilities", {}).get("processTemplate", {}) or {}).get("templateName", "?")
    print("   [ok] proyecto '%s' encontrado" % pr.get("name"))
    print("   [ok] plantilla de proceso:", proc)
    pl = str(proc).lower()
    if pl.startswith("basic"):
        print("   [FALLA] El proceso Basic NO sirve para este backlog:")
        print("           - no tiene el tipo 'User Story' (solo Epic / Issue / Task)")
        print("           - no tiene el campo 'Acceptance Criteria'  -> se perderian 653 criterios")
        print("           - no tiene el campo 'Story Points'         -> se perderian 1143 puntos")
        print()
        print("           Solucion (el proyecto esta vacio, asi que es gratis):")
        print("           Organization Settings > Boards > Process > Basic > pestana Projects")
        print("           > fila 'Outfit' > menu (...) > Change process > Agile")
        ok = False
    elif pl.startswith("scrum"):
        print("   [AVISO] Scrum usa 'Effort', no 'Story Points'.")
        print("           Cambia Microsoft.VSTS.Scheduling.StoryPoints por")
        print("           Microsoft.VSTS.Scheduling.Effort en patch_ops(), o se pierden los puntos.")
        ok = False
    elif not pl.startswith("agile"):
        print("   [AVISO] proceso no reconocido; verifica que exista el tipo 'User Story'")

    print()
    print("3. Campo de estimacion en User Story")
    fl = get("%s/%s/_apis/wit/workitemtypes/User%%20Story/fields?api-version=%s"
             % (ORG, urllib.parse.quote(PROJECT), API))
    if fl.get("__error__") == 404:
        print("   [FALLA] el tipo 'User Story' no existe en este proyecto")
        print("           Es la consecuencia del proceso Basic (ver punto 2)")
        ok = False
    elif "__error__" in fl:
        print("   [AVISO] no se pudo consultar (error %s); se omite" % fl["__error__"])
    else:
        refs = set(f.get("referenceName") for f in fl.get("value", []))
        if "Microsoft.VSTS.Scheduling.StoryPoints" in refs:
            print("   [ok] Story Points disponible")
        else:
            print("   [FALLA] User Story no tiene Story Points en este proceso")
            ok = False

    print()
    print("4. Iteraciones Sprint 0 a Sprint 23")
    it = get("%s/%s/_apis/wit/classificationnodes/iterations?$depth=3&api-version=%s"
             % (ORG, urllib.parse.quote(PROJECT), API))
    nombres = set()

    def walk(n):
        nombres.add(n.get("name", ""))
        for c in (n.get("children") or []):
            walk(c)

    if "__error__" not in it:
        walk(it)
    faltan = [i for i in range(24) if ("Sprint %d" % i) not in nombres]
    if not faltan:
        print("   [ok] las 24 iteraciones existen")
    else:
        muestra = ", ".join("Sprint %d" % i for i in faltan[:8])
        if len(faltan) > 8:
            muestra += " ..."
        print("   [FALLA] faltan %d iteraciones: %s" % (len(faltan), muestra))
        print("           Crealas de una vez con:  python import_ado.py --setup-iterations")
        ok = False

    print()
    print("5. Rutas de iteracion de los CSV")
    pref = set()
    for row in read_csv("03_historias.csv"):
        p = row.get("Iteration Path", "")
        if "\\" in p:
            pref.add(p.split("\\")[0])
    malas = [p for p in pref if p != PROJECT]
    if malas:
        print("   [FALLA] los CSV apuntan a '%s' pero el proyecto es '%s'"
              % (", ".join(malas), PROJECT))
        print("           Busca y reemplaza en la columna Iteration Path de los 3 CSV")
        ok = False
    else:
        print("   [ok] coinciden con el proyecto")

    print()
    print(line)
    print("LISTO PARA IMPORTAR" if ok else "CORRIGE LO MARCADO [FALLA] ANTES DE IMPORTAR")
    return 0 if ok else 1


# ------------------------------------------------------------------ iteraciones
def setup_iterations():
    """Crea Sprint 0..23 y las asigna al equipo por defecto."""
    if not ORG or has_placeholder(ORG) or not PAT or has_placeholder(PAT):
        raise SystemExit("Configura ADO_ORG y ADO_PAT reales antes de ejecutar esto.")

    print("Creando iteraciones Sprint 0..23 en '%s'" % PROJECT)
    print("=" * 62)

    base = "%s/%s/_apis/wit/classificationnodes/iterations?api-version=%s" % (
        ORG, urllib.parse.quote(PROJECT), API)

    existentes = {}
    it = get(base + "&$depth=3")
    if "__error__" not in it:
        for c in (it.get("children") or []):
            existentes[c.get("name")] = c.get("identifier")

    creadas = 0
    for i in range(24):
        nombre = "Sprint %d" % i
        if nombre in existentes:
            print("   [ya existe] %s" % nombre)
            continue
        r = request("POST", base, {"name": nombre})
        existentes[nombre] = r.get("identifier")
        creadas += 1
        print("   [creada]    %s" % nombre)

    # asignar al equipo por defecto
    equipo = "%s Team" % PROJECT
    print()
    print("Asignando al equipo '%s'" % equipo)
    turl = "%s/%s/%s/_apis/work/teamsettings/iterations?api-version=%s" % (
        ORG, urllib.parse.quote(PROJECT), urllib.parse.quote(equipo), API)
    ya = set()
    cur = get(turl)
    if "__error__" not in cur:
        ya = set(x.get("name") for x in cur.get("value", []))
    asignadas = 0
    for i in range(24):
        nombre = "Sprint %d" % i
        if nombre in ya:
            continue
        ident = existentes.get(nombre)
        if not ident:
            continue
        try:
            request("POST", turl, {"id": ident})
            asignadas += 1
        except SystemExit as e:
            print("   [aviso] no se pudo asignar %s: %s" % (nombre, str(e)[:80]))

    print()
    print("=" * 62)
    print("Creadas %d, asignadas al equipo %d." % (creadas, asignadas))
    print("Verifica con: python import_ado.py --check")
    return 0


# ------------------------------------------------------------------ creacion
def patch_ops(row, parent_id=None):
    """Construye el cuerpo JSON-Patch para crear un work item."""
    ops = []

    def add(path, value):
        if value not in (None, ""):
            ops.append({"op": "add", "path": "/fields/" + path, "value": value})

    add("System.Title", row["Title"][:255])
    add("System.Description", row.get("Description"))
    add("Microsoft.VSTS.Common.AcceptanceCriteria", row.get("Acceptance Criteria"))
    add("Microsoft.VSTS.Common.Priority", int(row["Priority"]) if row.get("Priority") else None)
    if row.get("Story Points"):
        add("Microsoft.VSTS.Scheduling.StoryPoints", float(row["Story Points"]))
    add("System.IterationPath", row.get("Iteration Path"))
    add("System.AreaPath", row.get("Area Path"))
    add("System.Tags", row.get("Tags"))
    if parent_id:
        ops.append({"op": "add", "path": "/relations/-", "value": {
            "rel": "System.LinkTypes.Hierarchy-Reverse",
            "url": "%s/_apis/wit/workItems/%d" % (ORG, parent_id)}})
    return ops


def create(row, parent_id=None, dry=False):
    wit = row["Work Item Type"]
    if dry:
        print("   [dry] %-11s %-70s parent=%s" % (wit, row["Title"][:70], parent_id))
        return -1
    url = "%s/%s/_apis/wit/workitems/$%s?api-version=%s" % (
        ORG, urllib.parse.quote(PROJECT), urllib.parse.quote(wit), API)
    res = request("POST", url, patch_ops(row, parent_id), "application/json-patch+json")
    return res["id"]


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("--check", action="store_true",
                    help="verifica conexion, proyecto, proceso e iteraciones sin crear nada")
    ap.add_argument("--setup-iterations", action="store_true",
                    help="crea Sprint 0..23 y las asigna al equipo")
    ap.add_argument("--dry-run", action="store_true")
    ap.add_argument("--only", choices=["epics", "features", "stories"],
                    help="importar solo una capa")
    args = ap.parse_args()

    if args.check:
        raise SystemExit(preflight())

    if args.setup_iterations:
        raise SystemExit(setup_iterations())

    if not args.dry_run:
        if not ORG or not PAT:
            raise SystemExit("Faltan ADO_ORG o ADO_PAT. Ver la cabecera de este archivo.")
        if has_placeholder(ORG) or has_placeholder(PAT):
            raise SystemExit(
                "ADO_ORG o ADO_PAT siguen con el texto de ejemplo.\n"
                "Reemplazalos por tus valores reales y ejecuta primero:\n"
                "    python import_ado.py --check")

    print("Organizacion:", ORG or "(dry-run)")
    print("Proyecto    :", PROJECT)
    print("Modo        :", "DRY-RUN (no crea nada)" if args.dry_run else "CREACION REAL")
    print()

    ids = {}
    state_file = os.path.join(HERE, ".ado_ids.json")
    if os.path.exists(state_file):
        ids = json.load(io.open(state_file, encoding="utf-8"))
        print("Reanudando: %d work items ya creados\n" % len(ids))

    def save():
        json.dump(ids, io.open(state_file, "w", encoding="utf-8"), indent=1)

    if args.only in (None, "epics"):
        print("1/3  Epicas")
        for row in read_csv("01_epicas.csv"):
            k = key_of(row["Title"])
            if k in ids:
                continue
            ids[k] = create(row, None, args.dry_run)
            if not args.dry_run:
                save()
        print("     %d epicas\n" % len([k for k in ids if k.startswith("EP-")]))

    if args.only in (None, "features"):
        print("2/3  Features (RF + RNF)")
        for row in read_csv("02_features_rf_rnf.csv"):
            k = key_of(row["Title"])
            if k in ids:
                continue
            ids[k] = create(row, ids.get(epic_tag(row)), args.dry_run)
            if not args.dry_run:
                save()
        print("     %d features\n" % len([k for k in ids if k.startswith(("RF-", "RNF-"))]))

    if args.only in (None, "stories"):
        print("3/3  Historias y habilitadores")
        for row in read_csv("03_historias.csv"):
            k = key_of(row["Title"])
            if k in ids:
                continue
            ids[k] = create(row, ids.get(epic_tag(row)), args.dry_run)
            if not args.dry_run:
                save()
        print("     %d historias\n" % len([k for k in ids if k.startswith(("US-", "EN-", "SP-"))]))

    if not args.dry_run:
        save()
        print("Listo. Mapa de IDs guardado en .ado_ids.json")
        print("Si algo falla, vuelve a ejecutar: se salta lo ya creado.")


if __name__ == "__main__":
    main()
