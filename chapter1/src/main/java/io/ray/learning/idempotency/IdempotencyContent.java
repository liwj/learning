package io.ray.learning.idempotency;

import cn.hutool.crypto.digest.MD5;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.extern.slf4j.Slf4j;

import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;

/**
 * @
 */
@Slf4j
public class IdempotencyContent {
    private String operationName;

    private SortedMap<String, Object> contentMap = new TreeMap<>();

    public static IdempotencyContent buildContent(String operationName) {
        return new IdempotencyContent(operationName);
    }
    private IdempotencyContent(String operationName) {
        this.operationName = operationName;
    }
    public IdempotencyContent addContent(String key, Object val) {
        contentMap.put(key, val);
        return this;
    }

    public String serialize() {
        String serializeStr = operationName;
        JsonNodeFactory factory = JsonNodeFactory.instance;
        ObjectNode root = factory.objectNode();
        for (Map.Entry<String, Object> entry : contentMap.entrySet()) {
            root.put(entry.getKey(), entry.getValue().toString());
        }
        serializeStr += ":";
        serializeStr += root.toString();
        if (log.isDebugEnabled())
            log.debug("IdempotencyContent: " + serializeStr);
        return MD5.create().digestHex(serializeStr,StandardCharsets.UTF_8);
    }
}
