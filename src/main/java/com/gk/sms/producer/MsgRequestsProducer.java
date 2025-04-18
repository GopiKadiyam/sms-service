package com.gk.sms.producer;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.gk.sms.entities.UserWiseKafkaPartition;
import com.gk.sms.model.MessageRequest;
import com.gk.sms.repository.TenantToPartitionRepository;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class MsgRequestsProducer {

    @Autowired
    private KafkaTemplate<String, MessageRequest> kafkaTemplate;

    @Value("${sms.requests.topic}")
    private String smsRequestsTopic;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private TenantToPartitionRepository tenantToPartitionRepository;

    public void postInsertMsgToKafka(MessageRequest messageRequest, String tenantId) {
        UserWiseKafkaPartition userWiseKafkaPartition = tenantToPartitionRepository.findByUser_Id(tenantId)
                .orElseThrow(() -> new RuntimeException("Partition not found for tenant: " + tenantId));

        ProducerRecord<String, MessageRequest> record = new ProducerRecord<>(
                smsRequestsTopic,
                userWiseKafkaPartition.getPartitionNum(), // partition
                tenantId,                             // key
                messageRequest                        // value
        );

        kafkaTemplate.send(record);
        log.info(" msgId {} with msgStatus {} for tenantId {} pushed into kafka", messageRequest.getMsgId(), messageRequest.getMsgStatus(), tenantId);
//        try {
//            log.info("message : {}", objectMapper.writeValueAsString(messageRequest));
//        } catch (Exception e) {
//            e.printStackTrace();
//        }
    }

}
