package com.kharcha.common.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
@Tag(name = "Health", description = "Application health check endpoints")
public class HealthController {

    @RequestMapping(value = "/health", method = {org.springframework.web.bind.annotation.RequestMethod.GET, org.springframework.web.bind.annotation.RequestMethod.HEAD}, produces = "text/plain")
    @Operation(summary = "Check health", description = "Returns 'ok' if the service is up and running")
    public ResponseEntity<String> health() {
        return ResponseEntity.ok("ok");
    }
}
