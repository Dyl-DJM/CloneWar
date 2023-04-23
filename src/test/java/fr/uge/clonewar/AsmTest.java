package fr.uge.clonewar;

import static org.junit.jupiter.api.Assertions.*;

import java.io.IOException;
import java.nio.file.Path;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * JUnit tests for Asm class.
 *
 * @author Dylan DE JESUS & Vincent RICHARD
 *
 */
/*
public class AsmTest {

  @Nested
  public class asmInstance {
    @Test
    public void instanceCreation() throws IOException {
      var asm = new Asm(Path.of("hashesBackupFile.txt"), Path.of("jarsBackupFile.txt"));
      assertNotNull(asm);
    }
  }

  @Nested
  public class testAnalyze {

    @Test
    public void pathNotGiven() throws IOException {
      var asm = new Asm(Path.of("hashesBackupFile.txt"), Path.of("jarsBackupFile.txt"));
      assertThrows(NullPointerException.class, () -> asm.analysis(null));
    }

    @Test
    public void mapNotNull() throws IOException {
      var asm = new Asm(Path.of("hashesBackupFile.txt"), Path.of("jarsBackupFile.txt"));
      assertNotNull(asm.getMap());
    }

    @Test
    public void firstProject() throws IOException {
      var asm = new Asm(Path.of("hashesBackupFile.txt"), Path.of("jarsBackupFile.txt"));
      var analysisCompletion = asm.analysis(Path.of("src\\test\\resources\\fr\\uge\\asm\\toy-0.0.1-SNAPSHOT.jar"));
      var map = asm.getMap();
      for (var elem : map.entrySet()) {
        assertEquals(false, elem.getValue().isEmpty());
      }
      assertEquals(true, analysisCompletion);
    }

    @Test
    public void secondProject() throws IOException {
      var asm = new Asm(Path.of("hashesBackupFile.txt"), Path.of("jarsBackupFile.txt"));
      var analysisCompletion = asm
          .analysis(Path.of("src\\test\\resources\\fr\\uge\\asm\\a_little_bit_different_toy-0.0.1-SNAPSHOT.jar"));
      assertEquals(true, analysisCompletion);
    }

    @Test
    public void projectDoesntExist() throws IOException {
      var asm = new Asm(Path.of("hashesBackupFile.txt"), Path.of("jarsBackupFile.txt"));
      var analysisCompletion = asm.analysis(Path.of("FileThatDoesntExist.jar"));
      assertEquals(false, analysisCompletion);
    }
  }

  @Nested
  public class testPercentageComparison {
    @Test
    public void totallyDifferentWithoutOneAnalysis() throws IOException {
      var asm = new Asm(Path.of("hashesBackupFile.txt"), Path.of("jarsBackupFile.txt"));
      var projectOne = "src\\test\\resources\\fr\\uge\\asm\\toy-0.0.1-SNAPSHOT.jar";
      var pathOne = Path.of(projectOne);
      asm.analysis(pathOne);
      var projectTwo = "src\\test\\resources\\fr\\uge\\asm\\animal-0.0.1-SNAPSHOT.jar";
      //
      // If these tests have been performed several times, then the analysis of
      // projectTwo will have been done and the return of the asm.percentageCloning()
      // method will therefore be different. To take this into account, the following
      // condition is performed. To restart a tests phase and therefore obtain the
      // return this test really want to test (cf. the else assertion), be sure to
      // delete the asm backup files (hashesBackupFile.txt and jarsBackupFile.txt)
      // located at the root of the project.
      //
      if (asm.analyzedJars().contains("src\\test\\resources\\fr\\uge\\asm\\animal-0.0.1-SNAPSHOT.jar")) {
        assertEquals(
            "L'artéfact \"" + projectTwo + "\" a plagié 25.0 % de l'artéfact \"" + projectOne + "\".\n"
                + "Au total, 10.0 % de l'artéfact \"" + projectTwo + "\" est issu du plagiat.\n",
            asm.percentageCloning(projectOne, projectTwo));
      } else {
        assertEquals("L'artéfact \"" + projectTwo + "\" n'a pas été analysé ou ne contient pas de fichier analysable.\n",
            asm.percentageCloning(projectOne, projectTwo));
      }
    }

    @Test
    public void exacltySameProject() throws IOException {
      var asm = new Asm(Path.of("hashesBackupFile.txt"), Path.of("jarsBackupFile.txt"));
      var project = "src\\test\\resources\\fr\\uge\\asm\\toy-0.0.1-SNAPSHOT.jar";
      var path = Path.of(project);
      asm.analysis(path);
      assertEquals("L'artéfact \"" + project + "\" a plagié 100.0 % de l'artéfact \"" + project + "\".\n"
              + "Au total, 100.0 % de l'artéfact \"" + project + "\" est issu du plagiat.\n",
          asm.percentageCloning(project, project));
    }

    @Test
    public void quiteTheSame() throws IOException {
      var asm = new Asm(Path.of("hashesBackupFile.txt"), Path.of("jarsBackupFile.txt"));
      var projectOne = "src\\test\\resources\\fr\\uge\\asm\\toy-0.0.1-SNAPSHOT.jar";
      var pathOne = Path.of(projectOne);
      var projectTwo = "src\\test\\resources\\fr\\uge\\asm\\a_little_bit_different_toy-0.0.1-SNAPSHOT.jar";
      var pathTwo = Path.of(projectTwo);
      asm.analysis(pathOne);
      asm.analysis(pathTwo);
      assertEquals(
          "L'artéfact \"" + projectTwo + "\" a plagié 41.66666793823242 % de l'artéfact \"" + projectOne + "\".\n"
              + "Au total, 19.230770111083984 % de l'artéfact \"" + projectTwo + "\" est issu du plagiat.\n",
          asm.percentageCloning(projectOne, projectTwo));
    }

    @Test
    public void oneProjectNotGiven() throws IOException {
      var asm = new Asm(Path.of("hashesBackupFile.txt"), Path.of("jarsBackupFile.txt"));
      var projectOne = "src\\test\\resources\\fr\\uge\\asm\\toy-0.0.1-SNAPSHOT.jar";
      var pathOne = Path.of(projectOne);
      asm.analysis(pathOne);
      assertThrows(NullPointerException.class, () -> asm.percentageCloning(projectOne, null));
    }

    @Test
    public void totallyDifferent() throws IOException {
      var asm = new Asm(Path.of("hashesBackupFile.txt"), Path.of("jarsBackupFile.txt"));
      var projectOne = "src\\test\\resources\\fr\\uge\\asm\\toy-0.0.1-SNAPSHOT.jar";
      var pathOne = Path.of(projectOne);
      var projectTwo = "src\\test\\resources\\fr\\uge\\asm\\animal-0.0.1-SNAPSHOT.jar";
      var pathTwo = Path.of(projectTwo);
      asm.analysis(pathOne);
      asm.analysis(pathTwo);
      assertEquals("L'artéfact \"" + projectTwo + "\" a plagié 25.0 % de l'artéfact \"" + projectOne + "\".\n"
              + "Au total, 10.0 % de l'artéfact \"" + projectTwo + "\" est issu du plagiat.\n",
          asm.percentageCloning(projectOne, projectTwo));
    }

    @Test
    public void stringOfAllFilesAnalyzed() throws IOException {
      var asm = new Asm(Path.of("hashesBackupFile.txt"), Path.of("jarsBackupFile.txt"));
      var allFilesAnalyzed = "src\\test\\resources\\fr\\uge\\asm\\a_little_bit_different_toy-0.0.1-SNAPSHOT.jar\n"
          + "src\\test\\resources\\fr\\uge\\asm\\animal-0.0.1-SNAPSHOT.jar\n"
          + "src\\test\\resources\\fr\\uge\\asm\\toy-0.0.1-SNAPSHOT.jar";
      assertEquals(allFilesAnalyzed, asm.analyzedJars());
    }

    @Test
    public void oneProjectDoesntExist() throws IOException {
      var asm = new Asm(Path.of("hashesBackupFile.txt"), Path.of("jarsBackupFile.txt"));
      var projectOne = "src\\test\\resources\\fr\\uge\\asm\\toy-0.0.1-SNAPSHOT.jar";
      var pathOne = Path.of(projectOne);
      asm.analysis(pathOne);
      assertEquals("L'artéfact \"projectThatDoesntExist\" n'a pas été analysé ou ne contient pas de fichier analysable.\n",
          asm.percentageCloning(projectOne, "projectThatDoesntExist"));
    }
  }
}
*/
