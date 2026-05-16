# Account Engine

Ce document formalise le moteur métier `Account` du projet NeoLedger.

Il couvre:

- le modèle de domaine
- le value object `BalanceAmount`
- les statuts et transitions
- les règles de réservation et de libération
- le lien avec la persistance

---

## 1. Domaine `Account`

### Champs métier

Le domaine conserve uniquement les données utiles au métier:

- `identity`
- `iban`
- `bic`
- `currentBalance`
- `reservedBalance`
- `currency`
- `accountType`
- `accountStatus`

### Champs techniques exclus du domaine

Ces informations sont considérées comme techniques et ne font pas partie du coeur métier:

- `createdAt`
- `updatedAt`

---

## 2. Value Object `BalanceAmount`

`BalanceAmount` représente un montant monétaire immuable.

### Invariants

- `amount` ne peut pas être `null`
- `amount` ne peut pas être négatif
- l'objet est immuable

### Opérations

- `credit(amount)`
  - `amount` doit être strictement positif
  - retourne un nouvel objet
- `debit(amount)`
  - `amount` doit être strictement positif
  - refuse si le solde deviendrait négatif
  - retourne un nouvel objet
- `isPositive()`
- `isGreaterThan(otherAmount)`

---

## 3. Statuts du compte

### Statuts

- `ACTIVE`
- `BLOCKED`
- `SUSPENDED`
- `CLOSED`

### Politique métier

- `ACTIVE` autorise les opérations d'argent
- `BLOCKED` interdit les opérations d'argent
- `SUSPENDED` interdit les opérations d'argent
- `CLOSED` interdit toutes les opérations

### Transitions autorisées

- `ACTIVE -> BLOCKED`
- `ACTIVE -> SUSPENDED`
- `ACTIVE -> CLOSED`
- `BLOCKED -> ACTIVE`
- `BLOCKED -> CLOSED`
- `SUSPENDED -> ACTIVE`
- `SUSPENDED -> CLOSED`

### Transitions interdites

- toute transition depuis `CLOSED`
- toute transition vers le même statut

---

## 4. Règles de balances

Le compte manipule deux soldes:

- `currentBalance`
- `reservedBalance`

### Crédit

`creditCurrentBalance(amount)`:

- compte `ACTIVE` uniquement
- `amount > 0`
- augmente `currentBalance`

### Débit

`debitCurrentBalance(amount)`:

- compte `ACTIVE` uniquement
- `amount > 0`
- refuse si `currentBalance` deviendrait négatif

### Réservation

`reserveFunds(amount)`:

- compte `ACTIVE` uniquement
- `amount > 0`
- refuse si `amount > currentBalance - reservedBalance`
- augmente `reservedBalance`

### Libération

`releaseFunds(amount)`:

- compte `ACTIVE` uniquement
- `amount > 0`
- diminue `reservedBalance`

---

## 5. Règles d'intégrité

- `id` est immuable
- `iban` est immuable
- `bic` est immuable
- `type` est immuable
- `currency` est immuable
- `reservedBalance` ne doit jamais dépasser le disponible
- les opérations d'argent sont interdites hors `ACTIVE`

---

## 6. Persistance

La persistance est séparée du domaine.

### Entité JPA

`AccountEntity` porte:

- `id`
- `iban`
- `bic`
- `type`
- `currentBalance`
- `reservedBalance`
- `currency`
- `status`
- `createdAt`
- `updatedAt`

### Mapper

`AccountMapper` est responsable de la conversion:

- `AccountEntity -> Account`
- `Account -> AccountEntity`

Le mapper reste dans la couche infrastructure.

---

## 7. Lecture métier

Résumé du cycle principal:

1. le compte est créé avec un statut et des soldes initiaux
2. une opération de crédit augmente `currentBalance`
3. une réservation bloque une partie du solde dans `reservedBalance`
4. une libération rend la réserve disponible à nouveau
5. un débit retire définitivement de l'argent

---

## 8. Convention de nommage

Les noms métier sont privilégiés:

- `reserveFunds`
- `releaseFunds`
- `blockAccount`
- `suspendAccount`
- `closeAccount`
- `reactivateAccount`

Les alias historiques plus courts peuvent exister, mais le vocabulaire métier reste la référence.
