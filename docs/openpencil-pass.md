# OpenPencil pass — menuisier-dz

## Design (Shared/OpenPencil/menuisier-dz/)
- `design.html` (cut optimizer + result card + FR/AR header, DZ tokens) → `design.fig`
- `lint --preset accessibility`: 0 errors, 0 warnings

## Code changes
- `MainActivity.kt`: rail + bar nav icons used `contentDescription = null`; now use the
  bilingual tab label (`Commandes/الطلبات`, `Découpe/القص`). Dark toggle + AR switch untouched.

## APKs
`android.yml` adds `assembleRelease` (unsigned) + release artifact + versioned
`v*-b*` GitHub Release with notes + both APKs.
