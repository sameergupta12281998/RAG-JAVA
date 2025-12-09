Spring Boot RAG — Full Project
This repository is a complete end-to-end RAG (Retrieval-Augmented Generation) system built with Java Spring Boot.

Features
Crawl a website (internal links only) using Jsoup
Extract and clean visible text
Chunk content with overlap
Generate embeddings via OpenAI Embeddings API
Store vectors and metadata in an in-memory VectorStore (simple cosine similarity)
Retrieval + answer generation using OpenAI Chat API
REST endpoints: /api/crawl, /api/reindex, /api/ask, /api/faq/generate
Requirements
Java 17+
Maven
OpenAI API Key (set OPENAI_API_KEY env variable or in application.properties)
Run
mvn clean package
export OPENAI_API_KEY=sk-...
java -jar target/spring-boot-rag-0.0.1-SNAPSHOT.jar
API will run at http://localhost:8080/api.

Endpoints
POST /api/crawl — body { "baseUrl": "https://example.com" }
POST /api/reindex — clears vectors (re-run /crawl to index)
POST /api/ask — body { "question": "..." } → returns answer + sources
POST /api/faq/generate — body { "questions": ["q1","q2"] } → returns FAQ
Notes
This demo uses OpenAI for embeddings & chat — pay attention to costs.
Vector store is in-memory but saved to data/vectorstore.dat after crawl.
For JS-heavy sites use Playwright / Selenium (not included).
Future Improvements
Integrate Pinecone / Qdrant for scalable vectors
Add concurrency to crawling
Improve HTML cleaning with heuristics and ML
Paginate large sites or use sitemap
