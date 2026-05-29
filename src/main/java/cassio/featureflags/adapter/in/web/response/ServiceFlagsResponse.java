package cassio.featureflags.adapter.in.web.response;

import cassio.featureflags.application.port.in.FeatureFlagUseCase.ServiceInfo;

import java.util.List;

public record ServiceFlagsResponse(String serviceName, List<FeatureFlagResponse> flags) {

    public static ServiceFlagsResponse from(ServiceInfo info) {
        return new ServiceFlagsResponse(
                info.serviceName(),
                info.flags().stream().map(FeatureFlagResponse::from).toList());
    }
}
