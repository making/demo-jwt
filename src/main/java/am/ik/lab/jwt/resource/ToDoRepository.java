package am.ik.lab.jwt.resource;

import org.springframework.stereotype.Repository;

import java.util.*;

@Repository
public class ToDoRepository {
    private final Map<String, ToDo> map = Collections.synchronizedMap(new LinkedHashMap<>());

    public Optional<ToDo> findById(String toDoId) {
        return Optional.ofNullable(this.map.get(toDoId));
    }

    public List<ToDo> findAll() {
        return new ArrayList<>(this.map.values());
    }

    public ToDo create(ToDo toDo) {
        this.map.put(toDo.getToDoId(), toDo);
        return toDo;
    }

    public ToDo updateById(ToDo toDo) {
        return this.map.put(toDo.getToDoId(), toDo);
    }

    public void deleteById(String toDoId) {
        this.map.remove(toDoId);
    }

    void clear() {
        this.map.clear();
    }
}
