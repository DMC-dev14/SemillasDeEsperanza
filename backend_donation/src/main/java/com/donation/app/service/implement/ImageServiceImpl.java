package com.donation.app.service.implement;

import java.io.IOException;
import java.util.Map;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

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