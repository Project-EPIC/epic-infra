package edu.colorado.cs.epic.eventsapi.tasks;

import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.Iterables;
import edu.colorado.cs.epic.eventsapi.api.Event;
import edu.colorado.cs.epic.eventsapi.core.DatabaseController;
import edu.colorado.cs.epic.eventsapi.core.DataprocController;
import edu.colorado.cs.epic.eventsapi.core.KubernetesController;
import io.dropwizard.servlets.tasks.Task;
import io.kubernetes.client.ApiException;
import org.apache.log4j.Logger;

import java.io.PrintWriter;
import java.util.*;

/**
 * Created by admin on 19/3/19.
 */
public class SyncEventsTask extends Task {

    private final Logger logger;
    private final DataprocController dataprocController;
    private KubernetesController k8scontroller;
    private DatabaseController dbcontroller;

    public SyncEventsTask(KubernetesController k8scontroller, DatabaseController dbcontroller, DataprocController dataprocController) {
        super("sync");
        this.k8scontroller = k8scontroller;
        this.dbcontroller = dbcontroller;
        this.dataprocController = dataprocController;
        this.logger = Logger.getLogger(SyncEventsTask.class.getName());
    }

    @Override
    public void execute(ImmutableMultimap<String, String> immutableMultimap, PrintWriter printWriter) throws Exception {
        List<Event> k8sEventsList = k8scontroller.getActiveEvents();
        List<Event> dbActiveList = dbcontroller.getActiveEvents();
        Collection<String> authors = immutableMultimap.asMap().getOrDefault("author", Collections.singleton("system@epic.cs.colorado.edu"));
        Set<Event> toBeStopped = new HashSet<>(k8sEventsList);
        toBeStopped.removeAll(dbActiveList);
        Set<Event> toBeStarted = new HashSet<>(dbActiveList);
        toBeStarted.removeAll(k8sEventsList);

        for (Event event : toBeStopped) {
            try {
                k8scontroller.stopEvent(event.getNormalizedName());
                dbcontroller.pauseEvent(event.getNormalizedName(), authors.iterator().next());
                String out = String.format("Stopped event filter for %s", event.getNormalizedName());
                logger.info(out);
                if (printWriter != null) {
                    printWriter.println(out);
                }
                dataprocController.startCollectTemplate(event.getNormalizedName());
            }  catch (ApiException e) {
                dbcontroller.setStatus(event.getNormalizedName(), Event.Status.FAILED);
                String out = String.format("Failed to stop filter for %s", event.getNormalizedName());
                logger.error(out, e);
                if (printWriter != null) {
                    printWriter.println(out);
                }
                throw e;
            }
        }

        for (Event event : toBeStarted) {
            try {
                k8scontroller.startEvent(event);
                dbcontroller.startEvent(event.getNormalizedName(), authors.iterator().next());
                String out = String.format("Started event filter for %s", event.getNormalizedName());
                logger.info(out);
                if (printWriter != null) {
                    printWriter.println(out);
                }
            } catch (ApiException e) {
                dbcontroller.setStatus(event.getNormalizedName(), Event.Status.FAILED);
                String out = String.format("Failed to start filter for %s", event.getNormalizedName());
                logger.error(out, e);
                if (printWriter != null) {
                    printWriter.println(out);
                }
                throw e;
            }
        }

        try {
            k8scontroller.setActiveStreamValues(dbcontroller.getActiveKeywords(), dbcontroller.getActiveFollows());
        } catch (ApiException e) {
            String out = "Failed to update keywords";
            logger.error(out, e);
            if (printWriter != null) {
                printWriter.println(out);
            }
            throw e;
        }


    }
}
