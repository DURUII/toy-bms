package com.mi.bms.signal.interfaces.rest;

import com.mi.bms.signal.application.SignalService;
import com.mi.bms.signal.interfaces.rest.dto.SignalRequest;
import com.mi.bms.signal.interfaces.rest.dto.SignalResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/signals")
@RequiredArgsConstructor
public class SignalController {

    private final SignalService signalService;

    @PostMapping
    public ResponseEntity<List<SignalResponse>> reportSignals(@Valid @RequestBody List<SignalRequest> requests) {
        return ResponseEntity.ok(signalService.reportSignals(requests));
    }

    @GetMapping
    public ResponseEntity<List<SignalResponse>> getSignals(
            @RequestParam(required = false) Integer carId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime to) {
        if (carId != null) {
            return ResponseEntity.ok(signalService.getSignalsByCarId(carId, from, to));
        }
        return ResponseEntity.ok(signalService.getAllSignals(from, to));
    }
}