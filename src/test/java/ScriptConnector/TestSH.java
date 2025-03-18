package ScriptConnector;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.BufferedReader;
import java.io.InputStreamReader;

import static org.junit.jupiter.api.Assertions.*;

public class TestSH {

    private String scriptPath;

    @BeforeEach
    public void setUp() {
        // Spécifiez le chemin de votre script shell ici
        scriptPath = "C:\\midpoint\\var\\scripts\\script.sh"; // Modifiez ce chemin si nécessaire
    }

    @Test
    public void testCreateOperation() {
        try {
            // Utilisation du chemin complet vers Bash
            ProcessBuilder processBuilder = new ProcessBuilder("C:\\Program Files\\Git\\bin\\bash.exe", scriptPath, "create", "testUser");
            processBuilder.redirectErrorStream(true);
            Process process = processBuilder.start();

            int exitCode = process.waitFor();

            // Vérifiez que le code de sortie est 0, ce qui signifie que l'exécution a réussi
            assertEquals(0, exitCode, "Le script create n'a pas réussi");

            // Capturez et affichez la sortie du script pour le débogage
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    System.out.println(line);  // Affiche la sortie du script
                }
            }

        } catch (Exception e) {
            fail("L'opération create a échoué: " + e.getMessage());
        }
    }

    @Test
    public void testUpdateOperation() {
        try {
            // Utilisation du chemin complet vers Bash
            ProcessBuilder processBuilder = new ProcessBuilder("C:\\Program Files\\Git\\bin\\bash.exe", scriptPath, "update", "testUser");
            processBuilder.redirectErrorStream(true);
            Process process = processBuilder.start();

            int exitCode = process.waitFor();

            // Vérifiez que le code de sortie est 0, ce qui signifie que l'exécution a réussi
            assertEquals(0, exitCode, "Le script update n'a pas réussi");

            // Capturez et affichez la sortie du script pour le débogage
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    System.out.println(line);  // Affiche la sortie du script
                }
            }

        } catch (Exception e) {
            fail("L'opération update a échoué: " + e.getMessage());
        }
    }

    @Test
    public void testDeleteOperation() {
        try {
            // Utilisation du chemin complet vers Bash
            ProcessBuilder processBuilder = new ProcessBuilder("C:\\Program Files\\Git\\bin\\bash.exe", scriptPath, "delete", "testUser");
            processBuilder.redirectErrorStream(true);
            Process process = processBuilder.start();

            int exitCode = process.waitFor();

            // Vérifiez que le code de sortie est 0, ce qui signifie que l'exécution a réussi
            assertEquals(0, exitCode, "Le script delete n'a pas réussi");

            // Capturez et affichez la sortie du script pour le débogage
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    System.out.println(line);  // Affiche la sortie du script
                }
            }

        } catch (Exception e) {
            fail("L'opération delete a échoué: " + e.getMessage());
        }
    }
}
