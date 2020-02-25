package am.ik.lab.jwt.resource;

import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.web.server.LocalServerPort;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.RequestEntity;
import org.springframework.http.ResponseEntity;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

import java.net.URI;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT;

@SpringBootTest(webEnvironment = RANDOM_PORT, properties = {
    "logging.level.ROOT=error",
    "logging.level.org.springframework.web.client.RestTemplate=DEBUG"})
class MessageControllerTest {

    private final int port;

    private final TestRestTemplate restTemplate;

    MessageControllerTest(@LocalServerPort int port, @Autowired TestRestTemplate restTemplate) {
        this.port = port;
        this.restTemplate = restTemplate;
    }

    String login(String username, String password) {
        final RequestEntity<MultiValueMap<String, String>> req = RequestEntity.post(URI.create("http://localhost:" + port + "/oauth/token"))
            .contentType(MediaType.APPLICATION_FORM_URLENCODED)
            .body(new LinkedMultiValueMap<String, String>() {

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

    @Test
    void index_unauthorized() {
        ResponseEntity<String> res = this.restTemplate.getForEntity("/", String.class);
        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void index_ok() {
        String accessToken = this.login("demo", "demo");
        RequestEntity<?> req = RequestEntity.get(URI.create("http://localhost:" + port))
            .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
            .build();
        ResponseEntity<String> res = this.restTemplate.exchange(req, String.class);
        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(res.getBody()).isEqualTo("Hello, demo!");
    }

    @Test
    void message() {
        String accessToken = this.login("demo", "demo");
        RequestEntity<?> req = RequestEntity.get(URI.create("http://localhost:" + port + "/message"))
            .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
            .build();
        ResponseEntity<String> res = this.restTemplate.exchange(req, String.class);
        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(res.getBody()).isEqualTo("secret message");
    }

    @Test
    void createMessage() {
        String accessToken = this.login("demo", "demo");
        RequestEntity<?> req = RequestEntity.post(URI.create("http://localhost:" + port + "/message"))
            .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
            .body("Hello");
        ResponseEntity<String> res = this.restTemplate.exchange(req, String.class);
        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(res.getBody()).isEqualTo("Message was created. Content: Hello");
    }
}