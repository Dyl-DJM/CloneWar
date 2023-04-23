# CLONEWAR ☕

Le but du projet Clonewar est d'écrire une application web qui analyse des fichiers jar (Java Archive) pour détecter des codes communs (on parle de clones).

L'application CloneWar est composée d'un back-end écrit en Java et utilisant un serveur [Helidon Nima 4.0](https://helidon.io/nima) (actuellement en alpha au 28/12/2022) offrant différents services REST permettant d'accéder aux informations de l'analyse d'une archive et d'un front-end écrit en JavaScript utilisant [React](https://fr.reactjs.org) et [Bulma](https://bulma.io) affichant ces informations et en particulier les codes sources considérés comme des clones.

Un artéfact est constitué de deux archives différentes, l'archive "main" contient le bytecode (les .class) de l'artéfact et l'archive "source" contient le code source associé (les .java).

La documentation de développement, nommée `dev.pdf`, est disponible en racine du projet.

## Installation 💾

Pour installer ce programme, il suffit de récupérer le fichier `clonewar-jar-with-dependencies.jar` présent dans le dossier `target`.

Avec Java 19 installé sur son ordinateur, une fois ce fichier récupéré, il suffit d'ouvrir un terminal à l'endroit où vous avez stocké ce fichier puis d'entrer ceci afin d'exécuter ce `jar` :

```bash
java -jar --enable-preview clonewar-jar-with-dependencies.jar
```

## Usage 💻

Une fois le que le fichier `clonewar-jar-with-dependencies.jar` est exécuté, le site web local [localhost:8080/index.html](localhost:8080/index.html) ou [127.0.0.1:8080/index.html](127.0.0.1:8080/index.html) consitue l'interface utilisateur du projet. C'est sur celui-ci qu'il sera possible d'effectuer des analyses d'artéfacts et des vérifications du taux de plagiat présents entre deux artéfacts.

## Auteurs 👨‍🎓👨‍🎓

Ce projet est développé par Dylan DE JESUS MILITAR et Vincent RICHARD.

## Statut d'avancement du projet 📅

Service fonctionnel et utilisable en pratique depuis le 28 décembre 2022.

Fonctionnalités additionnelles en cours de développement...