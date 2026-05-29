package cassio.featureflags.adapter.in.web.request;

import java.util.List;
import java.util.Map;

public record EvaluateBatchRequest(
        List<String> keys,
        String userId,
        String env,
        Map<String, String> attributes
) {}
