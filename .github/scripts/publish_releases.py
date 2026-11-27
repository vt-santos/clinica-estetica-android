#!/usr/bin/env python3
"""Cria no GitHub uma Release para cada versão do CHANGELOG.md que ainda não tem uma.

- Versões com "-beta" viram pré-release (🧪 Beta); as demais viram patch (🩹 Patch).
- Versões antigas trazem "<!-- commit: abc1234 -->" e a tag aponta para esse commit.
  As outras apontam para o commit atual do main.
- Para a versão do commit atual, o APK de teste é gerado e anexado à release.

Uso: python3 .github/scripts/publish_releases.py [--dry-run]
"""
import re
import subprocess
import sys
import tempfile
from datetime import date
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]
REPO_URL = "https://github.com/vt-santos/clinica-estetica-android"
HEADING = re.compile(r"^## \[(?P<version>[^\]]+)\] - (?P<date>\d{4}-\d{2}-\d{2})\s*$")
COMMIT = re.compile(r"^<!-- commit: (?P<sha>[0-9a-f]{7,40}) -->\s*$")
DRY_RUN = "--dry-run" in sys.argv


def parse_changelog():
    """Devolve as versões da mais antiga para a mais nova."""
    versions, current = [], None
    for line in (ROOT / "CHANGELOG.md").read_text(encoding="utf-8").splitlines():
        heading = HEADING.match(line)
        if heading:
            current = {"version": heading["version"], "date": date.fromisoformat(heading["date"]),
                       "commit": None, "body": []}
            versions.append(current)
        elif current is None:
            continue
        elif line.startswith("[") and "]: http" in line:
            current = None  # links de comparação do fim do arquivo
        elif COMMIT.match(line):
            current["commit"] = COMMIT.match(line)["sha"]
        else:
            current["body"].append(line)
    return list(reversed(versions))


def run(*args, check=True, capture=True):
    result = subprocess.run(args, cwd=ROOT, text=True, check=False,
                            capture_output=capture)
    if check and result.returncode != 0:
        raise SystemExit(f"Falhou: {' '.join(args)}\n{result.stderr}")
    return result


def release_exists(tag):
    if DRY_RUN:
        return False
    return run("gh", "release", "view", tag, check=False).returncode == 0


def notes(entry, previous):
    beta = "-beta" in entry["version"]
    kind = "🧪 **Versão beta**: funções novas, ainda em teste" if beta \
        else "🩹 **Patch**: correções e ajustes, sem função nova"
    lines = [f"> {kind} · lançada em **{entry['date']:%d/%m/%Y}**", ""]
    lines += "\n".join(entry["body"]).strip().splitlines()
    lines += ["", "---", ""]
    if previous:
        lines.append(f"🔍 **Mudanças no código:** [{previous} → v{entry['version']}]"
                     f"({REPO_URL}/compare/{previous}...v{entry['version']})  ")
    lines.append(f"📋 **Histórico completo:** [CHANGELOG.md]({REPO_URL}/blob/main/CHANGELOG.md)")
    return "\n".join(lines) + "\n"


def title(entry):
    beta = "-beta" in entry["version"]
    icon, kind = ("🧪", "Beta") if beta else ("🩹", "Patch")
    return f"{icon} v{entry['version']} · {kind} · {entry['date']:%d/%m/%Y}"


def build_apk(tag):
    """APK de teste da versão atual. Se o build falhar, a release continua sem o APK."""
    print(f"Gerando o APK de {tag}...")
    if run("bash", "gradlew", "--no-daemon", "assembleDebug", capture=False,
           check=False).returncode != 0:
        print("::warning::Não foi possível gerar o APK; a release foi publicada sem ele.")
        return
    apk = ROOT / "app/build/outputs/apk/debug/app-debug.apk"
    named = ROOT / f"ClinicaEstetica-{tag}.apk"
    named.write_bytes(apk.read_bytes())
    run("gh", "release", "upload", tag, f"{named}#📱 APK de teste ({tag})", "--clobber")


def main():
    head = run("git", "rev-parse", "HEAD").stdout.strip()
    previous = None
    for entry in parse_changelog():
        tag = f"v{entry['version']}"
        target = run("git", "rev-parse", entry["commit"]).stdout.strip() \
            if entry["commit"] else head
        if release_exists(tag):
            print(f"{tag}: já publicada")
            previous = tag
            continue
        print(f"{tag}: publicando no commit {target[:7]}")
        if DRY_RUN:
            print(title(entry))
            print(notes(entry, previous))
            previous = tag
            continue
        with tempfile.NamedTemporaryFile("w", suffix=".md", delete=False,
                                         encoding="utf-8") as body:
            body.write(notes(entry, previous))
        args = ["gh", "release", "create", tag, "--target", target,
                "--title", title(entry), "--notes-file", body.name]
        if "-beta" in entry["version"]:
            args.append("--prerelease")
        run(*args)
        if target == head:
            build_apk(tag)
        previous = tag


if __name__ == "__main__":
    main()
