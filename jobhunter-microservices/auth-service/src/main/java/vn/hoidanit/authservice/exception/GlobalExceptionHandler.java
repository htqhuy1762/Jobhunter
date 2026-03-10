package vn.hoidanit.authservice.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import lombok.extern.slf4j.Slf4j;
import vn.hoidanit.authservice.domain.response.RestResponse;

@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<RestResponse<Object>> handleResourceNotFoundException(ResourceNotFoundException ex) {
        log.error("Resource not found: ", ex);
        return RestResponse.error(HttpStatus.NOT_FOUND, ex.getMessage());
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<RestResponse<Object>> handleAllException(Exception ex) {
        log.error("Exception: ", ex);
        return RestResponse.error(HttpStatus.INTERNAL_SERVER_ERROR, ex.getMessage());
    }

    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<RestResponse<Object>> handleRuntimeException(RuntimeException ex) {
        log.error("RuntimeException: ", ex);
        return RestResponse.error(HttpStatus.BAD_REQUEST, ex.getMessage());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<RestResponse<Object>> handleValidationException(MethodArgumentNotValidException ex) {
        log.error("Validation error: ", ex);
        String errorMessage = ex.getBindingResult().getAllErrors().get(0).getDefaultMessage();
        return RestResponse.error(HttpStatus.BAD_REQUEST, errorMessage);
    }

    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<RestResponse<Object>> handleNotFoundException(NoResourceFoundException ex) {
        log.error("Resource not found: ", ex);
        return RestResponse.error(HttpStatus.NOT_FOUND, "Resource not found");
    }
}
