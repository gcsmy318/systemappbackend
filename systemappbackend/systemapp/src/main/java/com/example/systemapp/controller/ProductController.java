package com.example.systemapp.controller;

import com.example.systemapp.entity.Product;
import com.example.systemapp.repository.ProductRepository;
import com.jcraft.jsch.ChannelSftp;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.Session;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@RestController
@RequestMapping("/api/products")
@CrossOrigin(origins = "*")
@RequiredArgsConstructor
public class ProductController {

    private final ProductRepository productRepo;

    @Value("${sftp.host}")
    private String sftpHost;

    @Value("${sftp.port}")
    private int sftpPort;

    @Value("${sftp.username}")
    private String sftpUsername;

    @Value("${sftp.password}")
    private String sftpPassword;

    @Value("${sftp.remoteDir2}")
    private String sftpRemoteDir;

    @Value("${sftp.imageBaseUrl2}")
    private String imageBaseUrl;

    // 📦 Get all products
    @GetMapping
    public List<Product> getAll() {
        return productRepo.findAll();
    }

    @GetMapping("/category/{id}")
    public List<Product> getAllwithCode(@PathVariable String id) {
        return productRepo.findByCodeIgnoreCase(id);
    }

    // 🔍 Get product by ID
    @GetMapping("/{id}")
    public Product getById(@PathVariable String id) {
        return productRepo.findById(id).orElse(null);
    }

    // ➕ Create new product (with image upload to SFTP)
    @PostMapping(consumes = {"multipart/form-data"})
    public ResponseEntity<?> createProduct(
            @RequestParam("id") String id,
            @RequestParam("name") String name,
            @RequestParam("code") String code,
            @RequestParam("info") String info,
            @RequestParam("price") double price,
            @RequestParam("image") MultipartFile imageFile) {

        try {
            String originalFilename = imageFile.getOriginalFilename();
            String extension = originalFilename.substring(originalFilename.lastIndexOf("."));
            String uniqueFilename = UUID.randomUUID() + extension;

            uploadFileToSFTP(imageFile.getInputStream(), uniqueFilename);

            String imageUrl = imageBaseUrl + "/" + uniqueFilename;

            Product product = new Product();
            product.setId(id);
            product.setName(name);
            product.setCode(code);
            product.setInfo(info);
            product.setPrice(price);
            product.setImage(imageUrl);

            return ResponseEntity.ok(productRepo.save(product));

        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(500).body("เกิดข้อผิดพลาด: " + e.getMessage());
        }
    }

    // ✏️ Update product (with optional new image)
    @PutMapping(value = "/{id}", consumes = {"multipart/form-data"})
    public ResponseEntity<?> updateProduct(
            @PathVariable String id,
            @RequestParam("name") String name,
            @RequestParam("code") String code,
            @RequestParam("info") String info,
            @RequestParam("price") double price,
            @RequestParam(value = "image", required = false) MultipartFile imageFile) {

        Optional<Product> optionalProduct = productRepo.findById(id);
        if (optionalProduct.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        try {
            Product product = optionalProduct.get();
            product.setName(name);
            product.setCode(code);
            product.setInfo(info);
            product.setPrice(price);

            if (imageFile != null && !imageFile.isEmpty()) {
                String originalFilename = imageFile.getOriginalFilename();
                String extension = originalFilename.substring(originalFilename.lastIndexOf("."));
                String uniqueFilename = UUID.randomUUID() + extension;

                uploadFileToSFTP(imageFile.getInputStream(), uniqueFilename);

                String imageUrl = imageBaseUrl + "/" + uniqueFilename;
                product.setImage(imageUrl);
            }

            return ResponseEntity.ok(productRepo.save(product));
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(500).body("เกิดข้อผิดพลาด: " + e.getMessage());
        }
    }

    // ❌ Delete product
    @DeleteMapping("/{id}")
    public void delete(@PathVariable String id) {
        productRepo.deleteById(id);
    }

    // 📤 Upload image to SFTP server
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
