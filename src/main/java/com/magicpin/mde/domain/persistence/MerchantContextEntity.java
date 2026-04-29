package com.magicpin.mde.domain.persistence;

import com.fasterxml.jackson.databind.JsonNode;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Getter
@Setter
@Entity
@Table(name = "merchant_context")
public class MerchantContextEntity {
  @Id
  @Column(name = "merchant_id", nullable = false, updatable = false, length = 64)
  private String merchantId;

  @Column(name = "version", nullable = false)
  private long version;

  @Column(name = "payload", columnDefinition = "TEXT", nullable = false)
  private String payload;

  @Column(name = "updated_at", nullable = false)
  private Instant updatedAt;
}

