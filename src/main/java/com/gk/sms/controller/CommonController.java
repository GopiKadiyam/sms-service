package com.gk.sms.controller;

import com.gk.sms.entities.SMSCEntity;
import com.gk.sms.entities.MsgServiceTypeEntity;
import com.gk.sms.entities.SenderEntity;
import com.gk.sms.entities.ShortenUrlRegistryEntity;
import com.gk.sms.entities.TemplateEntity;
import com.gk.sms.entities.UserWiseKafkaPartition;
import com.gk.sms.entities.UserAccountEntity;
import com.gk.sms.entities.UserWiseAPIKeyEntity;
import com.gk.sms.entities.UserWiseServicePermissionEntity;
import com.gk.sms.entities.UserWiseWebhookRegistryEntity;
import com.gk.sms.exception.EntityNotFoundException;
import com.gk.sms.exception.InvalidRequestException;
import com.gk.sms.model.ShortenUrlRequest;
import com.gk.sms.model.UserWiseApiKey;
import com.gk.sms.repository.SMSCRepository;
import com.gk.sms.repository.SenderRepository;
import com.gk.sms.repository.ServiceTypeRepository;
import com.gk.sms.repository.ShortenUrlRepository;
import com.gk.sms.repository.TemplateRepository;
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
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
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
    @Autowired
    private SenderRepository senderRepository;
    @Autowired
    private TemplateRepository templateRepository;

    @GetMapping("/{senderId}/{shortUrlKey}")
    public ResponseEntity<?> redirectUrl(
            @PathVariable String senderId,
            @PathVariable String shortUrlKey) {
        ShortenUrlRegistryEntity entity = shortenUrlRepository.findBySenderIdAndShortUrlKeyAndActiveFlag(senderId, shortUrlKey, true)
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

        ShortenUrlRegistryEntity entity = new ShortenUrlRegistryEntity();
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
    public List<Long> saveServiceTypes(@RequestBody List<MsgServiceTypeEntity> serviceTypes) {
        return serviceTypeRepository.saveAll(serviceTypes).stream()
                .map(MsgServiceTypeEntity::getId)
                .collect(Collectors.toList());
    }

    @PostMapping("/smsc/save")
    public List<Long> saveSMSC(@RequestBody List<SMSCEntity> smscList) {
        return smscRepository.saveAll(smscList).stream()
                .map(SMSCEntity::getId)
                .collect(Collectors.toList());
    }

    @PostMapping("/users/save")
    public List<String> saveUsers(@RequestBody List<UserAccountEntity> users) {
        List<UserAccountEntity> encodedUsers = users.stream().map(u -> {
            u.setPassword(passwordEncoder.encode(u.getPassword()));
            return u;
        }).toList();
        return userRepository.saveAll(encodedUsers).stream()
                .map(UserAccountEntity::getId)
                .collect(Collectors.toList());
    }

    @PostMapping("/user/services/save")
    public List<Long> saveUserWiseServices(@RequestBody List<UserWiseApiKey> userWiseApiKeys) {
        List<String> serviceTypes = userWiseApiKeys.stream().map(UserWiseApiKey::getServiceType)
                .map(ServiceType::getValue)
                .collect(Collectors.toList());
        List<String> smscList = userWiseApiKeys.stream().map(UserWiseApiKey::getSmsc).collect(Collectors.toList());
        Map<String, MsgServiceTypeEntity> serviceTypeEntityMap = serviceTypeRepository.findAllByNameIn(serviceTypes)
                .stream().collect(Collectors.toMap(MsgServiceTypeEntity::getName, st -> st));
        Map<String, SMSCEntity> smscEntityMap = smscRepository.findAllByNameIn(smscList)
                .stream().collect(Collectors.toMap(SMSCEntity::getName, smsc -> smsc));

        return userWiseApiKeys.stream().map(uak -> {
            UserWiseServicePermissionEntity ust = new UserWiseServicePermissionEntity();
            UserAccountEntity userAccountEntity = userRepository.findByUsername(uak.getUserId())
                    .orElseThrow(() -> new EntityNotFoundException("userId", uak.getUserId() + " not found"));
            ust.setUser(userAccountEntity);
            ust.setServiceType(serviceTypeEntityMap.get(uak.getServiceType().toString()));
            ust.setSmsc(smscEntityMap.get(uak.getSmsc()));
            return userWiseServiceTypeRepository.save(ust).getId();
        }).collect(Collectors.toList());
    }

    @PostMapping("/user/api-key/save")
    public String saveUserApiKeys(@RequestBody Map<String, String> request) {
        UserAccountEntity userAccountEntity = userRepository.findByUsername(request.get("username"))
                .orElseThrow(() -> new EntityNotFoundException("userId", request.get("userName") + " not found"));

        UserWiseAPIKeyEntity userWiseAPIKeyEntity = new UserWiseAPIKeyEntity();
        userWiseAPIKeyEntity.setUser(userAccountEntity);
        userWiseAPIKeyEntity.setApiKey(generateApiKey());
        userWiseAPIKeyEntity.setValidity(ApiKeyValidity.QUARTERLY);
        userWiseAPIKeyEntity.setActiveFlag(true);
        return userWiseAPIKeyRepository.save(userWiseAPIKeyEntity).getApiKey();
    }

    private String generateApiKey() {
        String apiKey;
        do {
            apiKey = UUID.randomUUID().toString().replace("-", "");
        } while (userWiseAPIKeyRepository.existsByApiKey(apiKey));
        return apiKey;
    }

    @PostMapping("/user/webhook/save")
    public List<String> saveUserWebhook(@RequestBody List<Map<String, String>> requestList) {
        List<String> response = new ArrayList<>();
        requestList.forEach(request -> {
            UserAccountEntity userAccountEntity = userRepository.findByUsername(request.get("username"))
                    .orElseThrow(() -> new EntityNotFoundException("userId", request.get("userName") + " not found"));

            UserWiseWebhookRegistryEntity webhookEntity = new UserWiseWebhookRegistryEntity();
            webhookEntity.setUser(userAccountEntity);
            webhookEntity.setWebhookId(generateWebhookId());
            webhookEntity.setWebhookUrl(request.get("url"));
            webhookEntity.setActiveFlag(true);
            webhookEntity.setCrmType(CRMType.fromValue(request.get("crmType")));
            webhookEntity = userWiseWebhookRepository.save(webhookEntity);
            response.add(webhookEntity != null ? webhookEntity.getWebhookId() : null);
        });
        return response;
    }

    @PostMapping("/user/kafka/partition")
    public void mapUserWiseKafkaTopiPartition(@RequestBody List<Map<String, String>> requestList) {
        requestList.forEach(request -> {
            UserAccountEntity userAccountEntity = userRepository.findByUsername(request.get("username"))
                    .orElseThrow(() -> new EntityNotFoundException("userId", request.get("userName") + " not found"));

            UserWiseKafkaPartition userWiseKafkaPartition = new UserWiseKafkaPartition();
            userWiseKafkaPartition.setUser(userAccountEntity);
            userWiseKafkaPartition.setPartitionNum(Integer.parseInt(request.get("partition")));

            tenantToPartitionRepository.save(userWiseKafkaPartition);
        });
    }

    private String generateWebhookId() {
        String apiKey;
        do {
            apiKey = UUID.randomUUID().toString();
        } while (userWiseWebhookRepository.existsByWebhookId(apiKey));
        return apiKey;
    }

    @PostMapping("/sender")
    public String createSender(@RequestBody SenderEntity senderEntity) {
        return senderRepository.save(senderEntity).getSenderId();
    }

    @PostMapping("/template")
    public String createTemplate(@RequestBody TemplateEntity templateEntity,
                                 @RequestHeader("sender-id")String senderId) {
        SenderEntity sender=senderRepository.findBySenderId(senderId)
                .orElseThrow(()-> new EntityNotFoundException("",""));
        templateEntity.setSender(sender);
        return templateRepository.save(templateEntity).getTemplateId();
    }
}
