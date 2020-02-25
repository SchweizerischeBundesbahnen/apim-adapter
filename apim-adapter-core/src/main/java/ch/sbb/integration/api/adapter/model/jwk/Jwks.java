package ch.sbb.integration.api.adapter.model.jwk;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;

import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

public class Jwks {
    private static final TypeReference<Map<String, Object>> TYPE_REF = new TypeReference<Map<String, Object>>() {
    };

    private final Map<String, Jwk> keys = Collections.synchronizedMap(new HashMap<>());

    public Jwks(String jwksJson) throws IOException {
        final ObjectMapper mapper = new ObjectMapper();
        final JsonNode jwksRootNode = mapper.readTree(jwksJson);
        final ArrayNode keysNode = (ArrayNode) jwksRootNode.get("keys");
        for (JsonNode key : keysNode) {
            final Map<String, Object> jwkAsMap = mapper.convertValue(key, TYPE_REF);
            final Jwk jwk = Jwk.fromValues(jwkAsMap);
            keys.put(jwk.getId(), jwk);
        }
    }


    public Map<String, Jwk> getKeys() {
        return keys;
    }

    public Optional<Jwk> getKey(String kid) {
        return Optional.ofNullable(keys.get(kid));
    }

    public Set<String> getKeyIds() {
        return keys.values().stream().map(Jwk::getId).collect(Collectors.toSet());
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("Jwks{");
        sb.append("keys=[");
        sb.append(String.join(", ", keys.keySet()));
        sb.append("]}");
        return sb.toString();
    }
}
