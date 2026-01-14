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
import pproject.once_upon_a_time.domain.job.domain.JobType;
import pproject.once_upon_a_time.domain.job.service.JobService;
import pproject.once_upon_a_time.domain.member.repository.MemberRepository;
import pproject.once_upon_a_time.global.auth.config.JwtProvider;
import pproject.once_upon_a_time.global.exception.CustomException;
import pproject.once_upon_a_time.global.exception.ErrorCode;
import pproject.once_upon_a_time.global.exception.GlobalExceptionHandler;
import pproject.once_upon_a_time.global.storage.S3StorageService;

import java.util.UUID;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(JobController.class)
@AutoConfigureMockMvc(addFilters = false)
@ContextConfiguration(classes = {JobController.class, GlobalExceptionHandler.class})
class JobControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private JobService jobService;

    @MockitoBean
    private S3StorageService s3StorageService;

    @MockitoBean
    private JwtProvider jwtProvider;

    @MockitoBean
    private MemberRepository memberRepository;

    @Test
    void createJob_missingType_returnsBadRequest() throws Exception {
        mockMvc.perform(post("/api/v1/jobs")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{}"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.success").value(false))
            .andExpect(jsonPath("$.code").value("INVALID_INPUT"));
    }

    @Test
    void createJob_returnsCreated() throws Exception {
        UUID jobId = UUID.randomUUID();
        Job job = buildJob(jobId, JobType.STORY, JobStatus.PENDING, "jobs/input.json", null, null);

        when(jobService.createJobAndPublish(eq(JobType.STORY), eq("jobs/input.json"))).thenReturn(job);

        mockMvc.perform(post("/api/v1/jobs")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"type\":\"STORY\",\"inputKey\":\"jobs/input.json\"}"))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.id").value(jobId.toString()))
            .andExpect(jsonPath("$.data.type").value("STORY"))
            .andExpect(jsonPath("$.data.status").value("PENDING"))
            .andExpect(jsonPath("$.data.inputKey").value("jobs/input.json"));
    }

    @Test
    void getJob_notFound_returnsError() throws Exception {
        UUID jobId = UUID.randomUUID();
        when(jobService.getJob(jobId)).thenThrow(new CustomException(ErrorCode.JOB_NOT_FOUND));

        mockMvc.perform(get("/api/v1/jobs/{jobId}", jobId))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.success").value(false))
            .andExpect(jsonPath("$.error.code").value(ErrorCode.JOB_NOT_FOUND.getCode()));
    }

    @Test
    void getJob_returnsJob() throws Exception {
        UUID jobId = UUID.randomUUID();
        Job job = buildJob(jobId, JobType.STORY, JobStatus.PENDING, "jobs/input.json", null, null);

        when(jobService.getJob(jobId)).thenReturn(job);

        mockMvc.perform(get("/api/v1/jobs/{jobId}", jobId))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.id").value(jobId.toString()))
            .andExpect(jsonPath("$.data.status").value("PENDING"));
    }

    @Test
    void getJobResult_notReady_returnsConflict() throws Exception {
        UUID jobId = UUID.randomUUID();
        Job job = buildJob(jobId, JobType.STORY, JobStatus.RUNNING, "jobs/input.json", null, null);

        when(jobService.getJob(jobId)).thenReturn(job);

        mockMvc.perform(get("/api/v1/jobs/{jobId}/result", jobId))
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.error.code").value(ErrorCode.JOB_RESULT_NOT_READY.getCode()));
    }

    @Test
    void getJobResult_returnsPresignedUrl() throws Exception {
        UUID jobId = UUID.randomUUID();
        Job job = buildJob(jobId, JobType.STORY, JobStatus.SUCCEEDED, "jobs/input.json", "jobs/1/result.json", null);

        when(jobService.getJob(jobId)).thenReturn(job);
        when(s3StorageService.createPresignedGetUrl("jobs/1/result.json")).thenReturn("https://example.com/result");

        mockMvc.perform(get("/api/v1/jobs/{jobId}/result", jobId))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.outputKey").value("jobs/1/result.json"))
            .andExpect(jsonPath("$.data.presignedUrl").value("https://example.com/result"));
    }

    private Job buildJob(UUID jobId, JobType type, JobStatus status, String inputKey, String outputKey, String errorMessage)
        throws Exception {
        Job job = Job.builder()
            .type(type)
            .status(status)
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
