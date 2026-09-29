package io.github.moriyaeldar.luach.tasks.web;

import io.github.moriyaeldar.luach.tasks.api.PreviewRequest;
import io.github.moriyaeldar.luach.tasks.api.PreviewResponse;
import io.github.moriyaeldar.luach.tasks.api.WeekResponse;
import io.github.moriyaeldar.luach.tasks.service.WeekService;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

@RestController
public class WeekController {

    private final WeekService service;

    public WeekController(WeekService service) {
        this.service = service;
    }

    /** Seven days starting at {@code start} (the client decides where a week starts, usually Sunday). */
    @GetMapping("/api/week")
    public WeekResponse week(@RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate start) {
        return service.week(start);
    }

    /** The next occurrences of a rule, so users can see what a Hebrew date means in coming years. */
    @PostMapping("/api/recurrence/preview")
    public PreviewResponse preview(@Valid @RequestBody PreviewRequest request) {
        return service.preview(request);
    }
}
