package com.donation.app.service;

import com.donation.app.model.Image;

import java.io.IOException;

import org.springframework.web.multipart.MultipartFile;

import org.springframework.web.multipart.MultipartFile;

public interface ImageService {
    
    Image uploadImage(MultipartFile file) throws IOException;

    void deleteImage(Image image) throws IOException;
}
