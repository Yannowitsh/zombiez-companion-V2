# ZombieZ Companion — V2

Client-side, read-only quality-of-life companion mod for the **ZombieZ** Minecraft server.
Provides modular overlays and helpers (minimap, waypoints, event beacons/timers, HUD, etc.);
it never automates gameplay.

- **Minecraft:** 26.1.2 **and** 1.21.4 (single Stonecutter source → one jar per version)
- **Mod loader:** Fabric Loader ≥ 0.16.10 (1.21.4) / ≥ 0.19.3 (26.1.2)
- **Java:** 21 (1.21.4) / 25 (26.1.2)
- **Mappings:** official Mojang mappings (26.1.2 ships deobfuscated; 1.21.4 uses Mojmap via architectury-loom)
- **Build:** Stonecutter 0.9 — Fabric Loom 1.17.19 (26.1.2) / architectury-loom (1.21.4). `./gradlew "Set active project to <id>-fabric"` then `build`.

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

Historique des modifications par version. Nom du jar :
`zombiezcompanionV2-<version_minecraft>-<version_mod>`. Depuis 1.8.0, deux jars par version
(26.1.2 et 1.21.4) ; l'updater in-mod lit le canal correspondant (branche `26.1.2` ou `master`).

### 1.10.1
- **Bannière de mise à jour déplaçable** : la notification « mise à jour disponible » se personnalise
  désormais depuis le bouton de customisation HUD comme les autres éléments (position, taille), au lieu
  d'être fixe. Position par défaut : juste au-dessus du timer Marchand.

### 1.10.0
- **Statut en ligne corrigé pendant l'AFK** : rester immobile plus de 3 min ne fait plus disparaître de
  Discord ni de la liste d'amis — la diffusion de position se met en pause, mais la position affichée
  reste exacte puisqu'elle n'a pas changé.
- **Carte plein écran retirée** : redondante avec la carte déjà fournie par le serveur, et source de
  requêtes réseau inutiles. Le guidage vers un waypoint ou un crâne reste disponible depuis leurs
  propres écrans (Gestionnaire de waypoints, Crânes).
- **Optimisation réseau** : Amis & Groupe ne rafraîchit plus en continu en arrière-plan — uniquement
  pendant que cet écran est réellement ouvert.
- **Touches rapides** (non assignées par défaut, configurables dans les raccourcis clavier) : ouverture
  directe d'Amis & Groupe, d'AutoText, et du Traqueur de mobs.

### 1.9.3
- **Présence migrée vers le hub temps réel** : la carte, le statut en ligne des amis, le TP rapide et le
  badge « utilise le mod » passent désormais par le **WebSocket** déjà utilisé pour le suivi de groupe,
  au lieu d'écritures en base à chaque déplacement. Plus réactif, et supprime la principale source de
  coût d'infrastructure liée à la présence.
  ⚠️ **Nécessite cette mise à jour** : les versions antérieures ne montreront plus personne en ligne tant
  qu'elles ne sont pas mises à jour (tout le reste — événements, groupe, amis par pseudo, AutoText,
  etc. — continue de fonctionner normalement).
- **Optimisations réseau** : la carte ne rafraîchit la liste des joueurs que lorsqu'elle est réellement
  ouverte ; Amis/Groupe utilisent désormais une requête ciblée à leurs seuls membres au lieu du roster
  complet ; anti-AFK (coupe la diffusion de position après 3 min immobile, reprend au premier mouvement) ;
  roster Discord « en ligne » espacé à 3 min.
- **Bouton « Notes de version »** dans le menu (à côté de Feedback) : ouvre un aperçu des derniers
  changements sans quitter le jeu.

### 1.9.2
- **Infrastructure** : l'API du mod passe sur un **domaine dédié** (`zombiez.yannowitsh.dev`) — plus robuste
  et prêt pour des protections anti-abus renforcées. Transition transparente, rien à faire côté joueur.
- **Backend durci** : validation stricte des écritures et garde-fous anti-spam (invisible en jeu).

### 1.9.1
- **Amis cross-version corrigés** : l'identité amis/présence est désormais alignée sur le **vrai UUID
  Mojang** (résolu côté serveur), au lieu de l'UUID attribué par le serveur qui **diffère selon la version**.
  Les joueurs **26.1.2 et 1.21.4 peuvent enfin s'ajouter et se voir en ligne** entre versions. Les amitiés
  existantes sont **migrées automatiquement**.
- **Ajout d'ami instantané** : demandes et acceptations d'amis arrivent en **temps réel** (WebSocket) au
  lieu d'un délai pouvant atteindre ~30 s ; le polling de secours passe de 15 à 8 s.
- **Contenu du marchand partagé** : quand un utilisateur du mod **ouvre le SUPER Marchand**, ses objets
  vedettes s'affichent en **icônes à côté du timer marchand, pour tout le monde**, tant qu'il est en vie
  (~5 min) — on voit d'un coup d'œil ce qu'il vend avant d'y aller. Un objet **acheté** se barre d'une
  **croix rouge** (info perso) ; les icônes disparaissent au départ du marchand.

### 1.9.0
- **Timer du Monarque Damné** : compte à rebours **synchronisé en ligne** vers le prochain spawn du boss
  (respawn fixe d'1 h). Détecté à sa mort dans le chat (« LE MONARQUE DAMNÉ a été terrassé »), le chrono
  repart à 60 min pour tout le groupe. Dernière minute : passage **en secondes + clignotement** sur le HUD
  et **alerte sonore** configurable ; sinon affiche « Boss en vie ✓ ». Élément HUD déplaçable.
- **Roue de ping** : maintenir la touche de ping ouvre une **roue radiale** (caméra figée) pour choisir la
  catégorie — **Danger / Loot / Aide / Ennemi** (haut/bas/gauche/droite) ; un simple appui pose un ping
  générique. Un léger mouvement suffit à sélectionner. Couleur et libellé du ping selon la catégorie.
- **Suivi du chef en temps réel** : le suivi de téléport refuge/spawn passe aussi par **WebSocket**
  (livraison instantanée), en plus du POST + polling conservés comme filet de sécurité.
- **Alertes sonores de spawn configurables** : au spawn d'un **boss** ou d'un **marchand**, son vanilla
  au choix. Sélection façon sélecteur d'objet — **clic ouvre une liste de ~30 sons** (avec aperçu au clic)
  dont l'option **« Aucun »** (défaut). Volume réglable.
- **Menus d'options repliables** : les grosses sections de l'écran **Mini-Events** (Événements / Timers /
  Sons / Raccourci) se **plient/déplient** via une encoche `▾`/`▸` ; l'état est mémorisé.
- **Nettoyage de l'updater**.

### 1.8.0
- **Support multi-version (26.1.2 + 1.21.4)** : le mod se construit désormais pour **deux versions de
  Minecraft** à partir d'une seule base de code (Stonecutter). Les joueurs en **1.21.4** ont leur propre
  jar et reçoivent les mises à jour (canal `master`), en plus de la 26.1.2. Parité complète des
  fonctionnalités entre les deux versions.
- **Groupe — confirmation d'invitation** : le bouton « Inviter » passe à **« Invitation envoyée »**
  après le clic, pour confirmer que la demande est partie.
- **Groupe — suivi du chef sur tout téléport refuge** : les membres suivent maintenant aussi quand le
  chef choisit un refuge via le **menu `/refuge`** ou fait **`/spawn`** (avant : uniquement la commande
  `/refuge tp <n>`). Détection par position, **armée par l'ouverture de `/refuge`** pour ne jamais suivre
  un sort/blink.
- **Groupe — auto-join de donjon instantané** : plus d'attente avant de rejoindre (le chef valide déjà
  le lancement).
- **AutoText — aperçu au survol** : chat ouvert, **survoler une icône de preset** dans la barre affiche
  un aperçu du **texte/commande** avant de cliquer.

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
