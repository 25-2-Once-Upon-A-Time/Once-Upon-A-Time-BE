package pproject.once_upon_a_time.domain.job.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import pproject.once_upon_a_time.domain.job.domain.Job;
import pproject.once_upon_a_time.domain.job.dto.JobQueueMessageDto;
import pproject.once_upon_a_time.global.exception.CustomException;
import pproject.once_upon_a_time.global.exception.ErrorCode;
import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.model.SendMessageRequest;

@Service
@RequiredArgsConstructor
public class JobQueuePublisher {

    private final SqsClient sqsClient;
    private final ObjectMapper objectMapper;

    @Value("${aws.sqs.queue-url}")
    private String queueUrl;

    public void publish(Job job) {
        JobQueueMessageDto message = JobQueueMessageDto.builder()
            .jobId(job.getId())
            .jobType(job.getType())
            .inputKey(job.getInputKey())
            .build();
        String payload = toJson(message);

        SendMessageRequest request = SendMessageRequest.builder()
            .queueUrl(queueUrl)
            .messageBody(payload)
            .build();
        sqsClient.sendMessage(request);
    }

    private String toJson(JobQueueMessageDto message) {
        try {
            return objectMapper.writeValueAsString(message);
        } catch (JsonProcessingException e) {
            throw new CustomException(ErrorCode.JOB_MESSAGE_SERIALIZATION_FAILED, e.getMessage());
        }
    }
}
