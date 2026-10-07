package com.example.crudapi.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

/**
 * Bat @CreatedDate / @LastModifiedDate cho entity.
 *
 * Co tinh de o class rieng thay vi dat tren CrudApiApplication: @WebMvcTest van nap
 * class @SpringBootApplication nhung tat JPA auto-config, khi do bean jpaAuditingHandler
 * se khong tim thay JPA metamodel va context test web bi fail.
 */
@Configuration
@EnableJpaAuditing
public class JpaAuditingConfig {
}
