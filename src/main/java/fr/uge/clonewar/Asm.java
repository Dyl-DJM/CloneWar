package fr.uge.clonewar;

import java.io.IOException;
import java.lang.module.ModuleFinder;
import java.lang.module.ModuleReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.stream.Collectors;

import io.helidon.nima.webserver.cors.CorsSupport;
import io.helidon.nima.webserver.cors.CrossOriginConfig;
import io.helidon.nima.webserver.http.HttpRules;
import io.helidon.nima.webserver.http.HttpService;
import io.helidon.nima.webserver.http.ServerRequest;
import io.helidon.nima.webserver.http.ServerResponse;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;

import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

/**
 * Creates a structure which is able to analyze a project given in a jar format.
 * It analyzes all the jar structure to read the byte code of all the instructions.
 *
 * @author Dylan DE JESUS & Vincent RICHARD
 *
 */
public class Asm implements HttpService  {

  /**
   * Prime number for the polynomial formula.
   */
  private static final int PRIME_NUMBER = 2;

  /**
   * Size of the blocks analyzed in the current file of the jar.
   */
  private static final int WINDOW_SIZE = 4; // We choose to analyze the files by taking 4 lines of byte code for
  // a block of instructions
  /**
   * Value of the block hash value.
   */
  private long windowHash;

  /**
   * Window representing the block of operation we are calculating the hash value.
   */
  private final ArrayList<Long> window;

  /**
   * Database of hash for all the projects we analyze.
   */
  private final HashMap<String, ArrayList<Long>> map = new HashMap<>();

  /**
   * Count the lines already visited in the block window.
   */
  private int count;

  /**
   * String path of the current file analyzed.
   */
  private String currentFile;

  /**
   * Hashes and artifacts association backup file path.
   */
  private final Path hashesBackupFile;

  /**
   * Already analyzed artifacts (jar extension files) backup file path.
   */
  private final Path jarsBackupFile;

  /**
   * List of all the analyzed project.
   */
  private final ArrayList<String> analyzedJars = new ArrayList<>();

  /**
   * CorsSupport instance used to configure cross-origin resource sharing (CORS).
   */
  private static final CorsSupport CORS_SUPPORT = CorsSupport.builder()
      .addCrossOrigin(CrossOriginConfig.builder()
          .allowOrigins("http://127.0.0.1:8080", "http://localhost:8080", "http://127.0.0.1:3000", "http://localhost:3000")
          .build())
      .build();

  /**
   * Allow to configure some server routes.
   *
   * @param httpRules HTTP rules
   */
  @Override
  public void routing(HttpRules httpRules) {
    httpRules.get("/analyze", CORS_SUPPORT, this::getAnalysis)
        .get("/analyzedJars", CORS_SUPPORT, this::getAnalyzedJars)
        .get("/percentageCloning", CORS_SUPPORT, this::getPercentageCloning);
  }

  /**
   * Constructor of an object that will be able to analyze artifacts (jar extension files).
   *
   */
  public Asm(Path hashesBackupFile, Path jarsBackupFile) {
    Objects.requireNonNull(hashesBackupFile, "hashesBackupFile is null");
    Objects.requireNonNull(jarsBackupFile, "jarsBackupFile is null");
    window = new ArrayList<Long>(WINDOW_SIZE);  // Window of hash codes (each hash code is the hash corresponding to a
                                                // byte code line)
    this.hashesBackupFile = hashesBackupFile;
    this.jarsBackupFile = jarsBackupFile;
    loadHashesBackupFile();
    loadJarsBackupFile();
  }

  /**
   * Calculates the new hash using the rolling hash system. It is a representation
   * of the Rabin's fingerprint.
   *
   * @param hash             is the old hash value already calculated.
   * @param oldFirstHashCode is the hash value of the previous first byte code
   *                         line.
   * @param blockSize        is the size of the block used to calculates the hash
   *                         value.
   * @param addedHashCode    is the value of the new hash to add to the previous
   *                         one.
   * @return the new hash value.
   */
  private static long rollingHash(long hash, long oldFirstHashCode, int blockSize, long addedHashCode) {
    return (hash - oldFirstHashCode * Math.round(Math.pow(PRIME_NUMBER, blockSize))) * PRIME_NUMBER
        + addedHashCode * PRIME_NUMBER; // We remove the value of the old first hash value and we add the new one
  }

  /**
   * Hash Function. It returns a long value which represents the hash value of a
   * block of long (hash value). The window/block is a list of hash value that
   * represents the hash code of each line of byte code.
   *
   * @param block is a list given to make the long hash value.
   * @return the long value representing the hash of the whole block.
   */
  private static long hash(ArrayList<Long> block) {
    long hashCode = 0;
    var blockSize = block.size();
    for (var i = 0; i < block.size(); i++) {
      var byteCodeLineHash = block.get(i);
      var pow_index = blockSize - i;
      hashCode = hashCode + byteCodeLineHash * Math.round(Math.pow(PRIME_NUMBER, pow_index)); // polynomial formula
    }
    return hashCode;
  }

  /**
   * Update the hash value of the current window analyzed.
   *
   * @param addingHash is the new instruction hash code to add.
   */
  private void newHash(long addingHash) {
    if (count == WINDOW_SIZE) { // We have the window fulfilled for the first time
      windowHash = hash(window); // we calculate the hash of the whole block
      count++;
      return;
    }
    if (window.size() == WINDOW_SIZE) { // The window was already fulfilled
      windowHash = rollingHash(windowHash, window.remove(0), WINDOW_SIZE, addingHash); // Use of the rolling hash system
      window.add(addingHash);
      return;
    }
    window.add(addingHash);
    count++;
  }

  /**
   * Pushes the tuple of data to the map being the database.
   *
   * @param filename is the string path of the file the data belongs to.
   */
  private void putMap(String filename, ArrayList<Long> block) {
    map.put(filename, block);
  }

  /**
   * Add the hash of the current block if it doesn't worth 0.
   *
   * @param hashTable is the list of hash codes.
   */
  private void addHash(ArrayList<Long> hashTable) {
    if (windowHash != 0) {
      hashTable.add(windowHash);
    }
  }

  /**
   * Returns the database created thanks to the analyze of the project.
   *
   * @return the database which is a hash map of the files string path of the
   *         project.
   */
  HashMap<String, ArrayList<Long>> getMap() { // Mainly used to check the tests.
    return map;
  }

  /**
   * Calculates the result of the comparison between two lists of long values and
   * returns a string representation of the result. The formula returns the
   * percentage of elements found in the two given lists.
   *
   * @param projectNameReference   is the string path of the reference project.
   * @param projectNameClone       is the string path of the project we compare to
   *                               the reference one.
   * @param hashesProjectReference is the list of hash codes for the project of
   *                               reference.
   * @param hashesProjectClone     is the list of hash codes for the
   * @return the string representation of the comparison.
   */
  private static String resultComparison(String projectNameReference, String projectNameClone,
                                         ArrayList<Long> hashesProjectReference, ArrayList<Long> hashesProjectClone) {
    if (hashesProjectReference.isEmpty()) {
      return "L'artéfact \"" + projectNameReference + "\" n'a pas été analysé ou ne contient pas de fichier analysable.\n";
    }
    if (hashesProjectClone.isEmpty()) {
      return "L'artéfact \"" + projectNameClone + "\" n'a pas été analysé ou ne contient pas de fichier analysable.\n";
    }
    var hashesReference = new ArrayList<>(hashesProjectReference);
    var hashesClone = new ArrayList<>(hashesProjectClone);

    var tmp = new ArrayList<>(hashesProjectReference);
    tmp.retainAll(hashesClone);
    var commonHashes = tmp;

    var nbHashesReference = hashesReference.size(); // amount of hashes in the first project
    var nbHashesClone = hashesClone.size(); // amount of hashes in the second project

    hashesClone.retainAll(commonHashes);
    var cloneHashesInClone = hashesClone;

    hashesReference.retainAll(commonHashes);
    var cloneHashesInReference = hashesReference;

    double clonesFromReferencePercentage = nbHashesReference == 0 ? 0
        : cloneHashesInReference.size() * 100 / Integer.valueOf(nbHashesReference).floatValue();
    double totalClonesPercentage = nbHashesClone == 0 ? 0
        : cloneHashesInClone.size() * 100 / Integer.valueOf(nbHashesClone).floatValue();

    return "L'artéfact \"" + projectNameClone + "\" a plagié " + clonesFromReferencePercentage + " % de l'artéfact \""
        + projectNameReference + "\".\n" + "Au total, " + totalClonesPercentage + " % de l'artéfact \"" + projectNameClone
        + "\" est issu du plagiat.\n";
  }

  /**
   * Calculates a percentage of clone code between two jar project. Makes a String
   * representation of the result statement of the comparison between the
   * projects.
   *
   * @return a string representation of the comparison result of cloning between
   *         two jar file.
   */
  public String percentageCloning(String projectLocationReference, String projectLocationClone) {
    Objects.requireNonNull(projectLocationReference, "projectLocationReference is null");
    Objects.requireNonNull(projectLocationClone, "projectLocationClone is null");
    var hashReference = new ArrayList<Long>();
    var hashClone = new ArrayList<Long>();
    map.keySet().forEach(elem -> {
      if (elem.startsWith(projectLocationClone + " : ")) { // If its belongs to the project then we add all the hash
        // values
        hashClone.addAll(map.get(elem));
      }
      if (elem.startsWith(projectLocationReference + " : ")) { // If its belongs to the project then we add all the hash
        // values
        hashReference.addAll(map.get(elem));
      }
    });
    return resultComparison(projectLocationReference, projectLocationClone, hashReference, hashClone);
  }

  /**
   * Process the request to calculate percentage of cloning between two artifacts..
   *
   * @param req serveur request
   * @param res serveur response
   */
  private void getPercentageCloning(ServerRequest req, ServerResponse res) {
    var projectLocationReferenceOptional = req.query().first("referenceArtifact");
    var projectLocationCloneOptional = req.query().first("cloneArtifact");
    if (projectLocationReferenceOptional.isEmpty()) {
      res.send("Veuillez fournir le chemin absolu d'un artéfact de référence pour pouvoir réaliser une comparaison");
    } else if (projectLocationCloneOptional.isEmpty()) {
      res.send("Veuillez fournir le chemin absolu d'un artéfact à vérifier pour pouvoir réaliser une comparaison");
    } else {
      var projectLocationReference = projectLocationReferenceOptional.get();
      var projectLocationClone = projectLocationCloneOptional.get();
      res.send(percentageCloning(projectLocationReference, projectLocationClone));
    }
  }

  /**
   * Creates a MethodVisitor object. This object is made to visit the various
   * cases of byte code line we can meet.
   *
   * @param hashTable is the list where we stock the hash.
   * @return a MethodVisitor instance, that calculates the hash code of each byte
   *         code line encountered in a method.
   */
  private MethodVisitor methodVisitor(ArrayList<Long> hashTable) {
    return new MethodVisitor(Opcodes.ASM9) {

      /**
       * Visits the node of the call of a method. A new method declaration has been
       * found.
       */
      @Override
      public void visitCode() {
        window.clear(); // clear for the usage of it
        windowHash = 0; // clear of the current hash value
        count = 0;
      }

      /**
       * Visits a zero operand instruction. It is an instruction without any argument
       * to add like AALOAD, RETURN...
       */
      @Override
      public void visitInsn(int opcode) {
        newHash(opcode); // The type of the instruction is important, RETURN instruction and AALOAD are
        // too different
        addHash(hashTable);
      }

      /**
       * Visits the instructions that invoke another method.
       */
      @Override
      public void visitMethodInsn(int opcode, String owner, String name, String descriptor, boolean isInterface) {
        Objects.requireNonNull(name, "name is null");
        Objects.requireNonNull(descriptor, "descriptor is null");
        long hashCode = opcode + owner.hashCode(); // The least we take for the hash value is the...
        hashCode = hashCode + (name.startsWith("java/") ? name.hashCode() : 0); // We keep the method name in the hash
        // value only if it starts with java.*
        newHash(hashCode);
        addHash(hashTable);
      }

      /**
       * Visit a single instruction using an integer type of value.
       */
      @Override
      public void visitIntInsn(final int opcode, final int operand) {
        newHash(opcode + operand); // The operand is important for the meaning
        addHash(hashTable);
      }

      /**
       * Visit instruction using local variables declared inside the current method
       * analyzed.
       */
      @Override
      public void visitVarInsn(final int opcode, final int varIndex) {
        newHash(opcode); // The place of the local variable in the stack isn't important
        addHash(hashTable);
      }

      /**
       * Visit instructions that uses type like NEW, EXCEPTION, INSTANCEOF ...
       */
      @Override
      public void visitTypeInsn(final int opcode, final String type) {
        newHash(opcode + type.hashCode());
        addHash(hashTable);
      }

      /**
       * Visit field instruction like stores of field load...
       */
      @Override
      public void visitFieldInsn(final int opcode, final String owner, final String name, final String descriptor) {
        Objects.requireNonNull(owner, "owner is null");
        newHash(opcode + owner.hashCode()); // The name isn't important, the name don't give any clue of cloning.
        addHash(hashTable);
      }

      /**
       * Visits the instructions that represents the jumps to other byte code
       * instructions.
       */
      @Override
      public void visitJumpInsn(final int opcode, final Label label) {
        newHash(opcode); // We don't focus on the label where it jumps
        addHash(hashTable);
      }

      /**
       * Visit the LDC type instructions.
       */
      @Override
      public void visitLdcInsn(final Object value) {
        Objects.requireNonNull(value, "value is null");
        @SuppressWarnings("preview")
        String stringVersion = switch(value) {
          case String s -> "\"" + s + "\"";
          default -> value.toString();
        };
        newHash(Opcodes.LDC + stringVersion.hashCode());
        addHash(hashTable);
      }

      /**
       * Visit the integer incremental instructions.
       */
      @Override
      public void visitIincInsn(final int varIndex, final int increment) {
        newHash(Opcodes.IINC + increment); // We don't care about the place of the variable in the stack, the increment
        // value can be a clue in some cases
        addHash(hashTable);
      }

      /**
       * Visit the TABLESWITCH instructions.
       */
      @Override
      public void visitTableSwitchInsn(final int min, final int max, final Label dflt, final Label... labels) {
        newHash(Opcodes.TABLESWITCH + min + max + dflt.hashCode()); // The label doesn't matter
        addHash(hashTable);
      }

      /**
       * Called when the method visitor reaches the end of the current method.
       */
      @Override
      public void visitEnd() {
        if (count < WINDOW_SIZE) { // Case where the block size is lower than the amount of byte code instructions
          // in the method
          windowHash = hash(window);
          addHash(hashTable);
        }
      }
    };
  }

  /**
   * Runs through the tree structure of a class. It takes a module reader and
   * visit all the class methods.
   *
   * @param reader    is the module reader on the current jar.
   * @param filename  is a string name of a the file/.class we want to open a
   *                  class reader on.
   * @param hashTable is the list where the hash of the class analyzed are
   *                  stocked.
   * @throws IOException if there is a problem in the reading of the class.
   */
  private void classReader(ModuleReader reader, String filename, ArrayList<Long> hashTable) throws IOException {
    try (var inputStream = reader.open(filename).orElseThrow()) {
      var classReader = new ClassReader(inputStream); // Make a ClassReader
      classReader.accept(new ClassVisitor(Opcodes.ASM9) { // We put a classVisitor in the method of the class Reader to
        // visit the class
        /**
         * Methods case. We reach a method declaration.
         */
        @Override
        public MethodVisitor visitMethod(int access, String name, String descriptor, String signature,
                                         String[] exceptions) {
          return methodVisitor(hashTable); // Call a method visitor
        }
      }, 0);
    }
  }

  /**
   * Visit the tree structure of a given jar. It visits all the nodes of the
   * structure created on the jar given and fills the database with the hash
   * calculated on the byte code instructions encountered.
   *
   * @param jarLocation is the string path of the jar we want to visit the tree
   *                    structure.
   * @throws IOException            if an issue occurred due to the file system.
   * @throws NoSuchElementException if no module has been found.
   */
  private void treeVisit(Path jarLocation) throws IOException, NoSuchElementException {
    // The following instruction is used to re-analyze a jar that has been modified but which is at the same location.
    // All the information concerning the old jar will be erased in order to be able to do a new analysis of it.
    // Therefore, nly the hashes of the new jar will be associated with it in the map.
    map.entrySet().removeIf(entry -> entry.getKey().startsWith(jarLocation + " : "));
    var finder = ModuleFinder.of(jarLocation); // Find the module name of the .jar
    var moduleReference = finder.findAll().stream().findFirst().orElseThrow(); // Array with the module,
                                                                                              // and the location.
    try (var reader = moduleReference.open()) { // Makes a JarReader
      for (var filename : (Iterable<String>) reader.list()::iterator) { // We visit all the META-INF directory
        if (!filename.endsWith(".class")) { // if the file isn't a .class we break
          continue;
        }
        var hashTable = new ArrayList<Long>(); // List which will stock all the hash of the current file analyzed.
        currentFile = filename;
        classReader(reader, filename, hashTable); // Open a class Reader on the current .class file.
        saveHashesInBackupFile(jarLocation + " : " + currentFile, hashTable); // Stock the analysis
        putMap(jarLocation + " : " + currentFile, hashTable); // This adds the hash codes into the database
      }
    }
  }

  /**
   * Launch the analysis on the jar located at the string path given. If the
   * analysis has been fully completed then this function returns true otherwise
   * it returns false.
   *
   * @param jarLocation is the string of the path location of the jar file.
   * @return a boolean describing if the analysis has been fully completed or not.
   */
  public boolean analysis(Path jarLocation) {
    Objects.requireNonNull(jarLocation, "jarLocation is null");
    try {
      treeVisit(jarLocation); // Launch the analysis
      if(!analyzedJars.contains(jarLocation.toString())) {
        analyzedJars.add(jarLocation.toString());
        saveJarsBackupFile(jarLocation.toString());
      }
      return true;
    } catch (IOException e) { // An IOException occurred the analysis has failed
      return false;
    } catch (NoSuchElementException e) { // A NoSuchElementException occurred the analysis has failed
      return false;
    }
  }

  /**
   * Process the request to perform an analysis.
   *
   * @param req serveur request
   * @param res serveur response
   */
  private void getAnalysis(ServerRequest req, ServerResponse res) {
    var jarPathToAnalyzeOptional = req.query().first("jarPathToAnalyse");
    if (jarPathToAnalyzeOptional.isEmpty()) {
      res.send("Veuillez fournir le chemin absolu d'un artéfact à analyser");
    } else {
      var jarPathToAnalyze = jarPathToAnalyzeOptional.get();
      var jarLocation = Path.of(jarPathToAnalyze);
      if (analysis(jarLocation)) {
        res.send("L'analyse de l'artéfact \"" + jarPathToAnalyze + "\" est terminée et s'est déroulée correctement. Le résultat de cette analyse a été sauvegardé.");
      } else {
        res.send("L'analyse de l'artéfact \"" + jarPathToAnalyze + "\" a échoué. Veuillez vous assurer que l'emplacement de cet artéfact est correct et que celui-ci est bien accessible. Il se peut également qu'un problème soit survenue lors de l'accès aux fichiers de sauvegarde afin de sauvegarder le résultat de l'analyse.");
      }
    }
  }

  /**
   * Stocks the current state of the map of hashes files analysis in the save file.
   *
   * @param filename is the filename of the class analyzed.
   * @param hashes is the list of hashes for the file analyzed.
   */
  private void saveHashesInBackupFile(String filename, ArrayList<Long> hashes) throws IOException {
    var line = filename + "\n" + hashes.stream().map(String :: valueOf).collect(Collectors.joining(" ")) + "\n";
    Files.writeString(hashesBackupFile, line,  StandardCharsets.UTF_8, StandardOpenOption.APPEND, StandardOpenOption.CREATE);
  }

  /**
   * Retrieves the previous data of the files analyzed.
   */
  private void loadHashesBackupFile() {
    try (var reader = Files.newBufferedReader(hashesBackupFile)) { // Open the reader and close it
      String key;
      String rawValue;
      ArrayList<Long> value;
      while ((key = reader.readLine()) != null && (rawValue = reader.readLine()) != null) {
        value = new ArrayList<>(Arrays.stream(rawValue.split(" ")).map(Long::parseLong).toList());
        map.put(key, value);
      }
    } catch (IOException e) { // Issue in the reading, we stop the load
      System.err.println("Aucune sauvegarde précédente n'a été trouvée. Sinon, une erreur est survenue lors de l'accès au fichier de sauvegarde \"" + hashesBackupFile + "\" afin de restaurer les hashs asociés aux artéfacts déjà analysés.");
    }
  }

  /**
   * Retrieves the previous data of the jars projects analyzed.
   */
  private void loadJarsBackupFile() {
    try (var reader = Files.newBufferedReader(jarsBackupFile)) { // Open the reader and close it
      String line;
      while ((line = reader.readLine()) != null) {
        analyzedJars.add(line);
      }
    } catch (IOException e) { // Issue in the reading, we stop the load
      System.err.println("Aucune sauvegarde précédente n'a été trouvée. Sinon, une erreur est survenue lors de l'accès au fichier de sauvegarde \"" + jarsBackupFile + "\" afin de restaurer la liste des artéfacts déjà analysés.");
    }
  }

  /**
   * Stocks the current state of the list of jars analysis in the save file.
   *
   * @param jar is the string name of the jar.
   */
  private void saveJarsBackupFile(String jar) throws IOException {
    var line = jar + "\n";
      Files.writeString(jarsBackupFile, line,  StandardCharsets.UTF_8, StandardOpenOption.APPEND, StandardOpenOption.CREATE);
  }

  /**
   * Makes a string representation of the file string paths already analyzed.
   *
   * @return the string representation of the jars analyzed.
   */
  public String analyzedJars() {
    return analyzedJars.stream().sorted().collect(Collectors.joining("\n"));
  }

  /**
   * Process the request to get string representation of all analyzed artifacts (analyzed jars).
   *
   * @param req serveur request
   * @param res serveur response
   */
  private void getAnalyzedJars(ServerRequest req, ServerResponse res) {
    res.send(analyzedJars());
  }
}
