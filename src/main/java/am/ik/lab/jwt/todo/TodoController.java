package am.ik.lab.jwt.todo;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("todos")
@CrossOrigin
public class TodoController {
    private final TodoRepository todoRepository;

    public TodoController(TodoRepository todoRepository) {
        this.todoRepository = todoRepository;
    }

    @GetMapping(path = "")
    public ResponseEntity<List<Todo>> getTodos() {
        final List<Todo> todos = this.todoRepository.findAll();
        return ResponseEntity.ok(todos);
    }

    @GetMapping(path = "/{todoId}")
    public ResponseEntity<Todo> getTodo(@PathVariable("todoId") String todoId) {
        final Optional<Todo> todo = this.todoRepository.findById(todoId);
        return ResponseEntity.of(todo);
    }

    @PostMapping(path = "")
    public ResponseEntity<Todo> postTodos(@RequestBody Todo todo, @AuthenticationPrincipal Jwt jwt, UriComponentsBuilder builder) {
        final Todo initialized = todo.initializedBy(jwt.getSubject());
        final Todo created = this.todoRepository.create(initialized);
        final URI uri = builder.pathSegment("todos", created.getTodoId()).build().toUri();
        return ResponseEntity.created(uri).body(created);
    }

    @PutMapping(path = "/{todoId}")
    public ResponseEntity<Todo> putTodo(@PathVariable("todoId") String todoId, @RequestBody Todo todo, @AuthenticationPrincipal Jwt jwt) {
        final Optional<Todo> updated = this.todoRepository.findById(todoId)
                .map(t -> t.updatedBy(todo.getTodoTitle(), todo.isFinished(), jwt.getSubject()))
                .map(this.todoRepository::updateById);
        return ResponseEntity.of(updated);
    }

    @DeleteMapping(path = "/{todoId}")
    public ResponseEntity<Void> deleteTodo(@PathVariable("todoId") String todoId) {
        this.todoRepository.deleteById(todoId);
        return ResponseEntity.noContent().build();
    }
}
