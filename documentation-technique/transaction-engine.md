# Transaction Engine

Ce document formalise le moteur métier `transaction` du projet NeoLedger.

Il couvre:

- la lecture des fichiers SEPA
- le modèle de transaction parsé
- le rôle du `pain.001` et du `pain.008`
- la relation avec le compte métier
- l'outbox de publication

---

## 1. Rôle du moteur transaction

Le moteur `transaction` a pour rôle de:

1. recevoir un fichier SEPA
2. le parser
3. en extraire des transactions métier
4. transformer ces transactions en événements ou messages internes
5. les publier via une outbox

Le moteur ne porte pas la logique de solde du compte. Cette logique vit dans `Account`.

---

## 2. Formats SEPA

### `pain.001`

Le `pain.001` représente une instruction de virement initiée par un débiteur.

Dans le contexte métier:

- le `debtor` est le payeur
- le `creditor` est le bénéficiaire

### `pain.008`

Le `pain.008` représente une demande de prélèvement.

Dans le contexte métier:

- le `creditor` initie la collecte
- le `debtor` est le compte prélevé

---

## 3. Modèle de transaction parsée

Le domaine `transaction` manipule une représentation parsée, immuable, avant toute persistance ou publication.

### Champs typiques

- `endToEndId`
- `debtorIban`
- `creditorIban`
- `amount`
- `currency`
- `requestedDate`
- `isInstant`
- `remittanceInfo`
- `mandateId`
- `creditorSchemeId`
- `paymentType`

### Rôle métier

Cette représentation sert à:

- préserver les données utiles extraites du fichier
- éviter de dépendre directement du format XML
- transmettre une transaction standardisée au reste du système

---

## 4. Relation avec `Account`

Le moteur `transaction` ne modifie pas directement les soldes du compte.

Il produit un flux métier exploitable par le moteur `Account`.

### Interprétation métier

- `debtorIban` identifie le compte source
- `creditorIban` identifie le compte destination
- `amount` est le montant à appliquer
- `currency` est la devise commune

Selon le type de flux:

- un `pain.001` peut déclencher un débit du compte débiteur
- un `pain.008` peut déclencher une logique de prélèvement côté compte débiteur

---

## 5. Outbox

L'outbox est utilisée pour garantir la publication fiable des messages générés à partir des transactions parsées.

### Rôle

- garder une trace persistante des messages à publier
- découpler l'ingestion du fichier de la publication Kafka ou autre bus
- permettre des reprises en cas d'échec

### Principes

- l'ingestion doit rester atomique
- une erreur pendant le traitement annule le lot
- un message non publié reste traçable

---

## 6. Contrat du message brut

Le message brut partagé dans `common` contient les champs métiers essentiels:

- `end_to_end_id`
- `debtor_iban`
- `creditor_iban`
- `amount`
- `currency`
- `requested_date`
- `is_instant`
- `remittance_info`
- `mandate_id`
- `creditor_scheme_id`
- `payment_type`

Ce contrat sert de format pivot entre le parsing et la publication.

---

## 7. Frontiere de responsabilité

### Ce que fait le moteur transaction

- parser les fichiers SEPA
- normaliser les transactions
- préparer les messages de sortie
- piloter l'outbox

### Ce qu'il ne fait pas

- calculer les soldes
- gérer les invariants de compte
- décider seul des règles de réservation ou de débit du compte

---

## 8. Lecture métier

Un flux simplifié:

1. un fichier arrive
2. le parser en extrait une liste de transactions
3. chaque transaction est normalisée
4. un message brut est produit
5. le message est poussé dans l'outbox
6. la publication est réalisée ensuite de façon fiable

---

## 9. Séparation des modèles

Le moteur transaction manipule trois niveaux de représentation:

- **XML SEPA**: format d'entrée
- **Transaction parsée**: modèle métier interne
- **Message brut**: contrat de diffusion

Cette séparation évite de faire dépendre le coeur métier du format ISO 20022.
