package subarata.softlabio.jevspringboot.exception;

public class JevIntegrationException extends RuntimeException {

    private final int upstreamStatus;

    public JevIntegrationException(
            String message,
            int upstreamStatus
    ) {
        super(message);
        this.upstreamStatus = upstreamStatus;
    }

    public JevIntegrationException(
            String message,
            int upstreamStatus,
            Throwable cause
    ) {
        super(message, cause);
        this.upstreamStatus = upstreamStatus;
    }

    public int upstreamStatus() {
        return upstreamStatus;
    }
}