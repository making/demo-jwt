package am.ik.lab.jwt.resource;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

@RestController
@RequestMapping("todos")
public class ToDoController {
    private final ToDoRepository toDoRepository;

    public ToDoController(ToDoRepository toDoRepository) {
        this.toDoRepository = toDoRepository;
    }

    @GetMapping(path = "")
    public ResponseEntity<List<ToDo>> getToDos() {
        final List<ToDo> toDos = this.toDoRepository.findAll();
        return ResponseEntity.ok(toDos);
    }

    @GetMapping(path = "/{toDoId}")
    public ResponseEntity<ToDo> getToDo(@PathVariable("toDoId") String toDoId) {
        final Optional<ToDo> toDo = this.toDoRepository.findById(toDoId);
        return ResponseEntity.of(toDo);
    }

    @PostMapping(path = "")
    public ResponseEntity<ToDo> postToDos(@RequestBody ToDo toDo, @AuthenticationPrincipal Jwt jwt, UriComponentsBuilder builder) {
        toDo.setToDoId(UUID.randomUUID().toString());
        toDo.setCreatedAt(Instant.now());
        toDo.setCreatedBy(jwt.getSubject());
        final ToDo created = this.toDoRepository.create(toDo);
        final URI uri = builder.pathSegment("todos", created.getToDoId()).build().toUri();
        return ResponseEntity.created(uri).body(created);
    }

    @PutMapping(path = "/{toDoId}")
    public ResponseEntity<ToDo> putTodo(@PathVariable("toDoId") String toDoId, @RequestBody ToDo toDo, @AuthenticationPrincipal Jwt jwt) {
        final Optional<ToDo> updated = this.toDoRepository.findById(toDoId)
                .map(t -> {
                    if (toDo.getToDoTitle() != null) {
                        t.setToDoTitle(toDo.getToDoTitle());
                    }
                    if (!Objects.equals(toDo.isFinished(), t.isFinished())) {
                        t.setFinished(toDo.isFinished());
                    }
                    t.setUpdatedAt(Instant.now());
                    t.setUpdatedBy(jwt.getSubject());
                    return this.toDoRepository.updateById(t);
                });
        return ResponseEntity.of(updated);
    }

    @DeleteMapping(path = "/{toDoId}")
    public ResponseEntity<Void> deleteTodo(@PathVariable("toDoId") String toDoId) {
        this.toDoRepository.deleteById(toDoId);
        return ResponseEntity.noContent().build();
    }
}
