package com.example.systemapp.repository;

import com.example.systemapp.entity.Product;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;



public interface ProductRepository extends JpaRepository<Product, String> {
    List<Product> findByCodeIgnoreCase(String code);
}