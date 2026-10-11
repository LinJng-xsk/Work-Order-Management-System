package com.campus.woms.service;

import com.campus.woms.entity.WorkOrderMessage;

import java.util.List;

public interface WorkOrderMessageService {

    List<WorkOrderMessage> listMessages(Long orderId, Long userId, List<String> roles);

    void sendMessage(WorkOrderMessage message, Long orderId, Long userId, String userName, List<String> roles);
}
