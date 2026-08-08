# ZombieZ Companion — plan de correctif consolidé (v2)

## Contexte
Le serveur a ajouté une deuxième carte instanciée (zones 51+, dimension `minecraft:world2`)
en plus de la carte principale (zones 1-50, dimension `minecraft:overworld`). Le mod n'avait
aucune notion de dimension/monde -> waypoints et quick-tp cassés quand l'event ou le joueur
est sur la carte 2. Le format des messages du marchand a aussi changé avec la mise à jour.

Confirmé en jeu : la carte 2 n'a qu'UN SEUL refuge, accessible via la commande serveur
`refuge tp w2` (par opposition à `refuge tp <n>` pour les 11 refuges de la carte 1).
Dimension confirmée par logs : `minecraft:world2`.
L'Assaut du Marché des Sables reste toujours au même endroit sur la carte 1 (spawn fixe) —
pas concerné par la carte 2, ne pas y toucher.

## 1. Nouveaux formats de messages de chat à gérer (modules/minievents/MiniEventsModule.java)

### World Boss — déjà compatible, RAS
Apparition (contient déjà "world boss detecte" -> header OK ; "coordonn" présent -> coords OK) :
    ⬢ WORLD BOSS DÉTECTÉ ⬢
    ⟡ Menace majeure ⟡
    Identité : Doom, la Corrompue Sorcière
    Localisé en Zone 6
    Coordonnées : 274, 91, 8938
Disparition (contient déjà "boss vaincu") :
    BOSS VAINCU
    Yumptea a porté le coup fatal à Doom, la Corrompue Sorcière
    Le boss a lâché : ...

Seul ajout nécessaire : extraire le numéro de zone depuis "Localisé en Zone N" (regex
`(?i)zone\s+(\d+)`) pour déterminer map = (N <= 50) ? 1 : 2.

### Marchand — À CORRIGER (3 points, déjà identifiés précédemment)
Apparition (nouveau format réel) :
    ◆ SUPER MARCHAND · Zone 8
    709, 86, 8514 · 5:00 · stock personnel pour chacun
Disparition (nouveau format réel) :
    [Marchand] Le marchand ambulant est reparti.

Corrections à apporter dans handleMarchandLine() :
a) Déclencheur d'en-tête : ajouter `ascii.contains("super marchand")` en plus de
   `ascii.contains("marchand ambulant")`.
b) Retirer la condition `ascii.contains("coordonn")` avant le match de MARCHAND_COORDS
   (le mot n'apparaît plus dans le nouveau message).
c) Retirer la condition `ascii.contains("disparait")` avant le match de MARCHAND_DURATION
   (le timer "5:00" est sur la même ligne que les coordonnées désormais).
d) Détecteur de fin de vie : remplacer/élargir la condition actuelle
   (`"stock epuise"` / `"reparti en quete"`) pour matcher aussi `"est reparti"`
   (le nouveau message est "Le marchand ambulant est reparti.").
e) Extraire le numéro de zone depuis "· Zone N" (même regex que World Boss) pour
   déterminer map = (N <= 50) ? 1 : 2.

### Assaut du Marché des Sables — inchangé
Toujours limité à la carte 1 (spawn fixe). Ne pas ajouter de gestion de zone/carte 2 ici.

## 2. Logique de routage carte 1 / carte 2

Pour World Boss et Marchand uniquement (pas Assaut) :

- **map == 1** (zone <= 50) : comportement actuel inchangé.
  - Étiquette du waypoint : ZombieZMapData.nearestRefuge(x, z) (carte 1 uniquement, table
    REFUGES existante, ne pas y toucher).
  - Dimension du waypoint (nouveau champ, voir section 3) : "minecraft:overworld".
- **map == 2** (zone >= 51) : nouveau chemin.
  - Étiquette du waypoint : nom fixe (constante à ajouter dans ZombieZMapData, ex.
    `REFUGE_W2_NAME = "La Cité Brumeuse"`), PAS de recherche dans REFUGES (inutile pour un
    seul refuge).
  - Dimension du waypoint : "minecraft:world2".
  - Ne PAS ajouter ce refuge à la table REFUGES de ZombieZMapData (pas de champ de
    discrimination "map" à introduire pour un cas aussi simple — juste la constante de nom
    ci-dessus, utilisée directement dans MiniEventsModule).

## 3. Waypoints — tag de dimension (concerne AUSSI les waypoints posés manuellement)

Ajouter un champ `String dimension` (nullable) à MapConfig.Waypoint :
- Rempli automatiquement à la création, que ce soit :
  - un waypoint auto (marchand/world boss) -> "minecraft:overworld" ou "minecraft:world2"
    selon la zone parsée (voir section 2)
  - un waypoint posé manuellement (WaypointEditScreen / WaypointManagerScreen ou tout point
    d'entrée de création manuelle) -> dimension du joueur au moment de la création
    (`client.world.getRegistryKey()`)
- Waypoints existants déjà sauvegardés (créés avant ce patch, donc sans ce champ / valeur
  null) : les traiter comme "visibles dans toutes les dimensions" pour ne pas faire
  disparaître les waypoints manuels que l'utilisateur a déjà posés avant la mise à jour du
  mod (rétrocompatibilité).
- Dans WaypointsModule.renderBeacons() (et tout autre point qui affiche un beacon/liste les
  waypoints, y compris WaypointManagerScreen si pertinent) : n'afficher un waypoint que si
  `wp.dimension == null || wp.dimension.equals(currentDimensionId)`.

## 4. Quick-tp (ZombieZCompanionClient.tpToEventRefuge)

Nouvelle logique :
1. Si un event actif existe (MiniEventsModule.activeEventTarget(), World Boss ou Marchand) :
   - si sa dimension est "minecraft:world2" -> envoyer directement `refuge tp w2`
     (pas de calcul de distance, un seul refuge possible).
   - sinon (dimension "minecraft:overworld" ou event sans dimension connue, legacy) ->
     comportement actuel inchangé : nearestRefuge(x, z) sur la table carte 1 + `refuge tp <n>`.
2. Si aucun event actif :
   - si le JOUEUR est actuellement dans "minecraft:world2" -> envoyer directement
     `refuge tp w2`.
   - sinon -> comportement actuel inchangé (nearestRefuge sur position du joueur, carte 1).

## 5. Logs de debug à ajouter (déjà évoqué, à garder dans ce patch)

Dans onChatMessage() et handleMarchandLine() (et idéalement handleWorldBossLine()) :
`Log.debug(LogCategory.CHAT, ...)` à l'entrée et à chaque étape de décision (header ouvert,
zone extraite, coordonnées matchées ou non), activable via le flag debugMode existant.
Utile pour valider ce patch en jeu et pour tout futur changement de format de message.

## 6. Contenu de ce dossier
- decompiled-src/ : sources décompilées (CFR, mappings intermediary) du jar 1.0.2 d'origine,
  SANS aucune modification de dimension/monde — sert de base de code de référence complète.
- resources/ : fabric.mod.json, mixins, refmap, lang (fr_fr/en_us), icon — à réutiliser tels
  quels. Les tuiles de carte (assets/zombiezcompanion/map/) ne sont PAS incluses (trop
  volumineuses) : à recopier depuis le jar d'origine zombiez-companion-1_0_2.jar au moment du
  packaging final.

## 7. Environnement de build
Minecraft 1.21.4 / Fabric Loader >=0.16.9 / Loom 1.9.2 / Java 21
Package racine : io.github.keoz5.zombiezcompanion
