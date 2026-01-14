package pproject.once_upon_a_time.domain.job.scheduler;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import pproject.once_upon_a_time.domain.job.service.SpotInstanceService;
import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.model.GetQueueAttributesRequest;
import software.amazon.awssdk.services.sqs.model.QueueAttributeName;

@Slf4j
@Component
@RequiredArgsConstructor
public class SpotInstanceTerminationScheduler {

    private final SpotInstanceService spotInstanceService;
    private final SqsClient sqsClient;

    @Value("${aws.sqs.queue-url}")
    private String queueUrl;

    @Scheduled(fixedDelayString = "${aws.ec2.terminate-interval-ms:300000}")
    public void terminateIfIdle() {
        if (!spotInstanceService.isEnabled()) {
            return;
        }

        int backlog = getQueueBacklog();
        if (backlog > 0) {
            log.debug("SQS backlog 존재: {}", backlog);
            return;
        }

        spotInstanceService.terminateSpotInstances();
    }

    private int getQueueBacklog() {
        GetQueueAttributesRequest request = GetQueueAttributesRequest.builder()
            .queueUrl(queueUrl)
            .attributeNames(
                QueueAttributeName.APPROXIMATE_NUMBER_OF_MESSAGES,
                QueueAttributeName.APPROXIMATE_NUMBER_OF_MESSAGES_NOT_VISIBLE
            )
            .build();

        var attributes = sqsClient.getQueueAttributes(request).attributes();
        int visible = Integer.parseInt(attributes.getOrDefault(
            QueueAttributeName.APPROXIMATE_NUMBER_OF_MESSAGES, "0"));
        int invisible = Integer.parseInt(attributes.getOrDefault(
            QueueAttributeName.APPROXIMATE_NUMBER_OF_MESSAGES_NOT_VISIBLE, "0"));
        return visible + invisible;
    }
}
