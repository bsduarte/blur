# Web Crawler Service

A concurrent web crawler service that searches for keywords in web pages and their linked pages.

## Features

- Concurrent web crawling using multiple threads
- Case-insensitive keyword search
- Supports both relative and absolute URLs
- RESTful API endpoints
- Real-time status tracking
- Handles special characters in search terms

## API Endpoints

### 1. Start a New Crawl
- **POST** `/crawl`
- **Body**: `{"keyword": "your-search-term"}`
- **Constraints**: 
  - Keyword must be 4-32 characters long
- **Returns**: Search object with ID and initial status

### 2. Check Crawl Status
- **GET** `/crawl/:id`
- **Returns**: Search object with current status and found URLs

## Response Format

```json
{
    "id": "unique-id",
    "urls": ["array-of-found-urls"],
    "status": "active|done"
}
```

## Building and Running

1. Make sure you have Java and Maven installed

2. Build the project:

   ```bash
   mvn clean package
   ```

3. Run the application:

    Manually:
   ```bash
   export BASE_URL=[base-url]
   java -jar target/backend-test-1.0-SNAPSHOT.jar
   ```

   Or without needing to build previously:
   ```bash
   ./run [base-url]
   ```

## Docker Support

You can also run the application using Docker:

```bash
docker build . -t blur/backend
docker run -e BASE_URL=[base-url] -p 4567:4567
```

## Technical Details

- Built with Java and Spark Framework
- Uses concurrent data structures for thread safety
- Implements smart URL normalization
- Filters out non-HTML resources (images, videos, etc.)
- Proper error handling and logging