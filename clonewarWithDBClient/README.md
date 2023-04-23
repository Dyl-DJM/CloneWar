# Raison de l'existence de ce dossier

Le paragraphe suivant est un extrait de la documentation de développement que vous pouvez retrouver en racine du projet, nommée `dev.pdf`.

Cet extrait est issu du paragraphe **Problèmes persistants**, notamment du sous-paragraphe **Incompatibilité entre les dépendances Helidon Nima 4.0 et Helidon SE DBClient**.

## Incompatibilité entre les dépendances Helidon Nima 4.0 et Helidon SE DBClient

Initialement, nous devions utiliser Helidon Nima 4.0, actuellement en alpha, pour la partie serveur de notre application et Helidon SE DBClient en ce qui concerne la persistance de celui-ci.

Après de très nombreuses recherches, documentation et de nombreux essais, nous avons appris lors de notre soutenance intermédiaire que ces deux technologies sont en fait incompatibles. Nous avons compris que pour pouvoir utiliser ces deux technologies conjointement, il nous fallait écrire 3 fichiers `pom.xml`. Un fichier parent qui permet l’utilisation simultanée des deux fichiers pom enfants et donc un fichier pom enfant qui renomme la dépendance Helidon SE DBClient en utilisant le plugin Maven shade ainsi qu’un second fichier pom enfant qui recense toutes les autres dépendances et utilise la dépendance Helidon SE DBClient renommée.

Ayant compris cela, nous nous sommes documentés sur l’utilisation du plugin shade de Maven et la façon dont utiliser conjointement ces 3 fichiers pom. Malgré nos recherches et de multiples essais, aucun de ceux-ci n’était suffisamment fructueux pour que l’on puisse s’en servir dans le projet.

Comme nous avions déjà écrit des classes Java utilisant simultanément ces deux technologie, déjà recopié les méthodes de test du serveur et de l’accès à la base de données présentées sur le dépôt GitHub d’exemple d'utilisation d’Helidon de son auteur et déjà écrit le fichier de configuration `src/main/resources/application.yaml` ainsi que des requêtes SQL au sein de ce fichier en permettant d’éviter des injections de code au vu de la manière dont nous les utilisions, nous avons décidé de ne pas supprimer tout ce travail et de le déposer sur notre dépôt GitLab dans un dossier à part nommé `clonewarWithDBClient`.

Comme ce problème nous empêchait de continuer l’avancement du projet, nous avons décidé de réfléchir à une solution qui permette d’avoir une utilisation de l’application et un comportement de celle-ci identique par rapport à si nous avions pu utiliser la base de données Helidon SE DBClient.

Nous avons donc fait en sorte que tout ce que nous aurions stocké dans le base de données serait stocké dans un champ d’instance privé qui est un dictionnaire et qui stocke donc une chaîne de caractère représentant de manière unique l’artéfact analysé ainsi que la liste de ses hashes; son type est donc `HashMap<String, ArrayList>Long>>` et se nomme `map` . Un autre champ d’instance privé de type `ArrayList<String>` et nommé `analyzedJars` stocke les chemins absolus des artéfacts analysés. Son type n’est pas `ArrayList<Path>` car nous n’utilisons pas les éléments qu’il stocke comme des chemins mais comme des simples chaînes de caractères. Le contenu de ces deux champs est sauvegardé dans des fichiers de sauvegarde autonomes (cf. `hashesBackupFile.txt` et `jarsBackupFile.txt`).

De plus, afin de faire en sorte que les utilisateurs n’aient pas à réanalyser leurs artéfacts après chaque extinction de l’application, nous avons fait en sorte d’écrire des méthodes de restauration des données sauvegardées dans ces fichiers afin de garantir une pérennisation des informations issues des analyses d’artéfacts et donc de garantir un comportement identique à l'utilisation d’une base de donnée. Cependant, comme notre application doit réaliser des écritures dans un fichier durant les analyses d’artéfacts, celles-ci sont ralenties par ces écritures. En implantant une base de données à notre application dans le futur, notre méthode d’analyse sera plus performante.
