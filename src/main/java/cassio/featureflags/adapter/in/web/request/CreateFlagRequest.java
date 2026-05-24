package cassio.featureflags.adapter.in.web.request;

public record CreateFlagRequest(
        String flagName,
        String serviceName,
        String environmentName
) {}
