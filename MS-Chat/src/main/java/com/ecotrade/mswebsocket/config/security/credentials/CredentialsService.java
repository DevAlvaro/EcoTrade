package com.ecotrade.mswebsocket.config.security.credentials;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.ecotrade.mswebsocket.config.errors.BusinessException;
import com.ecotrade.mswebsocket.service.NoSqlService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class CredentialsService {

    private final NoSqlService noSqlService;
    private final BCryptPasswordEncoder encoder;
    @Value("${secret.key}")
    private String secret;
    @Value("${secret.time}")
    private Long time;
    @Value("${secret.host.time}")
    private Long hostTime;

    public String
    getHostToHostCreddential() {
        User hostUser = new User("HOST_CHAT", encoder.encode(UUID.randomUUID().toString()), Collections.singleton(new SimpleGrantedAuthority("ROLE_HOST_TO_HOST")));
        Algorithm algorithm = Algorithm.HMAC256(secret.getBytes());
        String accessToken = JWT.create().withSubject(hostUser.getUsername()).withExpiresAt(new Date(System.currentTimeMillis() + time)).withIssuer("HOST_TRADE").sign(algorithm);
        setInRedis(accessToken, hostUser);
        return String.format("Bearer %s", accessToken);
    }

    private void setInRedis(String accessToken, User user) {
        log.info("Setting HOST_TO_HOST token: {} in REDIS", accessToken);
        Map<String, Object> obj = new HashMap<>();
        obj.put("roles", user.getAuthorities().stream().map(it -> it.getAuthority()).collect(Collectors.toList()));
        obj.put("email", user.getUsername());
        obj.put("id", 0l);
        try {
            noSqlService.setValue(accessToken, new ObjectMapper().writeValueAsString(obj), TimeUnit.SECONDS, hostTime);
        } catch (JsonProcessingException e) {
            log.error("REDIS erro ao deserializar o usu??rio do token HOST_TO_HOST");
            throw new BusinessException("", "");
        }
    }
}
