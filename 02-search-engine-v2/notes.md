### Phase 2: The "Ferrari" Engine (Project 2)
* **Goal:** Integrate a real industry-standard search engine (Elasticsearch).
* **Tech Stack:**
    * **Java 21**
    * **Spring Boot 3.3.5**
    * **Elasticsearch 8.11.1** (via Docker)
    * **Docker Compose**
* **Key Features:**
    * **Data Ingestion:** Loads `News_Category_Dataset_v3.json` into Elasticsearch.
    * **Field Boosting:** Headlines are weighted **3x** more important than descriptions (`^3`).
    * **Pagination:** Efficiently handles large result sets using `Pageable`.
    * **Faceted Search (Aggregations):** Returns article counts grouped by Category (e.g., "BUSINESS: 15").

## 🛠️ Setup & Installation

### 1. Prerequisites
* Java 21
* Docker Desktop
* Maven

### 2. Start Elasticsearch
Run the docker-compose file to start a single-node cluster.
```bash
docker-compose up
````

### 3\. Build & Run

```bash
mvn clean install
mvn spring-boot:run
```

## 🔌 API Endpoints

### 1\. Load Data

Triggers the ingestion of the JSON dataset into Elasticsearch. Checks for existing data to avoid duplicates.

  * **URL:** `POST /api/news/load`
  * **Response:** "Data loading triggered..."

### 2\. Basic Search (Pagination)

Standard keyword search with pagination.

  * **URL:** `GET /api/news/search`
  * **Parameters:**
      * `keyword`: Search term (e.g., "bitcoin")
      * `page`: Page number (0-indexed)
      * `size`: Number of results per page
  * **Example:** `/api/news/search?keyword=politics&page=0&size=10`

### 3\. Faceted Search (Aggregations)

Advanced search that returns articles **plus** category counts.

  * **URL:** `GET /api/news/searchAgg`
  * **Parameters:** `keyword`, `page`, `size`
  * **Response Structure:**
    ```json
    {
      "articles": [ ... ],
      "categoryCounts": {
        "BUSINESS": 12,
        "TECH": 8
      }
    }
    ```

## 📂 Project Structure

  * **`NewsArticle.java`**: The Document entity defining the schema (`@Document`, `@Field`).
  * **`NewsRepository.java`**: Interface extending `ElasticsearchRepository`. Contains the custom `@Query` for boosted search.
  * **`NewsService.java`**: Business logic. Uses `ElasticsearchOperations` for complex aggregation queries.
  * **`SearchResponse.java`**: DTO (Record) to format the clean JSON response.
  * **`NewsController.java`**: REST API layer.

## 📝 Key Learnings

  * **Tokenization:** How text is split into searchable terms.
  * **Inverted Index:** Mapping terms to document IDs.
  * **Boosting:** Using `^` syntax to prioritize specific fields.
  * **Aggregations:** Using `Terms Aggregation` to create "buckets" of data for UI filters.
