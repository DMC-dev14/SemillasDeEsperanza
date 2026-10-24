package com.donation.app.service.implement;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Map;
import java.util.Objects;
import java.util.HashMap;
import java.nio.file.Files;
import com.cloudinary.utils.ObjectUtils;

import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.cloudinary.Cloudinary;
import com.donation.app.service.CloudinaryService;

@Service
public class CloudinaryServiceImpl implements CloudinaryService {

    private Cloudinary cloudinary;

    public CloudinaryServiceImpl() {
        Map<String, String> valuesMap = new HashMap<>();
        valuesMap.put("cloud_name", "ydjzbqxu");
        valuesMap.put("api_key", "558669994973357");
        valuesMap.put("api_secret", "HITjJdLJDf2NHtBaywhg91uNRJ4");
        cloudinary = new Cloudinary(valuesMap);
    }

    @Override
    public Map<?, ?> upload(MultipartFile multipartFile) throws IOException {
        File file = convert(multipartFile);
        Map<String, String> result = cloudinary.uploader().upload(file, ObjectUtils.emptyMap());
        Files.deleteIfExists(file.toPath());
        String url = (String) result.get("secure_url");
        String publicId = (String) result.get("public_id");
        Map<String, String> response = new HashMap<>();
        response.put("url", url);
        response.put("public_id", publicId);
        return response;
    }

    @Override
    public Map<?, ?> upload(File file) throws IOException {
        Map<String, String> result = cloudinary.uploader().upload(file, ObjectUtils.emptyMap());
        Files.deleteIfExists(file.toPath());
        String url = (String) result.get("secure_url");
        String publicId = (String) result.get("public_id");
        Map<String, String> response = new HashMap<>();
        response.put("url", url);
        response.put("public_id", publicId);
        return response;
    }

    @Override
    public Map<?, ?> delete(String id) throws IOException {
        Map<?, ?> result = cloudinary.uploader().destroy(id, ObjectUtils.emptyMap());
        return result;
    }

    private File convert(MultipartFile multipartFile) throws IOException {
        File file = new File(Objects.requireNonNull(multipartFile.getOriginalFilename()));
        try (FileOutputStream fo = new FileOutputStream(file)) {
            fo.write(multipartFile.getBytes());
        }
        return file;
    }

}
