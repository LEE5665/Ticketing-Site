package com.example.backend;

import com.example.backend.member.repository.MemberRepository;
import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class AuthIntegrationTests {
    @Autowired MockMvc mvc;
    @Autowired MemberRepository members;
    @Autowired PasswordEncoder encoder;

    private static final String SIGNUP = """
            {"name":"테스트 회원","email":"Member@example.com","password":"password123!"}
            """;

    @BeforeEach
    void clearMembers() { members.deleteAll(); }

    record Token(MockHttpSession session, String value) {}

    private Token csrf(MockHttpSession session) throws Exception {
        var request = get("/api/csrf");
        if (session != null) request.session(session);
        var result = mvc.perform(request).andExpect(status().isOk())
                .andExpect(jsonPath("$.headerName").value("X-CSRF-TOKEN"))
                .andReturn();
        return new Token((MockHttpSession) result.getRequest().getSession(),
                JsonPath.read(result.getResponse().getContentAsString(), "$.token"));
    }

    private void signup(Token token) throws Exception {
        mvc.perform(post("/api/members").session(token.session())
                        .header("X-CSRF-TOKEN", token.value())
                        .contentType(MediaType.APPLICATION_JSON).content(SIGNUP))
                .andExpect(status().isCreated()).andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.password").doesNotExist());
    }

    @Test
    void signupLoginSessionAndLogout() throws Exception {
        Token token = csrf(null);
        signup(token);
        var member = members.findByEmail("member@example.com").orElseThrow();
        assertThat(member.getPassword()).isNotEqualTo("password123!");
        assertThat(encoder.matches("password123!", member.getPassword())).isTrue();

        mvc.perform(get("/api/auth/me").session(token.session()))
                .andExpect(status().isUnauthorized());
        String anonymousSessionId = token.session().getId();
        mvc.perform(post("/api/auth/login").session(token.session())
                        .header("X-CSRF-TOKEN", token.value())
                        .param("email", "MEMBER@example.com").param("password", "password123!"))
                .andExpect(status().isNoContent());
        assertThat(token.session().getId()).isNotEqualTo(anonymousSessionId);
        mvc.perform(get("/api/auth/me").session(token.session()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("member@example.com"))
                .andExpect(jsonPath("$.name").value("테스트 회원"))
                .andExpect(jsonPath("$.password").doesNotExist());

        // Authentication rotates the CSRF token. The old one must fail.
        mvc.perform(post("/api/auth/logout").session(token.session())
                        .header("X-CSRF-TOKEN", token.value()))
                .andExpect(status().isForbidden());
        Token loggedIn = csrf(token.session());
        mvc.perform(post("/api/auth/logout").session(loggedIn.session())
                        .header("X-CSRF-TOKEN", loggedIn.value()))
                .andExpect(status().isNoContent());
        assertThat(loggedIn.session().isInvalid()).isTrue();
        mvc.perform(get("/api/auth/me")).andExpect(status().isUnauthorized());
        csrf(null); // A fresh anonymous session can obtain a new token after logout.
    }

    @Test
    void rejectsDuplicateInvalidAndOversizedSignup() throws Exception {
        Token token = csrf(null);
        signup(token);
        mvc.perform(post("/api/members").session(token.session())
                        .header("X-CSRF-TOKEN", token.value())
                        .contentType(MediaType.APPLICATION_JSON).content(SIGNUP))
                .andExpect(status().isConflict()).andExpect(jsonPath("$.message").isString());
        for (String body : new String[]{
                "{\"name\":\" \",\"email\":\"bad\",\"password\":\"short\"}",
                "{\"name\":\"회원\",\"email\":\"other@example.com\",\"password\":\"" + "가".repeat(25) + "\"}"
        }) {
            mvc.perform(post("/api/members").session(token.session())
                            .header("X-CSRF-TOKEN", token.value())
                            .contentType(MediaType.APPLICATION_JSON).content(body))
                    .andExpect(status().isBadRequest()).andExpect(jsonPath("$.message").isString());
        }
        assertThat(members.count()).isEqualTo(1);
    }

    @Test
    void rejectsMissingCsrfAndWrongCredentials() throws Exception {
        mvc.perform(post("/api/members").contentType(MediaType.APPLICATION_JSON).content(SIGNUP))
                .andExpect(status().isForbidden());
        mvc.perform(post("/api/auth/login").param("email", "member@example.com").param("password", "password123!"))
                .andExpect(status().isForbidden());
        Token token = csrf(null);
        signup(token);
        mvc.perform(post("/api/auth/login").session(token.session())
                        .header("X-CSRF-TOKEN", token.value())
                        .param("email", "member@example.com").param("password", "wrong"))
                .andExpect(status().isUnauthorized()).andExpect(jsonPath("$.message").isString());
        mvc.perform(get("/api/auth/me").session(token.session())).andExpect(status().isUnauthorized());
    }

    @Test
    void allowsOnlyConfiguredFrontendOrigin() throws Exception {
        mvc.perform(options("/api/auth/login")
                        .header("Origin", "http://localhost:3000")
                        .header("Access-Control-Request-Method", "POST")
                        .header("Access-Control-Request-Headers", "content-type,x-csrf-token"))
                .andExpect(status().isOk())
                .andExpect(header().string("Access-Control-Allow-Origin", "http://localhost:3000"))
                .andExpect(header().string("Access-Control-Allow-Credentials", "true"));
        mvc.perform(options("/api/auth/login")
                        .header("Origin", "https://untrusted.example")
                        .header("Access-Control-Request-Method", "POST"))
                .andExpect(status().isForbidden());
    }
}
