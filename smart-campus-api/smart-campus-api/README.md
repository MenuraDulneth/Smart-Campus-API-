# Smart Campus Sensor & Room Management API

**Module:** 5COSC022W Client-Server Architectures  
**Student:** [Your Name]  
**Technology:** JAX-RS (Jersey 2.41) + Grizzly HTTP Server  
**Base URL:** `http://localhost:8080/api/v1`

---

## Table of Contents

1. [API Overview](#api-overview)
2. [Project Structure](#project-structure)
3. [How to Build](#how-to-build)
4. [How to Run](#how-to-run)
5. [Sample curl Commands](#sample-curl-commands)
6. [Report: Question Answers](#report-question-answers)

---

## API Overview

The Smart Campus API is a fully RESTful web service built with **JAX-RS (Jersey)** and an **embedded Grizzly HTTP server**. It provides campus facilities managers with a programmatic interface to manage physical **Rooms** and the **Sensors** deployed within them, including a historical log of **Sensor Readings**.

### Core Resources

| Resource | Base Path | Description |
|---|---|---|
| Discovery | `GET /api/v1` | API metadata, versioning, and HATEOAS links |
| Rooms | `/api/v1/rooms` | Create, retrieve, and decommission campus rooms |
| Sensors | `/api/v1/sensors` | Register and query IoT sensors with type-based filtering |
| Readings | `/api/v1/sensors/{id}/readings` | Append and retrieve historical sensor data (sub-resource) |

### Key Design Decisions

- **In-memory storage only** using `ConcurrentHashMap` (thread-safe, no database).
- **Request-scoped resource classes** with a **Singleton DataStore** to safely share state.
- **Structured JSON error responses for custom application error conditions, with no raw stack traces exposed.
- **Sub-Resource Locator Pattern** delegates reading management to `SensorReadingResource`.
- **Global Exception Mapper** intercepts all unexpected `Throwable` exceptions.
- **Logging Filter** provides full observability on every request and response.

### HTTP Status Codes Used

| Code | Meaning | When Used |
|---|---|---|
| 200 | OK | Successful GET, DELETE |
| 201 | Created | Successful POST (room/sensor/reading) |
| 400 | Bad Request | Missing/invalid required fields |
| 403 | Forbidden | POST reading to a MAINTENANCE sensor |
| 404 | Not Found | Resource does not exist |
| 409 | Conflict | Duplicate ID, or DELETE room with active sensors |
| 415 | Unsupported Media Type | Wrong Content-Type (handled by JAX-RS automatically) |
| 422 | Unprocessable Entity | Valid JSON but invalid roomId reference |
| 500 | Internal Server Error | Any unexpected/unhandled server error |

---

## Project Structure

```
smart-campus-api/
├── pom.xml
└── src/
    └── main/
        └── java/
            └── com/
                └── smartcampus/
                    ├── application/
                    │   ├── Main.java                          # Grizzly server entry point
                    │   └── SmartCampusApplication.java        # JAX-RS @ApplicationPath config
                    ├── data/
                    │   └── DataStore.java                     # Thread-safe singleton in-memory store
                    ├── model/
                    │   ├── Room.java                          # Room POJO
                    │   ├── Sensor.java                        # Sensor POJO
                    │   └── SensorReading.java                 # SensorReading POJO
                    ├── resource/
                    │   ├── DiscoveryResource.java             # GET /api/v1
                    │   ├── RoomResource.java                  # /api/v1/rooms
                    │   ├── SensorResource.java                # /api/v1/sensors
                    │   └── SensorReadingResource.java         # Sub-resource: /readings
                    ├── exception/
                    │   ├── RoomNotEmptyException.java
                    │   ├── RoomNotEmptyExceptionMapper.java   # → 409
                    │   ├── LinkedResourceNotFoundException.java
                    │   ├── LinkedResourceNotFoundExceptionMapper.java  # → 422
                    │   ├── SensorUnavailableException.java
                    │   ├── SensorUnavailableExceptionMapper.java       # → 403
                    │   └── GlobalExceptionMapper.java         # Catch-all → 500
                    └── filter/
                        └── LoggingFilter.java                 # Request + Response logging
```

---

## How to Build

### Prerequisites

- **Java 11+** (JDK, not just JRE)
- **Apache Maven 3.6+**

Verify installations:

```bash
java -version
mvn -version
```

### Build Command

Clone the repository and run:

```bash
git clone https://github.com/YOUR_USERNAME/smart-campus-api.git
cd smart-campus-api
mvn clean package
```

This produces a single executable fat-JAR:

```
target/smart-campus-api-1.0.0.jar
```

---

## How to Run

```bash
java -jar target/smart-campus-api-1.0.0.jar
```

Expected console output:

```
INFO: Smart Campus API started at: http://localhost:8080/api/v1
INFO: Discovery endpoint: http://localhost:8080/api/v1
INFO: Press ENTER to stop the server...
```

The server starts immediately on **port 8080**. No application server or container required.

To stop: press `ENTER` in the terminal.

---

## Sample curl Commands

All commands assume the server is running on `http://localhost:8080`.

---

### 1. Discovery — GET /api/v1

```bash
curl -s -X GET http://localhost:8080/api/v1 \
     -H "Accept: application/json" | python3 -m json.tool
```

**Expected:** `200 OK` with JSON containing API version, contact info, and `_links` map to all resources.

---

### 2. Create a Room — POST /api/v1/rooms

```bash
curl -s -X POST http://localhost:8080/api/v1/rooms \
     -H "Content-Type: application/json" \
     -d '{
           "id": "GYM-001",
           "name": "Campus Sports Hall",
           "capacity": 150
         }' | python3 -m json.tool
```

**Expected:** `201 Created` with `Location: /api/v1/rooms/GYM-001` header and the created room body.

---

### 3. Get All Rooms — GET /api/v1/rooms

```bash
curl -s -X GET http://localhost:8080/api/v1/rooms \
     -H "Accept: application/json" | python3 -m json.tool
```

**Expected:** `200 OK` with a JSON array of all rooms (includes pre-seeded rooms LIB-301, LAB-101, HALL-A, and the newly created GYM-001).

---

### 4. Get a Specific Room — GET /api/v1/rooms/{roomId}

```bash
curl -s -X GET http://localhost:8080/api/v1/rooms/LIB-301 \
     -H "Accept: application/json" | python3 -m json.tool
```

**Expected:** `200 OK` with the full Room object including its `sensorIds` list.

---

### 5. Create a Sensor with a Valid roomId — POST /api/v1/sensors

```bash
curl -s -X POST http://localhost:8080/api/v1/sensors \
     -H "Content-Type: application/json" \
     -d '{
           "id": "HUM-001",
           "type": "Humidity",
           "status": "ACTIVE",
           "currentValue": 55.0,
           "roomId": "GYM-001"
         }' | python3 -m json.tool
```

**Expected:** `201 Created` with the sensor body and `Location` header.

---

### 6. Create a Sensor with an Invalid roomId — Expect 422

```bash
curl -s -X POST http://localhost:8080/api/v1/sensors \
     -H "Content-Type: application/json" \
     -d '{
           "id": "BAD-001",
           "type": "Temperature",
           "status": "ACTIVE",
           "currentValue": 0.0,
           "roomId": "DOES-NOT-EXIST"
         }' | python3 -m json.tool
```

**Expected:** `422 Unprocessable Entity` with `LINKED_RESOURCE_NOT_FOUND` error code.

---

### 7. Get All Sensors — GET /api/v1/sensors

```bash
curl -s -X GET http://localhost:8080/api/v1/sensors \
     -H "Accept: application/json" | python3 -m json.tool
```

**Expected:** `200 OK` with a JSON array of all registered sensors.

---

### 8. Filter Sensors by Type — GET /api/v1/sensors?type=CO2

```bash
curl -s -X GET "http://localhost:8080/api/v1/sensors?type=CO2" \
     -H "Accept: application/json" | python3 -m json.tool
```

**Expected:** `200 OK` with only CO2 sensors returned. Change the type value to `Temperature`, `Occupancy`, `Humidity`, etc.

---

### 9. Add a Reading to an ACTIVE Sensor — POST /api/v1/sensors/{id}/readings

```bash
curl -s -X POST http://localhost:8080/api/v1/sensors/TEMP-001/readings \
     -H "Content-Type: application/json" \
     -d '{
           "value": 24.8
         }' | python3 -m json.tool
```

**Expected:** `201 Created` showing the new reading and the updated `currentValue` on the sensor.

---

### 10. Attempt Reading on MAINTENANCE Sensor — Expect 403

```bash
curl -s -X POST http://localhost:8080/api/v1/sensors/OCC-001/readings \
     -H "Content-Type: application/json" \
     -d '{ "value": 12.0 }' | python3 -m json.tool
```

**Expected:** `403 Forbidden` with `SENSOR_UNAVAILABLE` error code. (`OCC-001` is pre-seeded as MAINTENANCE.)

---

### 11. Get Readings History — GET /api/v1/sensors/{id}/readings

```bash
curl -s -X GET http://localhost:8080/api/v1/sensors/TEMP-001/readings \
     -H "Accept: application/json" | python3 -m json.tool
```

**Expected:** `200 OK` with `sensorId`, `count`, and `readings` array.

---

### 12. Attempt to Delete a Room with Sensors — Expect 409

```bash
curl -s -X DELETE http://localhost:8080/api/v1/rooms/LIB-301 \
     -H "Accept: application/json" | python3 -m json.tool
```

**Expected:** `409 Conflict` with `ROOM_NOT_EMPTY` error code (LIB-301 has TEMP-001 and CO2-001).

---

### 13. Delete a Room that has No Sensors — Success

```bash
# First create a room with no sensors
curl -s -X POST http://localhost:8080/api/v1/rooms \
     -H "Content-Type: application/json" \
     -d '{"id":"EMPTY-ROOM","name":"Empty Room","capacity":10}'

# Then delete it
curl -s -X DELETE http://localhost:8080/api/v1/rooms/EMPTY-ROOM \
     -H "Accept: application/json" | python3 -m json.tool
```

**Expected:** `200 OK` with success message.

---

### 14. Wrong Content-Type — Expect 415

```bash
curl -s -X POST http://localhost:8080/api/v1/rooms \
     -H "Content-Type: text/plain" \
     -d 'plain text body' | python3 -m json.tool
```

**Expected:** `415 Unsupported Media Type` (handled automatically by JAX-RS).

---

## Report: Question Answers

---

### Part 1.1 — JAX-RS Resource Lifecycle & In-Memory Data Synchronisation

**Question:** Explain the default lifecycle of a JAX-RS Resource class. Is a new instance instantiated for every incoming request, or does the runtime treat it as a singleton? How does this impact in-memory data management?

**Answer:**

By default, JAX-RS resource classes are **request-scoped**: the runtime creates a brand-new instance of each resource class for every incoming HTTP request. Once the request completes and the response is sent, the instance is eligible for garbage collection. This is specified in the JAX-RS 2.x specification (Section 3.1) and applies unless a resource is explicitly annotated with `@Singleton`.

**Impact on in-memory data management:** Because each request gets its own resource object, instance fields on resource classes are entirely ephemeral — any data written to them during one request is lost when the instance is destroyed. Storing a `HashMap<String, Room>` as an instance field in `RoomResource` would mean each request starts with an empty map, making state persistence impossible.

**Solution — the Singleton DataStore pattern:** In this implementation, `DataStore` is a classic Java singleton (`private static final DataStore INSTANCE = new DataStore()`). It is instantiated once at JVM startup and shared across all resource instances and all threads. Resource classes obtain a reference via `DataStore.getInstance()` on every request, always pointing to the same shared data.

**Thread safety:** Because JAX-RS may serve multiple requests concurrently on different threads, any shared mutable state must be protected. This implementation uses `ConcurrentHashMap` for all data structures instead of `HashMap`. `ConcurrentHashMap` provides thread-safe read and write operations without full synchronisation locks, preventing race conditions, data corruption, and lost updates under concurrent load.

---

### Part 1.2 — HATEOAS and the Value of Hypermedia in RESTful APIs

**Question:** Why is the provision of "Hypermedia" (HATEOAS) considered a hallmark of advanced RESTful design? How does it benefit client developers compared to static documentation?

**Answer:**

HATEOAS — Hypermedia As The Engine Of Application State — is the principle that API responses should contain navigable links to related resources and available actions, rather than requiring clients to construct URLs manually. Roy Fielding, who defined REST in his 2000 doctoral dissertation, described HATEOAS as a mandatory constraint of a truly RESTful system.

**Benefits over static documentation:**

1. **Self-documenting at runtime:** A client can start from `GET /api/v1` and discover all available resources through the `_links` map in the response. This means the API itself is the authoritative source of truth, not a PDF document that may be out of date.

2. **Resilience to URI changes:** If the server changes `/api/v1/rooms` to `/api/v2/rooms`, clients following HATEOAS links adapt automatically. Clients that hard-coded paths from static docs break silently.

3. **Reduced coupling:** Clients depend on link relation types (e.g., `"rooms"`) rather than specific URL strings. The server can evolve its URL structure without requiring coordinated client updates.

4. **State-driven navigation:** Responses can include only the links that are currently valid for that resource's state (e.g., a `delete` link only appears if the room has no sensors), guiding clients toward legal actions.

In the Discovery endpoint, the `_links` object provides direct, machine-readable navigation to `/api/v1/rooms` and `/api/v1/sensors`, implementing this principle concretely.

---

### Part 2.1 — ID-only vs. Full Object Returns for Room Lists

**Question:** When returning a list of rooms, what are the implications of returning only IDs versus returning the full room objects?

**Answer:**

**Returning only IDs:**
- **Advantages:** Minimal payload size — the response is very small even for thousands of rooms. The server performs less serialisation work. Suitable for scenarios where clients only need to populate a dropdown list of room identifiers.
- **Disadvantages:** Clients must issue a separate `GET /rooms/{id}` request for each room they want detail on — this is the "N+1 problem", which drastically increases latency and server load when a client needs to display a full list. The API becomes chatty.

**Returning full room objects (this implementation's choice):**
- **Advantages:** A single request returns everything the client needs to render a full room management dashboard. Reduces round-trips, latency, and complexity on the client side. Better for bandwidth-intensive consumers that always need the full data.
- **Disadvantages:** Higher payload size per response. If the room object grows (e.g., large embedded sensor data), the list response becomes expensive.

**Industry best practice:** Return full objects by default for list endpoints, but consider pagination (e.g., `?page=1&size=20`) and field projection (e.g., `?fields=id,name`) for large datasets. For this campus system, the number of rooms is manageable, so returning full objects is the correct design choice.

---

### Part 2.2 — Idempotency of DELETE

**Question:** Is DELETE idempotent in your implementation? Justify by describing what happens across multiple identical DELETE requests.

**Answer:**

Yes, DELETE is **idempotent** in this implementation, consistent with RFC 7231.

**First DELETE call** (e.g., `DELETE /api/v1/rooms/EMPTY-ROOM`):
- The room is found in the DataStore.
- It has no sensors, so the deletion proceeds.
- The room is removed from the `ConcurrentHashMap`.
- Response: `200 OK` with a success message.

**Second (and subsequent) identical DELETE calls:**
- The room is looked up in the DataStore and not found (it was already deleted).
- The method returns `404 Not Found`.
- The server's state is identical to after the first call: the room is absent.

**Why this is idempotent:** The RFC definition of idempotency states that the side-effects of N identical requests must be the same as a single request. After the first call, the room is gone. After the second, third, or hundredth call, the room is still gone — the state of the server has not changed further. The HTTP response code changes (200 → 404), but the resource state does not. This is the correct and expected behaviour per the HTTP specification.

---

### Part 3.1 — @Consumes and Content-Type Mismatch Consequences

**Question:** Explain the technical consequences if a client sends data in a different format (e.g., text/plain) when the endpoint is annotated with @Consumes(APPLICATION_JSON).

**Answer:**

The `@Consumes(MediaType.APPLICATION_JSON)` annotation tells the JAX-RS runtime that this method only accepts request bodies with the `Content-Type: application/json` header.

When a client sends a request with a different `Content-Type` (e.g., `Content-Type: text/plain` or `Content-Type: application/xml`), the following occurs:

1. **Content negotiation at the framework level:** The JAX-RS runtime (Jersey in this case) inspects the incoming `Content-Type` header and attempts to find a resource method that can consume it.
2. **No matching method found:** Since no method is annotated with `@Consumes("text/plain")`, Jersey cannot match the request to any handler.
3. **Automatic rejection:** The runtime automatically returns **HTTP 415 Unsupported Media Type** — before the resource method is ever invoked.
4. **No business logic is executed:** The application code never runs, so there is no risk of partially processing malformed data.

This is a key advantage of declarative content negotiation in JAX-RS: the framework enforces input format contracts at the infrastructure level, reducing the amount of defensive validation code developers need to write inside business logic.

---

### Part 3.2 — @QueryParam vs. Path Parameter for Filtering

**Question:** Contrast filtering with @QueryParam (?type=CO2) against embedding the filter in the URL path (/sensors/type/CO2). Why is @QueryParam generally superior for filtering?

**Answer:**

**Path parameter approach** (`/api/v1/sensors/type/CO2`):
- The filter value becomes a mandatory part of the URI structure.
- Retrieving all sensors requires a completely different endpoint (e.g., `/api/v1/sensors/all`).
- Combining multiple filters creates URI explosion: `/sensors/type/CO2/status/ACTIVE/room/LIB-301`.
- The path implies a sub-resource hierarchy that does not semantically exist — "type" is not a resource, it is a filter criterion.
- Clients must know the exact path structure and order of filter segments.

**Query parameter approach** (`/api/v1/sensors?type=CO2`) — this implementation:
- Filters are optional by nature: `/api/v1/sensors` (no filter) and `/api/v1/sensors?type=CO2` (filtered) use the same endpoint.
- Multiple filters compose cleanly: `?type=CO2&status=ACTIVE&roomId=LIB-301`.
- The URL path correctly identifies the **resource** (the sensors collection), while query strings identify **parameters for operating on** that resource.
- Cacheable and bookmarkable: browsers, CDNs, and HTTP caches understand query strings as modifiers, not as different resources.
- REST URI design guidelines (RFC 3986) and industry conventions (Google API Design Guide) consistently recommend query strings for filtering, sorting, and searching collections.

The query parameter approach is superior because it maintains a clean, stable resource hierarchy while providing flexible, composable, and optional filtering.

---

### Part 4.1 — Sub-Resource Locator Pattern: Architectural Benefits

**Question:** Discuss the architectural benefits of the Sub-Resource Locator pattern and how it manages complexity in large APIs.

**Answer:**

The **Sub-Resource Locator Pattern** is a JAX-RS mechanism where a resource method — instead of returning a response directly — returns an instance of another resource class. JAX-RS then forwards the request to that class for further processing. In this implementation, `SensorResource.getReadingsResource()` is annotated with `@Path("/{sensorId}/readings")` and returns a `new SensorReadingResource(sensorId)`.

**Architectural Benefits:**

1. **Separation of Concerns:** `SensorResource` is solely responsible for sensor-level operations (create, list, filter, delete sensors). `SensorReadingResource` is solely responsible for reading-level operations (append readings, retrieve history). Each class has a single, clear responsibility.

2. **Reduced class complexity:** Without the sub-resource pattern, every nested path (`/sensors/{id}/readings`, `/sensors/{id}/readings/{rid}`) would need to be defined in one large controller class. As APIs grow, this creates "God classes" with dozens of methods that are difficult to test, maintain, and review.

3. **Testability:** Each resource class can be unit-tested in isolation. `SensorReadingResource` can be instantiated with a mock `sensorId` and tested without involving `SensorResource` at all.

4. **Context propagation:** The `sensorId` path parameter is extracted in the locator method and passed into `SensorReadingResource`'s constructor. The sub-resource class always has the correct sensor context without needing to re-parse path parameters.

5. **Scalability:** In a larger system, sub-resource classes can be moved into their own packages, teams can own different sub-resources independently, and the routing logic remains clear and hierarchical.

Compared to defining every path in one monolithic controller, the locator pattern mirrors the physical hierarchy of the data (a sensor *has* readings) in the code structure itself.

---

### Part 5.1 (5.2 in spec) — HTTP 422 vs. 404 for Missing Reference Inside a Payload

**Question:** Why is HTTP 422 often considered more semantically accurate than 404 when the issue is a missing reference inside a valid JSON payload?

**Answer:**

The distinction is about **what** is missing and **where** the error lies:

- **HTTP 404 Not Found** means the **requested URL** does not correspond to a known resource on the server. The problem is with the request path itself. For example, `GET /api/v1/rooms/DOES-NOT-EXIST` correctly returns 404 because the URI itself points to nothing.

- **HTTP 422 Unprocessable Entity** (defined in RFC 4918, adopted into HTTP semantics) means: "The server understands the Content-Type, the JSON is syntactically valid and parseable, but the **semantic content** of the payload cannot be processed." The request URL was correct, the JSON was well-formed, but a field value inside the payload references something that does not exist.

When a client POSTs a sensor with `"roomId": "GHOST-ROOM"`:
- The endpoint `/api/v1/sensors` **does** exist → 404 is wrong for the endpoint.
- The JSON is syntactically valid → 400 Bad Request is imprecise (the syntax is fine).
- The **value** of `roomId` inside the body is semantically invalid because it refers to a non-existent room → **422 is exactly right**.

Using 422 gives client developers a precise, actionable signal: "Your request structure is correct, but the data you provided contains an invalid reference. Fix the field value, not the URL or JSON structure."

---

### Part 5.4 — Cybersecurity Risks of Exposing Stack Traces

**Question:** From a cybersecurity standpoint, explain the risks of exposing internal Java stack traces to external API consumers.

**Answer:**

Exposing raw Java stack traces in API responses is a serious **information disclosure vulnerability** (classified under OWASP A05:2021 — Security Misconfiguration). The risks include:

1. **Internal path and package structure disclosure:** A stack trace reveals the full qualified class names and package hierarchy (e.g., `com.smartcampus.resource.SensorResource.addReading(SensorResource.java:47)`). Attackers gain a precise map of the application's internal architecture, which accelerates reverse engineering.

2. **Framework and library version fingerprinting:** Stack frames include third-party library names and versions (e.g., `org.glassfish.jersey.server.ServerRuntime$Responder.process(ServerRuntime.java:514)`). Attackers can cross-reference these against public vulnerability databases (NVD, CVE, Exploit-DB) to identify known unpatched exploits for that exact version.

3. **Logic flaw revelation:** A `NullPointerException` at a specific line tells an attacker exactly where a null-check is missing. A `ArrayIndexOutOfBoundsException` reveals that array bounds are not validated. Attackers can craft specific payloads to repeatedly trigger these conditions, potentially causing Denial of Service or exploiting the unstable state for further attacks.

4. **Infrastructure detail exposure:** Stack traces from database or network layers expose connection strings, SQL queries, table names, and internal hostnames. Even in this in-memory system, similar information about data structure organisation is revealed.

5. **Confidence boost for attackers:** Visible errors confirm to an attacker that their probe was partially successful and guide them toward refining their attack vectors.

**Mitigation (implemented in this API):** The `GlobalExceptionMapper` catches all `Throwable` instances, logs the full stack trace server-side using `java.util.logging.Logger` at `SEVERE` level (so developers can diagnose issues), and returns only a safe, generic JSON message to the client with no internal detail.

---

### Part 5.5 — Why Filters Are Superior to Manual Logging in Every Method

**Question:** Why is it advantageous to use JAX-RS filters for cross-cutting concerns like logging, rather than manually inserting Logger.info() statements inside every resource method?

**Answer:**

Cross-cutting concerns are behaviours that apply uniformly across many components, regardless of their specific business logic. Logging every request and response is a classic cross-cutting concern. Using a JAX-RS filter for this is superior to manual per-method logging for several reasons:

1. **DRY Principle (Don't Repeat Yourself):** A filter writes the logging code once. Manual insertion requires duplicating the same `Logger.info()` call in every single resource method — potentially dozens of places. Any bug in the logging format must be fixed in every copy.

2. **Guaranteed coverage:** A filter automatically applies to every request, including new endpoints added in the future. Manual logging requires developers to remember to add it to every new method. Omissions are silent and common.

3. **Single Responsibility Principle:** Resource methods should contain only business logic (manage rooms, validate sensors). Mixing infrastructure concerns (logging, authentication, rate limiting) into business methods violates SRP and makes classes harder to read, test, and maintain.

4. **Easy to change or disable:** To switch from `java.util.logging` to SLF4J, or to disable logging entirely, only the filter class needs modification. With manual insertion, every resource class must be edited.

5. **Consistency:** A filter enforces a uniform log format across all endpoints. Manual logging leads to inconsistent formats (`"Request: GET /rooms"` in one place, `"Called sensors endpoint"` in another), making log analysis and monitoring tools harder to configure.

6. **Performance profiling:** Filters have access to both the request entry and response exit points, making it straightforward to calculate and log request duration — something impossible to do cleanly with per-method logging without significant boilerplate.

The `@Provider` annotation on `LoggingFilter` ensures JAX-RS auto-discovers and applies it globally without any further configuration.

---

*Report prepared by: [Your Name] | Module: 5COSC022W | University of Westminster*
