package org.example.telegramkworkbot.admin.api;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/admin/api/dashboard")
@RequiredArgsConstructor
public class DashboardController {

    private final MetricsService metrics;

    @GetMapping("/summary")
    public Map<String, MetricsService.Kpi> summary(@RequestParam(required = false) Instant from,
                                                   @RequestParam(required = false) Instant to,
                                                   @RequestParam(required = false) Integer days) {
        return metrics.summary(Period.of(from, to, days));
    }

    /** Несколько рядов за один запрос: ?metrics=orders,deliveries */
    @GetMapping("/timeseries")
    public Map<String, List<MetricsService.Point>> timeseries(@RequestParam List<String> metrics,
                                                              @RequestParam(required = false) String bucket,
                                                              @RequestParam(required = false) Instant from,
                                                              @RequestParam(required = false) Instant to,
                                                              @RequestParam(required = false) Integer days) {
        Period period = Period.of(from, to, days);
        Map<String, List<MetricsService.Point>> result = new LinkedHashMap<>();
        for (String metric : metrics) {
            result.put(metric, this.metrics.series(metric, period, bucket));
        }
        return result;
    }

    @GetMapping("/funnel")
    public List<MetricsService.FunnelStep> funnel(@RequestParam(required = false) Instant from,
                                                  @RequestParam(required = false) Instant to,
                                                  @RequestParam(required = false) Integer days) {
        return metrics.funnel(Period.of(from, to, days));
    }

    @GetMapping("/categories")
    public List<MetricsService.CategoryStat> categories(@RequestParam(required = false) Instant from,
                                                        @RequestParam(required = false) Instant to,
                                                        @RequestParam(required = false) Integer days) {
        return metrics.categories(Period.of(from, to, days));
    }

    @GetMapping("/heatmap")
    public List<MetricsService.HeatCell> heatmap(@RequestParam(required = false) Instant from,
                                                 @RequestParam(required = false) Instant to,
                                                 @RequestParam(required = false) Integer days) {
        return metrics.heatmap(Period.of(from, to, days));
    }

    @GetMapping("/tokens")
    public MetricsService.Tokens tokens(@RequestParam(required = false) Instant from,
                                        @RequestParam(required = false) Instant to,
                                        @RequestParam(required = false) Integer days) {
        return metrics.tokens(Period.of(from, to, days));
    }

    @GetMapping("/health")
    public List<MetricsService.Health> health() {
        return metrics.health();
    }
}
