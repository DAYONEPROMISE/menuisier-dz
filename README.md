# MenuisierDZ — optimiseur de débit + suivi commandes (offline, FR/AR, DZD)

Application Android offline-first (Kotlin, Jetpack Compose Material 3, Room)
pour menuisiers bois/alu en Algérie : **liste de débit → barres/panneaux
avec estimation de chutes**, commandes clients avec **acompte en DZD**,
français/arabe RTL, 100 % hors-ligne, sans compte.

## 1. Recherche (vérifiée)

| Fait | Source |
|---|---|
| 1D cutting-stock : heuristique First Fit Decreasing — trier décroissant, placer chaque pièce dans la première barre qui a la place ; garantie ≤ 11/9·OPT + 1 | EZNESTING Linear Cut Optimizer (2026) ; MetricGate Cutting Stock Calculator ; Brandão & Pedroso, *Fast pattern-based algorithms for cutting stock* (2014) |
| Kerf : chaque coupe consomme la largeur de lame ; 10 coupes à 3 mm = 30 mm perdus | EZNESTING ; Lizely free cut list optimizer |
| 2D panneaux : plans de coupe « guillotine » (coupe bord-à-bord, compatible scie à panneaux), avec rotation/grain | CutOptim (2026) ; Cutlist Evolution |
| Atelier algérien : devis sur mesure, acompte ~40 % à la signature, suivi production, TVA 19 %/9 % | Almawarid — logiciel menuiserie/ébénisterie Algérie |
| Concurrent offline : MENUISERIE MZAB OUTILS PRO (Maroc) — volume/prix bois, découpe panneaux, placards, 100 % hors-ligne | Google Play `com.estimator_devis_decoupe_placard.monuiserie_mzab` |
| Concurrents à abonnement/online : CutList Optimizer (synchro online), Cutter Soft (Windows uniquement, abonnement), Almawarid/BTP Manager DZ (abonnement) | Pages éditeurs respectives |
| Borne basse théorique : ⌈ Σ(dᵢ·lᵢ) / L ⌉ barres minimum | MetricGate |

Choix validé : **FFD 1D + packer 2D « étagères » guillotine**, code propre
(MIT, aucune dépendance d'optimiseur), unités en mm entiers, kerf
conservateur (1 kerf/pièce), déterministe.

## 2. Fonctionnalités v1

- Commandes : client, téléphone, total DZD, acompte DZD, reste calculé, statut.
- Optimiseur 1D : `nom:longueurXquantité` (1/ligne) + longueur stock + kerf →
  plan barre-par-barre, chutes, rendement %, vs borne basse.
- Optimiseur 2D (moteur `CutOptimizer.optimizePanels`) : étagères avec
  rotation 90°, sans chevauchement, testé.
- FR/AR : bouton FR/AR, `supportsRtl=true`, `LayoutDirection.Rtl` en AR,
  montants formatés DZD.
- Stockage : Room (`client_orders`, `cut_parts`), aucune permission réseau.

## 3. Tests

`CutOptimizerTest` (7 tests, JUnit4, `gradle :app:testDebugUnitTest`) :

| # | Test | Attente |
|---|---|---|
| 1 | exact fit, kerf 0 | 1 barre, chute 0, borne 1 |
| 2 | kerf compté | 2×1000+2×10 dans 2500 → chute 480, kerf 20 |
| 3 | conservation | barres×stock = pièces + kerf + chutes |
| 4 | borne FFD | borne basse ≤ barres ≤ 11/9·OPT+1 |
| 5 | déterminisme | même entrée → même plan |
| 6 | 2D sans chevauchement | tout dans 2440×1220, aires conservées |
| 7 | reste DZD | acompte > total → reste 0 |

CI (`.github/workflows/android.yml`) : `testDebugUnitTest` + `lintDebug` +
`assembleDebug`, artifact `menuisierdz-debug-apk` (APK debug).

## 4. Compiler / obtenir l'APK

Cloud (recommandé) : push sur `main` → onglet Actions → run `android-ci` →
télécharger `menuisierdz-debug-apk`.

Local : JDK 17 + Android SDK 34, puis
`gradle :app:assembleDebug` (APK : `app/build/outputs/apk/debug/app-debug.apk`).

## 5. Limites connues

- FFD heuristique, non prouvée optimale ; cas pathologiques possibles.
- 2D « étagères » simple : pas de Nesting true-shape, pas de sens du fil,
  pas de scie CNC/PTX/DXF (export prévu v2).
- Stocks multiples et chutes réutilisables : v2.
- Pas de sauvegarde cloud (choix offline) ; export CSV/PDF : v2.

## 6. Coûts

- Dev : 0 DZD (MIT, libs AndroidX/Compose/Room gratuites).
- Build cloud : GitHub Actions gratuit (quota public) ; Play : 25 $ uniques
  si publication ; aucun serveur (offline).

## 7. Plan « 10 premiers clients » (Alger, Blida, Oran, Sétif)

1. 3 ateliers pilotes gratuits (contre retours + logo) via visites + photos avant/après chutes.
2. Démo 5 min sur leurs vraies commandes (acompte DZD + débit du jour).
3. Groupes Facebook/whasApp menuisiers DZ + vidéo AR/FR 60 s.
4. Offre lancement : 2 000 DZD/mois ou 20 000 DZD à vie, installment via CCP/BaridiMob.
5. Objectif : 10 payants en 60 jours, churn suivi dans l'app (statuts).

Licence : MIT (voir `LICENSE`). Code 100 % clean-room.
