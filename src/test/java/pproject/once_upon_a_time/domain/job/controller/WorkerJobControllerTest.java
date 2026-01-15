package pproject.once_upon_a_time.domain.job.controller;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import pproject.once_upon_a_time.domain.job.domain.Job;
import pproject.once_upon_a_time.domain.job.domain.JobStatus;
import pproject.once_upon_a_time.domain.job.domain.JobTargetType;
import pproject.once_upon_a_time.domain.job.domain.JobType;
import pproject.once_upon_a_time.domain.job.service.JobService;
import pproject.once_upon_a_time.domain.job.service.WorkerAuthService;
import pproject.once_upon_a_time.domain.member.repository.MemberRepository;
import pproject.once_upon_a_time.global.auth.config.JwtProvider;
import pproject.once_upon_a_time.global.exception.CustomException;
import pproject.once_upon_a_time.global.exception.ErrorCode;
import pproject.once_upon_a_time.global.exception.GlobalExceptionHandler;

import java.util.UUID;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(WorkerJobController.class)
@AutoConfigureMockMvc(addFilters = false)
@ContextConfiguration(classes = {WorkerJobController.class, GlobalExceptionHandler.class})
class WorkerJobControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private JobService jobService;

    @MockitoBean
    private WorkerAuthService workerAuthService;

    @MockitoBean
    private JwtProvider jwtProvider;

    @MockitoBean
    private MemberRepository memberRepository;

    @Test
    void markRunning_statusConflict_returnsConflict() throws Exception {
        UUID jobId = UUID.randomUUID();
        Job job = buildJob(jobId, JobType.STORY, JobStatus.SUCCEEDED, JobTargetType.STORY, 1L, "jobs/input.json", null, null);

        doNothing().when(workerAuthService).validate("token");
        when(jobService.getJob(jobId)).thenReturn(job);
        when(jobService.markRunning(jobId)).thenReturn(false);

        mockMvc.perform(post("/api/v1/internal/jobs/{jobId}/running", jobId)
                .header("X-Worker-Token", "token"))
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.error.code").value(ErrorCode.JOB_STATUS_NOT_ALLOWED.getCode()));
    }

    @Test
    void markRunning_success_returnsJob() throws Exception {
        UUID jobId = UUID.randomUUID();
        Job before = buildJob(jobId, JobType.STORY, JobStatus.PENDING, JobTargetType.STORY, 1L, "jobs/input.json", null, null);
        Job after = buildJob(jobId, JobType.STORY, JobStatus.RUNNING, JobTargetType.STORY, 1L, "jobs/input.json", null, null);

        doNothing().when(workerAuthService).validate("token");
        when(jobService.getJob(jobId)).thenReturn(before, after);
        when(jobService.markRunning(jobId)).thenReturn(true);

        mockMvc.perform(post("/api/v1/internal/jobs/{jobId}/running", jobId)
                .header("X-Worker-Token", "token"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.status").value("RUNNING"));
    }

    @Test
    void markSucceeded_invalidToken_returnsUnauthorized() throws Exception {
        UUID jobId = UUID.randomUUID();

        doThrow(new CustomException(ErrorCode.INVALID_CREDENTIALS))
            .when(workerAuthService)
            .validate("token");

        mockMvc.perform(post("/api/v1/internal/jobs/{jobId}/succeeded", jobId)
                .header("X-Worker-Token", "token")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"outputKey\":\"jobs/result.json\"}"))
            .andExpect(status().isUnauthorized())
            .andExpect(jsonPath("$.error.code").value(ErrorCode.INVALID_CREDENTIALS.getCode()));
    }

    @Test
    void markSucceeded_statusConflict_returnsConflict() throws Exception {
        UUID jobId = UUID.randomUUID();
        Job job = buildJob(jobId, JobType.STORY, JobStatus.RUNNING, JobTargetType.STORY, 1L, "jobs/input.json", null, null);

        doNothing().when(workerAuthService).validate("token");
        when(jobService.getJob(jobId)).thenReturn(job);
        when(jobService.markSucceeded(jobId, "jobs/result.json")).thenReturn(false);

        mockMvc.perform(post("/api/v1/internal/jobs/{jobId}/succeeded", jobId)
                .header("X-Worker-Token", "token")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"outputKey\":\"jobs/result.json\"}"))
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.error.code").value(ErrorCode.JOB_STATUS_NOT_ALLOWED.getCode()));
    }

    @Test
    void markSucceeded_success_returnsJob() throws Exception {
        UUID jobId = UUID.randomUUID();
        Job before = buildJob(jobId, JobType.STORY, JobStatus.RUNNING, JobTargetType.STORY, 1L, "jobs/input.json", null, null);
        Job after = buildJob(jobId, JobType.STORY, JobStatus.SUCCEEDED, JobTargetType.STORY, 1L, "jobs/input.json", "jobs/result.json", null);

        doNothing().when(workerAuthService).validate("token");
        when(jobService.getJob(jobId)).thenReturn(before, after);
        when(jobService.markSucceeded(eq(jobId), eq("jobs/result.json"))).thenReturn(true);

        mockMvc.perform(post("/api/v1/internal/jobs/{jobId}/succeeded", jobId)
                .header("X-Worker-Token", "token")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"outputKey\":\"jobs/result.json\"}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.status").value("SUCCEEDED"))
            .andExpect(jsonPath("$.data.outputKey").value("jobs/result.json"));
    }

    @Test
    void markFailed_missingBody_returnsBadRequest() throws Exception {
        UUID jobId = UUID.randomUUID();

        doNothing().when(workerAuthService).validate("token");

        mockMvc.perform(post("/api/v1/internal/jobs/{jobId}/failed", jobId)
                .header("X-Worker-Token", "token")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{}"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.code").value("INVALID_INPUT"));
    }

    @Test
    void markFailed_statusConflict_returnsConflict() throws Exception {
        UUID jobId = UUID.randomUUID();
        Job job = buildJob(jobId, JobType.STORY, JobStatus.RUNNING, JobTargetType.STORY, 1L, "jobs/input.json", null, null);

        doNothing().when(workerAuthService).validate("token");
        when(jobService.getJob(jobId)).thenReturn(job);
        when(jobService.markFailed(jobId, "boom")).thenReturn(false);

        mockMvc.perform(post("/api/v1/internal/jobs/{jobId}/failed", jobId)
                .header("X-Worker-Token", "token")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"errorMessage\":\"boom\"}"))
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.error.code").value(ErrorCode.JOB_STATUS_NOT_ALLOWED.getCode()));
    }

    @Test
    void markFailed_success_returnsJob() throws Exception {
        UUID jobId = UUID.randomUUID();
        Job before = buildJob(jobId, JobType.STORY, JobStatus.RUNNING, JobTargetType.STORY, 1L, "jobs/input.json", null, null);
        Job after = buildJob(jobId, JobType.STORY, JobStatus.FAILED, JobTargetType.STORY, 1L, "jobs/input.json", null, "boom");

        doNothing().when(workerAuthService).validate("token");
        when(jobService.getJob(jobId)).thenReturn(before, after);
        when(jobService.markFailed(eq(jobId), eq("boom"))).thenReturn(true);

        mockMvc.perform(post("/api/v1/internal/jobs/{jobId}/failed", jobId)
                .header("X-Worker-Token", "token")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"errorMessage\":\"boom\"}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.status").value("FAILED"))
            .andExpect(jsonPath("$.data.errorMessage").value("boom"));
    }

    private Job buildJob(
        UUID jobId,
        JobType type,
        JobStatus status,
        JobTargetType targetType,
        Long targetId,
        String inputKey,
        String outputKey,
        String errorMessage
    )
        throws Exception {
        Job job = Job.builder()
            .type(type)
            .status(status)
            .targetType(targetType)
            .targetId(targetId)
            .inputKey(inputKey)
            .outputKey(outputKey)
            .errorMessage(errorMessage)
            .build();

        java.lang.reflect.Field idField = Job.class.getDeclaredField("id");
        idField.setAccessible(true);
        idField.set(job, jobId);
        return job;
    }
}
