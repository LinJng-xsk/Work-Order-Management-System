package com.campus.woms.controller;

import com.campus.woms.common.result.Result;
import com.campus.woms.entity.WorkOrderMessage;
import com.campus.woms.security.SecurityUtils;
import com.campus.woms.service.WorkOrderMessageService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;

@RestController
@RequestMapping("/workorder")
@RequiredArgsConstructor
public class WorkOrderMessageController {

    private final WorkOrderMessageService messageService;

    @GetMapping("/{id}/messages")
    public Result<List<WorkOrderMessage>> list(@PathVariable Long id) {
        return Result.ok(messageService.listMessages(id,
                SecurityUtils.getUserId(), new ArrayList<>(SecurityUtils.getLoginUser().getRoles())));
    }

    @PostMapping("/{id}/messages")
    public Result<Void> send(@PathVariable Long id, @RequestBody WorkOrderMessage message) {
        messageService.sendMessage(message, id,
                SecurityUtils.getUserId(), SecurityUtils.getUsername(),
                new ArrayList<>(SecurityUtils.getLoginUser().getRoles()));
        return Result.ok();
    }
}
