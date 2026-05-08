package com.codegroup.portfolio.common.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Id;
import jakarta.persistence.MappedSuperclass;
import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

@Getter
@Setter
@MappedSuperclass
public abstract class AbstractBaseEntity {
    @Id
    @Column(name = "id", unique = true, nullable = false)
    protected UUID id;
}
