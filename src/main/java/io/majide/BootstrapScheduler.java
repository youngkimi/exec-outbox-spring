// src/main/java/io/youngkimi/springoutbox/BootstrapScheduler.java
package io.youngkimi.springoutbox;

import io.youngkimi.springoutbox.service.DagService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.*;

@Component
public class BootstrapScheduler {
	private static final Logger log = LoggerFactory.getLogger(BootstrapScheduler.class);

}
