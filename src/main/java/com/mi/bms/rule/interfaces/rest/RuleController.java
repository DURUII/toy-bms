package com.mi.bms.rule.interfaces.rest;

import com.mi.bms.rule.application.RuleService;
import com.mi.bms.vehicle.interfaces.rest.dto.RuleRequest;
import com.mi.bms.vehicle.interfaces.rest.dto.RuleResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.List;

@RestController
@RequestMapping("/api/rules")
@RequiredArgsConstructor
public class RuleController {

    private final RuleService ruleService;

    @PostMapping
    public ResponseEntity<RuleResponse> createRule(@Valid @RequestBody RuleRequest request) {
        return ResponseEntity.ok(ruleService.createRule(request));
    }

    @PutMapping("/{ruleId}")
    public ResponseEntity<RuleResponse> updateRule(
            @PathVariable Long ruleId,
            @Valid @RequestBody RuleRequest request) {
        return ResponseEntity.ok(ruleService.updateRule(ruleId, request));
    }

    @DeleteMapping("/{ruleId}")
    public ResponseEntity<Void> deleteRule(@PathVariable Long ruleId) {
        ruleService.deleteRule(ruleId);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/{ruleId}")
    public ResponseEntity<RuleResponse> getRuleById(@PathVariable Long ruleId) {
        return ResponseEntity.ok(ruleService.getRuleById(ruleId));
    }

    @GetMapping
    public ResponseEntity<List<RuleResponse>> getAllRules() {
        return ResponseEntity.ok(ruleService.getAllRules());
    }
}