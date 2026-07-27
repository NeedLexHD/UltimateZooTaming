<div align="center">

# 🦁 Ultimate Zoo Taming

**Un vrai Zoo Tycoon en survie Minecraft — apprivoise, construis, gère, accueille.**

![Minecraft](https://img.shields.io/badge/Minecraft-1.20.1-brightgreen?style=for-the-badge&logo=creativecommons&logoColor=white)
![Forge](https://img.shields.io/badge/Forge-47%2B-orange?style=for-the-badge)
![GeckoLib](https://img.shields.io/badge/GeckoLib-4.4%2B-blue?style=for-the-badge)
![License](https://img.shields.io/badge/License-MIT-yellow?style=for-the-badge)
![Modpack](https://img.shields.io/badge/Modpack-Animals%20Zoo-purple?style=for-the-badge)
![Author](https://img.shields.io/badge/Author-Lex3D-red?style=for-the-badge)

</div>

---

## 📋 Sommaire

- [🐾 Apprivoisement & Traits](#-apprivoisement-nourriture--traits)
- [🚛 Transport des animaux](#-transport-des-animaux)
- [🏗️ Les enclos](#️-les-enclos--en-volume)
- [❤️ Le bien-être](#️-le-bien-être--note-0-100)
- [🍖 Comment les animaux mangent](#-comment-les-animaux-mangent)
- [🦠 Les maladies](#-les-maladies)
- [👷 Les employés](#-les-employés)
- [🚶 Les visiteurs](#-les-visiteurs)
- [🛍️ Objets des visiteurs](#️-les-objets-des-visiteurs)
- [💰 L'argent](#-largent)
- [📅 La journée & événements](#-la-journée-événements-pause--reset)
- [🏆 Objectifs à primes](#-les-objectifs-à-primes)
- [🐘 Événements animaux](#-événements-animaux)
- [🗺️ La carte du zoo](#️-la-carte-du-zoo)
- [📸 Zones d'interaction](#-zones-dinteraction)
- [🌿 Aménagement & ambiance](#-aménagement--ambiance)
- [📊 Le tableau de bord](#-le-tableau-de-bord)
- [📱 La tablette du directeur](#-la-tablette-du-directeur)
- [🪑 Mobilier 3D animé](#-le-mobilier-3d-animé)
- [📦 Récapitulatif blocs & items](#-récapitulatif--blocs--items)

---

## 🐾 Apprivoisement, nourriture & traits

**Régimes** : herbivore / carnivore / piscivore.

**Croquettes** (9 = 3 régimes × 3 qualités) : nourris un animal sauvage avec la croquette de son régime → apprivoisé.
**Fourrage** (`fodder` / `meat` / `fish`) : remplit les mangeoires.

**Traits** (visibles dans la fiche, avec leur effet) :

| Trait | Effet |
|-------|-------|
| 🍗 Glouton | Mange plus souvent |
| 🤗 Câlin | Satisfaction +bonus directeur |
| 😠 Grognon | Satisfaction difficile à monter |
| ⚡ Énergique | Se déplace plus vite |
| 💪 Robuste | Résiste mieux aux maladies |
| 👥 Sociable | Bonus compagnie élevé |

---

## 🚛 Transport des animaux

Cage + filets pour déplacer un animal apprivoisé. Le câlin passe par la Tablette (§ Tablette du directeur). Le **Sifflet** ne touche jamais un animal déjà assigné à un enclos.

---

## 🏗️ Les enclos — en VOLUME

- Vrai volume : sol + 4 blocs dessous et **40 blocs au-dessus**
- Minimum 8 blocs (insectes)
- **Toise d'arpenteur** : clic dans un espace clos → flood-fill forme libre
- Verre et blocs pleins = murs (même sous l'eau)
- **Bassins** : le scan descend jusqu'au fond — toute la colonne d'eau fait partie de l'enclos
- **Sélecteur de parcelle** (mode Enclos) : 2 coins = enclos rectangulaire
- **Panneau de zoo** : diagnostic complet au clic droit
- **Supprimer un enclos** : onglet Enclos → bouton (2 clics), animaux libérés

---

## ❤️ Le bien-être — note 0-100

| Critère | Points |
|---------|--------|
| Espace | /30 |
| Habitat | /25 |
| Nourriture | /20 |
| Compagnie (congénères) | /15 |
| Santé | /10 |

> **Note zoo** = moyenne des enclos × 0.8 + espèces − malades

---

## 🍖 Comment les animaux mangent

Les animaux se **déplacent** vers une mangeoire de leur régime (`ZooEatGoal`).
**Mangeoires** : 6 styles, **waterloggables** (posables dans l'eau). Le Nourrisseur nage et plonge pour remplir les mangeoires aquatiques.

---

## 🦠 Les maladies

- **Malade** (✚) → Vétérinaire ou Remède animalier
- **Grave** (✚✚, 1 sur 4) → Remède supérieur (craft spécial)

---

## 👷 Les employés

Salaire prélevé sur la Caisse. **Caisse vide = GRÈVE.**

### 5 métiers

| Métier | Rôle | Vestiaire |
|--------|------|-----------|
| 🔧 Polyvalent | Ronde + tâches générales | Bois |
| 🩺 Vétérinaire | 1 par enclos, soins | Vert |
| 🍖 Nourrisseur | Tour du parc, remplit mangeoires | Jaune |
| 🛡️ Garde | Contient les évasions | Rouge |
| 🛒 Vendeur | Reste aux caisses | Bleu |

### Rondes crédibles
- **Vétérinaire & Polyvalent** : font le tour de leur enclos et s'arrêtent près des animaux (inspection 2-4s)
- **Nourrisseur & Garde** : circulent entre les enclos
- **Saluent le directeur** quand tu passes près d'eux (cooldown 10-20s)

### Bandeau de tâche 💬
Au-dessus de chaque employé, une icône + texte indique la tâche en cours en temps réel (visible à 8 blocs). Configurable via `showKeeperTask` dans `ultimatezootaming-client.toml`.

### Autres détails
- **Vitesse dans l'eau** : les soigneurs nagent activement
- **Protection** : employés, visiteurs et animaux apprivoisés sont invulnérables et ignorés par les mobs
- **Embauche** : Ordinateur de recrutement (3 candidats/jour) ou Sifflet
- **Dortoir** : Sélecteur (mode Dortoir) + lits vanilla → chaque employé dort dans un vrai lit libre la nuit

---

## 🚶 Les visiteurs

Apparaissent à l'Entrée si : zoo ouvert, note correcte, entrée dans le territoire, pas d'évasion.

### Cerveau de visite — parcours planifié
1. Fatigué (>75) → s'**assoit** sur un banc, récupère, repart
2. Soif/Faim (>70) → stand boissons/repas (sinon il **râle**)
3. Sinon → voir un enclos pas encore visité (3 plus proches, choix aléatoire)
4. Entre deux → une borne d'interaction
5. Content → achète un souvenir
6. Part **seulement après avoir vu tous les enclos**

### Comportements
- **Rythme** : pressé ~2.5s / normal ~5s / contemplatif ~9s (tiré au spawn)
- **Émotes** au-dessus de la tête : 💚 bel enclos · 💧 soif/faim · 💨 enclos vide · ✨ content
- **Groupes/familles** : 1 visiteur sur 3 arrive avec 1-2 accompagnants, dont ~40% d'enfants
- **Files d'attente** : les visiteurs se placent en file aux boutiques (patience ~15s)
- **Escaliers** : montés et empruntés normalement

---

## 🛍️ Les objets des visiteurs

Les visiteurs tiennent un vrai objet 3D en main :

| Objet | Type | Consommable |
|-------|------|-------------|
| 🥤 Soda | Boisson | ✅ |
| 🍿 Popcorn | Snack | ✅ |
| 🍦 Glace | Snack | ✅ |
| 🍭 Barbe à papa | Snack | ✅ |
| 🎈 Ballon (×6 couleurs) | Souvenir | ❌ |

---

## 💰 L'argent

- **Caisse du Zoo** : trésorerie unique
- **Caisse enregistreuse** : 5 types (Souvenirs / Repas / Glaces / Boissons / Photos), stock 9 slots, prix libres
- **Aucune vente sans Vendeur** à ≤ 3 blocs
- **Prix du billet** : Bas ×0.5 / Normal ×1 / Cher ×1.75

---

## 📅 La journée, événements, pause & reset

- Ouvre à l'aube, ferme à la nuit (bilan + graphe 7 jours)
- **Événement du jour** (1 chance sur 3) : Promo / Inspection sanitaire / Canicule
- **Bouton PAUSE** : ferme temporairement le zoo (visiteurs stoppés), `Rouvrir` le relance
- **Bouton RÉINITIALISER** (2 clics) : jour 0, argent/stats à zéro — ne touche ni enclos, ni animaux, ni bâtiments

---

## 🏆 Les objectifs à primes

| Objectif | Prime |
|----------|-------|
| 5 espèces | 50 ◆ |
| 10 espèces | 100 ◆ |
| 20 animaux | 100 ◆ |
| Note 25 | 30 ◆ |
| Note 50 | 80 ◆ |
| Note 70 | 150 ◆ |
| Note 90 | 300 ◆ |
| 100 visiteurs | 120 ◆ |
| 1000 ◆ gagnées | 200 ◆ |
| 5 employés | 80 ◆ |

> Primes versées à la fermeture du zoo.

---

## 🐘 Événements animaux

- **Vedettes** : +20% d'affluence chacune
- **Naissances** : bébé auto-assigné à l'enclos, appuie sur `K` pour le nommer
- **Évasions** : le Garde contient l'animal, entrée fermée automatiquement
- **Anti-disparition** : animaux persistants, protégés, chunks du territoire toujours chargés

---

## 🗺️ La carte du zoo

3 modes sur l'item `zoo_map` :
- 👁️ **Voir** : vue du ciel + marqueurs
- 🚩 **Territoire** : revendiquer les chunks (restent chargés)
- 🛤️ **Chemins** : les visiteurs préfèrent dalles, escaliers, Allée de zoo, ou blocs personnalisés

---

## 📸 Zones d'interaction

Trois bornes **animées GeckoLib** à placer face à un enclos :

| Borne | Effet | Gain |
|-------|-------|------|
| 📸 Point photo | Flash + étoiles + son | — |
| 🥕 Borne de nourrissage | Couvercle s'ouvre, cœurs sur l'animal | +2 |
| 💦 Jet d'eau | Buse tire, jet balistique | +3 |

Le visiteur paie **2 ◆**. Clic droit sur une borne = déclenche l'effet toi-même (test).

---

## 🌿 Aménagement & ambiance

Ambiance **0-10** par enclos selon : fleurs, feuillages, arbres hauts, lanternes, glace/neige, eau, bancs autour → jusqu'à **+40% d'affluence**.

- **Banc** : les visiteurs fatigués s'assoient
- **Poubelle** : collecte les déchets

---

## 📊 Le tableau de bord

Item : **Bâton de gestion** — 4 onglets :

| Onglet | Contenu |
|--------|---------|
| 🏗️ Enclos | Zones, animaux, renommage, affectation employés, Supprimer |
| 👷 Employés | Zones de repos, liste du personnel |
| 🛒 Boutiques | Caisses, type, stock, vendeur |
| 📊 Direction | Note, affluence, bilan, graphe 7j, avis, boutons de gestion |

---

## 📱 La tablette du directeur

Clic droit sur un animal = fiche complète :
- Nom, espèce, trait + effet
- Santé, note, 5 barres de stats
- **Câlin du directeur** : +5 satisfaction (cooldown 5 min)

---

## 🪑 Le mobilier 3D animé

Tous les blocs fonctionnels ont un **modèle GeckoLib** avec animations :

- Caisse enregistreuse (tiroir + clochette)
- Caisse du Zoo (molette + porte)
- Entrée (tourniquet)
- Panneau, Banc, Poubelle
- Mangeoires (×6 styles)
- Vestiaires (×5 couleurs)
- Bornes d'interaction (photo / nourrissage / jet d'eau)

---

## 📦 Récapitulatif — blocs & items

<details>
<summary><b>📦 Blocs (22)</b></summary>

`cash_register` `zoo_vault` `zoo_entrance` `zoo_sign` `zoo_bench` `zoo_bin` `zoo_path` `recruitment_computer` `photo_spot` `feed_station` `water_jet` `keeper_locker ×5` `feeder ×6`

</details>

<details>
<summary><b>🛍️ Items visiteurs</b></summary>

`visitor_soda` `visitor_popcorn` `visitor_icecream` `visitor_cotton_candy` *(consommables)* · `balloon` ×6 couleurs

</details>

<details>
<summary><b>🔧 Items outils</b></summary>

`surveyor_staff` `plot_selector` `zoo_map` `zoo_guide` `whistle` `director_tablet` `animal_remedy` `super_remedy` `fodder ×3` `croquettes ×9` `occupied_container` `zoo_keeper_spawn_egg`

</details>

---

<div align="center">

Fait avec ❤️ par **Lex3D** — pour signaler un bug ou une incohérence, [ouvre une issue](https://github.com/NeedLexHD/UltimateZooTaming/issues).

</div>
