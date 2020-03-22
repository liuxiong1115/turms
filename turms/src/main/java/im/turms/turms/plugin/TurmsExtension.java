package im.turms.turms.plugin;

import org.pf4j.ExtensionPoint;
import org.springframework.context.ApplicationContext;

public abstract class TurmsExtension implements ExtensionPoint {
    private ApplicationContext context;

    private boolean isServing;

    public ApplicationContext getContext() {
        return context;
    }

    public void setContext(ApplicationContext context) throws Exception {
        this.context = context;
    }

    public boolean isServing() {
        return isServing;
    }

    public void setServing(boolean serving) {
        isServing = serving;
    }
}
