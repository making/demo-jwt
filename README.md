# Demo Spring Security + JWT

This app demonstrates standalone JWT authentication with Spring Security using [OAuth 2.0 Resource Server feature](https://docs.spring.io/spring-security/site/docs/5.2.2.RELEASE/reference/htmlsingle/#oauth2resourceserver).

Actually it mimics OAuth2's Resource Owner Password Credentials flow.

```
ACCESS_TOKEN=$(curl -s localhost:8080/oauth/token -d grant_type=password -d username=demo -d password=demo | jq -r .access_token)

TODO_ID=$(curl -s localhost:8080/todos -H "Authorization: Bearer ${ACCESS_TOKEN}" -H "Content-Type: application/json" -d '{"toDoTitle": "Demo"}' | jq -r .toDoId)
curl -s localhost:8080/todos -H "Authorization: Bearer ${ACCESS_TOKEN}"
curl -s localhost:8080/todos/${TODO_ID} -H "Authorization: Bearer ${ACCESS_TOKEN}"
curl -s -X PUT localhost:8080/todos/${TODO_ID} -H "Authorization: Bearer ${ACCESS_TOKEN}" -H "Content-Type: application/json" -d '{"finished": "true"}'
curl -s -X DELETE localhost:8080/todos/${TODO_ID} -H "Authorization: Bearer ${ACCESS_TOKEN}"
curl -s localhost:8080/todos -H "Authorization: Bearer ${ACCESS_TOKEN}"
```