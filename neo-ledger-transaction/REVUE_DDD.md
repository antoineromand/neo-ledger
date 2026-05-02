# Revue De L'Implementation DDD

## Contexte

Cette revue porte sur le module `neo-ledger-transaction`, avec un focus sur la qualite de l'implementation DDD, la coherence des frontieres applicatives et l'alignement entre le modele et les cas d'usage reels.

L'objectif ici n'est pas de juger le projet sur son packaging seul, mais sur les responsabilites effectivement portees par les classes, les contrats exposes et les invariants qui sont ou non proteges par le modele.

## Synthese

L'implementation actuelle est plus proche d'une architecture hexagonale orientee ingestion XML que d'un domaine metier DDD pleinement exprime.

Les points positifs sont reels :

- la separation `web` / `application` / `domain` / `infrastructure` est lisible
- les ports sortants existent et evitent un couplage direct du service applicatif a JPA
- les parseurs et validateurs sont injectes par strategie, ce qui rend le module extensible
- le flux principal de traitement est simple a suivre

En revanche, plusieurs points importants limitent la qualite DDD de l'ensemble :

- le "domaine" reste fortement modele autour du format technique XML
- les invariants metier de fichier ne sont pas portes ni verifies par le modele
- l'unite de travail d'ingestion n'est pas atomique
- certaines notions stockees dans l'outbox relevent du transport et non d'un vrai aggregate métier

## Ce Qui Va Dans Le Bon Sens

### Separation des couches

La structure du module est saine sur le plan de l'organisation :

- `web` expose le point d'entree HTTP
- `application` orchestre le cas d'usage
- `domain` contient les contrats et les objets manipules par le coeur
- `infrastructure` contient les implementations techniques

Cette base est correcte pour une architecture de type hexagonal ou clean architecture.

### Ports et strategies

L'usage de ports comme `TransactionOutboxPort`, `TransactionMapperFactoryPort` et `XmlValidator` est pertinent.

Cela apporte :

- une meilleure testabilite
- une responsabilite plus claire des adapters techniques
- une extension plus simple vers d'autres formats ou d'autres sorties

### Orchestration applicative lisible

`IngestionService` joue bien le role d'orchestrateur applicatif :

1. detection du type de fichier
2. validation XML
3. parsing
4. transformation
5. persistence en outbox

La sequence est comprehensible et facile a maintenir.

## Points DDD Problematiques

### 1. Le domaine est encore tres technique

Le premier point important est que les objets du "domain" sont surtout des representations brutes du flux technique :

- `RawPaymentFile`
- `RawTransaction`
- `RawSepaTransaction`
- `PaymentParser` qui travaille directement sur `InputStream`

Dans une lecture DDD stricte, cela pose un probleme de niveau d'abstraction.

Le domaine ne manipule pas ici des concepts metier riches, mais des structures de transport parsees depuis du XML. Le prefixe `Raw` dit d'ailleurs explicitement que ces objets sont encore proches de la source technique.

En pratique, cela signifie que :

- le XML n'est pas vraiment isole dans une couche anticorruption ou adapter
- le domaine depend de details de parsing et de format
- le coeur du modele reste defini par le schema d'echange plus que par les regles metier

Conclusion : le package `domain` contient surtout un modele de donnees d'ingestion, pas un domaine metier fort.

### 2. Le contrat `PaymentParser` ne ressemble pas a un service de domaine

`PaymentParser` est place dans `domain.service`, mais son contrat est :

- base sur `InputStream`
- lie a `XMLStreamException`
- centre sur le parsing d'un format externe

Ce type de responsabilite est habituellement plus technique que metier.

Dans une approche DDD plus stricte, ce parsing devrait plutot vivre :

- en infrastructure
- ou dans une couche d'anticorruption
- ou dans un adapter d'entree specialise

Le domaine devrait commencer une fois la representation technique traduite en concepts metier internes.

### 3. L'unite de travail d'ingestion n'est pas atomique

Le point le plus sensible sur le comportement applicatif est l'absence de frontiere transactionnelle explicite autour de l'ingestion.

`IngestionService` boucle sur les transactions parsees et sauvegarde une entree d'outbox pour chacune.

Consequence :

- si 10 transactions doivent etre persistees
- et qu'une erreur survient a la 7e
- les 6 premieres peuvent deja etre en base
- les suivantes non

Autrement dit, un meme fichier peut etre ingere partiellement.

Pour un cas d'usage de commande, c'est un vrai probleme de consistance. Si le fichier constitue une unite metier coherente, son traitement devrait etre reussi ou echouer en bloc.

Ce point est plus qu'un detail technique : il touche directement a la notion d'unite de travail du cas d'usage.

### 4. L'outbox n'exprime pas une vraie identite d'agregat

Le contrat `TransactionOutboxPort.save(String endToEndId, String aggregateType, String eventType, byte[] payload)` melange des notions qui paraissent metier mais qui ne le sont pas vraiment ici.

En particulier :

- `aggregateType` est renseigne avec `SEPA_PAIN_001` ou `SEPA_PAIN_008`
- ce sont des types de format de fichier
- ce ne sont pas des types d'agregat metier

Le risque est double :

- semantique faible dans l'outbox
- ambiguite sur ce qui est reellement l'entite emettrice de l'evenement

Si l'on parle vraiment DDD, un `aggregateType` devrait designer un concept metier du type :

- `Transaction`
- `PaymentOrder`
- `DirectDebit`
- `Transfer`

Pas un format ISO 20022.

### 5. La contrainte d'unicite sur `end_to_end_id` est discutable

Dans `OutboxEntry`, la colonne `end_to_end_id` est marquee `unique = true`.

Cette decision merite d'etre challengee.

Pourquoi :

- `EndToEndId` est souvent une reference de paiement, pas forcement une cle globale absolue du systeme
- il peut etre unique dans un contexte donne sans etre universellement unique
- si deux flux ou deux fichiers reutilisent la meme valeur, le systeme refusera l'ecriture meme si le cas metier est legitime

En DDD, l'identite et les contraintes d'unicite doivent etre portees par une vraie regle metier explicite, pas seulement par une hypothese pratique issue du format d'echange.

En l'etat, on a une contrainte forte, mais sans justification metier visible dans le modele.

### 6. Les invariants du fichier sont parses mais pas proteges

`FileHeader` contient `expectedNbTxs`. C'est une information importante : elle declare combien de transactions le fichier est cense contenir.

Or, apres parsing :

- aucune verification explicite ne compare `expectedNbTxs` avec le nombre reel de transactions extraites
- aucun mecanisme ne protege cet invariant

Donc le modele sait qu'un invariant potentiel existe, mais il ne l'applique pas.

C'est un symptome classique d'un domaine anemique :

- les donnees sont presentes
- les regles ne vivent pas avec elles

Si ce nombre a une vraie valeur metier ou de controle, il devrait etre verifie dans le coeur du cas d'usage ou directement par le modele de fichier.

### 7. Le modele reste anemique

Les `record` du domaine sont propres et simples, mais ils ne portent quasiment aucun comportement.

Par exemple :

- pas de validation interne
- pas d'invariants metier explicites
- pas de comportement lie au cycle de vie
- pas de methodes metier exprimant une intention

Ce n'est pas forcement mauvais si le besoin reste simple, mais ce n'est pas une implementation DDD forte. C'est plutot un modele de transport interne.

## Points D'Architecture Generale

### 1. Le controleur reste mince, ce qui est bien

`TransactionIngestionController` est leger et delegue au cas d'usage. C'est un bon point.

En revanche, il reconstruit lui-meme un `InputStream` a partir d'une `String`, ce qui confirme encore que le flux est pense avant tout comme une pipeline technique de parsing XML.

Ce n'est pas choquant en soi, mais cela renforce l'idee que le coeur du module est actuellement centre sur l'ingestion de format et non sur un domaine metier plus riche.

### 2. Le mapping binaire est une responsabilite d'infrastructure

`SepaTransactionMapper` convertit l'objet interne vers un message binaire partage.

Cette responsabilite est bien placee en infrastructure.

En revanche, cela souligne aussi que l'objet `RawSepaTransaction` sert encore principalement de DTO intermediaire entre :

- le parseur XML
- le serializer evenementiel

Donc il faut etre prudent avant de le qualifier d'objet de domaine au sens fort.

### 3. Les factories sont utiles, mais elles restent tres framework-driven

`PaymentParserFactory`, `XmlValidatorFactory` et `TransactionMapperFactory` reposent sur des listes de beans Spring et un mecanisme `supports(...)`.

C'est efficace et propre.

Mais cela reste une extensibilite technique, pas un mecanisme metier. Dit autrement :

- c'est bien concu pour brancher des strategies
- cela ne renforce pas, en soi, la modelisation du domaine

## Incoherence Fonctionnelle A Surveiller

Un point annexe mais important : le module embarque `pain.001.001.12.xsd`, alors que le code actif ne cable que `pain.001.001.03`.

Cela peut vouloir dire l'une des deux choses suivantes :

- soit la version `001.001.12` est prevue mais pas encore supportee
- soit un support partiel a ete entame puis abandonne

Dans les deux cas, cela cree une zone de flou entre :

- ce que le projet semble annoncer
- ce qu'il supporte reellement

## Lecture Globale

Si je resitue l'ensemble, mon avis est le suivant :

- l'architecture est propre
- le code est lisible
- les couches sont relativement bien decouplees
- le module est testable

Mais du point de vue DDD strict, on est davantage sur :

- un pipeline d'ingestion XML bien structure
- avec ports et adapters
- qu'un coeur de domaine riche exprimant clairement des invariants, des entites, des agregats et leurs comportements

Ce n'est pas un defaut absolu. Cela depend du besoin reel.

Si l'objectif du module est simplement :

- recevoir un fichier ISO 20022
- le valider
- en extraire des transactions
- les pousser en outbox

alors l'implementation est globalement saine.

Si en revanche l'objectif annonce est une implementation DDD forte, alors le gap principal est ici :

- trop de concepts techniques vivent encore dans le "domain"
- pas assez de regles metier sont portees par le modele lui-meme

## Conclusion

Mon evaluation globale est la suivante :

- sur le plan architecture applicative : plutot bonne base
- sur le plan separation technique : correcte et lisible
- sur le plan DDD strict : partielle, encore immature

Le module donne une bonne impression de structure, mais le domaine reste essentiellement descriptif et technique.

Le point le plus important a retenir est probablement celui-ci :

Le projet utilise un vocabulaire et un packaging DDD, mais le coeur reel du module est aujourd'hui un moteur d'ingestion XML avec persistance en outbox, plus qu'un domaine metier fortement modele.

## Verification

La revue a ete faite sans modification du code applicatif. Seul ce document Markdown a ete ajoute.

Les tests du module ont ete verifies avec :

```bash
./gradlew :neo-ledger-transaction:test
```
