package com.gk.sms.repository;

import com.gk.sms.entities.UserMessagesInfoEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface UserMessagesInfoRepository extends JpaRepository<UserMessagesInfoEntity,Long> {
}
