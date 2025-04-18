package com.gk.sms.repository;

import com.gk.sms.entities.UserWiseWebhookRegistryEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserWiseWebhookRepository extends JpaRepository<UserWiseWebhookRegistryEntity,Long> {
    Optional<UserWiseWebhookRegistryEntity> findByWebhookId(String webhookId);

    boolean existsByWebhookId(String webhookId);
}
