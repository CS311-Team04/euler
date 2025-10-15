from __future__ import annotations

import argparse

from epfl_scraper.cli import parse_args, apply_lang_presets


def test_lang_fr_sets_defaults_when_unset():
    ns = parse_args(["--lang", "fr"])
    ns = apply_lang_presets(ns)
    assert ns.start == ["https://www.epfl.ch/education/fr/"]
    assert ns.allow_path == ["/education/fr"]


def test_lang_en_sets_defaults_when_unset():
    ns = parse_args(["--lang", "en"])
    ns = apply_lang_presets(ns)
    assert ns.start == ["https://www.epfl.ch/education/"]
    assert ns.allow_path == ["/education/"]


def test_explicit_flags_override_presets():
    ns = parse_args([
        "--lang", "fr",
        "--start", "https://custom/start/",
        "--allow-path", "/custom",
    ])
    ns = apply_lang_presets(ns)
    assert ns.start == ["https://custom/start/"]
    assert ns.allow_path == ["/custom"]


