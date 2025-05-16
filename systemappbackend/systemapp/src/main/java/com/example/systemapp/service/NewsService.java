package com.example.systemapp.service;

import com.example.systemapp.entity.News;
import com.example.systemapp.repository.NewsRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class NewsService {

    private final NewsRepository repository;

    public NewsService(NewsRepository repository) {
        this.repository = repository;
    }

    public List<News> findAll() {
        return repository.findAll();
    }

    public Optional<News> findById(Long id) {
        return repository.findById(id);
    }

    public News save(News news) {
        return repository.save(news);
    }

    public void delete(Long id) {
        repository.deleteById(id);
    }
}
