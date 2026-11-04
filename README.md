# TP3 REST : Microservices « Calculatrice » avec Spring Boot

## Environnement

| Élément | Version relevée |
|---|---|
| Système | Windows 11 (amd64) |
| JDK | OpenJDK 27.0.1 (cible Java 17 dans les `pom.xml`) |
| Maven | 3.9.16 (Maven Wrapper `mvnw.cmd`) |
| Spring Boot | 4.0.9 (Spring Framework 7.0.10) |
| Serveur embarqué | Apache Tomcat 11.0.27 (inclus dans `spring-boot-starter-webmvc`) |
| curl | 8.21.0 (`curl.exe`) |
| IDE | IntelliJ IDEA 2026.2.3 |
| Postman | [À COMPLÉTER : version] |

Aucun Tomcat n'est installé séparément : chaque microservice démarre avec son serveur embarqué.

## Structure du dépôt

```text
TP3_Rest/
├── calcul-service/        (port 8081, ressource /calcul)
│   └── src/main/java/com/exemple/calcul/{controller,service,model}
├── historique-service/    (port 8082, ressource /operations)
│   └── src/main/java/com/exemple/historique/{controller,service,client,model}
└── README.md
```

---

## Partie A : Du service web au microservice

**A.4.1 — Avantage et inconvénient d'un calcul par requête HTTP au lieu d'un appel de méthode**
*Avantage :* `calcul-service` est autonome : on peut le modifier, le redéployer ou le lancer en plusieurs exemplaires sans toucher à `historique-service`.
*Inconvénient :* l'appel traverse le réseau, il est plus lent qu'un appel de méthode, et `historique-service` dépend de la disponibilité de `calcul-service` (il doit gérer son indisponibilité).

**A.4.2 — Si `calcul-service` est arrêté**
*Continuent de fonctionner :* `GET /operations`, `GET /operations/{id}` et `DELETE /operations/{id}`, qui n'utilisent que la mémoire d'`historique-service`.
*Échouent :* les quatre `GET :8081/calcul/...` (le service est arrêté), ainsi que `POST /operations` et `PUT /operations/{id}`, qui ont besoin d'un calcul (réponse 503, voir F.7 et F.8).

**A.4.3 — Pourquoi deux microservices ne peuvent-ils pas écouter sur le même port ?**
Un port ne peut être ouvert en écoute que par un seul processus à la fois. Un second microservice sur le même port ne démarre pas (`Port 8081 was already in use`).

**A.4.4 — Statut quand `historique-service` ne joint pas `calcul-service`**
**503 Service Unavailable** : le client n'a pas commis d'erreur (pas un 4xx) et le serveur n'a pas de bug (pas un 500) : un service dont il dépend est momentanément indisponible.

---

## Partie C : Premier microservice (la somme)

### Correspondance Spring / JAX-RS (tableau de C.2)

| Spring (TP3) | JAX-RS (TP2) | Rôle dans cet exemple |
|---|---|---|
| `@RestController` | (pas d'équivalent direct) | Déclare un contrôleur REST : la valeur retournée devient le corps de la réponse HTTP |
| `@RequestMapping("/calcul")` | `@Path("/calcul")` sur la classe | Préfixe commun à toutes les méthodes de la classe |
| `@GetMapping("/somme")` | `@GET` + `@Path("/somme")` | La méthode répond aux requêtes GET sur `/calcul/somme` |
| `produces = MediaType.TEXT_PLAIN_VALUE` | `@Produces(MediaType.TEXT_PLAIN)` | La réponse est du texte brut (`text/plain`) |
| `@RequestParam("a") int a` | `@QueryParam("a") int a` | Lit le paramètre `a` de la chaîne de requête (`?a=2`) et le convertit en entier |
| `ResponseEntity.ok(...)` | `Response.ok(...).build()` | Construit une réponse de statut 200 avec un corps |

### Composition de l'URL (tableau de C.6)

URL : `http://localhost:8081/calcul/somme?a=2&b=3`

| Partie de l'URL | Origine |
|---|---|
| `localhost:8081` | Machine locale et port fixé par `server.port` dans `application.properties` |
| `/calcul` | Annotation `@RequestMapping("/calcul")` de la classe |
| `/somme` | Annotation `@GetMapping("/somme")` de la méthode |
| `?a=2&b=3` | Paramètres de requête lus par `@RequestParam("a")` et `@RequestParam("b")` |

**Question C.1 — Quelles parties de l'URL du TP2 ont disparu, et pourquoi ?**
Ont disparu `/RestWebApp` et `/api`. `/RestWebApp` était le nom du `.war` (le contexte de l'application dans le Tomcat partagé) ; `/api` venait de `@ApplicationPath("/api")`. Avec Spring Boot, l'application est seule dans son serveur embarqué : le contexte est `/` (log : `Tomcat started on port 8081 (http) with context path '/'`) et aucun préfixe n'est imposé. Le port a aussi changé (8080 → 8081), choisi dans `application.properties`.

**Question C.2 — Statut d'un POST sur `/calcul/somme`, identique au TP2 ?**
Attendu : **405 Method Not Allowed**, car la méthode n'est mappée que sur GET. **[À VÉRIFIER avec Postman ou `curl.exe -i -X POST "http://localhost:8081/calcul/somme?a=2&b=3"`, et comparer avec ce que vous avez obtenu au TP2.]**

---

## Partie D : Compléter la calculatrice

Résultats observés pour la division (`curl.exe -i`) :

| Requête | Statut observé | Corps observé |
|---|---|---|
| `GET /calcul/division?a=7&b=2` | 200 | `3.5` |
| `GET /calcul/division?a=7&b=0` | 400 | `Erreur : division par 0 impossible` |

### D.3 — Observations

| Requête | Statut | Corps |
|---|---|---|
| `GET /calcul/somme?a=2` (`b` absent) | [À COMPLÉTER : relever avec curl.exe -i] | [À COMPLÉTER] |
| `GET /calcul/somme?a=abc&b=3` (`a` non entier) | [À COMPLÉTER : relever avec curl.exe -i] | [À COMPLÉTER] |

**Question D.1 — Que fait Spring pour un paramètre entier absent ? Quelle attitude est la plus conforme à REST ?**
Attendu : `@RequestParam` est obligatoire par défaut ; Spring refuse la requête (statut 4xx, normalement 400) sans exécuter la méthode, alors que JAX-RS donnait 0 et exécutait `somme`. **[À VÉRIFIER avec votre observation de D.3.]** L'attitude de Spring est plus conforme à REST : une requête incomplète est une erreur du client, signalée par un code 4xx, au lieu de produire silencieusement un résultat faux (2 + 0).

**Question D.2 — Champs de l'objet JSON d'erreur de Spring**
[À COMPLÉTER : recopier les champs que vous voyez dans le corps de la réponse de D.3. Ce sont normalement `timestamp`, `status`, `error` et `path`, à confirmer sur votre sortie.]

### D.4 — `@PathVariable`

Méthode `produitChemin` : `GET /calcul/produit/6/7` doit retourner `42`. **[À COMPLÉTER : résultat observé]**

**Question D.3 — Correspondance et choix pour `/operations/{id}`**

| Spring | JAX-RS |
|---|---|
| `@RequestParam` | `@QueryParam` |
| `@PathVariable` | `@PathParam` |

Pour lire l'identifiant dans `/operations/{id}`, on utilise **`@PathVariable`** : l'identifiant fait partie du chemin qui désigne la ressource.

---

## Partie E : Échanger des données en JSON

Résultat observé : `GET /calcul/division/detail?a=7&b=2` → 200, `Content-Type: application/json`, corps `{"operation":"division","a":7,"b":2,"resultat":3.5}` ; avec `b=0` → 400 sans corps.

**Question E.1 — Les deux textes JSON sont-ils équivalents ? Qui a converti ?**
Oui : un objet JSON est un ensemble de paires nom/valeur sans ordre, et le client lit les champs par leur nom. Seul l'ordre diffère (JSON-B les triait par ordre alphabétique au TP2 ; ici l'ordre suit la déclaration de la classe, comme l'observation ci-dessus le montre). La conversion est faite par **Jackson**, inclus dans `spring-boot-starter-webmvc`, à partir des accesseurs de la classe `Resultat`.

**Question E.2 — Pourquoi `Accept: application/xml` est-il refusé ?**
Attendu : **406 Not Acceptable**. Spring choisit le format d'après l'en-tête `Accept` et les convertisseurs de messages disponibles. Pour un objet `Resultat`, seul Jackson (JSON) est présent : il n'existe aucun convertisseur XML, donc le serveur ne peut produire aucune représentation acceptable pour le client. **[À VÉRIFIER avec `curl.exe -i -H "Accept: application/xml" "http://localhost:8081/calcul/somme/detail?a=2&b=3"`.]**

---

## Partie F : historique-service

### F.6 — Création et lecture (résultats observés, `historique-service` démarré à vide)

| N° | Requête | Corps envoyé | Statut | Réponse observée |
|---|---|---|---|---|
| 1 | POST `/operations` | `{"operateur":"somme","a":8,"b":2}` | 201 | `{"a":8,"b":2,"id":1,"operateur":"somme","resultat":10.0}` ; `Location: http://localhost:8082/operations/1` |
| 2 | POST `/operations` | `{"operateur":"division","a":7,"b":2}` | 201 | `{"a":7,"b":2,"id":2,"operateur":"division","resultat":3.5}` ; `Location: .../operations/2` |
| 3 | GET `/operations` | — | 200 | Tableau JSON des opérations 1 et 2 |
| 4 | GET `/operations/2` | — | 200 | L'opération 2 |
| 5 | GET `/operations/99` | — | 404 | Corps vide |
| 6 | POST `/operations` | `{"operateur":"division","a":7,"b":0}` | 400 | `{"erreur":"Division par zero impossible"}` |
| 7 | POST `/operations` | `{"operateur":"racine","a":9,"b":0}` | 400 | `{"erreur":"Operateur invalide"}` |

Les messages d'erreur des tests 6 et 7 sont ceux du code ; le sujet ne les impose pas.

### F.7 — `calcul-service` arrêté

| N° | Requête | Corps envoyé | Statut | Réponse observée |
|---|---|---|---|---|
| 8 | GET `/operations` | — | 200 | Liste des opérations 1 et 2 |
| 9 | GET `/operations/1` | — | 200 | L'opération 1 |
| 10 | POST `/operations` | `{"operateur":"produit","a":3,"b":4}` | 503 | `{"erreur":"calcul-service est arrete ou injoignable"}` |

Après relance de `calcul-service`, la requête 10 répond **201** avec `"id":3` et `"resultat":12.0`.

**Question F.1 — Pourquoi les lectures continuent-elles de fonctionner ? Était-ce possible au TP2 ?**
Les lectures n'utilisent que les opérations conservées en mémoire par `historique-service` : elles n'appellent jamais `calcul-service`. Au TP2, ce n'était pas possible : calcul et historique étaient dans la même application `.war`, et le calcul était un appel de méthode. Si l'application s'arrêtait, tout s'arrêtait : il n'y avait pas de panne partielle.

**Question F.2 — Que se passe-t-il sans le bloc `catch (ResourceAccessException e)` ?**
L'exception n'est pas interceptée : elle remonte jusqu'à Spring, qui renvoie une erreur 500 (*Internal Server Error*). Le client croit à un bug du serveur alors que la cause est l'indisponibilité de `calcul-service` ; le `catch` permet de renvoyer le statut approprié, 503. **[À VÉRIFIER en mettant le `catch` en commentaire, comme le demande le sujet, puis le remettre.]**

### F.8 — `PUT /operations/{id}` (résultats observés)

| Situation | Statut observé | Corps observé |
|---|---|---|
| Identifiant inexistant (`PUT /operations/99`) | 404 | vide |
| Calcul impossible (`division 7/0` sur l'id 1) | 400 | `{"erreur":"Division par zero impossible"}` |
| `calcul-service` injoignable (`PUT /operations/2`) | 503 | `{"erreur":"calcul-service est arrete ou injoignable"}` |
| Modification réussie (`produit 8, 2` sur l'id 1) | 200 | `{"a":8,"b":2,"id":1,"operateur":"produit","resultat":16.0}` |

Le `GET /operations/1` qui suit confirme la modification (`produit`, `16.0`).

### F.9 — `DELETE /operations/{id}`

| Situation | Statut attendu | Statut observé |
|---|---|---|
| Suppression réussie (`DELETE /operations/1`) | 204, corps vide | [À COMPLÉTER] |
| `GET /operations/1` après suppression | 404 | [À COMPLÉTER] |
| Identifiant inexistant (`DELETE /operations/1` à nouveau) | 404, corps vide | [À COMPLÉTER] |

---

## Questions de synthèse

### 1. Tableau A.3 : étape où chaque différence est observée

| Aspect | Étape |
|---|---|
| Technologie (Spring Boot / Spring MVC au lieu de JAX-RS) | B.1, B.3 |
| Découpage en deux applications | B.1, F.1 |
| Résultat de la construction (`.jar` exécutable) | B.3, C.5 |
| Serveur (Tomcat embarqué) | C.5 (ligne `Tomcat started on port 8081`) |
| Port (un par microservice) | C.4, F.1 |
| Adresse (sans `/RestWebApp/api`) | C.6, C.7 |
| Activation (`@SpringBootApplication` + `main`) | B.4 |
| Ressource REST (`@RestController`, `@RequestMapping`) | C.2 |
| Verbes HTTP (`@GetMapping`, `@PostMapping`, `@PutMapping`, `@DeleteMapping`) | C.2, F.5, F.8, F.9 |
| Paramètres (`@RequestParam`, `@PathVariable`, `@RequestBody`) | C.2, D.4, F.5 |
| Réponse HTTP (`ResponseEntity`) | C.2, D.2 |
| Conversion JSON (Jackson) | E.1, E.2 |
| Création des objets (`@Service`, injection, singleton) | C.1, C.2, F.4 |
| Communication (requête HTTP entre services) | F.3, F.6 |

### 2. Correspondance complète JAX-RS ↔ Spring

| JAX-RS (TP2) | Spring (TP3) |
|---|---|
| `@ApplicationPath("/api")` + classe `Application` | `@SpringBootApplication` + méthode `main` |
| `@Path` sur la classe | `@RestController` + `@RequestMapping` |
| `@Path` sur la méthode | Chemin dans `@GetMapping("...")` etc. |
| `@GET` / `@POST` / `@PUT` / `@DELETE` | `@GetMapping` / `@PostMapping` / `@PutMapping` / `@DeleteMapping` |
| `@Produces` | `produces = ...` |
| `@Consumes` + paramètre objet | `@RequestBody` |
| `@QueryParam` | `@RequestParam` |
| `@PathParam` | `@PathVariable` |
| `Response` | `ResponseEntity` |
| `UriInfo.getAbsolutePathBuilder()` | `ServletUriComponentsBuilder.fromCurrentRequest()` |
| JSON-B (`jersey-media-json-binding`) | Jackson (inclus dans le starter) |

### 3. Construire et lancer les deux microservices sans IntelliJ

Terminal 1 :
```powershell
cd calcul-service
.\mvnw.cmd clean package
java -jar target/calcul-service-0.0.1-SNAPSHOT.jar
```
Terminal 2 :
```powershell
cd historique-service
.\mvnw.cmd clean package
java -jar target/historique-service-0.0.1-SNAPSHOT.jar
```
Rien n'est à déployer : chaque `.jar` contient l'application et son Tomcat. Il faut deux terminaux, un par microservice, et `calcul-service` doit tourner pour que les `POST` et `PUT` d'`historique-service` réussissent. Variante sans construire le `.jar` : `mvn spring-boot:run` dans chaque dossier.

### 4. Un microservice est-il un service web ? Un service web est-il toujours un microservice ?

Oui : `calcul-service` et `historique-service` exposent des ressources accessibles par HTTP, ce sont donc des services web. Non : le `RestWebApp.war` du TP2 était un service web, mais pas un microservice : calcul et historique y étaient réunis dans une seule application déployée dans un Tomcat installé à part. Un microservice est petit, autonome (propre serveur, propre port), responsable d'une seule fonction métier, et communique avec les autres par HTTP.

### 5. Problème d'une adresse fixe dans `application.properties` avec trois exemplaires de `calcul-service`

L'adresse `http://localhost:8081` désigne une seule instance : `historique-service` n'appellera toujours que celle-là, donc la charge n'est pas répartie, et si elle tombe il n'y a pas de bascule vers les deux autres. Toute modification d'adresse demande d'éditer la configuration à la main. Le TP4 y répond avec un annuaire de services et un répartiteur de charge.

### 6. Stateless et liste des opérations conservée

Le principe stateless interdit au serveur de mémoriser l'**état du client** (session, contexte de conversation) entre deux requêtes : chaque requête doit contenir toute l'information nécessaire. La liste des opérations n'est pas un état de session : ce sont des **ressources** du serveur, partagées par tous les clients et identifiées par des URI. Une requête n'a pas besoin d'une requête précédente du même client pour être comprise ; il n'y a donc pas de contradiction.

### 7. POST répété et PUT répété

- Deux `POST /operations` identiques créent **deux opérations distinctes** (identifiants différents) : POST n'est **pas idempotent**.
- Deux `PUT /operations/1` identiques remplacent l'opération 1 par les mêmes valeurs : l'état final est le même qu'après un seul envoi : PUT est **idempotent**.

La réponse n'est pas différente de celle du TP2 : l'idempotence est une propriété des verbes HTTP, pas de l'outil. Seule la façon de conserver les données change (attributs `static` au TP2, `@Service` singleton au TP3).
