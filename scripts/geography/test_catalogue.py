from pathlib import Path
import unittest
from build_catalogue import render

ROOT = Path(__file__).resolve().parents[2]


class CatalogueTest(unittest.TestCase):
    def test_bundled_catalogue_contains_hierarchy_and_is_unique(self):
        rows = (ROOT / 'auth/src/main/assets/issuing-locations.tsv').read_text().splitlines()[1:]
        self.assertEqual(len(rows), len(set(rows)))
        self.assertIn('MX\tChihuahua\tCiudad Juárez', rows)
        self.assertIn('US\tTexas\tEl Paso', rows)
        regions = {(parts[0], parts[1]) for row in rows if (parts := row.split('\t'))[2] == ''}
        self.assertTrue(all(tuple(row.split('\t')[:2]) in regions for row in rows))

    def test_reduction_does_not_invent_unknown_regions(self):
        city = '\t'.join(['1', 'Town', '', '', '', '', '', '', 'MX', '', 'XX'])
        output = render('MX.CH\tChihuahua\tChihuahua\t1', city)
        self.assertIn('MX\tChihuahua\t\n', output)
        self.assertNotIn('Town', output)


if __name__ == '__main__':
    unittest.main()
