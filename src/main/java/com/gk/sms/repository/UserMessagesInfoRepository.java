package com.gk.sms.repository;

import com.gk.sms.entities.UserMsgReqStatusEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface UserMessagesInfoRepository extends JpaRepository<UserMsgReqStatusEntity,Long> {
}
