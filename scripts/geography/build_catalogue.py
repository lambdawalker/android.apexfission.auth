"""Reduce GeoNames admin1/cities15000 snapshots to offline location suggestions.

Inputs: https://download.geonames.org/export/dump/admin1CodesASCII.txt
        https://download.geonames.org/export/dump/cities15000.zip
GeoNames CC BY 4.0. Preserve assets/geonames-NOTICE.txt with generated output.
"""
import argparse
from pathlib import Path
import zipfile


def render(regions_text, cities_text):
    regions = {}
    rows = set()
    for line in regions_text.splitlines():
        parts = line.split('\t')
        if len(parts) >= 2 and '.' in parts[0]:
            code, region = parts[0], parts[1]
            regions[code] = region
            rows.add((code.split('.')[0], region, ''))
    for line in cities_text.splitlines():
        parts = line.split('\t')
        if len(parts) >= 11:
            country, region_code = parts[8], parts[10]
            region = regions.get(country + '.' + region_code)
            if region:
                rows.add((country, region, parts[1]))
    return '# GeoNames CC BY 4.0; see geonames-NOTICE.txt\n' + ''.join('\t'.join(row) + '\n' for row in sorted(rows))


if __name__ == '__main__':
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--regions', type=Path, required=True)
    parser.add_argument('--cities', type=Path, required=True)
    parser.add_argument('--output', type=Path, required=True)
    args = parser.parse_args()
    with zipfile.ZipFile(args.cities) as archive:
        cities = archive.read('cities15000.txt').decode('utf-8')
    args.output.write_text(render(args.regions.read_text(), cities), encoding='utf-8')
