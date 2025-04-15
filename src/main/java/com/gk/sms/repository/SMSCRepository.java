package com.gk.sms.repository;

import com.gk.sms.entities.SMSCEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SMSCRepository extends JpaRepository<SMSCEntity,Long> {
    List<SMSCEntity> findAllByNameIn(List<String> smscList);
}
