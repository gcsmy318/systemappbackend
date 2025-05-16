package com.example.systemapp.repository;

import com.example.systemapp.entity.News;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/*
public interface NewsRepository extends JpaRepository<News, Long> {
}
*/
@Repository
public interface NewsRepository extends JpaRepository<News, Long> {
}
