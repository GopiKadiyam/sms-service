package com.gk.sms.repository;

import com.gk.sms.entities.UserMessagesEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface UserMessagesRepository extends JpaRepository<UserMessagesEntity, String> {
    boolean existsByMsgGroupId(String msgGroupId);
}
