package test;

import EDD.Lista;
import Modelos.Process;
import Utilidades.ProcessGenerator;

/**
 * Programa de prueba para verificar la generación de procesos con I/O
 */
public class TestProcessGeneratorIO {

    public static void main(String[] args) {
        System.out.println("╔════════════════════════════════════════════════════════════════╗");
        System.out.println("║     TEST: Generación de Procesos con Operaciones I/O          ║");
        System.out.println("╚════════════════════════════════════════════════════════════════╝\n");

        System.out.println("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━\n");
        System.out.println("TEST 1: Procesos SIN configuración I/O (método antiguo)");
        System.out.println("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━\n");
        
        Lista<Process> processesWithoutIO = ProcessGenerator.generateRandomProcesses(5);
        System.out.println(ProcessGenerator.printProcessSummary(processesWithoutIO));
        
        System.out.println("\n⚠️  PROBLEMA: IO_Ciclo = 0 significa que NUNCA se activará I/O\n");

        System.out.println("\n━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━\n");
        System.out.println("TEST 2: Procesos CON configuración I/O (método nuevo)");
        System.out.println("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━\n");
        
        Lista<Process> processesWithIO = ProcessGenerator.generateRandomProcessesWithIO(5);
        System.out.println(ProcessGenerator.printProcessSummary(processesWithIO));
        
        System.out.println("\n✅ SOLUCIÓN: IO_Ciclo > 0 permite transiciones a BLOCKED\n");

        System.out.println("\n━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━\n");
        System.out.println("TEST 3: Conjunto de ejemplo con I/O predefinido");
        System.out.println("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━\n");
        
        Lista<Process> exampleSet = ProcessGenerator.generateExampleSetWithIO();
        System.out.println(ProcessGenerator.printProcessSummary(exampleSet));

        System.out.println("\n━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━\n");
        System.out.println("TEST 4: Simulación de comportamiento de I/O");
        System.out.println("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━\n");
        
        Process testProcess = new Process(
            99, "TestApp", 0, 15, 2,
            Modelos.ProcessType.IO_BOUND, 4, 3, 100
        );
        
        System.out.println("Proceso: " + testProcess.getName());
        System.out.println("BurstTime: " + testProcess.getBurstTime());
        System.out.println("IO_Ciclo: " + testProcess.getIoCycle() + " (necesita I/O cada 4 ciclos)");
        System.out.println("IO_Duración: " + testProcess.getIoDuration() + " (I/O tarda 3 ciclos)\n");
        
        testProcess.setState(Modelos.ProcessState.RUNNING);
        
        for (int ciclo = 1; ciclo <= 15; ciclo++) {
            boolean continueExecution = testProcess.execute(ciclo);
            
            System.out.printf("Ciclo %2d: ", ciclo);
            
            if (!continueExecution) {
                System.out.println("❌ I/O REQUERIDO → Proceso debe ir a BLOCKED");
                System.out.println("         Estado esperado: BLOCKED");
                System.out.println("         IO_Remaining: " + testProcess.getIoRemaining());
                
                for (int ioCiclo = 1; ioCiclo <= testProcess.getIoDuration(); ioCiclo++) {
                    System.out.printf("         I/O Ciclo %d/%d...\n", 
                        ioCiclo, testProcess.getIoDuration());
                }
                
                System.out.println("         ✅ I/O completado → Proceso regresa a READY\n");
            } else {
                System.out.printf("✅ Ejecutando (Restante: %d, CurrentCycle: %d)\n",
                    testProcess.getRemainingTime(),
                    testProcess.getCurrentCycle());
            }
            
            if (testProcess.getRemainingTime() == 0) {
                System.out.println("\n🎉 Proceso TERMINADO");
                break;
            }
        }

        System.out.println("\n━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━\n");
        System.out.println("TEST 5: Procesos intensivos en I/O vs CPU");
        System.out.println("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━\n");
        
        System.out.println(">>> Procesos INTENSIVOS en I/O:");
        Lista<Process> ioIntensive = ProcessGenerator.generateIOIntensiveProcesses(3);
        System.out.println(ProcessGenerator.printProcessSummary(ioIntensive));
        
        System.out.println("\n>>> Procesos INTENSIVOS en CPU:");
        Lista<Process> cpuIntensive = ProcessGenerator.generateCPUIntensiveProcesses(3);
        System.out.println(ProcessGenerator.printProcessSummary(cpuIntensive));

        System.out.println("\n╔════════════════════════════════════════════════════════════════╗");
        System.out.println("║                    TESTS COMPLETADOS                           ║");
        System.out.println("╚════════════════════════════════════════════════════════════════╝");
    }
}
