package org.example.telegramkworkbot.admin.api;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/admin/api/proposals")
@RequiredArgsConstructor
public class ProposalsController {

    private final AdminProposalService proposals;

    @GetMapping
    public PageResult<AdminProposalService.ProposalRow> list(@RequestParam(required = false) Long userId,
                                                             @RequestParam(required = false) Long kworkId,
                                                             @RequestParam(defaultValue = "0") int page,
                                                             @RequestParam(defaultValue = "25") int size) {
        return proposals.list(userId, kworkId, Math.max(page, 0), UsersController.clamp(size));
    }
}
