package im.turms.turms.plugin;

import org.pf4j.ExtensionPoint;
import org.springframework.context.ApplicationContext;

public abstract class TurmsExtension implements ExtensionPoint {
    private ApplicationContext context;

    public ApplicationContext getContext() {
        return context;
    }

    public void setContext(ApplicationContext context) throws Exception {
        this.context = context;
    }
}
