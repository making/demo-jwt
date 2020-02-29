package am.ik.lab.jwt.resource;

import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.web.server.LocalServerPort;
import org.springframework.http.*;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

import java.net.URI;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT;

@SpringBootTest(webEnvironment = RANDOM_PORT, properties = {
        "logging.level.ROOT=error",
        "logging.level.org.springframework.web.client.RestTemplate=DEBUG"})
class TodoControllerTest {

    private final int port;
    private final TestRestTemplate restTemplate;
    private final ToDoRepository toDoRepository;
    private final ToDo toDo1 = new ToDo() {{
        setToDoId(UUID.randomUUID().toString());
        setToDoTitle("ToDo1");
        setFinished(false);
        setCreatedAt(Instant.now());
        setCreatedBy("admin");
    }};
    private final ToDo toDo2 = new ToDo() {{
        setToDoId(UUID.randomUUID().toString());
        setToDoTitle("ToDo2");
        setFinished(true);
        setCreatedAt(Instant.now());
        setCreatedBy("admin");
        setUpdatedAt(Instant.now());
        setUpdatedBy("admin");
    }};

    TodoControllerTest(@LocalServerPort int port,
                       @Autowired TestRestTemplate restTemplate,
                       @Autowired ToDoRepository toDoRepository) {
        this.port = port;
        this.restTemplate = restTemplate;
        this.toDoRepository = toDoRepository;
    }

    String login(String username, String password) {
        final RequestEntity<MultiValueMap<String, String>> req = RequestEntity.post(URI.create("http://localhost:" + port + "/oauth/token"))
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .body(new LinkedMultiValueMap<>() {

                    {
                        add("username", username);
                        add("password", password);
                        add("grant_type", "password");
                    }
                });
        final ResponseEntity<JsonNode> res = this.restTemplate.exchange(req, JsonNode.class);
        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.OK);
        return res.getBody().get("access_token").asText();
    }

    @BeforeEach
    void init() {
        this.toDoRepository.clear();
        this.toDoRepository.create(this.toDo1);
        this.toDoRepository.create(this.toDo2);
    }

    @Test
    void getToDos_unauthorized() {
        final ResponseEntity<String> res = this.restTemplate.getForEntity("/todos", String.class);
        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void getToDos_ok() {
        final String accessToken = this.login("demo", "demo");
        final RequestEntity<?> req = RequestEntity.get(URI.create("http://localhost:" + port + "/todos"))
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                .build();
        final ResponseEntity<JsonNode> res = this.restTemplate.exchange(req, JsonNode.class);
        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.OK);
        final JsonNode body = res.getBody();
        assertThat(body).isNotNull();
        assertThat(body.size()).isEqualTo(2);
        assertThat(body.get(0).get("toDoId").asText()).isEqualTo(this.toDo1.getToDoId());
        assertThat(body.get(0).get("toDoTitle").asText()).isEqualTo("ToDo1");
        assertThat(body.get(0).get("finished").asBoolean()).isFalse();
        assertThat(body.get(0).get("createdAt")).isNotNull();
        assertThat(body.get(0).get("createdBy").asText()).isEqualTo("admin");
        assertThat(body.get(0).get("updatedAt")).isNull();
        assertThat(body.get(0).get("updatedBy")).isNull();
        assertThat(body.get(1).get("toDoId").asText()).isEqualTo(this.toDo2.getToDoId());
        assertThat(body.get(1).get("toDoTitle").asText()).isEqualTo("ToDo2");
        assertThat(body.get(1).get("finished").asBoolean()).isTrue();
        assertThat(body.get(1).get("createdAt")).isNotNull();
        assertThat(body.get(1).get("createdBy").asText()).isEqualTo("admin");
        assertThat(body.get(1).get("updatedAt")).isNotNull();
        assertThat(body.get(1).get("updatedBy").asText()).isEqualTo("admin");
    }

    @Test
    void getToDo_ok() {
        final String accessToken = this.login("demo", "demo");
        final RequestEntity<?> req = RequestEntity.get(URI.create("http://localhost:" + port + "/todos/" + this.toDo1.getToDoId()))
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                .build();
        final ResponseEntity<JsonNode> res = this.restTemplate.exchange(req, JsonNode.class);
        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.OK);
        final JsonNode body = res.getBody();
        assertThat(body).isNotNull();
        assertThat(body.get("toDoId").asText()).isEqualTo(this.toDo1.getToDoId());
        assertThat(body.get("toDoTitle").asText()).isEqualTo("ToDo1");
        assertThat(body.get("finished").asBoolean()).isFalse();
        assertThat(body.get("createdAt")).isNotNull();
        assertThat(body.get("createdBy").asText()).isEqualTo("admin");
        assertThat(body.get("updatedAt")).isNull();
        assertThat(body.get("updatedBy")).isNull();
    }

    @Test
    void getToDo_notFound() {
        final String accessToken = this.login("demo", "demo");
        final RequestEntity<?> req = RequestEntity.get(URI.create("http://localhost:" + port + "/todos/xxxxxxxxxx"))
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                .build();
        final ResponseEntity<JsonNode> res = this.restTemplate.exchange(req, JsonNode.class);
        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void postToDos_created() {
        final String accessToken = this.login("demo", "demo");
        final RequestEntity<?> req = RequestEntity.post(URI.create("http://localhost:" + port + "/todos"))
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                .body(Map.of("toDoTitle", "Demo"));
        final ResponseEntity<JsonNode> res = this.restTemplate.exchange(req, JsonNode.class);
        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        final JsonNode body = res.getBody();
        assertThat(body).isNotNull();
        final String toDoId = body.get("toDoId").asText();
        assertThat(toDoId).isNotNull();
        assertThat(body.get("toDoTitle").asText()).isEqualTo("Demo");
        assertThat(body.get("finished").asBoolean()).isFalse();
        assertThat(body.get("createdAt")).isNotNull();
        assertThat(body.get("createdBy").asText()).isEqualTo("demo");
        assertThat(body.get("updatedAt")).isNull();
        assertThat(body.get("updatedBy")).isNull();
        assertThat(res.getHeaders().getLocation()).isEqualTo(URI.create("http://localhost:" + port + "/todos/" + toDoId));
    }

    @Test
    void putToDo_ok() {
        final String accessToken = this.login("demo", "demo");
        final RequestEntity<?> req = RequestEntity.put(URI.create("http://localhost:" + port + "/todos/" + this.toDo1.getToDoId()))
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                .body(Map.of("finished", true));
        final ResponseEntity<JsonNode> res = this.restTemplate.exchange(req, JsonNode.class);
        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.OK);
        final JsonNode body = res.getBody();
        assertThat(body).isNotNull();
        final String toDoId = body.get("toDoId").asText();
        assertThat(toDoId).isNotNull();
        assertThat(body.get("toDoTitle").asText()).isEqualTo("ToDo1");
        assertThat(body.get("finished").asBoolean()).isTrue();
        assertThat(body.get("createdAt")).isNotNull();
        assertThat(body.get("createdBy").asText()).isEqualTo("admin");
        assertThat(body.get("updatedAt")).isNotNull();
        assertThat(body.get("updatedBy").asText()).isEqualTo("demo");
    }

    @Test
    void putToDo_notFound() {
        final String accessToken = this.login("demo", "demo");
        final RequestEntity<?> req = RequestEntity.put(URI.create("http://localhost:" + port + "/todos/xxxxxxxxxx"))
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                .body(Map.of("finished", true));
        final ResponseEntity<JsonNode> res = this.restTemplate.exchange(req, JsonNode.class);
        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void deleteToDo_noContent() {
        final String accessToken = this.login("demo", "demo");
        final String toDoId = this.toDo1.getToDoId();
        final RequestEntity<?> req = RequestEntity.delete(URI.create("http://localhost:" + port + "/todos/" + toDoId))
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                .build();
        final ResponseEntity<JsonNode> res = this.restTemplate.exchange(req, JsonNode.class);
        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
        assertThat(this.toDoRepository.findById(toDoId).isPresent()).isFalse();
    }
}