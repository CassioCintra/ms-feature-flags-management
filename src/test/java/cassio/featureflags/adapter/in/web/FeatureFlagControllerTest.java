package cassio.featureflags.adapter.in.web;

import cassio.featureflags.adapter.in.web.config.SecurityConfig;
import cassio.featureflags.adapter.in.web.filter.CorrelatorFilter;
import cassio.featureflags.adapter.in.web.request.CreateFlagRequest;
import cassio.featureflags.adapter.in.web.request.UpdateFlagRequest;
import cassio.featureflags.application.port.in.FeatureFlagUseCase;
import cassio.featureflags.domain.FeatureFlag;
import cassio.featureflags.domain.exception.FeatureFlagAlreadyExistsException;
import cassio.featureflags.domain.exception.FeatureFlagNotFoundException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(FeatureFlagController.class)
@Import({SecurityConfig.class, GlobalExceptionHandler.class, CorrelatorFilter.class})
class FeatureFlagControllerTest {

    @Autowired
    private MockMvc mockMvc;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @MockitoBean
    private FeatureFlagUseCase useCase;

    private FeatureFlag flag(Long id, String name, String svc, String env, boolean enabled) {
        return FeatureFlag.builder()
                .id(id).flagName(name).serviceName(svc).environmentName(env).enabled(enabled)
                .build();
    }

    @Test
    void shouldCreateFlagDisabledAndReturn201() throws Exception {
        when(useCase.create(any())).thenReturn(flag(1L, "my-flag", "billing", "prod", false));

        mockMvc.perform(post("/flags")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new CreateFlagRequest("my-flag", "billing", "prod"))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.flagName").value("my-flag"))
                .andExpect(jsonPath("$.enabled").value(false))
                .andExpect(header().exists(CorrelatorFilter.HEADER));
    }

    @Test
    void shouldReuseCorrelatorFromRequestHeader() throws Exception {
        when(useCase.create(any())).thenReturn(flag(1L, "my-flag", "billing", "prod", false));

        mockMvc.perform(post("/flags")
                        .header(CorrelatorFilter.HEADER, "my-custom-correlator")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new CreateFlagRequest("my-flag", "billing", "prod"))))
                .andExpect(status().isCreated())
                .andExpect(header().string(CorrelatorFilter.HEADER, "my-custom-correlator"));
    }

    @Test
    void shouldReturn409WithErrorResponseWhenCreatingDuplicateFlag() throws Exception {
        when(useCase.create(any())).thenThrow(FeatureFlagAlreadyExistsException.alreadyExists("dup", "svc", "env"));

        mockMvc.perform(post("/flags")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new CreateFlagRequest("dup", "svc", "env"))))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.title").value("Conflict"))
                .andExpect(jsonPath("$.detail").exists())
                .andExpect(jsonPath("$.instance").value("/flags"))
                .andExpect(jsonPath("$.timestamp").exists());
    }

    @Test
    void shouldUpdateFlagAndReturn200() throws Exception {
        when(useCase.update(eq(1L), any())).thenReturn(flag(1L, "my-flag", "billing", "prod", true));

        mockMvc.perform(put("/flags/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new UpdateFlagRequest(true))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.enabled").value(true));
    }

    @Test
    void shouldReturn404WithErrorResponseWhenUpdatingNonExistentFlag() throws Exception {
        when(useCase.update(eq(999L), any())).thenThrow(FeatureFlagNotFoundException.notFound(999L));

        mockMvc.perform(put("/flags/999")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new UpdateFlagRequest(true))))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.title").value("Not Found"))
                .andExpect(jsonPath("$.detail").value("Feature flag not found: 999"))
                .andExpect(jsonPath("$.instance").value("/flags/999"))
                .andExpect(jsonPath("$.timestamp").exists());
    }

    @Test
    void shouldDeleteFlagAndReturn204() throws Exception {
        doNothing().when(useCase).delete(1L);

        mockMvc.perform(delete("/flags/1"))
                .andExpect(status().isNoContent());
    }

    @Test
    void shouldReturn404WhenDeletingNonExistentFlag() throws Exception {
        doThrow(FeatureFlagNotFoundException.notFound(999L)).when(useCase).delete(999L);

        mockMvc.perform(delete("/flags/999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404));
    }

    @Test
    void shouldReturnFlagsForServiceAndEnvironment() throws Exception {
        when(useCase.findByServiceAndEnvironment("billing", "prod")).thenReturn(List.of(
                flag(1L, "flag-a", "billing", "prod", false),
                flag(2L, "flag-b", "billing", "prod", true)
        ));

        mockMvc.perform(get("/flags/billing/prod"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].flagName").value("flag-a"))
                .andExpect(jsonPath("$[1].flagName").value("flag-b"));
    }

    @Test
    void shouldReturnEmptyListWhenNoFlagsFound() throws Exception {
        when(useCase.findByServiceAndEnvironment("unknown", "dev")).thenReturn(List.of());

        mockMvc.perform(get("/flags/unknown/dev"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }
}
