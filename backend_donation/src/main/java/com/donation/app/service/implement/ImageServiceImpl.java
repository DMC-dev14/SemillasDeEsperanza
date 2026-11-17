package com.donation.app.service.implement;

import java.io.IOException;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import com.donation.app.exception.BusinessException;
import com.donation.app.model.Image;
import com.donation.app.repository.ImageRepository;
import com.donation.app.service.CloudinaryService;
import com.donation.app.service.ImageService;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ImageServiceImpl implements ImageService {

    private final ImageRepository imageRepository;
    private final CloudinaryService cloudinaryService;

    @Override
    @Transactional
    public Image uploadImage(MultipartFile file) throws IOException {
        if (file == null || file.isEmpty()) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "Archivo vacío");
        }
        String contentType = file.getContentType();
        if (contentType == null || !contentType.startsWith("image/")) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "Solo se permiten imágenes");
        }
        long maxSize = 5 * 1024 * 1024;
        if (file.getSize() > maxSize) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "Máximo 5MB");
        }
        String filename = file.getOriginalFilename();
        if (filename == null || !filename.matches(".*\\.(jpg|jpeg|png|webp|gif)$")) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "Formato no soportado");
        }
        Map<?, ?> uploadResult = cloudinaryService.upload(file);
        String imageUrl = (String) uploadResult.get("url");
        String imageId = (String) uploadResult.get("public_id");

        Image image = Image.builder()
                .nombre(file.getOriginalFilename())
                .imageUrl(imageUrl)
                .imageId(imageId)
                .build();

        return imageRepository.save(image);
    }

    @Override
    @Transactional
    public void deleteImage(Image image) throws IOException {
        if (image == null) {
            return;
        }
        if (image.getImageId() != null) {
            cloudinaryService.delete(image.getImageId());
        }
        imageRepository.delete(image);
    }

}