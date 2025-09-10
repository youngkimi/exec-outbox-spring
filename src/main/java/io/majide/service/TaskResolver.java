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

    public TaskHandler<Map<String,Object>, Map<String,Object>> resolve(TaskDef def) {
        Object beanOrObj;
        if (def.getHandlerBean() != null && !def.getHandlerBean().isBlank()) {
            beanOrObj = appCtx.getBean(def.getHandlerBean());
        } else if (def.getHandlerClass() != null && !def.getHandlerClass().isBlank()) {
            Class<?> clazz = ClassUtils.resolveClassName(def.getHandlerClass(), appCtx.getClassLoader());
            beanOrObj = factory.createBean(clazz);
        } else {
            throw new IllegalStateException("No handler configured for taskKey=" + def.getTaskKey());
        }

        if (!(beanOrObj instanceof TaskHandler<?, ?> h)) {
            throw new IllegalStateException("Handler does not implement TaskHandler: " + def);
        }

        @SuppressWarnings("unchecked")
        TaskHandler<Map<String,Object>, Map<String,Object>> typed =
                (TaskHandler<Map<String,Object>, Map<String,Object>>) h;
        return typed;
    }
}