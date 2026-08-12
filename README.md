# ZombieZ Companion — V2

Client-side, read-only quality-of-life companion mod for the **ZombieZ** Minecraft server.
Provides modular overlays and helpers (minimap, waypoints, event beacons/timers, HUD, etc.);
it never automates gameplay.

- **Minecraft:** 26.1.2
- **Mod loader:** Fabric Loader ≥ 0.19.3
- **Java:** 25
- **Mappings:** official Mojang mappings (bundled — Minecraft 26.1+ ships deobfuscated, no mappings dependency)
- **Build:** Fabric Loom 1.17.19

## Building

```bash
./gradlew build
```

The packaged mod jar is written to `build/libs/`.

## What's new in V2

This is a full rewrite that adds support for the server's second, instanced map
(zones 51+, dimension `minecraft:world2`) alongside the primary map
(zones 1–50, dimension `minecraft:overworld`):

- **Second map / instanced world support.** Events and waypoints are now aware of which
  map they belong to. Map 2 has a single refuge (*La Cité Brumeuse*) reached via
  `refuge tp w2`.
- **New merchant & world boss chat formats.** Handles the updated *Super Marchand* spawn
  header, the coordinates/timer line, and the new departure message, plus zone parsing
  (`Zone N` / `Localisé en Zone N`) to route each event to the correct map.
- **Dimension tag on waypoints.** Every waypoint — automatic *and* manually placed —
  stores its dimension and is only shown in that dimension. Waypoints saved before this
  change (no dimension) stay visible everywhere for backward compatibility.
- **Smarter quick-teleport.** Uses the active event's dimension (or the player's current
  dimension when no event is active) to choose between `refuge tp <n>` (map 1) and
  `refuge tp w2` (map 2).
- **Debug logging** for chat parsing and quick-tp, gated behind the existing `debugMode` flag.

## Notes de version

Historique des modifications par version (branche **26.1.2**). Nom du jar :
`zombiezcompanionV2-<version_minecraft>-<version_mod>`.

### 1.7.0
- **AutoText — envoi auto par preset** : nouvelle option **« Envoi auto »** dans l'éditeur d'un preset.
  Désactivée, le raccourci ou le clic dans la barre **pré-remplit le chat sans envoyer** (message ou
  commande), pour éditer par ex. un pseudo dans une commande avant de valider. Activée (défaut), envoi
  immédiat comme avant.
- **AutoText — sélecteur d'objet visuel** : un clic sur l'icône d'aperçu ouvre une **grille de tous les
  items du jeu avec barre de recherche** ; le champ `minecraft:…` reste dispo pour la saisie manuelle.
- **Annonces temps réel** : un **toast bannière** (centré, en haut, ~5 s) poussé à **tous les joueurs
  en ligne** depuis Discord (commande `/broadcast`). Diffusion instantanée via WebSocket (backend
  Durable Object) ; position ajustable via l'éditeur HUD (« Toast annonce »).
- **Pings de groupe instantanés** : les pings partagés sont désormais **livrés en temps réel** par
  WebSocket (avant : polling jusqu'à ~2,5 s). Le POST est conservé pour la persistance et les membres
  non connectés ; le polling est ralenti et ne sert plus que de filet de sécurité.

### 1.6.0
- **AutoText refondu** (module `auto_text`) façon presets : liste unifiée jusqu'à **100 presets**
  (ajout automatique d'une case vide dès qu'on remplit la dernière), migration des anciennes entrées
  texte+touche. **Barre d'icônes cliquables** affichée quand le chat est ouvert (clic = envoi du
  message/commande) — pratique pour les modérateurs ; position via l'éditeur HUD. Éditeur par preset :
  nom, item de l'icône (aperçu live), visible dans la barre, couleurs texte/fond. Réglages de barre
  (activer, seulement chat ouvert, sens horizontal/vertical).
- **Amis & Groupe** : Amis et Groupe réunis en **un seul module**. Les amis deviennent un **annuaire**
  (on ne les traque plus dans le monde). Nouveau **groupe** (max 4) : créer / inviter / accepter /
  quitter / exclure / léguer le chef ; **visibilité entre membres** (chef en or, membres en vert) ;
  **ping partagé** (touche dédiée : 1 clic pose, double-clic retire) ; **suivi du chef** (rejoue
  automatiquement ses `refuge tp`, option) ; **donjon de groupe auto** (compte à rebours de 3 s,
  sneak pour annuler, option).
- **Crânes** : coordonnées synchronisées sur **CraneMod v1.1.4** (500 crânes) — 13 ajoutés, 2 corrigés.
  Progression des utilisateurs conservée (identifiants de crânes stables).

### 1.5.0
- **Refonte de l'affichage des amis** : rendu depuis l'**entité réelle chargée** (position fluide,
  instantanée), repli sur le snapshot de présence backend hors de portée de rendu. **3 styles de
  marqueur** (auto / waypoint / box), marqueurs **edge-clampés** (ne disparaissent plus hors écran ou
  derrière la caméra), **couleur par ami**. Rafraîchissement de la présence plus réactif.

### 1.4.1
- Version de validation de la **mise à jour intégrée** (aucun changement de gameplay).

### 1.4.0
- **Mise à jour intégrée** : le mod vérifie **toutes les minutes** s'il existe une version plus récente
  (manifeste `update.json` par branche sur GitHub, choisi selon la version Minecraft). Dans le menu, à
  côté de la version : un indicateur **« à jour »** ou un bouton **« METTRE À JOUR »** ; en jeu, un petit
  **bandeau** prévient. Le bouton demande confirmation (fermeture du jeu nécessaire), télécharge le
  nouveau jar dans `zzc-updates/` et ouvre le dossier — il ne reste qu'à remplacer l'ancien jar dans
  `mods/` et relancer.

### 1.3.0
- **Traqueur de mobs custom** (ancien « Capteur de mutants », module `mob_sensor`) : **5 slots de texte**
  où écrire un nom (ou bout de nom) de mob à surligner, chacun avec une **case on/off**. Recherche « au
  max » : id de type, nom custom, nom affiché, tags, et texte des rigs `text_display` (les mutants du
  profané restent trouvables via le slot **« mutant »** activé par défaut). Contour ESP + compteur HUD.
- **Histogramme de spawn** dans **le menu du module** (Événements → Options, un par event) : distribution
  des intervalles observés **par minute** (barres = %), avec un repère sur la médiane.
- **« Prochain spawn probable »** (marchand / World Boss) affiché dans **le menu du module** (pas dans le HUD).
- **Fourchette d'intervalle affinée** : filtre **bidirectionnel** (rejette aussi les intervalles trop
  courts, pas seulement trop longs) → bornes min/max plus fiables. Historique backend porté à 500 spawns.

### 1.2.0
- **Amis / groupe** (nouveau module `friends`, catégorie Joueurs, ON par défaut) : ajoute d'autres
  porteurs du mod **par pseudo** (champ texte) ou depuis la liste des joueurs en ligne, avec demande +
  acceptation ; coche par ami qui tu veux voir, et **TP vers un ami** via le refuge le plus proche
  (même mécanisme que les events WB/Marchand).
- **Voir ses amis** (même monde uniquement) : au-delà de 100 blocs, un **waypoint mobile** visible à
  travers les murs ; en deçà, un **point HUD** discret avec le pseudo.
- Côté réseau : `/presence` transporte désormais `y`, la dimension et l'UUID de compte ; nouveaux
  endpoints `/friends/*`. La position n'est renvoyée que **quand le joueur bouge** (≥ 3 blocs) ou change
  de map, avec un simple *heartbeat* toutes les ~90 s — économe pour le quota gratuit Cloudflare.

### 1.1.0
- **Capteur de mutants** (nouveau module `mob_sensor`, catégorie Événements, OFF par défaut) :
  met en avant les mutants du profané via un **contour ESP violet** (espace écran, visible à
  travers les murs) et un **compteur HUD** (nombre + distance du plus proche). Portée réglable 32–100.
- **Stats d'intervalle de spawn** (World Boss / Marchand) : le timer affiche désormais
  « dernier Xm · intervalle A–Bm », estimé localement depuis l'historique synchronisé. Côté
  backend : `SPAWN_CAP` porté à 200 et nouvel endpoint `GET /spawns/stats` (min/max/médiane).
- **Palette de couleurs** : écran « Couleurs » permettant de cycler la couleur de chaque
  élément d'affichage (cadre mutant, World Boss, Marchand, Faille, etc.), clic droit = défaut.

### 1.0.3
- Bascule sur le **backend live** (Cloudflare Worker) : présence, spawns, leaderboard et feedback.
- **Feedback jusqu'à 5000 caractères**, redirigé vers Discord (découpage automatique en messages).

### 1.0.1
- **Migration Minecraft 1.21.4 → 26.1.2** et rebrand **« Zombiez Companion V2 »**
  (id `zombiezcompanionv2`).
- **Nettoyage** : retrait des modules redondants avec les launchers (zoom, minimap, luminosité,
  coordonnées).
- **Bascule d'infrastructure** de l'ancien mainteneur (Keoz) vers le mainteneur actuel.

## Repository layout

- `src/main/java` — mod sources (official Mojang mappings).
- `src/main/resources` — `fabric.mod.json`, mixins, lang files, and map tile assets.
- `patch-kit/` — reference material used for the rewrite (diagnostic notes and the
  decompiled sources of the original 1.0.2 build).

## License

MIT — see [LICENSE](LICENSE).
