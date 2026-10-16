package cassio.featureflags.application;

import java.util.UUID;

public class TenantContext {

    private record TenantData(String userId, String userEmail, String userName, UUID workspaceId) {}

    private static final ThreadLocal<TenantData> CONTEXT = new ThreadLocal<>();

    private TenantContext() {}

    public static void set(String userId, String userEmail, String userName, UUID workspaceId) {
        CONTEXT.set(new TenantData(userId, userEmail, userName, workspaceId));
    }

    public static String getUserId() {
        TenantData d = CONTEXT.get();
        return d != null ? d.userId() : null;
    }

    public static String getUserEmail() {
        TenantData d = CONTEXT.get();
        return d != null ? d.userEmail() : null;
    }

    public static String getUserName() {
        TenantData d = CONTEXT.get();
        return d != null ? d.userName() : null;
    }

    public static UUID getWorkspaceId() {
        TenantData d = CONTEXT.get();
        if (d == null || d.workspaceId() == null) {
            throw new IllegalStateException("No workspace in the current request");
        }
        return d.workspaceId();
    }

    public static void clear() {
        CONTEXT.remove();
    }
}
