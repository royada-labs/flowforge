package io.tugrandsolutions.flowforge.workflow.instance;

enum TaskStatus {
    PENDING,    // aún no elegible
    READY,      // dependencias satisfechas
    RUNNING,    // en ejecución
    COMPLETED,  // éxito
    FAILED,     // error no opcional
    SKIPPED     // opcional + dependencia fallida
}