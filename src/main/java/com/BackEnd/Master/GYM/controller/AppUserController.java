package com.BackEnd.Master.GYM.controller;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.List;

import java.nio.file.Path;


import org.springframework.core.io.FileSystemResource;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.util.StringUtils;

import com.BackEnd.Master.GYM.Exceptions.ResourceNotFoundException;
import com.BackEnd.Master.GYM.dto.AppUserDto;
import com.BackEnd.Master.GYM.entity.AppUsers;
import com.BackEnd.Master.GYM.entity.Roles;
import com.BackEnd.Master.GYM.Mapper.AppUserMapper;
import com.BackEnd.Master.GYM.services.AppUserService;
import com.BackEnd.Master.GYM.repository.RolesRepo;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@RestController
@RequestMapping("/user")
@CrossOrigin("*")
public class AppUserController {

    private final AppUserService appUserService;
    private final AppUserMapper appUserMapper;
    private final RolesRepo rolesRepo;
    private final PasswordEncoder passwordEncoder;

    // @PreAuthorize("hasAnyAuthority('ROLE_Admin', 'ROLE_Coach')")
    @GetMapping("/{id}")
    public ResponseEntity<AppUserDto> findById(@PathVariable Long id) {
        AppUsers entity = appUserService.findById(id);
        AppUserDto userDto = appUserMapper.map(entity);
        return ResponseEntity.ok(userDto);
    }

    // @PreAuthorize("hasAuthority('ROLE_Admin')")
    @GetMapping()
    public ResponseEntity<List<AppUserDto>> findAll() {
        List<AppUsers> entities = appUserService.findAll();
        List<AppUserDto> userDtos = appUserMapper.map(entities);
        return ResponseEntity.ok(userDtos);
    }
    

    @GetMapping("/count")
    public ResponseEntity<Long> countAllUsers() {
        long entities = appUserService.count();
        return ResponseEntity.ok(entities);
    }


    @GetMapping("/count-coach")
    public ResponseEntity<Long> countByRole(@RequestParam String roleName) {
        long entities = appUserService.countByRoleRoleName(roleName);
        return ResponseEntity.ok(entities);
    }


    @GetMapping("/search")
    public ResponseEntity<List<AppUserDto>> searchUsers(@RequestParam String query) {
        List<AppUsers> entities = appUserService.searchUsers(query);
        return ResponseEntity.ok(appUserMapper.map(entities));
    }

    @GetMapping("/by-role")
    public ResponseEntity<List<AppUserDto>> findByRoleName(@RequestParam String roleName) {
        List<AppUsers> entities = appUserService.findByRoleRoleName(roleName);
        List<AppUserDto> userDtos = appUserMapper.map(entities);
        return ResponseEntity.ok(userDtos);
    }

    @PreAuthorize("hasAnyAuthority('ROLE_Admin', 'ROLE_Coach')")
    @GetMapping("/filtre")
    public ResponseEntity<AppUserDto> filtre(@RequestParam String userName) {
        AppUsers entity = appUserService.findByUserName(userName);
        AppUserDto userDto = appUserMapper.map(entity);
        return ResponseEntity.ok(userDto);
    }

    @GetMapping("/images/{imageName}")
    public ResponseEntity<?> getImage(@PathVariable String imageName) {
        String imagePath = "C:/Users/USER/Desktop/projet PFE/FrontEnd-MasterGYM-main/FrontEnd-MasterGYM-main/src/assets/Profile-img/" + imageName;
        File imgFile = new File(imagePath);

        if (!imgFile.exists()) {
            return ResponseEntity.notFound().build();
        }

        try {
            return ResponseEntity.ok()
                    .contentType(MediaType.IMAGE_JPEG) // Remplacez par le type MIME correct
                    .body(new FileSystemResource(imgFile));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }


@PostMapping(consumes = { "multipart/form-data" })
public ResponseEntity<AppUserDto> insert(
        @RequestParam("userName") String userName,
        @RequestParam("email") String email,
        @RequestParam("telephone") String telephone,
        @RequestParam("motDePasse") String motDePasse,
        @RequestParam("roleName") String roleName,
        @RequestParam("description") String description,
        @RequestParam("profileImage") MultipartFile profileImage) throws IOException {

    Roles role = rolesRepo.findByRoleName(roleName)
            .orElseThrow(() -> new RuntimeException("Role not found"));

    String hashedPassword = passwordEncoder.encode(motDePasse);

    AppUsers user = new AppUsers();
    user.setUserName(userName);
    user.setEmail(email);
    user.setTelephone(telephone);
    user.setMotDePasse(hashedPassword);
    user.setRole(role);
    user.setDescription(description);

    if (profileImage.isEmpty()) {
        throw new RuntimeException("Profile image is required");
    }

    String imageName = StringUtils.cleanPath(profileImage.getOriginalFilename());

    String uploadDir = "C:/Users/USER/Desktop/projet PFE/FrontEnd-MasterGYM-main/FrontEnd-MasterGYM-main/src/assets/Profile-img/";
    Path uploadPath = Paths.get(uploadDir);

    if (!Files.exists(uploadPath)) {
        Files.createDirectories(uploadPath);
    }


    Path filePath = uploadPath.resolve(imageName);
    Files.copy(profileImage.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);


    user.setProfileImage(imageName);

    AppUsers entity = appUserService.insert(user);
    AppUserDto responseDto = appUserMapper.map(entity);

    return ResponseEntity.ok(responseDto);
}


@PutMapping(consumes = { "multipart/form-data" })
public ResponseEntity<AppUserDto> update(
        @RequestParam("id") Long id,
        @RequestParam("userName") String userName,
        @RequestParam("email") String email,
        @RequestParam("telephone") String telephone,
        @RequestParam("motDePasse") String motDePasse,
        @RequestParam("roleName") String roleName,
        @RequestParam("description") String description,
        @RequestParam(value = "profileImage", required = false) MultipartFile profileImage) throws IOException {

    AppUsers currentUser = appUserService.findById(id);
    if (currentUser == null) {
        throw new ResourceNotFoundException("User not found with ID: " + id);
    }

    String oldImageName = currentUser.getProfileImage();

    currentUser.setUserName(userName);
    currentUser.setEmail(email);
    currentUser.setTelephone(telephone);
    currentUser.setDescription(description);

    String hashedPassword = passwordEncoder.encode(motDePasse);
    currentUser.setMotDePasse(hashedPassword);

    Roles role = rolesRepo.findByRoleName(roleName)
            .orElseThrow(() -> new ResourceNotFoundException("Role not found with name: " + roleName));
    currentUser.setRole(role);

    if (profileImage != null && !profileImage.isEmpty()) {
        String imageName = StringUtils.cleanPath(profileImage.getOriginalFilename());
        String uploadDir = "C:/Users/USER/Desktop/projet PFE/FrontEnd-MasterGYM-main/FrontEnd-MasterGYM-main/src/assets/Profile-img";
        Path uploadPath = Paths.get(uploadDir);

        if (!Files.exists(uploadPath)) {
            Files.createDirectories(uploadPath);
        }

        Path filePath = uploadPath.resolve(imageName);
        Files.copy(profileImage.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);

        currentUser.setProfileImage(imageName);

        if (oldImageName != null && !oldImageName.equals(imageName)) {
            Path oldImagePath = uploadPath.resolve(oldImageName);
            try {
                Files.deleteIfExists(oldImagePath);
                System.out.println("Old image deleted: " + oldImagePath);
            } catch (IOException e) {
                System.out.println("Failed to delete old image: " + e.getMessage());
            }
        }
    }

    AppUsers updatedUser = appUserService.update(currentUser);
    AppUserDto responseDto = appUserMapper.map(updatedUser);

    return ResponseEntity.ok(responseDto);
}


@DeleteMapping("/{id}")
public ResponseEntity<String> deleteById(@PathVariable Long id) {

    AppUsers user = appUserService.findById(id);
    if (user == null) {
        throw new ResourceNotFoundException("User not found with ID: " + id);
    }

    String uploadDir = "C:/Users/USER/Desktop/projet PFE/FrontEnd-MasterGYM-main/FrontEnd-MasterGYM-main/src/assets/Profile-img";
    String imageName = user.getProfileImage();

    if (imageName != null && !imageName.isEmpty()) {
        Path imagePath = Paths.get(uploadDir, imageName);
        try {
            boolean deleted = Files.deleteIfExists(imagePath);
            if (deleted) {
                System.out.println("Image deleted: " + imagePath);
            } else {
                System.out.println("Image not found: " + imagePath);
            }
        } catch (IOException e) {
            System.out.println("Failed to delete image: " + e.getMessage());
        }
    }

    appUserService.deleteById(id);

    String jsonResponse = "{\"message\": \"User and associated image deleted successfully.\"}";
    return ResponseEntity.ok().contentType(MediaType.APPLICATION_JSON).body(jsonResponse);
}


    @PatchMapping("/password")
    public ResponseEntity<AppUserDto> updatePassword(@RequestBody AppUserDto pass) {

        AppUsers currentUser = appUserService.findById(pass.getId());
        if (currentUser == null) {
            throw new ResourceNotFoundException("User not found with ID: " + pass.getId());
        }

        // Hachage du mot de passe avec BCrypt
        String hashedPassword = passwordEncoder.encode(pass.getMotDePasse());
        currentUser.setMotDePasse(hashedPassword);

        AppUsers updatedUser = appUserService.update(currentUser);

        AppUserDto responseDto = appUserMapper.map(updatedUser);

        return ResponseEntity.ok(responseDto);
    }

}
