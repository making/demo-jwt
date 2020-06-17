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
    private final TodoRepository todoRepository;
    private final Todo todo1 = new Todo() {{
        setTodoId(UUID.randomUUID().toString());
        setTodoTitle("Todo1");
        setFinished(false);
        setCreatedAt(Instant.now());
        setCreatedBy("admin");
    }};
    private final Todo todo2 = new Todo() {{
        setTodoId(UUID.randomUUID().toString());
        setTodoTitle("Todo2");
        setFinished(true);
        setCreatedAt(Instant.now());
        setCreatedBy("admin");
        setUpdatedAt(Instant.now());
        setUpdatedBy("admin");
    }};

    TodoControllerTest(@LocalServerPort int port,
                       @Autowired TestRestTemplate restTemplate,
                       @Autowired TodoRepository todoRepository) {
        this.port = port;
        this.restTemplate = restTemplate;
        this.todoRepository = todoRepository;
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
        this.todoRepository.clear();
        this.todoRepository.create(this.todo1);
        this.todoRepository.create(this.todo2);
    }

    @Test
    void getTodos_unauthorized() {
        final ResponseEntity<String> res = this.restTemplate.getForEntity("/todos", String.class);
        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void getTodos_ok() {
        final String accessToken = this.login("demo", "demo");
        final RequestEntity<?> req = RequestEntity.get(URI.create("http://localhost:" + port + "/todos"))
                .headers(headers -> headers.setBearerAuth(accessToken))
                .build();
        final ResponseEntity<JsonNode> res = this.restTemplate.exchange(req, JsonNode.class);
        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.OK);
        final JsonNode body = res.getBody();
        assertThat(body).isNotNull();
        assertThat(body.size()).isEqualTo(2);
        assertThat(body.get(0).get("todoId").asText()).isEqualTo(this.todo1.getTodoId());
        assertThat(body.get(0).get("todoTitle").asText()).isEqualTo("Todo1");
        assertThat(body.get(0).get("finished").asBoolean()).isFalse();
        assertThat(body.get(0).get("createdAt")).isNotNull();
        assertThat(body.get(0).get("createdBy").asText()).isEqualTo("admin");
        assertThat(body.get(0).get("updatedAt")).isNull();
        assertThat(body.get(0).get("updatedBy")).isNull();
        assertThat(body.get(1).get("todoId").asText()).isEqualTo(this.todo2.getTodoId());
        assertThat(body.get(1).get("todoTitle").asText()).isEqualTo("Todo2");
        assertThat(body.get(1).get("finished").asBoolean()).isTrue();
        assertThat(body.get(1).get("createdAt")).isNotNull();
        assertThat(body.get(1).get("createdBy").asText()).isEqualTo("admin");
        assertThat(body.get(1).get("updatedAt")).isNotNull();
        assertThat(body.get(1).get("updatedBy").asText()).isEqualTo("admin");
    }

    @Test
    void getTodo_ok() {
        final String accessToken = this.login("demo", "demo");
        final RequestEntity<?> req = RequestEntity.get(URI.create("http://localhost:" + port + "/todos/" + this.todo1.getTodoId()))
                .headers(headers -> headers.setBearerAuth(accessToken))
                .build();
        final ResponseEntity<JsonNode> res = this.restTemplate.exchange(req, JsonNode.class);
        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.OK);
        final JsonNode body = res.getBody();
        assertThat(body).isNotNull();
        assertThat(body.get("todoId").asText()).isEqualTo(this.todo1.getTodoId());
        assertThat(body.get("todoTitle").asText()).isEqualTo("Todo1");
        assertThat(body.get("finished").asBoolean()).isFalse();
        assertThat(body.get("createdAt")).isNotNull();
        assertThat(body.get("createdBy").asText()).isEqualTo("admin");
        assertThat(body.get("updatedAt")).isNull();
        assertThat(body.get("updatedBy")).isNull();
    }

    @Test
    void getTodo_notFound() {
        final String accessToken = this.login("demo", "demo");
        final RequestEntity<?> req = RequestEntity.get(URI.create("http://localhost:" + port + "/todos/xxxxxxxxxx"))
                .headers(headers -> headers.setBearerAuth(accessToken))
                .build();
        final ResponseEntity<JsonNode> res = this.restTemplate.exchange(req, JsonNode.class);
        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void postTodos_created() {
        final String accessToken = this.login("demo", "demo");
        final RequestEntity<?> req = RequestEntity.post(URI.create("http://localhost:" + port + "/todos"))
                .headers(headers -> headers.setBearerAuth(accessToken))
                .body(Map.of("todoTitle", "Demo"));
        final ResponseEntity<JsonNode> res = this.restTemplate.exchange(req, JsonNode.class);
        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        final JsonNode body = res.getBody();
        assertThat(body).isNotNull();
        final String todoId = body.get("todoId").asText();
        assertThat(todoId).isNotNull();
        assertThat(body.get("todoTitle").asText()).isEqualTo("Demo");
        assertThat(body.get("finished").asBoolean()).isFalse();
        assertThat(body.get("createdAt")).isNotNull();
        assertThat(body.get("createdBy").asText()).isEqualTo("demo");
        assertThat(body.get("updatedAt")).isNotNull();
        assertThat(body.get("updatedBy").asText()).isEqualTo("demo");
        assertThat(res.getHeaders().getLocation()).isEqualTo(URI.create("http://localhost:" + port + "/todos/" + todoId));
    }

    @Test
    void putTodo_ok() {
        final String accessToken = this.login("demo", "demo");
        final RequestEntity<?> req = RequestEntity.put(URI.create("http://localhost:" + port + "/todos/" + this.todo1.getTodoId()))
                .headers(headers -> headers.setBearerAuth(accessToken))
                .body(Map.of("finished", true));
        final ResponseEntity<JsonNode> res = this.restTemplate.exchange(req, JsonNode.class);
        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.OK);
        final JsonNode body = res.getBody();
        assertThat(body).isNotNull();
        final String todoId = body.get("todoId").asText();
        assertThat(todoId).isNotNull();
        assertThat(body.get("todoTitle").asText()).isEqualTo("Todo1");
        assertThat(body.get("finished").asBoolean()).isTrue();
        assertThat(body.get("createdAt")).isNotNull();
        assertThat(body.get("createdBy").asText()).isEqualTo("admin");
        assertThat(body.get("updatedAt")).isNotNull();
        assertThat(body.get("updatedBy").asText()).isEqualTo("demo");
    }

    @Test
    void putTodo_notFound() {
        final String accessToken = this.login("demo", "demo");
        final RequestEntity<?> req = RequestEntity.put(URI.create("http://localhost:" + port + "/todos/xxxxxxxxxx"))
                .headers(headers -> headers.setBearerAuth(accessToken))
                .body(Map.of("finished", true));
        final ResponseEntity<JsonNode> res = this.restTemplate.exchange(req, JsonNode.class);
        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void deleteTodo_noContent() {
        final String accessToken = this.login("demo", "demo");
        final String todoId = this.todo1.getTodoId();
        final RequestEntity<?> req = RequestEntity.delete(URI.create("http://localhost:" + port + "/todos/" + todoId))
                .headers(headers -> headers.setBearerAuth(accessToken))
                .build();
        final ResponseEntity<JsonNode> res = this.restTemplate.exchange(req, JsonNode.class);
        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
        assertThat(this.todoRepository.findById(todoId).isPresent()).isFalse();
    }
}