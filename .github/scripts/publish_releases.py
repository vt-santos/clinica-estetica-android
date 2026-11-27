#!/usr/bin/env python3
"""Mantém uma Release no GitHub para cada versão do CHANGELOG.md.

- Versões com "-beta" viram pré-release (🧪 Beta); as demais viram patch (🩹 Patch).
- A tag de cada versão aponta para o commit daquela versão:
  1. o commit indicado em "<!-- commit: abc1234 -->" logo abaixo do título, se houver;
  2. senão, o commit que acrescentou o título da versão ao CHANGELOG.md.
  Assim, várias versões que chegam ao main de uma vez ficam cada uma no seu commit.
- Release que já existe tem título e notas atualizados. Se a tag estiver no commit
  errado, a release e a tag são recriadas no commit certo.
- Só a versão mais nova recebe o APK de teste, gerado a partir do main atual.
  Uma falha no APK não impede as releases.

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


def tag_commit(tag):
    """Commit para onde a tag aponta hoje no GitHub, ou None se a release não existe."""
    if DRY_RUN or run("gh", "release", "view", tag, check=False).returncode != 0:
        return None
    refs = dict(reversed(line.split("\t")) for line in run(
        "git", "ls-remote", "--tags", "origin", f"refs/tags/{tag}", check=False
    ).stdout.splitlines() if "\t" in line)
    # Tag anotada: o commit é a linha "^{}"; tag simples: a própria linha da tag.
    return refs.get(f"refs/tags/{tag}^{{}}") or refs.get(f"refs/tags/{tag}", "")


def version_commit(entry, head):
    """Commit da versão: o indicado no CHANGELOG ou o que criou o título dela."""
    if entry["commit"]:
        return run("git", "rev-parse", f"{entry['commit']}^{{commit}}").stdout.strip()
    added = run("git", "log", "--reverse", "--format=%H", "-S",
                f"## [{entry['version']}]", "--", "CHANGELOG.md").stdout.split()
    return added[0] if added else head


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
    """APK de teste da versão mais nova. Se algo falhar, as releases continuam valendo."""
    print(f"Gerando o APK de {tag}...")
    if run("bash", "gradlew", "--no-daemon", "assembleDebug", capture=False,
           check=False).returncode != 0:
        print("::warning::Não foi possível gerar o APK; a release ficou sem ele.")
        return
    apk = ROOT / "app/build/outputs/apk/debug/app-debug.apk"
    named = ROOT / f"ClinicaEstetica-{tag}.apk"
    named.write_bytes(apk.read_bytes())
    # O GitHub recusa emoji (caractere de 4 bytes) no rótulo do arquivo.
    upload = run("gh", "release", "upload", tag, f"{named}#APK de teste ({tag})",
                 "--clobber", check=False)
    if upload.returncode != 0:
        print(f"::warning::Não foi possível anexar o APK: {upload.stderr.strip()}")


def main():
    head = run("git", "rev-parse", "HEAD").stdout.strip()
    entries = parse_changelog()
    previous = None
    for entry in entries:
        tag = f"v{entry['version']}"
        target = version_commit(entry, head)
        current = tag_commit(tag)
        if DRY_RUN:
            print(f"{tag}: commit {target[:7]}")
            print(title(entry))
            print(notes(entry, previous))
            previous = tag
            continue
        with tempfile.NamedTemporaryFile("w", suffix=".md", delete=False,
                                         encoding="utf-8") as body:
            body.write(notes(entry, previous))
        if current == target:
            print(f"{tag}: já publicada no commit certo, atualizando título e notas")
            run("gh", "release", "edit", tag, "--title", title(entry),
                "--notes-file", body.name)
        else:
            if current is not None:
                print(f"{tag}: tag em {current[:7]}, deveria estar em {target[:7]}; recriando")
                run("gh", "release", "delete", tag, "--cleanup-tag", "--yes")
            else:
                print(f"{tag}: publicando no commit {target[:7]}")
            args = ["gh", "release", "create", tag, "--target", target,
                    "--title", title(entry), "--notes-file", body.name]
            if "-beta" in entry["version"]:
                args.append("--prerelease")
            run(*args)
        previous = tag

    if entries and not DRY_RUN:
        build_apk(f"v{entries[-1]['version']}")


if __name__ == "__main__":
    main()
