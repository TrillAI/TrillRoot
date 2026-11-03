package com.trillai.trillroot.core;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/** A documented route to evaluate, not a script or a compatibility guarantee. */
public final class Route {
    public final String id;
    public final String title;
    public final String category;
    public final String summary;
    public final String suitability;
    public final List<String> requirements;
    public final List<String> steps;
    public final String warning;
    public final String documentation;
    public final String documentationLabel;
    public final String readOnlyCommand;

    public Route(String id, String title, String category, String summary, String suitability,
                 List<String> requirements, List<String> steps, String warning,
                 String documentation, String documentationLabel, String readOnlyCommand) {
        this.id = id;
        this.title = title;
        this.category = category;
        this.summary = summary;
        this.suitability = suitability;
        this.requirements = Collections.unmodifiableList(Arrays.asList(requirements.toArray(new String[0])));
        this.steps = Collections.unmodifiableList(Arrays.asList(steps.toArray(new String[0])));
        this.warning = warning;
        this.documentation = documentation;
        this.documentationLabel = documentationLabel;
        this.readOnlyCommand = readOnlyCommand;
    }
}
