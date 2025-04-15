package com.gk.sms.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.gk.sms.entities.UserEntity;
import com.gk.sms.entities.UserWiseWebhookEntity;
import com.gk.sms.exception.AuthenticationException;
import com.gk.sms.exception.EntityNotFoundException;
import com.gk.sms.exception.InvalidRequestException;
import com.gk.sms.model.MessageRequest;
import com.gk.sms.model.MtAdapterMsgReq;
import com.gk.sms.model.MtAdapterMsgRes;
import com.gk.sms.model.MtAdapterMsgResWrapper;
import com.gk.sms.model.UpdateMsgReq;
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
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static com.gk.sms.utils.enums.Country.IN;
import static com.gk.sms.utils.enums.Country.INTL;
import static com.gk.sms.utils.enums.ServiceType.GLOBAL;
import static com.gk.sms.utils.enums.ServiceType.OTP;
import static com.gk.sms.utils.enums.ServiceType.PROMO;
import static com.gk.sms.utils.enums.ServiceType.TRANS;

@RestController
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

    @PostMapping("/sms/send")
    public ResponseEntity<String> sendSms(@RequestBody @Valid MessageRequest request,
                                          @RequestHeader("tenantId") String tenantId,
                                          @RequestHeader("apiKey") String apiKey) {
        UserEntity userEntity = userRepository.findById(tenantId)
                .orElseThrow(() -> new EntityNotFoundException("tenantId", "tenantId -" + tenantId + " not found "));
        userEntity.getUserApiKeys().stream()
                .filter(aKey -> apiKey.equalsIgnoreCase(aKey.getApiKey()) && aKey.isActiveFlag())
                .findFirst()
                .orElseThrow(() -> new AuthenticationException("apiKey", " apiKey - " + apiKey + " is not valid for TenantId - " + tenantId));
        validateMsgReqAndThrowException(request);
        userEntity.getUserServices().stream()
                .filter(us -> us.getServiceType().getName().equalsIgnoreCase(request.getServiceType().getValue()))
                .findFirst()
                .orElseThrow(() -> new InvalidRequestException("serviceType", " serviceType - " + request.getServiceType() + " is not supported for TenantId " + tenantId));
        request.setMsgId(generateUniqueMsgId());
        request.setTenantId(tenantId);
        request.setSmsSentOn(LocalDateTime.now(ZoneOffset.UTC));
        request.setKafkaMsgType(KafkaMsgType.INSERT_MSG);
        request.setMsgStatus(MsgStatus.CREATED);
        msgProducer.postInsertMsgToKafka(request, tenantId);
        return ResponseEntity.ok(request.getMsgId());
    }

    @PostMapping("/web-engage/sms/send")
    public ResponseEntity<WebEngageResponse> sendSmsForWebEngage(@RequestBody @Valid WebEngageSMSRequest request,
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
            messageRequest.setMsgId(generateUniqueMsgId());
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
            messageRequest.setUpdateMsgReq(null);
            validateMsgReqAndThrowException(messageRequest);
            msgProducer.postInsertMsgToKafka(messageRequest, tenantId);
            response.setStatus("sms_accepted");
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

    @PostMapping("/mt-adapter/sms/send")
    public ResponseEntity<MtAdapterMsgResWrapper> sendSmsForMtAdapter(@RequestBody @Valid MtAdapterMsgReq request,
                                                                      @RequestHeader("tenantId") String tenantId,
                                                                      @RequestHeader("apiKey") String apiKey) {
        UserEntity userEntity = userRepository.findById(tenantId)
                .orElseThrow(() -> new EntityNotFoundException("tenantId", "tenantId -" + tenantId + " not found "));
        userEntity.getUserApiKeys().stream()
                .filter(aKey -> apiKey.equalsIgnoreCase(aKey.getApiKey()) && aKey.isActiveFlag())
                .findFirst()
                .orElseThrow(() -> new AuthenticationException("apiKey", " apiKey - " + apiKey + " is not valid for TenantId - " + tenantId));
        List<String> toList = request.getTo();
        String msgGroupId = generateUniqueMsgGroupId();
        MtAdapterMsgResWrapper response = new MtAdapterMsgResWrapper();
        List<MtAdapterMsgRes> data = new ArrayList<>();
        for (String to : toList) {
            MessageRequest messageRequest = new MessageRequest();
            MessageType type = MessageType.fromValue(request.getType());
            messageRequest.setMessageType(type != null ? type : MessageType.A);
            messageRequest.setTenantId(tenantId);
            messageRequest.setMsgId(generateUniqueMsgId());
            messageRequest.setMsgGroupId(msgGroupId);
            messageRequest.setCountry(IN);
            messageRequest.setServiceType(request.getService().equalsIgnoreCase("T") ? TRANS : (request.getService().equalsIgnoreCase("P") ? PROMO : null));
            messageRequest.setFrom(request.getSender());
            messageRequest.setTo(to);
            messageRequest.setBody(request.getMessage());
            messageRequest.setTemplateId(request.getTemplate_id());
            messageRequest.setEntityId(request.getEntity_id());
            messageRequest.setMetadata(request.getMeta());
            messageRequest.setCustomId(request.getCustom());
            messageRequest.setFlash(request.getFlash() == 1 ? true : false);
            messageRequest.setWebhookId((request.getWebhook_id() != null && !request.getWebhook_id().isBlank()) ? request.getWebhook_id() : null);
            messageRequest.setKafkaMsgType(KafkaMsgType.INSERT_MSG);
            messageRequest.setMsgStatus(MsgStatus.CREATED);
            messageRequest.setCrmMsgType(CRMType.MT_ADAPTER);
            messageRequest.setSmsSentOn(LocalDateTime.now(ZoneOffset.UTC));
            messageRequest.setSmsLength(request.getMessage().length());
            messageRequest.setCredits(getUnits(request.getMessage(), messageRequest.getMessageType()));
            messageRequest.setUpdateMsgReq(null);
            boolean isNotValid = validateMsgReqWithoutThrowingException(messageRequest);
            if (!isNotValid) {
                msgProducer.postInsertMsgToKafka(messageRequest, tenantId);
                MtAdapterMsgRes mtAdapterMsgRes = new MtAdapterMsgRes();
                mtAdapterMsgRes.setId(messageRequest.getMsgId());
                mtAdapterMsgRes.setCharges("0.023");
                mtAdapterMsgRes.setCustomid(request.getCustom());
                mtAdapterMsgRes.setCustomid1("");
                mtAdapterMsgRes.setIso_code("IN");
                mtAdapterMsgRes.setLength(messageRequest.getSmsLength());
                mtAdapterMsgRes.setMobile(to);
                mtAdapterMsgRes.setStatus("AWAITING-DLR");
                mtAdapterMsgRes.setSubmitted_at("2025-04-15 16:26:59");
                mtAdapterMsgRes.setUnits(messageRequest.getCredits());

                data.add(mtAdapterMsgRes);
            }
        }
        int count = data != null ? data.size() : 0;
        response.setMessage(count + " numbers accepted for delivery");
        response.setGroup_id(msgGroupId);
        response.setStatus(200);
        response.setData(data);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/mt-adapter/template/sms/send")
    public ResponseEntity<List<String>> sendSmsAsPerTemplate(@RequestBody @Valid MtAdapterMsgReq request,
                                                             @RequestHeader("tenantId") String tenantId,
                                                             @RequestHeader("apiKey") String apiKey) {
        return null;
    }

    public String generateUniqueMsgId() {
        String uuid;
        do {
            uuid = UUID.randomUUID().toString();
        } while (userMessagesRepository.existsById(uuid));
        return uuid;
    }

    public String generateUniqueMsgGroupId() {
        String uuid;
        do {
            uuid = UUID.randomUUID().toString();
        } while (userMessagesRepository.existsByMsgGroupId(uuid));
        return uuid;
    }

    private static int getUnits(String msgBody, MessageType messageType) {
        if (msgBody == null || msgBody.equalsIgnoreCase(""))
            return 1;
        int unit;
        switch (messageType) {
            case N -> unit = (msgBody.length() <= 160) ? 1 : (int) Math.ceil((double) msgBody.length() / 153);
            case U -> unit = (msgBody.length() <= 70) ? 1 : (int) Math.ceil((double) msgBody.length() / 67);
            case A -> {
                boolean isNonAscii = msgBody.chars().anyMatch(c -> c >= 128);
                if (!isNonAscii) {
                    // ASCII logic
                    unit = (msgBody.length() <= 160) ? 1 : (int) Math.ceil((double) msgBody.length() / 153);
                } else {
                    // Non-ASCII logic
                    unit = (msgBody.length() <= 70) ? 1 : (int) Math.ceil((double) msgBody.length() / 67);
                }
            }
            default -> unit = 0;
        }
        return unit;
    }

    private void validateMsgReqAndThrowException(MessageRequest request) {
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

    private boolean validateMsgReqWithoutThrowingException(MessageRequest request) {
        boolean isNotValid = false;
        String from = request.getFrom();
        Country country = request.getCountry();
        String to = request.getTo();
        ServiceType serviceType = request.getServiceType();
        if (country == IN && serviceType == GLOBAL) {
            isNotValid = true;
        } else if (country == INTL && !(serviceType == GLOBAL)) {
            isNotValid = true;
        }

        if ((serviceType == TRANS || serviceType == OTP) && !from.matches("^[a-zA-Z]{5,6}$")) {
            isNotValid = true;
        } else if (serviceType == PROMO && !from.matches("^\\d{6}$")) {
            isNotValid = true;
        }
        if (to.matches("^[6789]\\d{9}$") || to.matches("^\\d{12}$") || to.matches("^\\+\\d{12}$")) {
            if (to.length() != 10 && country == IN && !to.matches("^(\\+91|91|)[6789]\\d{9}$")) {
                isNotValid = true;
            }
        } else {
            isNotValid = true;
        }
        if (request.getBody().equalsIgnoreCase("null"))
            isNotValid = true;

        if (request.getTemplateId() == null || request.getTemplateId().length() != 19
                || request.getEntityId() == null || request.getEntityId().length() != 19)
            isNotValid = true;
        return isNotValid;
    }

    @GetMapping("/sms/status/update")
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
        UpdateMsgReq updateMsgReq = new UpdateMsgReq();
        updateMsgReq.setStatusJson(statusJson);
        updateMsgReq.setDlrUrl(fullUrl);
        updateMsgReq.setPId(pId);
        updateMsgReq.setSmscId(smscId);
        updateMsgReq.setTm(tm);
        updateMsgReq.setType(type);
        messageRequest.setUpdateMsgReq(updateMsgReq);
        msgProducer.postInsertMsgToKafka(messageRequest, tenantId);
    }

    @PostMapping("/sms/webhook")
    public ResponseEntity<Object> dummyWebhook(@RequestBody Object request) {
        try {
            log.info("/sms/webhook request {}", objectMapper.writeValueAsString(request));
        } catch (Exception e) {
            e.printStackTrace();
        }
        return ResponseEntity.ok(request);
    }

}
