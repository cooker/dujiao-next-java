#!/usr/bin/env python3
"""
静态检查 Java API 响应格式是否与 Go 约定一致；可选 --update-checklist 自动勾选 docs/API_RESPONSE_CHECKLIST.md 中带 <!--check:...--> 的条目。
在 dujiao-next-java 目录执行：python3 scripts/check_api_response_format.py [--update-checklist]
"""
from __future__ import annotations

import argparse
import re
import sys
from pathlib import Path


def repo_root(java_root: Path) -> Path:
    return java_root.parent


def parse_go_codes(go_file: Path) -> dict[str, int]:
    text = go_file.read_text(encoding="utf-8")
    out: dict[str, int] = {}
    for m in re.finditer(r"^\s*Code(\w+)\s*=\s*(\d+)\s*$", text, re.MULTILINE):
        out[m.group(1)] = int(m.group(2))
    return out


def parse_java_response_codes(java_file: Path) -> dict[str, int]:
    text = java_file.read_text(encoding="utf-8")
    out: dict[str, int] = {}
    for m in re.finditer(
        r"public\s+static\s+final\s+int\s+(\w+)\s*=\s*(\d+)\s*;", text
    ):
        out[m.group(1)] = int(m.group(2))
    return out


GO_TO_JAVA_NAMES = {
    "OK": "OK",
    "BadRequest": "BAD_REQUEST",
    "Unauthorized": "UNAUTHORIZED",
    "Forbidden": "FORBIDDEN",
    "NotFound": "NOT_FOUND",
    "TooManyRequests": "TOO_MANY_REQUESTS",
    "Internal": "INTERNAL",
}


def check_codes_core(java_root: Path) -> bool:
    go_codes = parse_go_codes(
        repo_root(java_root) / "internal" / "http" / "response" / "codes.go"
    )
    java_codes = parse_java_response_codes(
        java_root / "src" / "main" / "java" / "com" / "dujiao" / "api" / "common" / "api" / "ResponseCodes.java"
    )
    for go_name, java_name in GO_TO_JAVA_NAMES.items():
        if go_codes.get(go_name) != java_codes.get(java_name):
            print(
                f"FAIL codes: Go Code{go_name}={go_codes.get(go_name)} "
                f"vs Java {java_name}={java_codes.get(java_name)}"
            )
            return False
    return True


def check_file_contains(path: Path, *needles: str) -> bool:
    if not path.is_file():
        print(f"FAIL missing file: {path}")
        return False
    text = path.read_text(encoding="utf-8")
    for n in needles:
        if n not in text:
            print(f"FAIL {path.name}: expected {n!r}")
            return False
    return True


def check_envelope_fields(java_root: Path) -> bool:
    api = (
        java_root
        / "src"
        / "main"
        / "java"
        / "com"
        / "dujiao"
        / "api"
        / "common"
        / "api"
        / "ApiResponse.java"
    )
    return check_file_contains(
        api,
        '@JsonProperty("status_code")',
        "private final String msg",
        "private final T data",
    )


def check_success_values(java_root: Path) -> bool:
    api = (
        java_root
        / "src"
        / "main"
        / "java"
        / "com"
        / "dujiao"
        / "api"
        / "common"
        / "api"
        / "ApiResponse.java"
    )
    text = api.read_text(encoding="utf-8")
    if '"success"' not in text or "ResponseCodes.OK" not in text:
        print("FAIL ApiResponse success constants")
        return False
    return True


def check_global_exception(java_root: Path) -> bool:
    path = (
        java_root
        / "src"
        / "main"
        / "java"
        / "com"
        / "dujiao"
        / "api"
        / "common"
        / "exception"
        / "GlobalExceptionHandler.java"
    )
    text = path.read_text(encoding="utf-8")
    if "ResponseEntity.ok(ApiResponse.error" not in text:
        print("FAIL GlobalExceptionHandler should use ResponseEntity.ok(ApiResponse.error(...))")
        return False
    return True


def check_no_controller_http_error(java_root: Path) -> bool:
    root = java_root / "src" / "main" / "java"
    bad_patterns = [
        "ResponseEntity.badRequest",
        "ResponseEntity.notFound",
        "ResponseEntity.status(HttpStatus.BAD_REQUEST",
        "ResponseEntity.status(HttpStatus.NOT_FOUND",
        "ResponseEntity.status(HttpStatus.UNAUTHORIZED",
        "ResponseEntity.status(HttpStatus.FORBIDDEN",
        "ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR",
    ]
    failures: list[str] = []
    for p in root.rglob("*.java"):
        if "test" in p.parts:
            continue
        text = p.read_text(encoding="utf-8")
        for pat in bad_patterns:
            if pat in text:
                failures.append(f"{p.relative_to(java_root)}: {pat}")
    if failures:
        for f in failures[:20]:
            print(f"FAIL forbidden pattern: {f}")
        if len(failures) > 20:
            print(f"... and {len(failures) - 20} more")
        return False
    return True


def check_page_envelope(java_root: Path) -> bool:
    page = (
        java_root
        / "src"
        / "main"
        / "java"
        / "com"
        / "dujiao"
        / "api"
        / "common"
        / "api"
        / "PageResponse.java"
    )
    pag = (
        java_root
        / "src"
        / "main"
        / "java"
        / "com"
        / "dujiao"
        / "api"
        / "common"
        / "api"
        / "PaginationDto.java"
    )
    if not check_file_contains(
        page,
        '@JsonProperty("status_code")',
        "private final String msg",
        "private final T data",
        "private final PaginationDto pagination",
    ):
        return False
    if not check_file_contains(
        pag,
        '@JsonProperty("page_size")',
        '@JsonProperty("total_page")',
    ):
        return False
    return True


def check_pagination_math(java_root: Path) -> bool:
    path = (
        java_root
        / "src"
        / "main"
        / "java"
        / "com"
        / "dujiao"
        / "api"
        / "common"
        / "api"
        / "PaginationDto.java"
    )
    text = path.read_text(encoding="utf-8")
    if "(total + ps - 1) / ps" not in text and "(total + ps - 1)/ps" not in text.replace(" ", ""):
        print("FAIL PaginationDto total_page formula")
        return False
    if "Math.max(page, 1)" not in text:
        print("FAIL PaginationDto page normalize")
        return False
    return True


def check_http_200_errors(java_root: Path) -> bool:
    """与 global_exception + no 4xx controller 一并覆盖；此处确认 Error 未改用 status()."""
    return check_global_exception(java_root) and check_no_controller_http_error(java_root)


CHECKS: dict[str, callable] = {
    "codes-core": check_codes_core,
    "envelope-fields": check_envelope_fields,
    "success-values": check_success_values,
    "http-200-errors": check_http_200_errors,
    "global-exception": check_global_exception,
    "no-responseentity-4xx": check_no_controller_http_error,
    "page-envelope": check_page_envelope,
    "pagination-math": check_pagination_math,
}


def update_checklist(java_root: Path, results: dict[str, bool]) -> None:
    md = java_root / "docs" / "API_RESPONSE_CHECKLIST.md"
    if not md.is_file():
        return
    lines = md.read_text(encoding="utf-8").splitlines(keepends=True)
    out: list[str] = []
    for line in lines:
        m = re.search(r"<!--check:([\w-]+)-->", line)
        if m and line.strip().startswith("- [ ]") and results.get(m.group(1), False):
            line = line.replace("- [ ]", "- [x]", 1)
        out.append(line)
    md.write_text("".join(out), encoding="utf-8")
    print(f"Updated checklist: {md.relative_to(java_root)}")


def main() -> int:
    parser = argparse.ArgumentParser()
    parser.add_argument(
        "--update-checklist",
        action="store_true",
        help="勾选 docs/API_RESPONSE_CHECKLIST.md 中已通过且带 <!--check:...--> 的项",
    )
    args = parser.parse_args()
    java_root = Path(__file__).resolve().parent.parent
    results: dict[str, bool] = {}
    failed = False
    for check_id, fn in CHECKS.items():
        ok = bool(fn(java_root))
        results[check_id] = ok
        sym = "OK " if ok else "FAIL "
        print(f"{sym}{check_id}")
        if not ok:
            failed = True
    if args.update_checklist:
        update_checklist(java_root, results)
    return 1 if failed else 0


if __name__ == "__main__":
    sys.exit(main())
