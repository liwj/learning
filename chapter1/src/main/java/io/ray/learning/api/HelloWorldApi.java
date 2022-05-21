package io.ray.learning.api;

import io.ray.learning.idempotency.annotation.Idempotent;
import io.ray.learning.idempotency.annotation.IdempotentContent;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
public class HelloWorldApi {
    @GetMapping("/exec/{id}")
    @Idempotent
    public ResponseEntity exec1(@PathVariable("id") @IdempotentContent(@IdempotentContent.Key(name = "id")) String id) throws InterruptedException {
        Thread.sleep(5000);
        return ResponseEntity.ok(String.format("id = %s ,successful",id));
    }
    @GetMapping("/exec2")
    @Idempotent
    public ResponseEntity exec2(@IdempotentContent(@IdempotentContent.Key(name = "id"))String id) throws InterruptedException {
        Thread.sleep(5000);
        return ResponseEntity.ok(String.format("id = %s ,successful",id));
    }
    @GetMapping("/exec3")
    @Idempotent(operationName = Idempotent.OperationName.CUSTOMIZE, customizeOperationName = "exec3")
    public ResponseEntity exec3(String id) throws InterruptedException {
        Thread.sleep(5000);
        return ResponseEntity.ok(String.format("id = %s ,successful",id));
    }
    @PostMapping("/exec4")
    @Idempotent(operationName = Idempotent.OperationName.CUSTOMIZE, customizeOperationName = "exec4")
    public ResponseEntity exec4(@IdempotentContent(@IdempotentContent.Key(name = "param.refNo",valuePath = "param.refNo")) @RequestBody Map<String,Object> param) throws InterruptedException {
        Thread.sleep(5000);
        return ResponseEntity.ok(String.format("id = %s ,successful",param));
    }
}
