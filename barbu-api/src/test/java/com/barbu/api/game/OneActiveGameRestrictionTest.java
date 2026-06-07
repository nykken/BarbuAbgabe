package com.barbu.api.game;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@ActiveProfiles("test")
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class OneActiveGameRestrictionTest {

    @Autowired
    private WebApplicationContext wac;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(wac).apply(springSecurity()).build();
    }

    private static final String STANDARD_VARIANT = "{\"variantId\":\"standard\"}";

    @Test
    void user_cannotCreateSecondGame_whileAlreadyInActiveGame() throws Exception {
        MockHttpSession session = loginAs("alice", "password123");

        mockMvc.perform(post("/api/games")
                        .session(session).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(STANDARD_VARIANT))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/games")
                        .session(session).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(STANDARD_VARIANT))
                .andExpect(status().isConflict());
    }

    @Test
    void user_cannotCreateAnotherGame_whileAlreadyInActiveGame() throws Exception {
        MockHttpSession session = loginAs("alice", "password123");

        mockMvc.perform(post("/api/games")
                        .session(session).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(STANDARD_VARIANT))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/games")
                        .session(session).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(STANDARD_VARIANT))
                .andExpect(status().isConflict());
    }

    @Test
    void differentUsers_canEachCreateTheirOwnGame() throws Exception {
        MockHttpSession aliceSession = loginAs("alice", "password123");
        MockHttpSession bobSession   = loginAs("bob",   "password123");

        mockMvc.perform(post("/api/games")
                        .session(aliceSession).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(STANDARD_VARIANT))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/games")
                        .session(bobSession).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(STANDARD_VARIANT))
                .andExpect(status().isCreated());
    }

    // -------------------------------------------------------------------------

    private MockHttpSession loginAs(String username, String password) throws Exception {
        String body = "{\"username\":\"" + username + "\",\"password\":\"" + password + "\"}";

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().is2xxSuccessful());

        MvcResult login = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andReturn();

        return (MockHttpSession) login.getRequest().getSession();
    }
}