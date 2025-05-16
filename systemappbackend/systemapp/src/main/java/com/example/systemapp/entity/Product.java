package com.example.systemapp.entity;

import jakarta.persistence.*;
import lombok.Data;

@Entity
@Data
@Table(name = "products")
public class Product {
    @Id
    @Column(length = 4)
    private String id;

    private String name;

    private String code;

    @Column(length = 1000)
    private String info;

    private double price;

    private String image; // image URL or path
}
