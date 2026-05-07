# CRS implementation notes

## Supported in MVP

| Code | Description |
|---|---|
| `WGS84_GEO` | WGS-84 ellipsoid, lat/lon/height |
| `WGS84_UTM_35N`, `WGS84_UTM_36N`, `WGS84_UTM_37N` | UTM zones, most common in Russian territory |
| `GSK2011_GEO` | ГСК-2011 geocentric |
| `GSK2011_GK_<zone>` | ГСК-2011 6° Gauss-Krüger, zones 1–60 |
| `SK42_GK_<zone>` | СК-42 6° GK |
| `SK95_GK_<zone>` | СК-95 6° GK |
| `WEB_MERC` | Web Mercator (map tiles only) |

## Ellipsoids

| Ellipsoid | Used by | a (semi-major) | 1/f (inverse flattening) |
|---|---|---|---|
| WGS-84 | WGS-84, Web Merc | 6378137.0 | 298.257223563 |
| GRS80 | ГСК-2011 | 6378137.0 | 298.257222101 |
| Krasovsky-1940 | СК-42, СК-95 | 6378245.0 | 298.3 |

## Helmert 7-parameter transforms

Applied when transforming between ellipsoids. For MVP, hard-coded in `:crs/CrsPresets.kt`.

**WGS-84 → ГСК-2011** (from Rosreestr 2020 order):
```
ΔX = +0.013 m
ΔY = −0.092 m
ΔZ = −0.030 m
ωX = −0.001 "
ωY = +0.003 "
ωZ = +0.002 "
ms = +0.000 ppm
```

**WGS-84 → СК-42** (approximate, from widely used parameters):
```
ΔX = +23.57 m
ΔY = −140.95 m
ΔZ = −79.80 m
ωX = 0.000 "
ωY = −0.35 "
ωZ = −0.79 "
ms = −0.22 ppm
```

**WGS-84 → СК-95**:
```
ΔX = +24.47 m
ΔY = −130.89 m
ΔZ = −81.56 m
ωX = 0.000 "
ωY = 0.000 "
ωZ = −0.13 "
ms = −0.22 ppm
```

Inverse transforms: negate all seven parameters.

## Gauss-Krüger projection

- 6° zones by default for Russian CRSs.
- Zone N spans meridians `(N−1)*6°` to `N*6°` east of prime meridian.
- Central meridian: `(zone−1)*6° + 3°`.
- False easting: `500_000 + zone*1_000_000` (so X includes zone prefix). Example: ГСК-2011 GK zone 8, central meridian 45°, CM has X = 8,500,000.
- Scale factor on CM: 1.0 (not 0.9996 as UTM).

Implementation: series-expansion forward/inverse (Bowring, Karney). Use 10-term Krüger series for sub-mm precision or 6-term simplified for 1 cm precision (enough for MVP).

## UTM projection

- 6° zones globally, longitude of CM = `(zone−1)*6° − 177°`.
- False easting 500,000, false northing 10,000,000 (southern hemisphere only, 0 for northern).
- Scale factor on CM: **0.9996**.
- Latitude limits: [−80°, +84°].

## EGM-86 geoid

- Built-in resolution: 15'×15'.
- File format in `:crs/src/main/resources/egm86_15min.bin`: 1440 × 720 float32 grid.
- Interpolation: bilinear between grid points.
- Usage: `undulation = egm86.at(lat, lon)`, where positive undulation means geoid is above ellipsoid (typical in Russia, +10 to +30 m).

## Orthometric vs ellipsoidal height

```
H_ortho = H_ellipsoid - N_undulation
```

Project CRS config records which form the user wants stored. When saving a point:
- If project is ellipsoidal: store H as reported by receiver (GGA height is in ellipsoidal mode if receiver geoid = wgs84).
- If project is orthometric: store H − N(lat,lon) from the chosen geoid.

When exporting: values are in project's chosen form. No silent conversion.

## Changing CRS on a project

Workflow (in code):

1. Load all points.
2. Transform each point to new CRS (incl. geoid change if applicable).
3. Compute delta statistics: avg, min, max horizontal/vertical shift.
4. Show confirmation dialog with stats.
5. On confirm: transaction that updates all rows in place. Transaction, not per-row — so either complete or rollback.
6. Update `crs_configs` FK for project.

## Integration with Newton receiver

Newton has its own `coordsystem set geoid` command. Our service applies this via the command queue when the user changes geoid on PRJ-006A. **This does not change how the app computes coordinates** — it only tells the receiver which height form to emit. The app independently transforms ellipsoidal → orthometric (or vice versa) using the app's chosen geoid file. Keep these two decoupled.

## Unit tests

Every transform has reference data in `:crs/src/test/resources/reference/`:

```
reference/
  wgs84_to_gsk2011_gk8.txt         # columns: lat, lon, height, X, Y
  sk42_to_gsk2011_gk8.txt
  egm86_reference_points.txt       # lat, lon, expected undulation
```

Test reads reference file, runs transform, asserts within tolerance (1 mm projected, 1e-9 deg geographic).
