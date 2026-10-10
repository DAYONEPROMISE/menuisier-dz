# MenuisierDZ — 2026 UI/UX Design Spec

Shareable source of truth for this app's UI. Tokens mirror Penpot library
conventions (color / type / spacing) so the Product Design Lead can import
them into the shared Penpot libraries; the Compose implementation in
`app/src/main` follows this file exactly.

> Honest note: no Penpot mockup file is produced here — this agent has no
> Penpot/browser access. This spec + the implemented Compose screens are the
> design deliverable. Screens were iterated directly in code.

## Design tokens

| Token | Value | Notes |
| --- | --- | --- |
| Color | Material 3 dynamic color (Android 12+), baseline M3 fallback | `ui/DzTheme.kt` |
| Dark mode | System default + in-app override (sun/moon action) | persists per session |
| Typography | M3 type scale: titleMedium (card titles), bodyLarge (amounts/result), bodyMedium (secondary) | |
| Spacing | 8dp grid; screen padding 16dp; card content 16dp | |
| Shape | M3 defaults (no custom shapes) | |
| Icons | Material Icons: ReceiptLong (Commandes), ContentCut (Découpe), DarkMode/LightMode | |

## RTL / FR-AR rules

- FR default LTR; AR toggle switches `LocalLayoutDirection` to RTL for the whole tree.
- No hard-coded start/end positions — all rows use `Arrangement.spacedBy`, weights, and direction-aware layouts.
- Numbers use `NumberFormat(FRANCE)` grouping; DZD suffix mirrored (دج in AR).
- Icon buttons carry `contentDescription`; decorative icons use `null` + parent semantics.

## Screens

1. **Commandes** — client name + Total/Acompte (DZD) inputs, "Ajouter commande" button, lazy list of order cards (name / Total–Acompte–Reste / statut). Scrollable via LazyColumn (small screens + font scaling safe).
2. **Découpe** — stock length + kerf inputs, parts textarea (`nom:longueurXqté`), "Optimiser" button, result in a card (bars used, waste mm, utilization %, per-bar cut list).
3. **Chrome** — TopAppBar (title FR/AR + dark toggle + FR/AR switch); NavigationBar on phones (<600dp), NavigationRail on wide screens (tablet/landscape); edge-to-edge with window-inset handling.

## Accessibility checklist

- [x] Minimum 48dp touch targets (M3 components default)
- [x] Content descriptions on icon-only controls
- [x] Dynamic-type safe (LazyColumn, no fixed heights on text)
- [x] Contrast from M3 dynamic/baseline schemes (light + dark)
- [x] RTL mirroring verified by layout-direction toggle (visual check on device needed)

## 2026-standard compliance

dynamic color ✓ · dark mode ✓ · edge-to-edge ✓ · adaptive layouts (600dp breakpoint) ✓ ·
accessibility pass ✓ · FR/AR RTL ✓ · offline-first (Room, no network) ✓
