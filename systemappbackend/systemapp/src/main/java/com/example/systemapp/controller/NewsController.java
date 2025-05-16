package com.example.systemapp.controller;

import com.example.systemapp.entity.News;
import com.example.systemapp.service.NewsService;
import com.jcraft.jsch.ChannelSftp;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.Session;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@RestController
@RequestMapping("/api/news")
@CrossOrigin(origins = "*")
public class NewsController {

    private final NewsService service;

    public NewsController(NewsService service) {
        this.service = service;
    }

    @Value("${sftp.host}")
    private String sftpHost;

    @Value("${sftp.port}")
    private int sftpPort;

    @Value("${sftp.username}")
    private String sftpUsername;

    @Value("${sftp.password}")
    private String sftpPassword;

    @Value("${sftp.remoteDir}")
    private String sftpRemoteDir;

    @Value("${sftp.imageBaseUrl}")
    private String imageBaseUrl;

    @GetMapping
    public List<News> getAll() {
        return service.findAll();
    }

    @GetMapping("/{id}")
    public News getOne(@PathVariable Long id) {
        return service.findById(id).orElse(null);
    }

    @PostMapping(consumes = {"multipart/form-data"})
    public ResponseEntity<?> createNews(
            @RequestParam("title") String title,
            @RequestParam("content") String content,
            @RequestParam("image") MultipartFile imageFile) {

        try {
            // สร้างชื่อไฟล์ไม่ซ้ำ
            String originalFilename = imageFile.getOriginalFilename();
            String extension = originalFilename.substring(originalFilename.lastIndexOf("."));
            String uniqueFilename = UUID.randomUUID() + extension;

            // อัปโหลดรูปไปยัง CPanel ผ่าน SFTP
            uploadFileToSFTP(imageFile.getInputStream(), uniqueFilename);

            // URL ที่เก็บใน DB
            String imageUrl = imageBaseUrl + "/" + uniqueFilename;

            // สร้างข่าวใหม่
            News news = new News();
            news.setTitle(title);
            news.setContent(content);
            news.setImageUrl(imageUrl);

            // บันทึกลงฐานข้อมูล
            News savedNews = service.save(news);

            return ResponseEntity.ok(savedNews);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(500).body("เกิดข้อผิดพลาด: " + e.getMessage());
        }
    }

    @PutMapping("/{id}")
    public News update(@PathVariable Long id, @RequestBody News news) {
        news.setId(id);
        return service.save(news);
    }

    @PutMapping(value = "/{id}", consumes = {"multipart/form-data"})
    public ResponseEntity<?> updateNews(
            @PathVariable Long id,
            @RequestParam("title") String title,
            @RequestParam("content") String content,
            @RequestParam(value = "image", required = false) MultipartFile imageFile) {
        try {
            // หา news ตัวเดิม
            Optional<News> optionalNews = service.findById(id);
            if (optionalNews.isEmpty()) {
                return ResponseEntity.notFound().build();
            }

            News news = optionalNews.get();
            news.setTitle(title);
            news.setContent(content);

            if (imageFile != null && !imageFile.isEmpty()) {
                // อัปโหลดภาพใหม่
                String originalFilename = imageFile.getOriginalFilename();
                String extension = originalFilename.substring(originalFilename.lastIndexOf("."));
                String uniqueFilename = UUID.randomUUID() + extension;

                uploadFileToSFTP(imageFile.getInputStream(), uniqueFilename);

                // อัปเดต URL รูปภาพ
                String imageUrl = imageBaseUrl + "/" + uniqueFilename;
                news.setImageUrl(imageUrl);
            }

            News updatedNews = service.save(news);
            return ResponseEntity.ok(updatedNews);

        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(500).body("เกิดข้อผิดพลาด: " + e.getMessage());
        }
    }


    @DeleteMapping("/{id}")
    public void delete(@PathVariable Long id) {
        service.delete(id);
    }

    // ฟังก์ชัน upload ไฟล์ไป SFTP
    private void uploadFileToSFTP(InputStream inputStream, String remoteFilename) throws Exception {
        JSch jsch = new JSch();
        Session session = jsch.getSession(sftpUsername, sftpHost, sftpPort);
        session.setPassword(sftpPassword);

        java.util.Properties config = new java.util.Properties();
        config.put("StrictHostKeyChecking", "no");
        session.setConfig(config);

        session.connect();
        ChannelSftp channelSftp = (ChannelSftp) session.openChannel("sftp");
        channelSftp.connect();

        channelSftp.cd(sftpRemoteDir);
        channelSftp.put(inputStream, remoteFilename);

        channelSftp.disconnect();
        session.disconnect();
    }
}
