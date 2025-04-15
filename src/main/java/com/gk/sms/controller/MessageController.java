package com.gk.sms.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.gk.sms.entities.UserEntity;
import com.gk.sms.entities.UserWiseWebhookEntity;
import com.gk.sms.exception.AuthenticationException;
import com.gk.sms.exception.EntityNotFoundException;
import com.gk.sms.exception.InvalidRequestException;
import com.gk.sms.exception.WebExchangeException;
import com.gk.sms.model.MessageRequest;
import com.gk.sms.model.WebEngageIndiaDLT;
import com.gk.sms.model.WebEngageMetadata;
import com.gk.sms.model.WebEngageResponse;
import com.gk.sms.model.WebEngageSMSData;
import com.gk.sms.model.WebEngageSMSRequest;
import com.gk.sms.producer.MsgRequestsProducer;
import com.gk.sms.repository.UserMessagesRepository;
import com.gk.sms.repository.UserRepository;
import com.gk.sms.utils.enums.CRMType;
import com.gk.sms.utils.enums.Country;
import com.gk.sms.utils.enums.KafkaMsgType;
import com.gk.sms.utils.enums.MessageType;
import com.gk.sms.utils.enums.MsgStatus;
import com.gk.sms.utils.enums.ServiceType;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.UUID;

import static com.gk.sms.utils.enums.Country.IN;
import static com.gk.sms.utils.enums.Country.INTL;
import static com.gk.sms.utils.enums.ServiceType.GLOBAL;
import static com.gk.sms.utils.enums.ServiceType.OTP;
import static com.gk.sms.utils.enums.ServiceType.PROMO;
import static com.gk.sms.utils.enums.ServiceType.TRANS;

@RestController
@RequestMapping("/sms")
@Slf4j
public class MessageController {

    @Autowired
    private UserRepository userRepository;
    @Autowired
    private UserMessagesRepository userMessagesRepository;
    @Autowired
    private MsgRequestsProducer msgProducer;
    @Autowired
    private ObjectMapper objectMapper;

    @PostMapping("/send")
    public ResponseEntity<String> sendInsertMsgToKafka(@RequestBody @Valid MessageRequest request,
                                                       @RequestHeader("tenantId") String tenantId,
                                                       @RequestHeader("apiKey") String apiKey) {
        UserEntity userEntity = userRepository.findById(tenantId)
                .orElseThrow(() -> new EntityNotFoundException("tenantId", "tenantId -" + tenantId + " not found "));
        userEntity.getUserApiKeys().stream()
                .filter(aKey -> apiKey.equalsIgnoreCase(aKey.getApiKey()) && aKey.isActiveFlag())
                .findFirst()
                .orElseThrow(() -> new AuthenticationException("apiKey", " apiKey - " + apiKey + " is not valid for TenantId - " + tenantId));
        validateCampaignRequest(request);
        userEntity.getUserServices().stream()
                .filter(us -> us.getServiceType().getName().equalsIgnoreCase(request.getServiceType().getValue()))
                .findFirst()
                .orElseThrow(() -> new InvalidRequestException("serviceType", " serviceType - " + request.getServiceType() + " is not supported for TenantId " + tenantId));
        request.setMsgId(generateUniqueId());
        request.setTenantId(tenantId);
        request.setSmsSentOn(LocalDateTime.now(ZoneOffset.UTC));
        request.setKafkaMsgType(KafkaMsgType.INSERT_MSG);
        request.setMsgStatus(MsgStatus.CREATED);
        msgProducer.postInsertMsgToKafka(request, tenantId);
        return ResponseEntity.ok(request.getMsgId());
    }

    @PostMapping("/send/web-engage")
    public ResponseEntity<WebEngageResponse> sendSMSForWebEngage(@RequestBody @Valid WebEngageSMSRequest request,
                                                                 @RequestHeader("tenantId") String tenantId,
                                                                 @RequestHeader("apiKey") String apiKey) {
        WebEngageResponse response = new WebEngageResponse();
        try {
            UserEntity userEntity = userRepository.findById(tenantId)
                    .orElseThrow(() -> new EntityNotFoundException("tenantId", "tenantId -" + tenantId + " not found "));
            userEntity.getUserApiKeys().stream()
                    .filter(aKey -> apiKey.equalsIgnoreCase(aKey.getApiKey()) && aKey.isActiveFlag())
                    .findFirst()
                    .orElseThrow(() -> new AuthenticationException("apiKey", " apiKey - " + apiKey + " is not valid for TenantId - " + tenantId));
            MessageRequest messageRequest = new MessageRequest();
            WebEngageMetadata metadata = request.getMetadata();
            WebEngageSMSData smsData = request.getSmsData();
            WebEngageIndiaDLT indiaDLT = metadata.getIndiaDLT();
            messageRequest.setMessageType(MessageType.A);
            messageRequest.setTenantId(tenantId);
            messageRequest.setMsgId(generateUniqueId());
            messageRequest.setCountry(IN);
            messageRequest.setServiceType(metadata.getCampaignType().equalsIgnoreCase("TRANSACTIONAL") ? TRANS : (metadata.getCampaignType().equalsIgnoreCase("PROMOTIONAL") ? PROMO : null));
            messageRequest.setFrom(smsData.getFromNumber1() != null ? smsData.getFromNumber1() : smsData.getFromNumber2());
            messageRequest.setTo(smsData.getToNumber());
            messageRequest.setBody(smsData.getBody());
            messageRequest.setTemplateId(indiaDLT.getContentTemplateId());
            messageRequest.setEntityId(indiaDLT.getPrincipalEntityId());
            messageRequest.setMetadata(metadata.getCustom());
            messageRequest.setFlash(false);
            messageRequest.setCrmMsgId(metadata.getMessageId());
            messageRequest.setWebEngageVersion(request.getVersion());
            UserWiseWebhookEntity webhookEntity = userEntity.getUserWebhooks().stream().findFirst().orElse(null);
            messageRequest.setWebhookId(webhookEntity != null ? webhookEntity.getWebhookId() : null);
            messageRequest.setKafkaMsgType(KafkaMsgType.INSERT_MSG);
            messageRequest.setMsgStatus(MsgStatus.CREATED);
            messageRequest.setCrmMsgType(CRMType.WEB_ENGAGE);
            messageRequest.setSmsSentOn(LocalDateTime.now(ZoneOffset.UTC));
            validateCampaignRequest(messageRequest);
            msgProducer.postInsertMsgToKafka(messageRequest, tenantId);
            response.setStatus("sms_accepted");
            //return ResponseEntity.ok(WebEngageResponse.builder().status("sms_accepted").build());
        } catch (Exception e) {
            response.setVersion(request.getVersion());
            response.setMessageId(request.getMetadata().getMessageId());
            response.setToNumber(request.getSmsData().getToNumber());
            response.setStatus("sms_failed");
            response.setStatusCode(9988);
            response.setMessage("Unknown Reason");
            log.info(" error for webMsgId {} is {}", request.getMetadata().getMessageId(), e.getMessage());
        }
//        catch (EntityNotFoundException e) {

//            throw new WebExchangeException(request.getVersion(), request.getMetadata().getMessageId(),
//                    request.getSmsData().getToNumber(), "sms_failed", 2007, e.getMessage(), HttpStatus.NOT_FOUND);
//        } catch (AuthenticationException e) {
//            throw new WebExchangeException(request.getVersion(), request.getMetadata().getMessageId(),
//                    request.getSmsData().getToNumber(), "sms_failed", 2007, e.getMessage(), HttpStatus.UNAUTHORIZED);
//        } catch (InvalidRequestException e) {
//            throw new WebExchangeException(request.getVersion(), request.getMetadata().getMessageId(),
//                    request.getSmsData().getToNumber(), "sms_failed", 2007, e.getMessage(), HttpStatus.BAD_REQUEST);
//        } catch (Exception e) {
//            throw new WebExchangeException(request.getVersion(), request.getMetadata().getMessageId(),
//                    request.getSmsData().getToNumber(), "sms_failed", 2007, e.toString(), HttpStatus.INTERNAL_SERVER_ERROR);
//        }

        return ResponseEntity.ok(response);
    }

    public String generateUniqueId() {
        String uuid;
        do {
            uuid = UUID.randomUUID().toString();
        } while (userMessagesRepository.existsById(uuid));
        return uuid;
    }

    private void validateCampaignRequest(MessageRequest request) {
        /*
        below are mandatory field : from,country,to,serviceType,templateId
        from: senderId   : if 'serviceType' is TRANS or OTP  then 'from' value should be alpha and length is 5-6 .
                            if serviceType is PROMO then 'from' value should numeric and length is 6 only.
        country: for now it should be IN or INTL for now . future might get add other countries. based on country we should validate below "to" field country code
                  IN -> allowed serviceType are TRANS,PROMO,OTP
                  INTL -> allowed  serviceType are GLOBAL
        to : mobile number length should be in either -> 10 validation is  only numeric ( should start with either of 6,7,8,9). ex :9182353052
                                                      -> 12 validation is only numbers (first 2 are country code then next 10 are mobile number so should start with 6,7,8,9) ex: 919182353052
                                                      -> 13 validation is combination of + and number (first one is + , then next 2 are country code, next 10 are mobile number so should start with 6,7,8,9) ex: +919182353052

        body : should not be null and "null"
        */
        String from = request.getFrom();
        Country country = request.getCountry();
        String to = request.getTo();
        ServiceType serviceType = request.getServiceType();
        if (country == IN && serviceType == GLOBAL) {
            throw new InvalidRequestException("serviceType", "invalid 'serviceType' value");
        } else if (country == INTL && !(serviceType == GLOBAL)) {
            throw new InvalidRequestException("serviceType", "invalid 'serviceType' value");
        }

        if ((serviceType == TRANS || serviceType == OTP) && !from.matches("^[a-zA-Z]{5,6}$")) {
            throw new InvalidRequestException("from", "invalid 'from' value");
        } else if (serviceType == PROMO && !from.matches("^\\d{6}$")) {
            throw new InvalidRequestException("from", "invalid 'from' value");
        }
        if (to.matches("^[6789]\\d{9}$") || to.matches("^\\d{12}$") || to.matches("^\\+\\d{12}$")) {
            if (to.length() != 10 && country == IN && !to.matches("^(\\+91|91|)[6789]\\d{9}$")) {
                throw new InvalidRequestException("to", "Invalid 'to' value ");
            }
        } else {
            throw new InvalidRequestException("to", "Invalid 'to' value");
        }
        if (request.getBody().equalsIgnoreCase("null"))
            throw new InvalidRequestException("body", "Invalid 'body' value");

        if (request.getTemplateId() == null || request.getTemplateId().length() != 19
                || request.getEntityId() == null || request.getEntityId().length() != 19)
            throw new InvalidRequestException("templateId/entityId", "templateId/entityId should be 19 length");
    }

    @GetMapping("/status/update")
    public void updateCampaignMessageStatus(@RequestParam(name = "status") String statusJson, @RequestParam(name = "type") String type,
                                            @RequestParam(name = "pid") String pId, @RequestParam(name = "smscid") String smscId,
                                            @RequestParam(name = "tm") String tm, @RequestParam(name = "mid") String msgId,
                                            @RequestParam(name = "tenid") String tenantId, HttpServletRequest request) {
        //http://localhost:8094/message/status?status=%A&type=%d&pid=%F&smscid=%i&tm=%T&mid=Message_P_KEY&tenid=97292
        String fullUrl = request.getRequestURL().toString() + "?" + request.getQueryString();     // Full URL with scheme, host, port, and path
        //messageSenderService.updateCampaignMessageStatus(statusJson,type,pId,smscId,tm,msgId,fullUrl);
        MessageRequest messageRequest = new MessageRequest();
        messageRequest.setMsgId(msgId);
        messageRequest.setKafkaMsgType(KafkaMsgType.UPDATE_MSG);
        messageRequest.setMsgStatus(MsgStatus.DLR_CB_SUCCESS);

        messageRequest.setStatusJson(statusJson);
        messageRequest.setDlrUrl(fullUrl);
        messageRequest.setPId(pId);
        messageRequest.setSmscId(smscId);
        messageRequest.setTm(tm);
        messageRequest.setType(type);

        msgProducer.postInsertMsgToKafka(messageRequest, tenantId);
    }

    @PostMapping("/webhook")
    public ResponseEntity<Object> dummyWebhook(@RequestBody Object request) {
        try {
            log.info("/sms/webhook request {}", objectMapper.writeValueAsString(request));
        } catch (Exception e) {
            e.printStackTrace();
        }
        return ResponseEntity.ok(request);
    }

}
