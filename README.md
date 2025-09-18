# Microblogging App

A microservices application. A mini-social network with basic functionality: creating posts, subscribing to users, likes/coments, creating a personal news feed.

<img width="1713" height="1667" alt="schema" src="https://github.com/user-attachments/assets/d4b2230d-f24c-4c2c-83e7-c3d7c4ac0f72" />

## Tech stack

- **Java 21**
- **Spring Boot 3**
  - Web
  - Data
- **Hibernate**
- **PostgreSQL**
- **Redis**
- **Kafka**
- **Maven**
- **Feign Client**
- **Flyway**
- **MinIO**
- **MapStruct**
- **JUnit and Mockito**
- **Testcontainers**
- **Docker**

---

## Microservices description  

**API Gateway:**  
Uses Spring Cloud Gateway and Eureka to route requests.  

---

**Service Discovery:**  
Eureka server for service registration.  

---

**User Service:**  
Responsible for user registration and storage.

  **Features:**  
  - Gives general user info
  - Feign client for cross-service communication.
  - No authorization(X-User-Id is used).
  - Docker containerization.

---

**Profile Service**  
Service aggregator. Designed in order to show aggregated user profiles.  

**Features:**  
  - Aggregates data from User Service, Post Service, and Comment Service.  
  - Uses CQRS projections in order to collect required information quickly.  
  - Contains two tables: post-read-model and comment-read-model that are filled via Kafka events.  
  - Redis caching for database performance and cache invalidating by events.  
  - Resilience4j for external calls to User Service.  
  - Even if User Service goes wrong, the user profile always will be open  
    (posts and comments are available and general user info can be received from cache - partial response)

    <img width="953" height="748" alt="user-profile feature" src="https://github.com/user-attachments/assets/61197223-9d4d-4215-889d-680f49a0f4b5" />

---
 
**Post Service:**  
Manages the creation and viewing of posts.

**Features:**  
  - Kafka-producer: sends post-created and post-removed events.
  - Redis caching the result of received post page.
  - Integration with User Service (via Feign Client) for author validation.
  - Rate-limiting on creation posts(5 posts per minute) using Redis
  - Uses Redis to quickly count posts by userId.

---

**Feed Service:**  
Forms a user's personalized feed from the posts of the authors to whom they are subscribed.  
Also manages subscription/unsubscription.  

**Features:**  
- News feed is implemented through push model:  
  ```bash
  when a user creates a post, a message is sent to the 'post-created' topic.    
  The Feed Service is the consumer, and when the message(PostCreatedEventDto(post_id, author_id)) arrives there,  
  all subscribers of this post author are found, and a new entry is added to the feed table via batch-insert.
  ````
- Kafka-producer: sends subscription-created and subscription-removed events.
- Kafka-consumer: handles post-created and post-removed events. Distributes post to subscribers and removes them accordingly.
- Redis caching the news feed and manually removing it on new posts.
- Integration with User Service for checking user exists.

---
  
**Like Service:**  
Handles user actions related to likes on post.  

**Features:**  
  - Kafka-producer: sends like-sent and like-removed events.
  - Kafka-consumer: handles post-removed event to remove likes on post.
  - Limits the number of likes per User via Redis(rate limiter).
  - Uses Redis to quickly count likes by postId.
  - Integration with Post Service for checking post exists.
  
---

**Comment Service:**  
Handles user actions related to comments on post.  

**Features:**  
  - Kafka-producer: sends comment-created and comment-removed events.
  - Kafka-consumer: handles post-removed event to remove comments on post.
  - Redis caching the comments for post and manually removing it on new comments.
  - Limits the number of comments per User via Redis(rate limiter).
  - Uses Redis to quickly count comments by postId.
  - Uses Redis to quickly count replies on comment by parentId.
  - Supports nested comments
  - Integration with Post Service for checking post exists.

---

**Notification Service:**  
Listens for events from Kafka and is responsible for creating and removing notifications to users.  

**Features:**  
- Kafka-consumer: handles all events to create or remove notification accordingly.
- Only works on incoming events.
- Uses Redis to quickly count unread notifications.

---

**Media Service:**  
Uploading/storing media files.  

---

**Common-lib:**  
It's not a microservice. It contains common classes such as kafka events and exceptions.

---

## How to run  

**Requirements:**
- Java 21
- Maven
- Docker & Docker Compose

1. **Clone the repository**
   ```bash
    htpps://github.com/merfonteen/microblogging.git
    cd microblogging
   ````
2. **Build all microservices:**
   ````bash
     mvn clean package -DskipTests
   ````

3. **Run the project in Docker:**
   ````bash
     docker-compose up --build
   ````
