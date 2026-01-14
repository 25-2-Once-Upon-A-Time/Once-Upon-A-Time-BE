package pproject.once_upon_a_time.domain.job.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import pproject.once_upon_a_time.global.exception.CustomException;
import pproject.once_upon_a_time.global.exception.ErrorCode;

@Service
public class WorkerAuthService {

    private final String workerToken;

    public WorkerAuthService(@Value("${worker.token:}") String workerToken) {
        this.workerToken = workerToken;
    }

    public void validate(String providedToken) {
        if (workerToken == null || workerToken.isBlank() || !workerToken.equals(providedToken)) {
            throw new CustomException(ErrorCode.INVALID_CREDENTIALS);
        }
    }
}
