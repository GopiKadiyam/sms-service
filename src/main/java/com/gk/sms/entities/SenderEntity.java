package com.gk.sms.entities;

import com.gk.sms.utils.enums.Country;
import com.gk.sms.utils.enums.ServiceType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.GenericGenerator;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "sender")
public class SenderEntity {

    @Id
    @GeneratedValue(generator = "UUID")
    @GenericGenerator(name = "UUID", strategy = "org.hibernate.id.UUIDGenerator")
    @Column(name = "id", updatable = false, nullable = false, length = 36) // Store as string
    private String id;
    @NotBlank
    @Size(max = 50)
    @Column(name = "sender_id",unique = true)
    private String senderId;
    @NotBlank
    @Size(max = 500)
    private String description;
    @Enumerated(EnumType.STRING)
    private Country country;
    @Enumerated(EnumType.STRING)
    @Column(name = "service_type")
    private ServiceType serviceType;
    @NotBlank
    @Size(max = 50)
    @Column(name = "entity_id")
    private String entityId;
    @Column(name = "is_open")
    private String openFlag;
    @NotNull
    @Column(name = "status_flag")
    private boolean statusFlag;

}
