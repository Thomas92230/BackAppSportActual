# SportActual Backend

Application backend pour l'actualité sportive en temps réel avec Spring Boot.

## 🚀 Fonctionnalités

- **📡 Récupération de données** : Scores, matchs, news depuis ESPN API et API-Sports
- **⚡ Temps réel** : Live scores via WebSocket
- **🔍 Filtrage avancé** : Par sport, équipe, compétition
- **🔔 Notifications** : Match en cours, buts, résultats
- **📱 API RESTful** : Endpoints complets pour le frontend
- **🔐 Sécurité** : JWT et Spring Security
- **📊 Monitoring** : Actuator et Prometheus

## 🏗️ Architecture

### Stack Technique
- **Backend** : Spring Boot 3.2.4 (Java 21)
- **Base de données** : PostgreSQL avec Flyway
- **Cache** : Redis
- **Temps réel** : WebSocket (STOMP)
- **Cloud** : AWS (SNS, SQS, S3)
- **Monitoring** : Actuator + Prometheus
- **Containerisation** : Docker & Docker Compose

### Sports Supportés
- Football ⚽
- Basketball 🏀
- Tennis 🎾
- Hockey sur glace 🏒
- Rugby 🏉
- Cyclisme 🚴
- Formule 1 🏎️
- Judo 🥋
- Natation 🏊

## 🛠️ Installation

### Prérequis
- Java 21+
- Maven 3.8+
- PostgreSQL 15+
- Redis 7+
- Docker (optionnel)

### Configuration
1. Copier le fichier d'environnement :
```bash
cp .env.example .env
```

2. Configurer les variables d'environnement dans `.env` :
- Clés API (API-Sports, ESPN)
- Configuration AWS
- JWT secret

### Lancement avec Docker
```bash
docker-compose up -d
```

### Lancement en local
1. Démarrer PostgreSQL et Redis
2. Lancer l'application :
```bash
mvn spring-boot:run
```

## 📚 API Documentation

### Endpoints principaux

#### Sports
- `GET /api/sports` - Liste des sports
- `GET /api/sports/{code}` - Détails d'un sport
- `POST /api/sports/initialize` - Initialiser les sports par défaut

#### Matchs
- `GET /api/matches/live` - Matchs en direct
- `GET /api/matches/live/{sportCode}` - Matchs live par sport
- `GET /api/matches/sport/{sportCode}` - Matchs par sport
- `GET /api/matches/team/{teamId}` - Matchs par équipe

#### News
- `GET /api/news/sport/{sportCode}` - Actualités par sport
- `GET /api/news/recent` - Actualités récentes
- `GET /api/news/search` - Rechercher des actualités

#### Équipes et Compétitions
- `GET /api/teams/sport/{sportCode}` - Équipes par sport
- `GET /api/competitions/sport/{sportCode}` - Compétitions par sport

#### Authentification
- `POST /api/auth/login` - Connexion
- `POST /api/auth/refresh` - Rafraîchir le token
- `GET /api/auth/validate` - Valider le token

### WebSocket
- Connexion : `ws://localhost:8080/ws`
- Abonnements :
  - `/topic/matches/{sportCode}` - Matchs live par sport
  - `/topic/notifications/{sportCode}` - Notifications par sport

## 🏃‍♂️ Développement

### Structure du projet
```
src/main/java/com/actuSport/
├── domain/                 # Entités et repositories
│   ├── model/              # Entités JPA
│   └── repository/         # Interfaces Spring Data
├── application/            # Services métier
│   └── service/           # Logique métier
├── infrastructure/         # Configuration externe
│   ├── config/            # Configuration Spring
│   ├── external/          # Clients API externes
│   ├── websocket/         # Configuration WebSocket
│   ├── security/          # Sécurité JWT
│   └── aws/               # Services AWS
└── interfaces/            # Points d'entrée
    └── rest/              # Controllers REST
```

### Synchronisation des données
Les données sont synchronisées automatiquement :
- **Matchs live** : toutes les 30 secondes
- **Actualités** : toutes les 5 minutes

### Monitoring
- Health checks : `/actuator/health`
- Metrics : `/actuator/metrics`
- Prometheus : `/actuator/prometheus`

## 🧪 Tests

```bash
# Tests unitaires
mvn test

# Tests d'intégration
mvn verify

# Coverage
mvn jacoco:report
```

## 📦 Déploiement

### Build
```bash
mvn clean package
```

### Docker
```bash
docker build -t sport-actual-back .
```

### Production
1. Configurer le profile `prod`
2. Définir les variables d'environnement
3. Déployer avec Docker Compose ou Kubernetes

## 🔧 Configuration

### Variables d'environnement
- `SPRING_PROFILES_ACTIVE` : Profile Spring (dev/prod/docker)
- `API_SPORTS_KEY` : Clé API-Sports
- `AWS_ACCESS_KEY` : Clé d'accès AWS
- `JWT_SECRET` : Secret pour les tokens JWT

### Profiles
- **dev** : Développement local
- **docker** : Conteneurs Docker
- **prod** : Production

## 🤝 Contribuer

1. Forker le projet
2. Créer une branche feature
3. Commiter les changements
4. Pusher la branche
5. Créer une Pull Request

## 📄 Licence

Ce projet est sous licence MIT.

## 📞 Support

Pour toute question ou problème :
- Créer une issue sur GitHub
- Contacter l'équipe de développement
