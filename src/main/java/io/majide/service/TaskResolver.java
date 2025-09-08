// com/example/orch/service/TaskResolver.java
package io.majide.service;

import io.majide.core.TaskHandler;
import io.majide.domain.TaskDef;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.config.AutowireCapableBeanFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;
import org.springframework.util.ClassUtils;

@Component
@RequiredArgsConstructor
public class TaskResolver {
    private final ApplicationContext appCtx;
    private final AutowireCapableBeanFactory factory;

    public TaskHandler<?,?> resolve(TaskDef def) {
        if (def.getHandlerBean() != null && !def.getHandlerBean().isBlank()) {
            Object bean = appCtx.getBean(def.getHandlerBean());
            return cast(bean, def);
        }
        if (def.getHandlerClass() != null && !def.getHandlerClass().isBlank()) {
            Class<?> clazz = ClassUtils.resolveClassName(def.getHandlerClass(), appCtx.getClassLoader());
            Object bean = factory.createBean(clazz);
            return cast(bean, def);
        }
        throw new IllegalStateException("No handler configured for taskKey=" + def.getTaskKey());
    }

    private TaskHandler<?,?> cast(Object o, TaskDef def) {
        if (!(o instanceof TaskHandler<?,?> h)) {
            throw new IllegalStateException("Handler does not implement TaskHandler: " + def);
        }
        return h;
    }
}
