// Reads a workflow or a composite action the way it ran before its scripts moved out of the YAML: every
// `run:` that points at a script of `scripts/ci/` (or at one next to the action) comes back as a `run: |`
// block holding that script, without its header. The tests that hold the workflows to the lane, the
// fastlane and each other read them through here, and keep matching the script's lines.
import { readFileSync } from "node:fs";
import { dirname, join } from "node:path";

const HEADER = /^(#!.*|#.*|set -[a-z]+|)$/;

/** The script's lines after its header: the shebang, the leading comments and `set -e...`. */
function body(script) {
  const lines = script.replace(/\n$/, "").split("\n");
  let start = 0;
  while (start < lines.length && HEADER.test(lines[start])) start += 1;
  return lines.slice(start);
}

/** The file at `path` with each script it runs written back into its `run:`. */
export function readWithScripts(repoRoot, path) {
  const source = readFileSync(join(repoRoot, path), "utf8");
  return source.replace(
    /^(\s*)((?:- )?)run: '?"?(?:\$GITHUB_WORKSPACE\/|\$GITHUB_ACTION_PATH\/)?((?:scripts\/ci\/)?[A-Za-z0-9._-]+\.sh)"?'?\n/gm,
    (_, indent, dash, name) => {
      const file = name.includes("/") ? name : join(dirname(path), name);
      const lines = body(readFileSync(join(repoRoot, file), "utf8"));
      const pad = " ".repeat(indent.length + dash.length + 2);
      return `${indent}${dash}run: |\n${lines.map((line) => (line ? pad + line : "")).join("\n")}\n`;
    },
  );
}
