module org.engcia {
    requires javafx.controls;
    requires javafx.fxml;
    requires org.kie.api;
    requires org.drools.core;
    requires slf4j.api;
    requires org.drools.compiler;
    requires org.kie.internal.api;
    requires reflections;
    requires org.apache.poi.poi;
    requires org.apache.poi.ooxml;
    requires com.fasterxml.jackson.databind;

    exports org.engcia;
    opens org.engcia to javafx.fxml;
    exports org.engcia.controller;
    opens org.engcia.controller to javafx.fxml;
    exports org.engcia.model;
    exports org.engcia.services;
    exports org.engcia.view;
    exports org.engcia.model.common;
}
