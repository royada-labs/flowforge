# Guía De Migración (Estilo Prompt)

Esta guía está diseñada para copiar/pegar en un agente IA (o usarla como checklist interno) y migrar un sistema que usa FlowForge legacy hacia la versión tipada y endurecida actual.

## Prompt Base (Copiar/Pegar)

```text
Actúa como arquitecto Java senior especializado en migraciones de librerías.

Contexto:
- Mi sistema usa una versión anterior de FlowForge.
- Necesito migrar a la versión tipada actual, sin romper comportamiento funcional.
- Quiero eliminar patrones inseguros y dejar el código compatible con:
  - TaskDefinition tipado
  - FlowKey tipado
  - DSL fluido tipado (flow/startTyped/then/fork/join/parallel)
  - Resolución segura por method references con firma completa
  - Registro seguro de tasks sin colisiones silenciosas

Objetivo:
1) Auditar el código y detectar uso de APIs legacy o inseguras.
2) Proponer y aplicar cambios de migración por fases.
3) Ejecutar tests y validar no regresiones.
4) Entregar diff resumido + riesgos remanentes.

Reglas obligatorias:
- No uses fallback por nombre para resolver method references.
- No permitas colisiones de TaskDefinition.
- Mantén tipado fuerte end-to-end.
- Toda inferencia/reflexión en startup/build-time; cero overhead extra en runtime.
- Si una root task requiere input no-void, validar que execute(...) reciba input compatible.
- Si roots son void, permitir compatibilidad (input extra ignorado).

Checklist de migración:
Fase 1: Inventario
- Lista workflows, tasks, handlers y puntos de entrada execute/executeResult.
- Identifica tasks sobrecargadas, anotaciones @FlowTask y uso de method references.

Fase 2: Contrato tipado
- Reemplaza usos ambiguos por TaskDefinition<I,O> consistentes.
- Verifica que cada task tenga input/output coherente entre metadata y runtime.
- Asegura acceso a contexto solo por FlowKey<T>.

Fase 3: DSL
- Migra definiciones a:
  - dsl.startTyped(TASK) o dsl.flow(MyTasks::method)
  - .then(...)
  - .fork(...)
  - .join(...)
  - .parallel(...)
- Elimina helpers o wrappers legacy redundantes.

Fase 4: Registro y resolución
- Garantiza registro seguro sin sobrescritura silenciosa.
- Method references: resolver por firma completa (implClass+method+descriptor).
- Eliminar cualquier fallback heurístico por nombre.

Fase 5: Root input
- Revisar workflows con root input no-void:
  - Añadir/ajustar llamadas execute(workflowId, input) con tipo correcto.
- Revisar workflows root void:
  - Mantener compatibilidad.

Fase 6: Tests obligatorios
- Unit tests:
  - colisión de TaskDefinition => excepción clara
  - method references sobrecargadas => resolución correcta
  - mismatch metadata vs runtime => fallo en startup
- Integración:
  - secuencial, fork/join, parallel
  - validación de root input (correcto/incorrecto)
- Ejecutar suite completa y reportar resultado.

Formato de salida esperado:
1) Hallazgos (severidad alta/media/baja)
2) Cambios aplicados (archivo + resumen)
3) Antes vs Después (API y comportamiento)
4) Evidencia de tests
5) Riesgos remanentes y próximos pasos
```

## Mapeo Rápido Legacy -> Nuevo

1. Definición de task
- Antes: definiciones ambiguas o acopladas a IDs sueltos.
- Ahora: `TaskDefinition<I, O>` como contrato explícito.

2. Acceso a contexto
- Antes: acceso no tipado o conversiones manuales.
- Ahora: `FlowKey<T>` (`taskDefinition.outputKey()` + `ctx.get/getOrThrow`).

3. DSL
- Antes: composición con patrones menos estrictos.
- Ahora: composición tipada con `startTyped/flow -> then -> fork/join/parallel`.

4. Registro/resolución
- Antes: potencial fallback por nombre.
- Ahora: resolución estricta por firma completa + validaciones de colisión.

## Prompt Corto (Para Uso Diario)

```text
Migra este módulo a FlowForge tipado actual:
- elimina patrones legacy/ambiguos,
- usa DSL tipada (flow/startTyped/then/fork/join/parallel),
- valida root input no-void en execute(...),
- asegura method refs por firma completa sin fallback por nombre,
- agrega tests de colisión, overload y consistencia metadata/runtime,
- ejecuta tests y entrégame un resumen Before/After.
```
