package io.flowforge.workflow.instance;

public enum TaskStatus {
    PENDING, // aún no elegible
    READY, // dependencias satisfechas
    RUNNING, // en ejecución
    COMPLETED, // éxito
    FAILED, // error no opcional
    SKIPPED // opcional + dependencia fallida
}