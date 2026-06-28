package cassio.featureflags.application;

public class TenantContext {

    private record TenantData(String userId, String userEmail, String userName) {}

    private static final ThreadLocal<TenantData> CONTEXT = new ThreadLocal<>();

    private TenantContext() {}

    public static void set(String userId, String userEmail, String userName) {
        CONTEXT.set(new TenantData(userId, userEmail, userName));
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

    public static void clear() {
        CONTEXT.remove();
    }
}
