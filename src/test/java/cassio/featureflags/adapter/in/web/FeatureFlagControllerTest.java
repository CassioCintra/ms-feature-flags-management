package cassio.featureflags.adapter.in.web;

import cassio.featureflags.adapter.in.web.config.SecurityConfig;
import cassio.featureflags.adapter.in.web.filter.CorrelatorFilter;
import cassio.featureflags.adapter.in.web.request.CreateFlagRequest;
import cassio.featureflags.adapter.in.web.request.EvaluateBatchRequest;
import cassio.featureflags.adapter.in.web.request.PatchFlagRequest;
import cassio.featureflags.application.port.in.FeatureFlagUseCase;
import cassio.featureflags.application.port.in.FeatureFlagUseCase.ServiceInfo;
import cassio.featureflags.domain.EvaluationResult;
import cassio.featureflags.domain.FeatureFlag;
import cassio.featureflags.domain.FlagType;
import cassio.featureflags.domain.exception.FeatureFlagAlreadyExistsException;
import cassio.featureflags.domain.exception.FeatureFlagNotFoundException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.Map;

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

    private final ObjectMapper objectMapper = new ObjectMapper()
            .registerModule(new JavaTimeModule())
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

    @MockitoBean
    private FeatureFlagUseCase useCase;

    private FeatureFlag flag(Long id, String name, String svc, FlagType type, boolean enabled) {
        return FeatureFlag.builder()
                .id(id).flagName(name).serviceName(svc)
                .type(type).envs(List.of("prod")).tags(List.of())
                .enabled(enabled).build();
    }

    // ── POST /flags ───────────────────────────────────────────────────────────

    @Test
    @WithMockUser
    void shouldCreateFlagAndReturn201() throws Exception {
        when(useCase.create(any())).thenReturn(flag(1L, "my-flag", "billing", FlagType.BOOLEAN, false));

        mockMvc.perform(post("/flags")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new CreateFlagRequest("my-flag", "billing", FlagType.BOOLEAN, null,
                                        List.of("prod"), List.of(), null, null))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.flagName").value("my-flag"))
                .andExpect(jsonPath("$.type").value("BOOLEAN"))
                .andExpect(jsonPath("$.enabled").value(false))
                .andExpect(header().exists(CorrelatorFilter.HEADER));
    }

    @Test
    @WithMockUser
    void shouldReturn409WhenCreatingDuplicateFlag() throws Exception {
        when(useCase.create(any())).thenThrow(FeatureFlagAlreadyExistsException.alreadyExists("dup"));

        mockMvc.perform(post("/flags")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new CreateFlagRequest("dup", "svc", FlagType.BOOLEAN, null,
                                        List.of(), List.of(), null, null))))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409));
    }

    // ── GET /flags ────────────────────────────────────────────────────────────

    @Test
    @WithMockUser
    void shouldListFlagsWithFilters() throws Exception {
        when(useCase.listFlags(any())).thenReturn(List.of(
                flag(1L, "flag-a", "billing", FlagType.BOOLEAN, false),
                flag(2L, "flag-b", "billing", FlagType.ROLLOUT, true)));

        mockMvc.perform(get("/flags").param("service", "billing"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].flagName").value("flag-a"))
                .andExpect(jsonPath("$[1].type").value("ROLLOUT"));
    }

    @Test
    void shouldReturnEmptyListWhenNoFlagsMatch() throws Exception {
        when(useCase.listFlags(any())).thenReturn(List.of());

        mockMvc.perform(get("/flags").param("env", "dev"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }

    // ── PATCH /flags/{key} ────────────────────────────────────────────────────

    @Test
    @WithMockUser
    void shouldPatchFlagAndReturn200() throws Exception {
        when(useCase.patch(eq("my-flag"), any()))
                .thenReturn(flag(1L, "my-flag", "billing", FlagType.ROLLOUT, true));

        mockMvc.perform(patch("/flags/my-flag")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new PatchFlagRequest(FlagType.ROLLOUT, 50, null, null, null, null, true))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.type").value("ROLLOUT"))
                .andExpect(jsonPath("$.enabled").value(true));
    }

    @Test
    @WithMockUser
    void shouldReturn404WhenPatchingNonExistentFlag() throws Exception {
        when(useCase.patch(eq("ghost"), any())).thenThrow(FeatureFlagNotFoundException.notFound("ghost"));

        mockMvc.perform(patch("/flags/ghost")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new PatchFlagRequest(null, null, null, null, null, null, true))))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404));
    }

    // ── DELETE /flags/{key} ───────────────────────────────────────────────────

    @Test
    @WithMockUser
    void shouldDeleteFlagAndReturn204() throws Exception {
        doNothing().when(useCase).delete("my-flag");

        mockMvc.perform(delete("/flags/my-flag"))
                .andExpect(status().isNoContent());
    }

    @Test
    @WithMockUser
    void shouldReturn404WhenDeletingNonExistentFlag() throws Exception {
        doThrow(FeatureFlagNotFoundException.notFound("ghost")).when(useCase).delete("ghost");

        mockMvc.perform(delete("/flags/ghost"))
                .andExpect(status().isNotFound());
    }

    // ── GET /flags/{key}/evaluate ─────────────────────────────────────────────

    @Test
    @WithMockUser
    void shouldEvaluateFlagAndReturn200() throws Exception {
        when(useCase.evaluate(eq("my-flag"), any()))
                .thenReturn(new EvaluationResult("my-flag", true, FlagType.BOOLEAN, null));

        mockMvc.perform(get("/flags/my-flag/evaluate")
                        .param("userId", "user-1")
                        .param("env", "prod"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.flagName").value("my-flag"))
                .andExpect(jsonPath("$.enabled").value(true))
                .andExpect(jsonPath("$.type").value("BOOLEAN"));
    }

    @Test
    @WithMockUser
    void shouldReturn404WhenEvaluatingNonExistentFlag() throws Exception {
        when(useCase.evaluate(eq("ghost"), any())).thenThrow(FeatureFlagNotFoundException.notFound("ghost"));

        mockMvc.perform(get("/flags/ghost/evaluate").param("userId", "u1").param("env", "prod"))
                .andExpect(status().isNotFound());
    }

    // ── POST /flags/evaluate-batch ────────────────────────────────────────────

    @Test
    @WithMockUser
    void shouldEvaluateBatchAndReturnResults() throws Exception {
        when(useCase.evaluateBatch(any(), any())).thenReturn(Map.of(
                "flag-a", new EvaluationResult("flag-a", true,  FlagType.BOOLEAN, null),
                "flag-b", new EvaluationResult("flag-b", false, FlagType.ROLLOUT, 30)));

        mockMvc.perform(post("/flags/evaluate-batch")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new EvaluateBatchRequest(List.of("flag-a", "flag-b"), "user-1", "prod", Map.of()))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.['flag-a'].enabled").value(true))
                .andExpect(jsonPath("$.['flag-b'].enabled").value(false));
    }

    // ── GET /services ─────────────────────────────────────────────────────────

    @Test
    @WithMockUser
    void shouldReturnServicesWithTheirFlags() throws Exception {
        when(useCase.getServices()).thenReturn(List.of(
                new ServiceInfo("billing", List.of(flag(1L, "flag-a", "billing", FlagType.BOOLEAN, true))),
                new ServiceInfo("orders",  List.of(flag(2L, "flag-b", "orders",  FlagType.ROLLOUT,  false)))));

        mockMvc.perform(get("/services"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].serviceName").value("billing"))
                .andExpect(jsonPath("$[0].flags[0].flagName").value("flag-a"))
                .andExpect(jsonPath("$[1].serviceName").value("orders"));
    }

    // ── Correlator ────────────────────────────────────────────────────────────

    @Test
    @WithMockUser
    void shouldReuseCorrelatorFromRequestHeader() throws Exception {
        when(useCase.create(any())).thenReturn(flag(1L, "my-flag", "billing", FlagType.BOOLEAN, false));

        mockMvc.perform(post("/flags")
                        .header(CorrelatorFilter.HEADER, "my-custom-correlator")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new CreateFlagRequest("my-flag", "billing", FlagType.BOOLEAN, null,
                                        List.of(), List.of(), null, null))))
                .andExpect(status().isCreated())
                .andExpect(header().string(CorrelatorFilter.HEADER, "my-custom-correlator"));
    }
}
