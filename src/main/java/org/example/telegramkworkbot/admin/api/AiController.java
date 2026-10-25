package org.example.telegramkworkbot.admin.api;

import lombok.RequiredArgsConstructor;
import org.example.telegramkworkbot.admin.AuditService;
import org.example.telegramkworkbot.ai.LlmException;
import org.example.telegramkworkbot.entities.KworkEntity;
import org.example.telegramkworkbot.repository.KworkRepo;
import org.example.telegramkworkbot.service.PromptBuilder;
import org.example.telegramkworkbot.service.ProposalService;
import org.example.telegramkworkbot.settings.SettingsService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Optional;

@RestController
@RequestMapping("/admin/api/ai")
@RequiredArgsConstructor
public class AiController {

    /** Тест промпта: заказ, черновик промпта (null — текущий) и описание исполнителя. */
    public record TestRequest(Long kworkId, String systemPrompt, String about) {}

    public record TestResult(String text, String model, Integer promptTokens, Integer completionTokens, long durationMs) {}

    private final AdminProposalService proposals;
    private final ProposalService proposalService;
    private final KworkRepo kworkRepo;
    private final SettingsService settings;
    private final AuditService audit;

    @GetMapping("/calls")
    public PageResult<AdminProposalService.LlmCallRow> calls(@RequestParam(required = false) String status,
                                                            @RequestParam(defaultValue = "0") int page,
                                                            @RequestParam(defaultValue = "25") int size) {
        return proposals.calls(status, Math.max(page, 0), UsersController.clamp(size));
    }

    @GetMapping("/prompt")
    public Map<String, String> prompt() {
        return Map.of("current", settings.systemPrompt(), "default", PromptBuilder.SYSTEM);
    }

    @PostMapping("/test")
    public ResponseEntity<?> test(@RequestBody TestRequest body) {
        KworkEntity kwork = body.kworkId() != null
                ? kworkRepo.findById(body.kworkId()).orElseThrow(() -> new NoSuchElementException("Заказ не найден"))
                : kworkRepo.findById(kworkRepo.findMaxId()).orElseThrow(() -> new NoSuchElementException("В базе нет заказов"));
        String prompt = body.systemPrompt() == null || body.systemPrompt().isBlank() ? settings.systemPrompt() : body.systemPrompt();
        try {
            var timed = proposalService.complete(null, kwork, Optional.ofNullable(body.about()), prompt);
            audit.record("AI_TEST", String.valueOf(kwork.getId()), null);
            var r = timed.result();
            return ResponseEntity.ok(new TestResult(r.text(), r.model(), r.promptTokens(), r.completionTokens(), timed.durationMs()));
        } catch (LlmException e) {
            return ResponseEntity.status(HttpStatus.BAD_GATEWAY).body(Map.of("error", e.getMessage()));
        }
    }
}
