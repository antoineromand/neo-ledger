# Plan D'Action Pour Ameliorer L'Implementation DDD

## Objectif

Ce document transforme la revue DDD en liste de taches concretes, priorisees et directement exploitables.

Le but n'est pas de tout refaire, mais de corriger les points qui ont le plus d'impact sur :

- la coherence metier
- la robustesse du cas d'usage
- la clarte des responsabilites
- l'evolutivite du module

## Ordre Recommande

Si tu veux perdre le moins de temps possible, je te conseille cet ordre :

1. securiser le comportement applicatif
2. clarifier la semantique de l'outbox
3. proteger les invariants de fichier
4. nettoyer la frontiere entre technique et domaine
5. enrichir le modele seulement si le besoin metier le justifie

## Priorite 1

### Tache 1. Rendre l'ingestion atomique

**Pourquoi**

Aujourd'hui, l'ingestion d'un fichier peut reussir partiellement. Si une erreur survient au milieu de la boucle d'ecriture dans l'outbox, une partie des transactions peut deja etre persistee.

**Objectif**

Faire en sorte que le traitement d'un fichier soit considere comme une seule unite de travail :

- soit tout est ecrit
- soit rien n'est ecrit

**Ce qu'il faut faire**

- ajouter une vraie frontiere transactionnelle autour du cas d'usage d'ingestion
- verifier que la sauvegarde des entrees d'outbox participe bien a la meme transaction
- s'assurer que les erreurs de mapping ou de persistence provoquent bien un rollback global

**Definition de fini**

- un fichier de N transactions n'est jamais ingere partiellement
- un test prouve qu'une erreur sur une transaction annule l'ensemble du traitement

**Gain**

Tu supprimes le risque le plus critique du module sans refonte majeure.

### Tache 2. Ajouter un test d'echec partiel sur l'outbox

**Pourquoi**

Le comportement atomique ne doit pas rester implicite.

**Objectif**

Verrouiller le comportement attendu avec un test.

**Ce qu'il faut faire**

- simuler un echec de mapping ou de persistence apres une ou plusieurs transactions
- verifier qu'aucune transaction n'est finalement consideree comme validee ou conservee

**Definition de fini**

- un test d'integration ou de service couvre explicitement le rollback complet

**Gain**

Tu evites les regressions futures sur le point le plus sensible.

## Priorite 2

### Tache 3. Redefinir ce que represente `aggregateType` dans l'outbox

**Pourquoi**

Aujourd'hui, le champ qui porte le type de flux sert a la fois a la semantique de routage et a la distinction technique entre `SEPA_PAIN_001` et `SEPA_PAIN_008`.

**Objectif**

Donner une signification claire a cette metadata et arreter d'utiliser un concept d'agregat pour un besoin de routage.

**Ce qu'il faut faire**

- garder un nom qui correspond au role reel du champ
- utiliser ce champ comme cle de routage ou type de message, pas comme type d'agregat
- documenter explicitement que `SEPA_PAIN_001` et `SEPA_PAIN_008` sont des valeurs de routage

**Exemples de direction**

- `Transaction`
- `CreditTransfer`
- `DirectDebit`
- `PaymentInstruction`

**Definition de fini**

- `aggregateType` designe un concept metier compréhensible sans connaitre le format ISO

**Gain**

Tu améliores immédiatement la lisibilite du modele d'evenement et tu previens une dette semantique.

### Tache 4. Revoir la cle d'unicite de l'outbox

**Pourquoi**

L'unicite sur `end_to_end_id` est potentiellement trop forte si cette valeur n'est pas une identite globale garantie par le metier.

**Objectif**

S'assurer que la contrainte d'unicite exprime une vraie regle metier.

**Ce qu'il faut faire**

- verifier si `EndToEndId` est reellement unique a l'echelle du systeme
- si non, redefinir la cle d'unicite
- envisager une unicite composee si le contexte doit entrer en compte

**Questions a trancher**

- unique par fichier ?
- unique par type de flux ?
- unique par emetteur ?
- pas unique du tout dans l'outbox ?

**Definition de fini**

- la contrainte en base correspond a une regle metier explicite et documentee

**Gain**

Tu evites des rejets artificiels ou des collisions mal comprises en production.

### Tache 5. Documenter la semantique de chaque champ de l'outbox

**Pourquoi**

Aujourd'hui, plusieurs champs donnent l'impression d'etre metier alors qu'ils sont techniques ou ambigus.

**Objectif**

Rendre le contrat d'outbox clair pour toi et pour les autres.

**Ce qu'il faut faire**

- ecrire un court document ou JavaDoc indiquant le role de :
- `id`
- `endToEndId`
- `routingKey`
- `eventType`
- `payload`
- `status`
- `retryCount`
- `nextAttemptAt`
- `lastError`
- `createdAt`
- `processedAt`

**Definition de fini**

- il n'y a plus d'ambiguite sur ce que chaque champ veut dire

**Gain**

Tu reduis le temps perdu plus tard sur des incomprehensions de contrat.

## Priorite 3

### Tache 6. Verifier explicitement les invariants du fichier apres parsing

**Pourquoi**

Le header contient deja des informations utiles comme `expectedNbTxs`, mais elles ne sont pas protegees par le traitement.

**Objectif**

Transformer les metadonnees du fichier en regles effectivement controlees.

**Ce qu'il faut faire**

- comparer `expectedNbTxs` avec le nombre reel de transactions parsees
- decider quoi faire si l'ecart existe :
- rejet complet
- erreur metier explicite
- log + rejet

**Definition de fini**

- un fichier incoherent entre header et contenu est refuse proprement

**Gain**

Tu fais passer le modele de "descriptif" a "protecteur d'invariants".

**Etat actuel**

- resolu dans `IngestionService`
- exception metier dediee : `InconsistentPaymentFileException`
- test ajoute dans `IngestionServiceUnitTest`

### Tache 7. Identifier les autres invariants metier minima a proteger

**Pourquoi**

Le nombre de transactions n'est probablement pas le seul point qui merite un controle.

**Objectif**

Definir une premiere liste minimale de regles metier qui doivent vivre dans le coeur du traitement.

**Points a verifier**

- montant obligatoire
- devise obligatoire
- `endToEndId` obligatoire
- identifiants debiteur/crediteur obligatoires
- date demandee obligatoire selon le type de flux
- mandat obligatoire pour un direct debit

**Ce qu'il faut faire**

- lister les regles vraiment importantes
- ne garder que celles qui ont une vraie valeur metier
- les faire porter par le traitement ou le modele

**Definition de fini**

- tu disposes d'une liste courte et defendable d'invariants critiques

**Gain**

Tu evites de sur-modeliser tout en securisant les cas importants.

### Tache 8. Ajouter des tests pour les invariants

**Pourquoi**

Une regle non testee revient vite a une convention implicite.

**Objectif**

Verrouiller les invariants importants.

**Ce qu'il faut faire**

- ajouter un test par invariant critique
- verifier que l'erreur remontee est compréhensible

**Definition de fini**

- les cas incoherents ont des tests dedies

**Gain**

Tu rends les evolutions futures plus sures.

## Priorite 4

### Tache 9. Sortir le parsing XML du "domain"

**Pourquoi**

Le contrat `PaymentParser` est trop technique pour ressembler a un vrai service de domaine.

**Objectif**

Nettoyer la frontiere entre logique metier et logique de lecture de format externe.

**Ce qu'il faut faire**

- deplacer mentalement et structurellement le parsing dans une zone adapter/infrastructure
- faire en sorte que le domaine ne depenne plus de `InputStream` ni d'exceptions XML
- reserver le package `domain` a des concepts metier et a leurs regles

**Approche pragmatique**

Pas besoin de gros big bang. Tu peux faire cela en deux temps :

1. changer l'emplacement et la responsabilite des parseurs
2. ensuite seulement nettoyer les types manipules par le coeur

**Definition de fini**

- le domaine ne connait plus le XML ni les details de streaming

**Gain**

Tu renforces nettement la credibilite DDD du module.

### Tache 10. Renommer les objets "raw" selon leur vrai role

**Pourquoi**

Le prefixe `Raw` montre bien que ces objets sont encore au stade technique ou intermediaire.

**Objectif**

Rendre explicite la difference entre :

- objet technique parse depuis le XML
- objet metier interne

**Ce qu'il faut faire**

- decider si ces types doivent rester des DTO d'ingestion
- si oui, les renommer ou les repositionner hors du domaine
- si non, les enrichir pour qu'ils meritent vraiment leur statut d'objet de domaine

**Definition de fini**

- le nom des objets correspond a leur vraie nature

**Gain**

Tu supprimes une ambiguite importante dans le projet.

### Tache 11. Introduire une couche de traduction entre format externe et modele interne

**Pourquoi**

Aujourd'hui, le modele interne ressemble fortement au format d'entree.

**Objectif**

Mieux isoler le module contre les variations du schema ISO 20022.

**Ce qu'il faut faire**

- faire produire au parseur une representation technique assumee
- traduire ensuite cette representation vers un modele interne plus stable
- faire porter les regles metier apres cette traduction

**Definition de fini**

- un changement de schema XML affecte surtout la couche de parsing et peu le coeur

**Gain**

Tu gagnes en evolutivite et en resistance au couplage technique.

## Priorite 5

### Tache 12. Decider si tu veux un vrai domaine riche ou un pipeline d'ingestion propre

**Pourquoi**

C'est la decision structurante. Sans elle, tu risques de faire des refactors contradictoires.

**Objectif**

Clarifier l'ambition du module.

**Deux options saines**

- Option A : assumer un module d'ingestion technique bien structure
- Option B : investir dans un vrai modele DDD metier

**Si tu choisis l'option A**

- minimise les notions DDD trop ambitieuses
- traite le module comme un pipeline d'ingestion hexagonal
- garde le domaine leger

**Si tu choisis l'option B**

- identifie les vraies entites
- identifie les agregats
- explicite les invariants metier
- donne plus de comportement aux objets du coeur

**Definition de fini**

- le module a une direction architecturale claire

**Gain**

Tu evites de rester entre deux styles.

### Tache 13. Enrichir le modele uniquement si la complexite metier le justifie

**Pourquoi**

Un modele riche n'est utile que si tu as de vraies regles, pas juste un flux de transformation.

**Objectif**

Eviter le sur-design.

**Ce qu'il faut faire**

- n'ajouter du comportement au modele que s'il encapsule une vraie regle
- eviter d'introduire artificiellement des aggregates ou value objects juste pour "faire DDD"

**Definition de fini**

- chaque nouvel objet metier introduit a une raison fonctionnelle claire

**Gain**

Tu gardes un design propre sans inflation de concepts.

## Priorite 6

### Tache 14. Aligner les versions de formats reellement supportees

**Pourquoi**

Le projet contient un XSD `pain.001.001.12`, mais le code ne supporte explicitement que `pain.001.001.03`.

**Objectif**

Eliminer le flou entre support affiche et support reel.

**Ce qu'il faut faire**

- decider quelles versions sont officiellement supportees
- supprimer ce qui n'est pas utilise
- ou brancher proprement ce qui doit etre supporte
- completer les tests en consequence

**Definition de fini**

- il n'existe plus de faux signal dans les ressources ou dans le code

**Gain**

Tu reduis les mauvaises surprises fonctionnelles.

### Tache 15. Ajouter une matrice simple de support de formats

**Pourquoi**

Quand plusieurs versions de normes existent, il faut que cela soit visible rapidement.

**Objectif**

Documenter :

- format
- version
- namespace
- parseur
- validateur
- statut de support

**Definition de fini**

- une personne peut comprendre en 30 secondes ce que le module accepte vraiment

**Gain**

Tu gagnes du temps sur toute future evolution de format.

## Backlog Court Terme Recommande

Si tu veux un plan ultra pragmatique sur peu de jours, voici la meilleure sequence :

1. rendre l'ingestion atomique
2. ajouter le test de rollback global
3. redefinir la semantique du champ de routage dans l'outbox
4. revoir l'unicite de `endToEndId`
5. clarifier le support des versions `pain.001`
6. deplacer le parsing XML hors du domaine

## Backlog Moyen Terme

Quand les points critiques sont regles :

1. introduire une vraie traduction entre objets techniques et objets internes
2. decider explicitement si le module reste un pipeline ou devient un domaine riche
3. enrichir le modele seulement sur les regles qui ont une vraie valeur metier

## Ce Qu'Il Ne Faut Pas Faire

Pour gagner du temps, evite ces erreurs :

- renommer massivement les packages sans corriger d'abord les problemes de comportement
- introduire plein de `ValueObject` juste pour faire "plus DDD"
- refondre tout le module avant d'avoir clarifie les invariants critiques
- confondre meilleur packaging et meilleur domaine

## Definition De Succes

Tu pourras considerer le module nettement amelioré quand :

- un fichier n'est plus jamais ingere partiellement
- les invariants de fichier sont verifies explicitement
- l'outbox porte une semantique claire et documentee
- le domaine ne depend plus directement du XML
- le support des formats est sans ambiguite

## Conclusion

Le meilleur retour sur investissement est simple :

- d'abord la consistance transactionnelle
- ensuite la semantique de l'outbox
- puis la protection des invariants
- enfin le nettoyage DDD plus structurel

Si tu commences par la modelisation "pure" avant de traiter ces points, tu vas depenser plus d'energie pour un gain moindre.
