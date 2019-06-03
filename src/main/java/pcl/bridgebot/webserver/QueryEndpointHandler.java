package pcl.bridgebot.webserver;

import java.util.Map;

import com.google.common.base.Predicate;

public abstract class QueryEndpointHandler {
    public abstract String onFailureMessage();

    public abstract String onSuccessRedirectPath();

    public abstract boolean shouldExecute(Map<String, String> context);

    public abstract boolean execute(Map<String, String> context);

    public static QueryEndpointHandler create(String onFailureMessage, String onSuccessRedirectPath,
            Predicate<Map<String, String>> canExecute, Predicate<Map<String, String>> execute) {
        return new QueryEndpointHandler() {
            @Override
            public boolean shouldExecute(Map<String, String> context) {
                return canExecute.apply(context);
            }

            @Override
            public boolean execute(Map<String, String> context) {
                return execute.apply(context);
            }

            @Override
            public String onSuccessRedirectPath() {
                return onSuccessRedirectPath;
            }

            @Override
            public String onFailureMessage() {
                return onFailureMessage;
            }
        };
    }
}