"""The generated modules must match spec/endpoints.json (run scripts/generate.py)."""

from __future__ import annotations

import subprocess
import sys
from pathlib import Path

import pytest

ROOT = Path(__file__).resolve().parents[1]


def test_generated_code_is_up_to_date() -> None:
    ruff = Path(sys.executable).parent / "ruff"
    if not ruff.exists():
        pytest.skip("ruff not installed next to the interpreter")
    result = subprocess.run(
        [sys.executable, "-I", str(ROOT / "scripts" / "generate.py"), "--check"],
        cwd=ROOT,
        capture_output=True,
        text=True,
        check=False,
    )
    assert result.returncode == 0, result.stdout + result.stderr
