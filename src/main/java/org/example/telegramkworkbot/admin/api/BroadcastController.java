package org.example.telegramkworkbot.admin.api;

import lombok.RequiredArgsConstructor;
import org.example.telegramkworkbot.sender.SendStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/admin/api/broadcasts")
@RequiredArgsConstructor
public class BroadcastController {

    public record BroadcastRequest(String text, BroadcastService.Segment segment) {}

    private final BroadcastService broadcasts;

    @GetMapping
    public List<BroadcastService.BroadcastRow> list() {
        return broadcasts.list();
    }

    @GetMapping("/{id}")
    public BroadcastService.BroadcastRow get(@PathVariable long id) {
        return broadcasts.get(id);
    }

    @PostMapping("/preview")
    public Map<String, Long> preview(@RequestBody BroadcastService.Segment segment) {
        return Map.of("recipients", broadcasts.countRecipients(segment));
    }

    @PostMapping("/test")
    public Map<String, SendStatus> test(@RequestBody BroadcastRequest body) {
        return Map.of("status", broadcasts.sendTest(body.text()));
    }

    @PostMapping
    public Map<String, Long> start(@RequestBody BroadcastRequest body) {
        return Map.of("id", broadcasts.start(body.text(), body.segment()));
    }

    @PostMapping("/{id}/cancel")
    public void cancel(@PathVariable long id) {
        broadcasts.cancel(id);
    }
}
