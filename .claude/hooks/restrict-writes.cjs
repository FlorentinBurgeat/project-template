#!/usr/bin/env node
// PreToolUse guard: blocks Write/Edit/NotebookEdit outside the allowed directories.
// Usage: node restrict-writes.cjs <dir> [<dir>...]   (dirs relative to the project root)
const path = require("path");

const root = path.resolve(process.env.CLAUDE_PROJECT_DIR || process.cwd());
const allowed = process.argv.slice(2).map((d) => path.resolve(root, d));

let raw = "";
process.stdin.on("data", (c) => (raw += c));
process.stdin.on("end", () => {
  let input;
  try {
    input = JSON.parse(raw);
  } catch {
    process.exit(0);
  }
  const target = input.tool_input?.file_path || input.tool_input?.notebook_path;
  if (!target) process.exit(0);

  const abs = path.resolve(root, target);
  const ok = allowed.some((dir) => {
    const rel = path.relative(dir, abs);
    return rel === "" || (!rel.startsWith("..") && !path.isAbsolute(rel));
  });
  if (ok) process.exit(0);

  const list = process.argv.slice(2).join(", ");
  process.stderr.write(`Blocked: this agent may only write inside: ${list}. Target: ${target}\n`);
  process.exit(2);
});
