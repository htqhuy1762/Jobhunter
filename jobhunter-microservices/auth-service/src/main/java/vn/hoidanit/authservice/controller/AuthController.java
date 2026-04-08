package vn.hoidanit.authservice.controller;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import vn.hoidanit.authservice.annotation.RateLimit;
import vn.hoidanit.authservice.domain.User;
import vn.hoidanit.authservice.domain.dto.ReqChangePasswordDTO;
import vn.hoidanit.authservice.domain.dto.ReqLoginDTO;
import vn.hoidanit.authservice.domain.dto.ResLoginDTO;
import vn.hoidanit.authservice.domain.response.RestResponse;
import vn.hoidanit.authservice.service.JwtService;
import vn.hoidanit.authservice.service.TokenService;
import vn.hoidanit.authservice.service.UserService;
import vn.hoidanit.authservice.util.SecurityUtil;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
@Slf4j
public class AuthController {

    private static final String COOKIE_NAME = "refresh_token";
    private static final String BEARER_PREFIX = "Bearer ";
    private static final int BEARER_PREFIX_LENGTH = 7;

    private final AuthenticationManagerBuilder authenticationManagerBuilder;
    private final JwtService jwtService;
    private final UserService userService;
    private final TokenService tokenService;

    @Value("${hoidanit.jwt.refresh-token-validity-in-seconds}")
    private long refreshTokenExpiration;

    @Value("${hoidanit.jwt.cookie-secure}")
    private boolean cookieSecure;

    @RateLimit(name = "login")
    @PostMapping("/login")
    public ResponseEntity<RestResponse<ResLoginDTO>> login(@Valid @RequestBody ReqLoginDTO loginDTO) {
        log.info("Login attempt for user: {}", loginDTO.getUsername());

        User currentUserDB = userService.handleGetUserByUsernameWithPermissions(loginDTO.getUsername());
        if (currentUserDB == null) {
            log.error("Login failed: User not found - {}", loginDTO.getUsername());
            throw new RuntimeException("Invalid username or password");
        }

        UsernamePasswordAuthenticationToken authenticationToken = new UsernamePasswordAuthenticationToken(
                loginDTO.getUsername(), loginDTO.getPassword());
        Authentication authentication = authenticationManagerBuilder.getObject().authenticate(authenticationToken);
        SecurityContextHolder.getContext().setAuthentication(authentication);

        ResLoginDTO res = buildLoginResponse(currentUserDB);
        String accessToken = jwtService.createAccessToken(authentication.getName(), res);
        res.setAccessToken(accessToken);

        String refreshToken = jwtService.createRefreshToken(loginDTO.getUsername(), res);
        tokenService.saveRefreshToken(loginDTO.getUsername(), refreshToken);

        log.info("Login successful for user: {}", loginDTO.getUsername());

        RestResponse<ResLoginDTO> response = new RestResponse<>();
        response.setStatusCode(HttpStatus.OK.value());
        response.setMessage("Login successfully");
        response.setData(res);

        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, createRefreshTokenCookie(refreshToken).toString())
                .body(response);
    }

    @GetMapping("/account")
    public ResponseEntity<RestResponse<ResLoginDTO.UserGetAccount>> getAccount() {
        String email = SecurityUtil.getCurrentUserLogin().orElse("");
        log.debug("Fetching account info for user: {}", email);

        User currentUserDB = userService.handleGetUserByUsername(email);
        ResLoginDTO.UserGetAccount userGetAccount = new ResLoginDTO.UserGetAccount();

        if (currentUserDB != null) {
            ResLoginDTO.UserLogin userLogin = new ResLoginDTO.UserLogin();
            userLogin.setId(currentUserDB.getId());
            userLogin.setEmail(currentUserDB.getEmail());
            userLogin.setName(currentUserDB.getName());
            userLogin.setRole(currentUserDB.getRole());
            userGetAccount.setUser(userLogin);
        }

        return RestResponse.ok(userGetAccount, "Fetch account successfully");
    }

    @GetMapping("/refresh")
    public ResponseEntity<RestResponse<ResLoginDTO>> getRefreshToken(
            @CookieValue(name = COOKIE_NAME, defaultValue = "abc") String refreshToken) {

        log.debug("Refresh token request received");

        Jwt decodedToken = jwtService.checkValidRefreshToken(refreshToken);
        String email = decodedToken.getSubject();

        if (!tokenService.validateRefreshToken(email, refreshToken)) {
            log.error("Refresh token invalid for user: {}", email);
            throw new RuntimeException("Refresh token is invalid");
        }

        User currentUserDB = userService.handleGetUserByUsername(email);
        ResLoginDTO res = buildLoginResponse(currentUserDB);

        tokenService.deleteRefreshToken(email);

        String newAccessToken = jwtService.createAccessToken(email, res);
        res.setAccessToken(newAccessToken);

        String newRefreshToken = jwtService.createRefreshToken(email, res);
        tokenService.saveRefreshToken(email, newRefreshToken);

        log.debug("Refresh token successful for user: {}", email);

        RestResponse<ResLoginDTO> response = new RestResponse<>();
        response.setStatusCode(HttpStatus.OK.value());
        response.setMessage("Refresh token successfully");
        response.setData(res);

        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, createRefreshTokenCookie(newRefreshToken).toString())
                .body(response);
    }

    @PostMapping("/logout")
    public ResponseEntity<RestResponse<Void>> logout(
            @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authHeader) {

        String email = SecurityUtil.getCurrentUserLogin().orElse("");

        if (authHeader != null && authHeader.startsWith(BEARER_PREFIX)) {
            String accessToken = authHeader.substring(BEARER_PREFIX_LENGTH);
            blacklistAccessTokenIfValid(accessToken, email);
        }

        tokenService.deleteRefreshToken(email);
        log.info("User logged out: {}", email);

        RestResponse<Void> response = new RestResponse<>();
        response.setStatusCode(HttpStatus.OK.value());
        response.setMessage("Logout successfully");
        response.setData(null);

        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, createDeleteCookie().toString())
                .body(response);
    }

    @RateLimit(name = "register")
    @PostMapping("/register")
    public ResponseEntity<RestResponse<ResLoginDTO>> register(@Valid @RequestBody User user) {
        log.info("Register attempt for email: {}", user.getEmail());

        if (userService.isEmailExist(user.getEmail())) {
            log.error("Register failed: Email already exists - {}", user.getEmail());
            throw new RuntimeException("Email đã tồn tại");
        }

        User newUser = userService.handleCreateUser(user);
        ResLoginDTO res = buildLoginResponse(newUser);

        String accessToken = jwtService.createAccessToken(newUser.getEmail(), res);
        res.setAccessToken(accessToken);

        String refreshToken = jwtService.createRefreshToken(newUser.getEmail(), res);
        tokenService.saveRefreshToken(newUser.getEmail(), refreshToken);

        log.info("Register successful for user: {}", newUser.getEmail());

        RestResponse<ResLoginDTO> response = new RestResponse<>();
        response.setStatusCode(HttpStatus.CREATED.value());
        response.setMessage("Register successfully");
        response.setData(res);

        return ResponseEntity.status(HttpStatus.CREATED)
                .header(HttpHeaders.SET_COOKIE, createRefreshTokenCookie(refreshToken).toString())
                .body(response);
    }

    @PostMapping("/change-password")
    @RateLimit(name = "changePassword")
    public ResponseEntity<RestResponse<Void>> changePassword(
            @Valid @RequestBody ReqChangePasswordDTO changePasswordDTO) {

        String email = SecurityUtil.getCurrentUserLogin().orElse("");
        log.info("Change password request for user: {}", email);

        if (email.isEmpty()) {
            throw new RuntimeException("User not authenticated");
        }

        if (!changePasswordDTO.getNewPassword().equals(changePasswordDTO.getConfirmPassword())) {
            log.error("Change password failed: Passwords do not match for user {}", email);
            throw new RuntimeException("New password and confirm password do not match");
        }

        User currentUser = userService.handleGetUserByUsername(email);
        if (currentUser == null) {
            throw new RuntimeException("User not found");
        }

        verifyOldPassword(email, changePasswordDTO.getOldPassword());

        if (!userService.updatePassword(currentUser, changePasswordDTO.getNewPassword())) {
            log.error("Change password failed: Could not update password for user {}", email);
            throw new RuntimeException("Could not update password");
        }

        log.info("Password changed successfully for user: {}", email);
        return RestResponse.ok(null, "Change password successfully");
    }

    private ResLoginDTO buildLoginResponse(User user) {
        ResLoginDTO res = new ResLoginDTO();
        ResLoginDTO.UserLogin userLogin = new ResLoginDTO.UserLogin(
                user.getId(),
                user.getEmail(),
                user.getName(),
                user.getRole());
        res.setUser(userLogin);
        return res;
    }

    private ResponseCookie createRefreshTokenCookie(String refreshToken) {
        return ResponseCookie.from(COOKIE_NAME, refreshToken)
                .httpOnly(true)
                .secure(cookieSecure)
                .path("/")
                .maxAge(refreshTokenExpiration)
                .build();
    }

    private ResponseCookie createDeleteCookie() {
        return ResponseCookie.from(COOKIE_NAME, "")
                .httpOnly(true)
                .secure(cookieSecure)
                .path("/")
                .maxAge(0)
                .build();
    }

    private void blacklistAccessTokenIfValid(String accessToken, String email) {
        try {
            Jwt jwt = jwtService.decodeToken(accessToken);
            long expiresAt = jwt.getExpiresAt() != null ? jwt.getExpiresAt().getEpochSecond() : 0;
            long now = java.time.Instant.now().getEpochSecond();
            long remainingTime = expiresAt - now;

            if (remainingTime > 0) {
                tokenService.blacklistAccessToken(accessToken, email, remainingTime);
                log.debug("Blacklisted access token for user: {}", email);
            }
        } catch (Exception e) {
            log.warn("Could not blacklist access token for user: {}", email);
        }
    }

    private void verifyOldPassword(String email, String oldPassword) {
        try {
            UsernamePasswordAuthenticationToken authenticationToken = new UsernamePasswordAuthenticationToken(email,
                    oldPassword);
            authenticationManagerBuilder.getObject().authenticate(authenticationToken);
        } catch (Exception e) {
            log.error("Change password failed: Old password incorrect for user {}", email);
            throw new RuntimeException("Old password is incorrect");
        }
    }
}
