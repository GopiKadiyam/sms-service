package com.gk.sms.repository;

import com.gk.sms.entities.ShortenUrlRegistryEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ShortenUrlRepository extends JpaRepository<ShortenUrlRegistryEntity, Long> {
    Optional<ShortenUrlRegistryEntity> findBySenderIdAndShortUrlKeyAndActiveFlag(String senderId, String shortUrlKey, boolean activeFlag);
    boolean existsByShortUrlKey(String shortUrlKey);
}
