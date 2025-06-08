package com.BackEnd.Master.GYM.controller;

import com.BackEnd.Master.GYM.dto.PhotoDto;
import com.BackEnd.Master.GYM.entity.Album;
import com.BackEnd.Master.GYM.entity.Photo;
import com.BackEnd.Master.GYM.repository.AlbumRepo;
import com.BackEnd.Master.GYM.Mapper.PhotoMapper;
import com.BackEnd.Master.GYM.services.PhotoService;
import lombok.RequiredArgsConstructor;

import org.springframework.core.io.FileSystemResource;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import com.BackEnd.Master.GYM.Exceptions.ResourceNotFoundException;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDate;
import java.util.List;
import java.nio.file.Path;

@RequiredArgsConstructor
@RestController
@RequestMapping("/photos")
@CrossOrigin("*")
public class PhotoController {

    private final PhotoService photoService;
    private final PhotoMapper photoMapper;
    private final AlbumRepo albumRepo;

    @GetMapping("/{id}")
    public ResponseEntity<PhotoDto> findById(@PathVariable Long id) {
        Photo entity = photoService.findById(id);
        PhotoDto dto = photoMapper.map(entity);
        return ResponseEntity.ok(dto);
    }

    @GetMapping
    public ResponseEntity<List<PhotoDto>> findAll() {
        List<Photo> entities = photoService.findAll();
        List<PhotoDto> dtos = photoMapper.map(entities);
        return ResponseEntity.ok(dtos);
    }

    @GetMapping("/album/{albumId}")
    public ResponseEntity<List<PhotoDto>> findByAlbumId(@PathVariable Long albumId) {
        List<Photo> entities = photoService.findByAlbumId(albumId);
        List<PhotoDto> dtos = photoMapper.map(entities);
        return ResponseEntity.ok(dtos);
    }

    @GetMapping("/images/{imageName}")
    public ResponseEntity<?> getImage(@PathVariable String imageName) {
        String imagePath = "C:/Users/USER/Desktop/projet PFE/FrontEnd-MasterGYM-main/FrontEnd-MasterGYM-main/src/assets/Gallery/" + imageName;
        
        File imgFile = new File(imagePath);

        if (!imgFile.exists()) {
            return ResponseEntity.notFound().build();
        }

        try {
            return ResponseEntity.ok()
                    .contentType(MediaType.IMAGE_JPEG)
                    .body(new FileSystemResource(imgFile));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @PostMapping(consumes = { "multipart/form-data" })
public ResponseEntity<PhotoDto> insert(
        @RequestParam("name") String name,
        @RequestParam("description") String description,
        @RequestParam("albumId") Long albumId,
        @RequestParam("photoImage") MultipartFile photoImage) throws IOException {

    if (photoImage.isEmpty()) {
        throw new RuntimeException("Photo image is required");
    }

    Album album = albumRepo.findById(albumId)
            .orElseThrow(() -> new ResourceNotFoundException("Album not found with ID: " + albumId));

    Photo photo = new Photo();
    photo.setName(name);
    photo.setDescription(description);
    photo.setUploadDate(LocalDate.now());
    photo.setAlbum(album);

    String imageName = StringUtils.cleanPath(photoImage.getOriginalFilename());
    Path uploadPath = Paths.get("C:/Users/USER/Desktop/projet PFE/FrontEnd-MasterGYM-main/FrontEnd-MasterGYM-main/src/assets/Gallery/");
    Files.createDirectories(uploadPath);

    Path imagePath = uploadPath.resolve(imageName);
    Files.copy(photoImage.getInputStream(), imagePath, StandardCopyOption.REPLACE_EXISTING);

    photo.setImageName(imageName);

    Photo savedPhoto = photoService.insert(photo);
    PhotoDto responseDto = photoMapper.map(savedPhoto);

    return ResponseEntity.ok(responseDto);
}



@PutMapping(consumes = { "multipart/form-data" })
public ResponseEntity<PhotoDto> update(
        @RequestParam("id") Long id,
        @RequestParam("name") String name,
        @RequestParam("description") String description,
        @RequestParam("albumId") Long albumId,
        @RequestParam(value = "photoImage", required = false) MultipartFile photoImage) throws IOException {

    Photo currentPhoto = photoService.findById(id);
    if (currentPhoto == null) {
        throw new ResourceNotFoundException("Photo not found with ID: " + id);
    }

    Album album = albumRepo.findById(albumId)
            .orElseThrow(() -> new ResourceNotFoundException("Album not found with ID: " + albumId));

    String oldImageName = currentPhoto.getImageName();

    currentPhoto.setName(name);
    currentPhoto.setDescription(description);
    currentPhoto.setAlbum(album);

    if (photoImage != null && !photoImage.isEmpty()) {
        String newImageName = StringUtils.cleanPath(photoImage.getOriginalFilename());
        Path uploadPath = Paths.get("C:/Users/USER/Desktop/projet PFE/FrontEnd-MasterGYM-main/FrontEnd-MasterGYM-main/src/assets/Gallery/");
        Files.createDirectories(uploadPath);

        Path newImagePath = uploadPath.resolve(newImageName);
        Files.copy(photoImage.getInputStream(), newImagePath, StandardCopyOption.REPLACE_EXISTING);

        currentPhoto.setImageName(newImageName);

        if (oldImageName != null && !oldImageName.equals(newImageName)) {
            Path oldImagePath = uploadPath.resolve(oldImageName);
            Files.deleteIfExists(oldImagePath);
        }
    }

    Photo updatedPhoto = photoService.update(currentPhoto);
    PhotoDto responseDto = photoMapper.map(updatedPhoto);

    return ResponseEntity.ok(responseDto);
}


@DeleteMapping("/{id}")
public ResponseEntity<String> deleteById(@PathVariable Long id) {
    Photo photo = photoService.findById(id);
    if (photo == null) {
        throw new ResourceNotFoundException("Photo not found with ID: " + id);
    }

    if (photo.getImageName() != null && !photo.getImageName().isEmpty()) {
        Path imagePath = Paths.get("C:/Users/USER/Desktop/projet PFE/FrontEnd-MasterGYM-main/FrontEnd-MasterGYM-main/src/assets/Gallery/").resolve(photo.getImageName());
        try {
            Files.deleteIfExists(imagePath);
        } catch (IOException e) {
            System.err.println("Failed to delete image: " + e.getMessage());
        }
    }

    photoService.deleteById(id);

    String jsonResponse = "{\"message\": \"Photo and associated image deleted successfully.\"}";
    return ResponseEntity.ok().contentType(MediaType.APPLICATION_JSON).body(jsonResponse);
}


}