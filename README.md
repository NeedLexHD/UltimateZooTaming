# ULTIMATE ZOO TAMING — Documentation complète des mécaniques
Mod Forge 1.20.1 pour le modpack **Animals Zoo**. Un vrai Zoo Tycoon en survie :
apprivoise, construis des enclos, embauche du personnel, accueille des visiteurs
qui se baladent, mangent, achètent et repartent contents.

═══════════════════════════════════════════════════════════════════════════════
## SOMMAIRE
1. Apprivoisement, nourriture & traits
2. Transport des animaux
3. Les enclos (en VOLUME, bassins jusqu'au fond)
4. Le bien-être (note 0-100)
5. Comment les animaux mangent
6. Les maladies
7. Les employés (métiers, rondes, dortoir, vestiaires)
8. Les visiteurs (IA de parcours, groupes, files d'attente)
9. Les objets des visiteurs (soda, popcorn, glace, barbe à papa, ballons)
10. L'argent
11. La journée, les événements, pause & reset
12. Les objectifs à primes
13. Événements animaux
14. La carte du zoo
15. Zones d'interaction (photo, nourrissage, jet d'eau — animées)
16. Aménagement & ambiance
17. Le tableau de bord
18. La tablette du directeur
19. Le mobilier 3D animé
20. Récapitulatif blocs & items

═══════════════════════════════════════════════════════════════════════════════
## 1. APPRIVOISEMENT, NOURRITURE & TRAITS
Régimes : herbivore / carnivore / piscivore.
**Croquettes** (9 = 3 régimes × 3 qualités) : nourris un animal sauvage avec la
croquette de son régime → apprivoisé. **Fourrage** (fodder/meat/fish) : remplit
les mangeoires.
**Traits** (visibles dans la fiche, avec l'effet) : Glouton, Câlin, Grognon,
Énergique, Robuste, Sociable, ou aucun.

═══════════════════════════════════════════════════════════════════════════════
## 2. TRANSPORT DES ANIMAUX
Cage + filets pour déplacer un animal apprivoisé. Le câlin passe par la Tablette
(§18) ; le Sifflet ne touche jamais un animal assigné à un enclos.

═══════════════════════════════════════════════════════════════════════════════
## 3. LES ENCLOS — en VOLUME
Vrai volume : sol + 4 blocs dessous et **40 au-dessus**. Mini 8 blocs (insectes).
**Toise d'arpenteur** : clic dans un espace clos → flood-fill forme libre. Verre
et blocs pleins = murs (même sous l'eau). Sur les BASSINS, le scan descend
**jusqu'au fond** : toute la colonne d'eau fait partie de l'enclos (poissons à
toute profondeur, mangeoires immergées comptées).
**Sélecteur de parcelle** (mode Enclos) : 2 coins = enclos rectangulaire.
**Panneau de zoo** : diagnostic complet au clic.
**Supprimer un enclos** : onglet Enclos → bouton (2 clics), animaux libérés.

═══════════════════════════════════════════════════════════════════════════════
## 4. LE BIEN-ÊTRE — note 0-100
Espace /30 · Habitat /25 · Nourriture /20 (mangeoire du bon régime n'importe où
dans l'enclos) · Compagnie /15 (congénères) · Santé /10. Note zoo = moyenne des
enclos ×0.8 + espèces − malades.

═══════════════════════════════════════════════════════════════════════════════
## 5. COMMENT LES ANIMAUX MANGENT
Les animaux se DÉPLACENT vers une mangeoire de leur régime (ZooEatGoal).
**Mangeoires** : 6 styles, WATERLOGGABLES (posables dans l'eau). Le Nourrisseur
nage/plonge pour remplir les mangeoires aquatiques.

═══════════════════════════════════════════════════════════════════════════════
## 6. LES MALADIES
Malade (✚) → Vétérinaire ou Remède animalier. 1 sur 4 est GRAVE (✚✚) → Remède
supérieur (ton job).

═══════════════════════════════════════════════════════════════════════════════
## 7. LES EMPLOYÉS
Salaire prélevé sur la Caisse ; Caisse vide = GRÈVE. **5 métiers** (bouton dans
l'onglet Enclos) : Polyvalent / Vétérinaire (1 par enclos) / Nourrisseur (tour
du parc) / Garde / Vendeur.

**RONDES CRÉDIBLES** (le jour, quand pas de tâche urgente) :
- Vétérinaire & Polyvalent : font le tour de LEUR enclos et s'arrêtent près des
  animaux comme s'ils les **inspectaient** (2-4s chacun).
- Nourrisseur & Garde : circulent **entre les enclos** (tour du parc).
- Ils **saluent le directeur** (toi) quand tu passes près d'eux (petit signe,
  cooldown 10-20s).

**Vitesse dans l'eau** : les soigneurs nagent activement (traversent les bassins
sans lambiner) — modéré pour rester crédible.
**Protection** : employés, visiteurs et animaux apprivoisés ne subissent AUCUN
dégât, ne sont pas ciblés par les mobs, et les mobs ne s'attaquent pas entre eux.

**Embauche** : Ordinateur de recrutement (3 candidats/jour) ou Sifflet.
**Dortoir** (Sélecteur, mode Dortoir) + lits vanilla : la NUIT, chaque employé
dort dans un vrai lit libre (jamais dans le vide). Le repos au vestiaire ne se
fait QUE zoo fermé — le jour, ils travaillent.
**Vestiaires par métier** (5 blocs teintés) : Polyvalent bois, Vétérinaire vert,
Nourrisseur jaune, Garde rouge, Vendeur bleu.

═══════════════════════════════════════════════════════════════════════════════
## 8. LES VISITEURS (IA de parcours)
Apparaissent à l'Entrée si : zoo ouvert, note correcte, entrée dans le
territoire, pas d'évasion.

**Cerveau de visite** — vrai parcours planifié :
1. Fatigué (>75) → va s'ASSEOIR sur un banc, récupère, repart
2. Soif/Faim (>70) → stand boissons/repas (sinon il RÂLE)
3. Sinon → voir un enclos pas encore visité (choisi AU HASARD parmi les 3 plus
   proches), s'attarde et réagit selon le bien-être réel (heureux/triste)
4. Entre deux → une borne d'interaction (photo/nourrissage/jet d'eau)
5. Content → un souvenir
6. Il ne part QU'APRÈS avoir vu TOUS les enclos

**Flânerie** : temps d'observation variable selon le RYTHME du visiteur (pressé
~2.5s / normal ~5s / contemplatif ~9s, tiré au spawn) et la beauté de l'enclos.
**Émotes** au-dessus de la tête : cœur (bel enclos), goutte (soif/faim), fumée
(enclos vide), paillettes (content).
**Boutiques/bornes** choisies au hasard parmi les plus proches.
**Escaliers** : les visiteurs les montent et les empruntent (comptés comme allée).

**GROUPES / FAMILLES** : ~1 visiteur sur 3 arrive accompagné de 1-2 autres, dont
~40% d'ENFANTS (plus petits). Les accompagnants SUIVENT le chef de groupe à
travers le parc (les enfants trottinent pour suivre).

**FILES D'ATTENTE** : aux boutiques et bornes, les visiteurs se placent en file
derrière ceux déjà là (patients ~15s) au lieu de se superposer.

═══════════════════════════════════════════════════════════════════════════════
## 9. LES OBJETS DES VISITEURS
Les visiteurs tiennent un vrai objet 3D en main (comme un villageois) :
- 🥤 **Soda**, 🍿 **Popcorn**, 🍦 **Glace**, 🍭 **Barbe à papa** — CONSOMMABLES
  par le joueur aussi (le soda se boit, les autres se mangent)
- 🎈 **Ballon** en 6 couleurs (rouge, bleu, vert, jaune, rose, violet), avec
  ficelle — non consommable
Ils en ont un au spawn (~1/3, ballon pour les enfants) et en achètent aux stands
(soda=boissons, snack=repas, ballon=souvenir).

═══════════════════════════════════════════════════════════════════════════════
## 10. L'ARGENT
**Caisse du Zoo** : trésorerie unique. **Caisse enregistreuse** : 5 types
(Souvenirs/Repas/Glaces/Boissons/Photos), stock 9 slots, prix libres. AUCUNE
VENTE SANS VENDEUR à ≤3 blocs. Billet : Bas ×0.5 / Normal / Cher ×1.75.

═══════════════════════════════════════════════════════════════════════════════
## 11. LA JOURNÉE, ÉVÉNEMENTS, PAUSE & RESET
Ouvre à l'aube, ferme à la nuit (bilan + graphe 7 jours).
**Événement du jour** (1/3) : Promo / Inspection sanitaire / Canicule.
**Bouton PAUSE** (onglet Direction) : ferme le zoo temporairement (même le jour,
les visiteurs ne viennent plus) ; "Rouvrir" le relance. Sauvegardé.
**Bouton RÉINITIALISER** (2 clics) : jour 0, argent/stats à zéro. Ne touche NI
enclos NI animaux NI bâtiments.

═══════════════════════════════════════════════════════════════════════════════
## 12. LES OBJECTIFS À PRIMES (10)
5 espèces (50◆) · 10 espèces (100◆) · 20 animaux (100◆) · note 25 (30◆) · note
50 (80◆) · note 70 (150◆) · note 90 (300◆) · 100 visiteurs (120◆) · 1000◆ gagnées
(200◆) · 5 employés (80◆). Versées à la fermeture.

═══════════════════════════════════════════════════════════════════════════════
## 13. ÉVÉNEMENTS ANIMAUX
Vedettes (+20% affluence chacune) · Naissances (bébé auto-assigné, "appuie sur K"
pour nommer) · Évasions (le Garde contient, entrée fermée) · Anti-disparition
(animaux persistants, protégés, chunks du territoire chargés).

═══════════════════════════════════════════════════════════════════════════════
## 14. LA CARTE DU ZOO (zoo_map)
Vue du ciel + marqueurs. 3 modes : Voir / Territoire (revendiquer les chunks,
restent chargés) / Chemins (les visiteurs préfèrent dalles, escaliers, Allée de
zoo, ou blocs ajoutés au bouton).

═══════════════════════════════════════════════════════════════════════════════
## 15. ZONES D'INTERACTION (bornes ANIMÉES GeckoLib)
Trois bornes face à un enclos. Modèles 3D orientés + animations :
- 📸 **Point photo** : le flash s'allume (anim), étoiles + son
- 🥕 **Borne de nourrissage** : le couvercle s'ouvre, cœurs sur l'animal (+2)
- 💦 **Jet d'eau** (canon métal type lance) : la buse tire, jet balistique qui
  retombe sur l'animal (+3)
Le visiteur paie 2◆. **CLIC DROIT** sur une borne = déclenche l'effet toi-même
(pour tester).

═══════════════════════════════════════════════════════════════════════════════
## 16. AMÉNAGEMENT & AMBIANCE
Ambiance 0-10 par enclos (fleurs, feuillages, ARBRES hauts, lanternes, glace/
neige, eau, bancs autour) → jusqu'à +40% d'affluence. Banc (les fatigués
s'assoient), Poubelle (déchets).

═══════════════════════════════════════════════════════════════════════════════
## 17. LE TABLEAU DE BORD (Bâton de gestion) — 4 onglets
- **Enclos** : zones, animaux, renommage, affectation employés + métier, Supprimer
- **Employés** : zones de repos + liste du personnel (affichage seul : nom+métier,
  l'affectation/rôle se gère dans l'onglet Enclos)
- **Boutiques** : caisses, type, stock, vendeur
- **Direction** : note décomposée, affluence, bilan, graphe 7j, avis, boutons
  Objectifs / Habitats / prix billet / Pause / Réinitialiser

═══════════════════════════════════════════════════════════════════════════════
## 18. LA TABLETTE DU DIRECTEUR (director_tablet)
Clic droit sur un animal = fiche (nom, espèce, trait+effet, santé, note, 5
barres) + câlin du directeur (+5 satisfaction, cooldown 5 min).

═══════════════════════════════════════════════════════════════════════════════
## 19. LE MOBILIER 3D ANIMÉ (GeckoLib)
Caisse enregistreuse (tiroir+clochette), Caisse du Zoo (molette+porte), Entrée
(tourniquet), Panneau/Banc/Poubelle, Mangeoires, Vestiaires, et les 3 Bornes
d'interaction (photo/nourrissage/jet d'eau).

═══════════════════════════════════════════════════════════════════════════════
## 20. RÉCAPITULATIF — BLOCS & ITEMS

**Blocs (22)** : cash_register, zoo_vault, zoo_entrance, zoo_sign, zoo_bench,
zoo_bin, zoo_path, recruitment_computer, photo_spot, feed_station, water_jet,
keeper_locker ×5, feeder ×6.

**Items visiteurs (NEW)** : visitor_soda, visitor_popcorn, visitor_icecream,
visitor_cotton_candy (consommables), balloon ×6 couleurs.

**Items outils** : surveyor_staff, plot_selector, zoo_map, zoo_guide, whistle,
director_tablet, animal_remedy, super_remedy, fodder ×3, croquettes ×9,
occupied_container, zoo_keeper_spawn_egg.

═══════════════════════════════════════════════════════════════════════════════
FIN — pour signaler un doublon ou une incohérence, cite la section concernée.
