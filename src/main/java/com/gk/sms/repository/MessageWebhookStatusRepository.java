package com.gk.sms.repository;

import com.gk.sms.entities.MessageWebhookStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface MessageWebhookStatusRepository  extends JpaRepository<MessageWebhookStatus,Long> {
}
