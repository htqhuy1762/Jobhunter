package vn.hoidanit.fileservice.controller;

import java.time.Instant;

import org.springframework.core.io.InputStreamResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import vn.hoidanit.fileservice.annotation.RateLimit;
import vn.hoidanit.fileservice.domain.response.RestResponse;
import vn.hoidanit.fileservice.service.FileService;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
@Slf4j
public class FileController {

    private final FileService fileService;

    @PostMapping("/files")
    @RateLimit(name = "uploadFile")
    @PreAuthorize("hasAnyAuthority('ROLE_USER', 'ROLE_HR', 'ROLE_ADMIN')")
    public ResponseEntity<RestResponse<ResUploadFileDTO>> uploadFile(
            @RequestParam(name = "file", required = false) MultipartFile file,
            @RequestParam("folder") String folder) throws Exception {

        // MinioService returns full path: "resume/filename.pdf"
        String fullPath = this.fileService.uploadFile(file, folder);

        // Extract only filename without folder prefix for frontend
        // This allows FE to build URL: /storage/{folder}/{filename}
        String filenameOnly = fullPath;
        if (fullPath.startsWith(folder + "/")) {
            filenameOnly = fullPath.substring((folder + "/").length());
        }

        log.info("File uploaded: {} (returned to FE as: {})", fullPath, filenameOnly);

        ResUploadFileDTO res = new ResUploadFileDTO(filenameOnly, Instant.now());
        return RestResponse.ok(res, "Upload file successfully");
    }

    @GetMapping("/files")
    @RateLimit(name = "downloadFile")
    @PreAuthorize("hasAnyAuthority('ROLE_USER', 'ROLE_HR', 'ROLE_ADMIN')")
    public ResponseEntity<Resource> download(
            @RequestParam(name = "fileName", required = false) String fileName,
            @RequestParam(name = "folder", required = false) String folder) throws Exception {

        if (fileName == null || folder == null) {
            throw new RuntimeException("File name or folder is required");
        }

        long fileLength = this.fileService.getFileSize(fileName, folder);
        if (fileLength == 0) {
            throw new RuntimeException("File with the name = " + fileName + " not found");
        }

        InputStreamResource resource = this.fileService.downloadFile(fileName, folder);

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + fileName + "\"")
                .contentLength(fileLength)
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .body(resource);
    }

    /**
     * RESTful endpoint to serve files by path pattern: /storage/{folder}/{filename}
     * Example: GET /storage/resume/1234.pdf
     */
    @GetMapping("/storage/{folder}/{filename:.+}")
    @RateLimit(name = "serveFile")
    public ResponseEntity<Resource> serveFile(
            @PathVariable("folder") String folder,
            @PathVariable("filename") String filename,
            @RequestParam(value = "download", required = false, defaultValue = "false") boolean forceDownload)
            throws Exception {

        long fileLength = this.fileService.getFileSize(filename, folder);
        if (fileLength == 0) {
            throw new RuntimeException("File not found: " + filename);
        }

        InputStreamResource resource = this.fileService.downloadFile(filename, folder);
        String contentType = determineContentType(filename);

        ResponseEntity.BodyBuilder response = ResponseEntity.ok()
                .contentLength(fileLength)
                .contentType(MediaType.parseMediaType(contentType));

        if (forceDownload) {
            response.header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"");
        } else {
            response.header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + filename + "\"");
        }

        return response.body(resource);
    }

    private String determineContentType(String filename) {
        if (filename.endsWith(".pdf")) {
            return "application/pdf";
        } else if (filename.endsWith(".png")) {
            return "image/png";
        } else if (filename.endsWith(".jpg") || filename.endsWith(".jpeg")) {
            return "image/jpeg";
        } else if (filename.endsWith(".doc")) {
            return "application/msword";
        } else if (filename.endsWith(".docx")) {
            return "application/vnd.openxmlformats-officedocument.wordprocessingml.document";
        }
        return "application/octet-stream";
    }

    public static record ResUploadFileDTO(String fileName, Instant uploadedAt) {
    }
}
