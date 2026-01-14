package pproject.once_upon_a_time.domain.job.service;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.services.ec2.Ec2Client;
import software.amazon.awssdk.services.ec2.model.DescribeInstancesRequest;
import software.amazon.awssdk.services.ec2.model.Filter;
import software.amazon.awssdk.services.ec2.model.Instance;
import software.amazon.awssdk.services.ec2.model.InstanceInterruptionBehavior;
import software.amazon.awssdk.services.ec2.model.InstanceMarketOptionsRequest;
import software.amazon.awssdk.services.ec2.model.LaunchTemplateSpecification;
import software.amazon.awssdk.services.ec2.model.MarketType;
import software.amazon.awssdk.services.ec2.model.ResourceType;
import software.amazon.awssdk.services.ec2.model.RunInstancesRequest;
import software.amazon.awssdk.services.ec2.model.SpotInstanceType;
import software.amazon.awssdk.services.ec2.model.Tag;
import software.amazon.awssdk.services.ec2.model.TagSpecification;
import software.amazon.awssdk.services.ec2.model.TerminateInstancesRequest;

@Slf4j
@Service
@RequiredArgsConstructor
public class SpotInstanceService {

    private static final Set<String> ACTIVE_STATES = Set.of("pending", "running");
    private static final Set<String> TERMINATABLE_STATES = Set.of("pending", "running", "stopping", "stopped");

    private final Ec2Client ec2Client;

    @Value("${aws.ec2.enabled:true}")
    private boolean enabled;

    @Value("${aws.ec2.launch-template-id:}")
    private String launchTemplateId;

    @Value("${aws.ec2.launch-template-version:$Latest}")
    private String launchTemplateVersion;

    @Value("${aws.ec2.spot-tag-name:ouat-gpu-worker}")
    private String spotTagName;

    @Value("${aws.ec2.spot-tag-role:worker}")
    private String spotTagRole;

    public boolean ensureSpotInstanceRunning() {
        if (!enabled) {
            return false;
        }
        if (launchTemplateId == null || launchTemplateId.isBlank()) {
            log.warn("Launch Template ID가 없어 Spot 인스턴스를 생성하지 않습니다.");
            return false;
        }
        if (hasActiveSpotInstance()) {
            return false;
        }
        try {
            runSpotInstance();
            return true;
        } catch (Exception ex) {
            log.error("Spot 인스턴스 생성 실패", ex);
            return false;
        }
    }

    public void terminateSpotInstances() {
        if (!enabled) {
            return;
        }
        List<String> instanceIds = listSpotInstanceIds(TERMINATABLE_STATES);
        if (instanceIds.isEmpty()) {
            return;
        }
        TerminateInstancesRequest request = TerminateInstancesRequest.builder()
            .instanceIds(instanceIds)
            .build();
        ec2Client.terminateInstances(request);
        log.info("Spot 인스턴스 종료 요청: {}", instanceIds);
    }

    public boolean isEnabled() {
        return enabled;
    }

    private boolean hasActiveSpotInstance() {
        return !listSpotInstanceIds(ACTIVE_STATES).isEmpty();
    }

    private List<String> listSpotInstanceIds(Set<String> states) {
        List<Filter> filters = List.of(
            Filter.builder().name("tag:Name").values(spotTagName).build(),
            Filter.builder().name("tag:Role").values(spotTagRole).build(),
            Filter.builder().name("instance-state-name").values(states).build()
        );

        DescribeInstancesRequest request = DescribeInstancesRequest.builder()
            .filters(filters)
            .build();

        List<String> instanceIds = new ArrayList<>();
        ec2Client.describeInstances(request).reservations().forEach(reservation -> {
            for (Instance instance : reservation.instances()) {
                instanceIds.add(instance.instanceId());
            }
        });
        return instanceIds;
    }

    private void runSpotInstance() {
        LaunchTemplateSpecification templateSpecification = LaunchTemplateSpecification.builder()
            .launchTemplateId(launchTemplateId)
            .version(launchTemplateVersion)
            .build();

        InstanceMarketOptionsRequest marketOptions = InstanceMarketOptionsRequest.builder()
            .marketType(MarketType.SPOT)
            .spotOptions(builder -> builder
                .instanceInterruptionBehavior(InstanceInterruptionBehavior.TERMINATE)
                .spotInstanceType(SpotInstanceType.ONE_TIME))
            .build();

        TagSpecification tagSpecification = TagSpecification.builder()
            .resourceType(ResourceType.INSTANCE)
            .tags(
                Tag.builder().key("Name").value(spotTagName).build(),
                Tag.builder().key("Role").value(spotTagRole).build()
            )
            .build();

        RunInstancesRequest request = RunInstancesRequest.builder()
            .launchTemplate(templateSpecification)
            .instanceMarketOptions(marketOptions)
            .tagSpecifications(tagSpecification)
            .minCount(1)
            .maxCount(1)
            .build();

        ec2Client.runInstances(request);
        log.info("Spot 인스턴스 생성 요청 완료. launchTemplateId={}, version={}", launchTemplateId, launchTemplateVersion);
    }
}
