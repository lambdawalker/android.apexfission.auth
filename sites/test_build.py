"""Auth-owned contracts for the shared renderer; no local Markdown rendering."""
import hashlib
import json
import re
import sys
import unittest
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
OUTPUT = '--output' in sys.argv
if OUTPUT:
    sys.argv.remove('--output')
BASE = '/android.apexfission.auth/'


def page_map():
    text = (ROOT / '.project/docs/config.yaml').read_text()
    section = text.split('  pages:\n', 1)[1].split('  translation_manifest:', 1)[0]
    return {path: slug.strip("\"'") for path, slug in re.findall(r'^    (.+): (.*)$', section, re.M)}


class SourceContracts(unittest.TestCase):
    def test_wide_documentation_remains_in_shared_navigation(self):
        pages = page_map()
        self.assertEqual(27, len(pages))
        for path in ['README.md', 'AGENTS.md', 'AI_INTEGRATION_GUIDE.md', 'docs/identity/http-contract.md',
                     'docs/screenshots.md', 'docs/android-signing.md', 'docs/runtime-configuration.md', 'sites/README.md']:
            self.assertIn(path, pages)
            self.assertTrue((ROOT / path).is_file())
        self.assertEqual('', pages['docs/agents/index.md'])

    def test_reviewed_translations_preserve_hashes_provenance_and_code(self):
        manifest = json.loads((ROOT / 'sites/translations.json').read_text())
        self.assertEqual(10, len(manifest))
        fences = lambda text: re.findall(r'^(?:```|~~~)[^\n]*\n.*?^(?:```|~~~)', text, re.M | re.S)
        for path, record in manifest.items():
            canonical = (ROOT / path).read_text()
            translated = (ROOT / record['path']).read_text()
            self.assertEqual(record['source_sha256'], hashlib.sha256(canonical.encode()).hexdigest(), path)
            self.assertTrue(record['provenance'], path)
            self.assertEqual(fences(canonical), fences(translated), path)
            self.assertNotEqual(canonical, translated, path)

    def test_legacy_image_paths_match_reviewed_canonical_assets(self):
        captures = json.loads((ROOT / 'docs/screenshots/documentation.json').read_text())['captures']
        for capture in captures:
            canonical = ROOT / capture['path']
            legacy = ROOT / 'sites/astro/public/assets' / capture['path']
            self.assertEqual(canonical.read_bytes(), legacy.read_bytes(), legacy)
            self.assertEqual(capture['image_sha256'], hashlib.sha256(legacy.read_bytes()).hexdigest())

    def test_no_fictional_published_release(self):
        catalog = json.loads((ROOT / 'sites/versions.json').read_text())
        self.assertEqual([], catalog['releases'])
        self.assertEqual(['development'], [channel['kind'] for channel in catalog['channels']])
        records = list((ROOT / 'docs/releases/history').glob('*.json'))
        self.assertEqual([], records)

    def test_backend_and_identity_are_explicit(self):
        config = (ROOT / '.project/docs/config.yaml').read_text()
        self.assertIn('  backend: astro', config)
        self.assertIn('  base_url: https://lambdawalker.github.io/android.apexfission.auth/', config)
        self.assertNotIn('sites/build.py', config)
        self.assertIn('  translation_manifest: sites/translations.json', config)


@unittest.skipUnless(OUTPUT, 'run with --output after shared build')
class OutputContracts(unittest.TestCase):
    def test_all_guides_and_old_human_raw_aliases_resolve(self):
        for language in ['en', 'es']:
            for source, slug in page_map().items():
                scope = ROOT / f'sites/dist/{language}/auth/development'
                page = scope / slug / 'index.html'
                self.assertTrue(page.is_file(), page)
                alias = ROOT / f'sites/dist/{language}/main/{source[:-3]}.html'
                self.assertTrue(alias.is_file(), alias)
                target = BASE + f'{language}/auth/development/' + (slug + '/' if slug else '')
                self.assertIn(target, alias.read_text())
                raw = scope / 'raw' / f'{slug or "index"}.md'
                old_raw = ROOT / f'sites/dist/raw/{language}/main/{source}'
                self.assertEqual(raw.read_bytes(), old_raw.read_bytes(), old_raw)

    def test_reviewed_spanish_and_same_source_fallback(self):
        spanish = (ROOT / 'sites/dist/es/auth/development/index.html').read_text()
        self.assertIn('Autenticación Android de Attestra', spanish)
        self.assertNotIn('La traducción está ausente o desactualizada', spanish)
        for image in ['email-start-light.png', 'read-document-dark.png', 'details-saved-light.png']:
            self.assertIn(image, spanish)
        raw = (ROOT / 'sites/dist/es/auth/development/raw/identity/http-contract.md').read_text()
        self.assertIn('La traducción está ausente o desactualizada', raw)
        self.assertIn('Development (unreleased)', raw)

    def test_spanish_links_retain_scope_and_actual_source(self):
        raw = (ROOT / 'sites/dist/es/auth/development/raw/index.md').read_text()
        self.assertIn(BASE + 'es/auth/development/raw/quickstart.md', raw)
        self.assertIn(BASE + 'es/auth/development/raw/contributing.md', raw)
        self.assertNotIn('github.com/lambdawalker/android.attestra.auth', raw)
        self.assertNotIn('/docs/es/assets/', raw)

    def test_untranslated_spanish_siblings_use_same_scope_english_fallback(self):
        scope = ROOT / 'sites/dist/es/auth/development/raw'
        for guide in ['api', 'index']:
            raw = (scope / f'{guide}.md').read_text()
            self.assertIn(BASE + 'es/auth/development/raw/api-inventory.md', raw)
            self.assertNotIn('/docs/es/agents/api-inventory.md', raw)
        raw = (scope / 'index.md').read_text()
        for guide in ['coverage', 'evaluation']:
            self.assertIn(BASE + f'es/auth/development/raw/{guide}.md', raw)
            self.assertNotIn(f'/docs/es/agents/{guide}.md', raw)

    def test_discovery_and_development_only_metadata(self):
        dist = ROOT / 'sites/dist'
        for path in ['agents/index.md', 'IMPORT.md', 'llms.txt', 'versions.json']:
            self.assertTrue((dist / path).is_file(), path)
        for capture in json.loads((ROOT / 'docs/screenshots/documentation.json').read_text())['captures']:
            self.assertEqual((ROOT / capture['path']).read_bytes(), (dist / 'assets' / capture['path']).read_bytes())
        scopes = json.loads((dist / 'versions.json').read_text())
        self.assertEqual(2, len(scopes))
        self.assertEqual({'en', 'es'}, {scope['language'] for scope in scopes})
        self.assertTrue(all(scope['development'] and scope['version'] == 'development' for scope in scopes))
        self.assertTrue(all(len(scope['pages']) == 27 for scope in scopes))


if __name__ == '__main__':
    unittest.main()
