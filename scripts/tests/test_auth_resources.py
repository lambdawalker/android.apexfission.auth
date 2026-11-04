"""Require full library-owned UI translations and compatible Android format arguments."""
from collections import Counter
from pathlib import Path
import re
import unittest
import xml.etree.ElementTree as ET

ROOT = Path(__file__).resolve().parents[2]
RES = ROOT / 'auth/src/main/res'
LOCALES = ('de', 'es', 'fr', 'it', 'ja', 'ko', 'pl', 'pt', 'ru', 'uk', 'zh-rCN', 'zh-rTW')


def strings(path):
    items = ET.parse(path).getroot().findall('string')
    result = {item.attrib['name']: ''.join(item.itertext()) for item in items}
    if len(result) != len(items):
        raise AssertionError('Duplicate string resource in ' + str(path))
    return result


def placeholders(value):
    return Counter(re.findall(r'%\d+\$[sd]', value))


class AuthResourcesTest(unittest.TestCase):
    def test_every_supported_locale_is_complete_and_preserves_arguments(self):
        default = strings(RES / 'values/strings.xml')
        self.assertGreater(len(default), 250)
        for locale in LOCALES:
            with self.subTest(locale=locale):
                translated = strings(RES / f'values-{locale}/strings.xml')
                self.assertEqual(default.keys(), translated.keys())
                for key, source in default.items():
                    self.assertTrue(translated[key].strip('" '), key)
                    self.assertEqual(placeholders(source), placeholders(translated[key]), key)

    def test_all_source_resource_references_exist(self):
        default = strings(RES / 'values/strings.xml')
        references = set()
        for path in (ROOT / 'auth/src').rglob('*.kt'):
            references.update(re.findall(r'R\.string\.(auth_\w+)', path.read_text()))
        self.assertFalse(references - default.keys(), references - default.keys())

    def test_navigation_and_document_side_are_distinct(self):
        default = strings(RES / 'values/strings.xml')
        self.assertNotEqual(default['auth_back'], default['auth_back_side'])


if __name__ == '__main__':
    unittest.main()
