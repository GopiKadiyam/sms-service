package com.gk.sms.controller;

import com.gk.sms.entities.SMSCEntity;
import com.gk.sms.entities.ServiceTypeEntity;
import com.gk.sms.entities.ShortenUrlEntity;
import com.gk.sms.entities.TenantToPartition;
import com.gk.sms.entities.UserEntity;
import com.gk.sms.entities.UserWiseAPIKeyEntity;
import com.gk.sms.entities.UserWiseServiceTypeEntity;
import com.gk.sms.entities.UserWiseWebhookEntity;
import com.gk.sms.exception.EntityNotFoundException;
import com.gk.sms.exception.InvalidRequestException;
import com.gk.sms.model.ShortenUrlRequest;
import com.gk.sms.model.UserWiseApiKey;
import com.gk.sms.repository.SMSCRepository;
import com.gk.sms.repository.ServiceTypeRepository;
import com.gk.sms.repository.ShortenUrlRepository;
import com.gk.sms.repository.TenantToPartitionRepository;
import com.gk.sms.repository.UserRepository;
import com.gk.sms.repository.UserWiseAPIKeyRepository;
import com.gk.sms.repository.UserWiseServiceTypeRepository;
import com.gk.sms.repository.UserWiseWebhookRepository;
import com.gk.sms.utils.common.ShortUrlKeyGenerator;
import com.gk.sms.utils.common.UrlEncoderUtils;
import com.gk.sms.utils.enums.ApiKeyValidity;
import com.gk.sms.utils.enums.CRMType;
import com.gk.sms.utils.enums.ServiceType;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@RestController
@Slf4j
public class CommonController {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private UserWiseAPIKeyRepository userWiseAPIKeyRepository;

    @Autowired
    private ServiceTypeRepository serviceTypeRepository;
    @Autowired
    private UserWiseServiceTypeRepository userWiseServiceTypeRepository;
    @Autowired
    private SMSCRepository smscRepository;
    @Autowired
    private UserWiseWebhookRepository userWiseWebhookRepository;
    @Autowired
    private BCryptPasswordEncoder passwordEncoder;
    @Autowired
    private TenantToPartitionRepository tenantToPartitionRepository;
    @Autowired
    private ShortenUrlRepository shortenUrlRepository;

    @GetMapping("/{senderId}/{shortUrlKey}")
    public ResponseEntity<?> redirectUrl(
            @PathVariable String senderId,
            @PathVariable String shortUrlKey) {
        ShortenUrlEntity entity = shortenUrlRepository.findBySenderIdAndShortUrlKeyAndActiveFlag(senderId, shortUrlKey, true)
                .orElseThrow(() -> new EntityNotFoundException("", "no link present with senderid :" + senderId + " and token :" + shortUrlKey));
        HttpHeaders headers = new HttpHeaders();
        headers.add("Location", UrlEncoderUtils.decodeURL(entity.getRedirectUrl()));
        return new ResponseEntity<>(headers, HttpStatus.FOUND); // 302 redirect
    }

    @PostMapping("/short/link")
    public ResponseEntity<String> createShortLink(@RequestHeader("tenant-id") String tenantId,
                                                  @RequestHeader("api-key") String apiKey,
                                                  @RequestBody ShortenUrlRequest request,
                                                  HttpServletRequest httpRequest) {
        String shortUrlKey = request.getToken();
        if (shortUrlKey != null && shortenUrlRepository.existsByShortUrlKey(shortUrlKey)) {
            throw new InvalidRequestException("token", "token " + request.getToken() + " is already present, use another one");
        }

        ShortenUrlEntity entity = new ShortenUrlEntity();
        entity.setSenderId(request.getSender());
        entity.setShortUrlKey(shortUrlKey != null ? shortUrlKey : generateUniqueShortUrlKey());
        entity.setRedirectUrl(UrlEncoderUtils.encodeURL(request.getDestinationUrl()));
        entity.setActiveFlag(request.isActiveFlag());
        entity.setComments(request.getComments());
        entity = shortenUrlRepository.save(entity);
        return ResponseEntity.ok(entity.getShortUrlKey());
    }

    public String generateUniqueShortUrlKey() {
        String shortUrlKey;
        do {
            shortUrlKey = ShortUrlKeyGenerator.generateShortUrlKey();
        } while (shortenUrlRepository.existsByShortUrlKey(shortUrlKey));
        return shortUrlKey;
    }

    @PostMapping("/service-types/save")
    public List<Long> saveServiceTypes(@RequestBody List<ServiceTypeEntity> serviceTypes) {
        return serviceTypeRepository.saveAll(serviceTypes).stream()
                .map(ServiceTypeEntity::getId)
                .collect(Collectors.toList());
    }

    @PostMapping("/smsc/save")
    public List<Long> saveSMSC(@RequestBody List<SMSCEntity> smscList) {
        return smscRepository.saveAll(smscList).stream()
                .map(SMSCEntity::getId)
                .collect(Collectors.toList());
    }

    @PostMapping("/users/save")
    public List<String> saveUsers(@RequestBody List<UserEntity> users) {
        List<UserEntity> encodedUsers = users.stream().map(u -> {
            u.setPassword(passwordEncoder.encode(u.getPassword()));
            return u;
        }).toList();
        return userRepository.saveAll(encodedUsers).stream()
                .map(UserEntity::getId)
                .collect(Collectors.toList());
    }

    @PostMapping("/user/services/save")
    public List<Long> saveUserWiseServices(@RequestBody List<UserWiseApiKey> userWiseApiKeys) {
        List<String> serviceTypes = userWiseApiKeys.stream().map(UserWiseApiKey::getServiceType)
                .map(ServiceType::getValue)
                .collect(Collectors.toList());
        List<String> smscList = userWiseApiKeys.stream().map(UserWiseApiKey::getSmsc).collect(Collectors.toList());
        Map<String, ServiceTypeEntity> serviceTypeEntityMap = serviceTypeRepository.findAllByNameIn(serviceTypes)
                .stream().collect(Collectors.toMap(ServiceTypeEntity::getName, st -> st));
        Map<String, SMSCEntity> smscEntityMap = smscRepository.findAllByNameIn(smscList)
                .stream().collect(Collectors.toMap(SMSCEntity::getName, smsc -> smsc));

        return userWiseApiKeys.stream().map(uak -> {
            UserWiseServiceTypeEntity ust = new UserWiseServiceTypeEntity();
            UserEntity userEntity = userRepository.findByUsername(uak.getUserId())
                    .orElseThrow(() -> new EntityNotFoundException("userId", uak.getUserId() + " not found"));
            ust.setUser(userEntity);
            ust.setServiceType(serviceTypeEntityMap.get(uak.getServiceType().toString()));
            ust.setSmsc(smscEntityMap.get(uak.getSmsc()));
            return userWiseServiceTypeRepository.save(ust).getId();
        }).collect(Collectors.toList());
    }

    @PostMapping("/user/api-key/save")
    public String saveUserApiKeys(@RequestBody Map<String, String> request) {
        UserEntity userEntity = userRepository.findByUsername(request.get("username"))
                .orElseThrow(() -> new EntityNotFoundException("userId", request.get("userName") + " not found"));

        UserWiseAPIKeyEntity userWiseAPIKeyEntity = new UserWiseAPIKeyEntity();
        userWiseAPIKeyEntity.setUser(userEntity);
        userWiseAPIKeyEntity.setApiKey(generateApiKey());
        userWiseAPIKeyEntity.setValidity(ApiKeyValidity.QUARTERLY);
        userWiseAPIKeyEntity.setActiveFlag(true);
        return userWiseAPIKeyRepository.save(userWiseAPIKeyEntity).getApiKey();
    }

    private String generateApiKey() {
        String apiKey;
        do {
            apiKey = UUID.randomUUID().toString().replace("-", "");
            ;
        } while (userWiseAPIKeyRepository.existsByApiKey(apiKey));
        return apiKey;
    }

    @PostMapping("/user/webhook/save")
    public String saveUserWebhook(@RequestBody Map<String, String> request) {
        UserEntity userEntity = userRepository.findByUsername(request.get("username"))
                .orElseThrow(() -> new EntityNotFoundException("userId", request.get("userName") + " not found"));

        UserWiseWebhookEntity webhookEntity = new UserWiseWebhookEntity();
        webhookEntity.setUser(userEntity);
        webhookEntity.setWebhookId(generateWebhookId());
        webhookEntity.setWebhookUrl(request.get("url"));
        webhookEntity.setActiveFlag(true);
        webhookEntity.setCrmType(CRMType.fromValue(request.get("crmType")));

        return userWiseWebhookRepository.save(webhookEntity).getWebhookId();
    }

    @PostMapping("/user/kafka/partition")
    public void mapUserWiseKafkaTopiPartition(@RequestBody Map<String, String> request) {
        UserEntity userEntity = userRepository.findByUsername(request.get("username"))
                .orElseThrow(() -> new EntityNotFoundException("userId", request.get("userName") + " not found"));

        TenantToPartition tenantToPartition = new TenantToPartition();
        tenantToPartition.setUser(userEntity);
        tenantToPartition.setPartitionNum(Integer.parseInt(request.get("partition")));

        tenantToPartitionRepository.save(tenantToPartition);
    }

    private String generateWebhookId() {
        String apiKey;
        do {
            apiKey = UUID.randomUUID().toString();
        } while (userWiseWebhookRepository.existsByWebhookId(apiKey));
        return apiKey;
    }
}
