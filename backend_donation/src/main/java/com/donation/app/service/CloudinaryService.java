package com.donation.app.service;

import java.io.File;
import java.io.IOException;
import java.util.Map;

import org.springframework.web.multipart.MultipartFile;

public interface CloudinaryService {

    Map<?, ?> upload(MultipartFile multipartFile) throws IOException;

    Map<?, ?> upload(File file) throws IOException;

    Map<?, ?> delete(String id) throws IOException;
    
}
