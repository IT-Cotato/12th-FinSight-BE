package com.finsight.finsight.domain.example.persistence.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@Table(name = "example_entity")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ExampleEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "example_id")
    private Long id;

    @Column(name = "name", nullable = false)
    private String name;

    @Builder
    public ExampleEntity(String name) {
        this.name = name;
    }
}
