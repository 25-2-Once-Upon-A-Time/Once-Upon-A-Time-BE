package pproject.once_upon_a_time.domain.job.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import pproject.once_upon_a_time.domain.job.domain.Job;
import pproject.once_upon_a_time.domain.job.domain.JobStatus;

import java.util.UUID;

public interface JobRepository extends JpaRepository<Job, UUID> {

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
        update Job j
        set j.status = :nextStatus
        where j.id = :id
          and j.status = :currentStatus
        """)
    int updateStatusIfMatches(@Param("id") UUID id,
                              @Param("currentStatus") JobStatus currentStatus,
                              @Param("nextStatus") JobStatus nextStatus);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
        update Job j
        set j.status = :nextStatus,
            j.outputKey = :outputKey,
            j.errorMessage = null
        where j.id = :id
          and j.status = :currentStatus
        """)
    int markSucceeded(@Param("id") UUID id,
                      @Param("currentStatus") JobStatus currentStatus,
                      @Param("nextStatus") JobStatus nextStatus,
                      @Param("outputKey") String outputKey);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
        update Job j
        set j.status = :nextStatus,
            j.errorMessage = :errorMessage
        where j.id = :id
          and j.status = :currentStatus
        """)
    int markFailed(@Param("id") UUID id,
                   @Param("currentStatus") JobStatus currentStatus,
                   @Param("nextStatus") JobStatus nextStatus,
                   @Param("errorMessage") String errorMessage);
}
