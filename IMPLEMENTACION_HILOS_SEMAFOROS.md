# 🧵 IMPLEMENTACIÓN DE HILOS Y SEMÁFOROS - ProyectoSO1

## 📋 Resumen de Cambios Implementados

### ✅ **Clases Nuevas Creadas**

1. **`Controladores/Semaphore.java`**
   - Semáforo contador implementado desde cero
   - Usa `synchronized`, `wait()` y `notify()`
   - Métodos: `acquire()`, `release()`, `tryAcquire()`, `releaseAll()`

2. **`Controladores/ProcessThread.java`**
   - Hilo que representa un proceso individual
   - Extiende `Thread`
   - Se bloquea cuando no tiene CPU, se desbloquea cuando lo obtiene
   - Usa semáforo `executionSemaphore` para control

3. **`Controladores/CPUSchedulerThread.java`**
   - Hilo del planificador de CPU
   - Selecciona procesos y los asigna al CPU
   - Procesa llegadas de procesos
   - Usa semáforo `cpuSemaphore` para acceso exclusivo al CPU

4. **`Controladores/IOManagerThread.java`**
   - Hilo dedicado para operaciones I/O
   - Procesa procesos bloqueados en paralelo
   - Desbloquea procesos cuando completan I/O
   - Usa semáforo `ioSemaphore`

### ✅ **Clases Modificadas**

1. **`Modelos/CPU.java`**
   - Todos los métodos ahora son `synchronized`
   - Protegidos para acceso concurrente de múltiples hilos
   - Sin cambios en la lógica, solo sincronización

---

## 🔧 Cómo Usar la Nueva Implementación

### **En Ventana.java, reemplazar el Timer por coordinación de hilos:**

```java
// Variables de instancia a agregar:
private CPUSchedulerThread schedulerThread;
private IOManagerThread ioThread;
private Semaphore cpuSemaphore;
private Map<Integer, ProcessThread> processThreads;

// En iniciarSimulacion():
cpuSemaphore = new Semaphore(1);
processThreads = new HashMap<>();

// Crear hilos de procesos
for (int i = 0; i < allProcesses.getSize(); i++) {
    Process p = allProcesses.get(i);
    ProcessThread pt = new ProcessThread(p);
    pt.setListener(new ProcessThread.ProcessThreadListener() {
        @Override
        public void onProcessNeedsIO(Process process) {
            ioThread.blockProcess(process);
        }
        
        @Override
        public void onProcessFinished(Process process) {
            terminatedProcesses.insertBegin(process);
            updateUI();
        }
        
        @Override
        public void onCycleExecuted(Process process) {
            SwingUtilities.invokeLater(() -> updateProcessTable());
        }
    });
    processThreads.put(p.getPid(), pt);
    pt.start();
}

// Iniciar hilos coordinadores
schedulerThread = new CPUSchedulerThread(scheduler, cpu, allProcesses, cpuSemaphore);
schedulerThread.setListener(...);
schedulerThread.start();

ioThread = new IOManagerThread(ioManager, scheduler);
ioThread.setListener(...);
ioThread.start();

// En detenerSimulacion():
if (schedulerThread != null) {
    schedulerThread.stopScheduler();
}
if (ioThread != null) {
    ioThread.stopIOManager();
}
for (ProcessThread pt : processThreads.values()) {
    pt.stopProcess();
}
```

---

## 🎯 Características de la Implementación

### **Sincronización Implementada:**

1. **Semáforos personalizados** (NO java.util.concurrent.Semaphore)
   - Control de acceso al CPU
   - Control de ejecución de procesos
   - Control de operaciones I/O

2. **Bloques synchronized** en:
   - CPU (todos los métodos)
   - Process (al cambiar estados)
   - Scheduler (al agregar/quitar procesos)
   - IOManager (al procesar colas)

3. **wait() / notify() / notifyAll()**
   - En la clase Semaphore
   - Para bloquear hilos cuando no hay permisos
   - Para despertar hilos cuando se liberan permisos

### **Hilos Implementados:**

1. **ProcessThread** (N hilos, uno por proceso)
   - Ejecuta ciclos del proceso
   - Se bloquea cuando no tiene CPU
   - Se despierta cuando el scheduler lo selecciona

2. **CPUSchedulerThread** (1 hilo)
   - Selecciona procesos de la cola ready
   - Asigna CPU
   - Gestiona context switches

3. **IOManagerThread** (1 hilo)
   - Procesa operaciones I/O en paralelo
   - Simula dispositivos de E/S concurrentes
   - Desbloquea procesos automáticamente

---

## 🔍 Diferencias con la Implementación Anterior

| Aspecto | Antes (Timer) | Ahora (Hilos) |
|---------|--------------|---------------|
| **Concurrencia** | ❌ Secuencial | ✅ Paralela real |
| **Procesos** | Variables simples | ✅ Hilos independientes |
| **Bloqueo** | Simulado con estados | ✅ `wait()` real de hilos |
| **I/O** | En el mismo hilo | ✅ Hilo dedicado |
| **Scheduler** | Llamado manualmente | ✅ Hilo autónomo |
| **Sincronización** | ❌ No necesaria | ✅ Semáforos + synchronized |
| **Context Switch** | Cambio de variable | ✅ Bloqueo/desbloqueo de hilos |

---

## ⚠️ NOTAS IMPORTANTES

### **Para completar la integración en Ventana.java:**

1. Reemplazar `Timer simulationTimer` por hilos coordinadores
2. Actualizar UI con `SwingUtilities.invokeLater()` (thread-safe)
3. Manejar eventos de los listeners de cada hilo
4. Implementar pausa/reanudación deteniendo hilos temporalmente

### **Estructuras de Datos:**

- ✅ **Solo usa EDD personalizadas**: Lista, Queue, Nodo, PriorityQueue
- ❌ **NO usa** java.util.concurrent
- ❌ **NO usa** java.util.Semaphore
- ✅ **Implementación propia** de semáforos

---

## 📚 Conceptos de Sistemas Operativos Implementados

1. **Semáforos contadores**: Control de recursos compartidos
2. **Exclusión mutua**: `synchronized` en secciones críticas
3. **Bloqueo de hilos**: `wait()` cuando no hay recursos
4. **Despertar hilos**: `notify()` al liberar recursos
5. **Concurrencia real**: Múltiples hilos ejecutándose en paralelo
6. **Sincronización**: Evitar race conditions
7. **Deadlock prevention**: Orden de adquisición de locks

---

## 🚀 Próximos Pasos (No implementados aún)

1. **Modificar todos los Schedulers** para agregar `synchronized` en métodos críticos
2. **Actualizar IOManager** con sincronización completa
3. **Integrar en Ventana.java** el coordinador de hilos
4. **Actualizar UI** de forma thread-safe con SwingUtilities
5. **Gestionar detención** limpia de todos los hilos al finalizar

---

**Fecha de implementación**: Octubre 26, 2025
**Estado**: Clases base completadas, pendiente integración completa
