/* DigiDoc4J library
*
* This software is released under either the GNU Library General Public
* License (see LICENSE.LGPL).
*
* Note that the only valid version of the LGPL license as far as this
* project is concerned is the original GNU Library General Public License
* Version 2.1, February 1999
*/

package org.digidoc4j;

import static org.apache.commons.lang.StringUtils.containsIgnoreCase;
import static org.digidoc4j.ContainerBuilder.BDOC_CONTAINER_TYPE;
import static org.digidoc4j.ContainerBuilder.DDOC_CONTAINER_TYPE;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.digidoc4j.exceptions.InvalidDataFileException;
import org.digidoc4j.impl.DigiDoc4JTestHelper;
import org.digidoc4j.impl.bdoc.BDocContainer;
import org.digidoc4j.impl.ddoc.DDocContainer;
import org.digidoc4j.impl.ddoc.DDocSignature;
import org.digidoc4j.testutils.TestContainer;
import org.digidoc4j.testutils.TestDataBuilder;
import org.junit.After;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

public class ContainerBuilderTest extends DigiDoc4JTestHelper {

  public static final Configuration TEST_CONFIGURATION = new Configuration(Configuration.Mode.TEST);
  private static final String BDOC_TEST_FILE = "testFiles/asics_for_testing.bdoc";
  private static final String DDOC_TEST_FILE = "testFiles/ddoc_for_testing.ddoc";

  @Rule
  public TemporaryFolder testFolder = new TemporaryFolder();


  @After
  public void tearDown() throws Exception {
    ContainerBuilder.removeCustomContainerImplementations();
  }

  @Test
  public void buildEmptyContainer() throws Exception {
    ContainerBuilder builder = ContainerBuilder.aContainer();
    Container container = builder.build();
    assertEquals("BDOC", container.getType());
    assertTrue(container.getDataFiles().isEmpty());
    assertTrue(container.getSignatures().isEmpty());
  }

  @Test
  public void buildDDocContainer() throws Exception {
    Container container = ContainerBuilder.
        aContainer(DDOC_CONTAINER_TYPE).
        build();
    assertEquals("DDOC", container.getType());
    assertTrue(container.getDataFiles().isEmpty());
    assertTrue(container.getSignatures().isEmpty());
  }

  @Test
  public void buildBDocContainer() throws Exception {
    Configuration configuration = new Configuration(Configuration.Mode.TEST);
    configuration.setTspSource("test-value");
    Container container = ContainerBuilder.
        aContainer(BDOC_CONTAINER_TYPE).
        withConfiguration(configuration).
        build();
    BDocContainer bDocContainer = (BDocContainer) container;
    assertEquals("BDOC", container.getType());
    assertEquals("test-value", bDocContainer.getAsicFacade().getConfiguration().getTspSource());
  }

  @Test
  public void buildBDocContainerWithDataFiles() throws Exception {
    File testFile1 = createTestFile("testFile.txt");
    File testFile2 = createTestFile("testFile2.txt");
    Container container = ContainerBuilder.
        aContainer().
        withDataFile(testFile1.getPath(), "text/plain").
        withDataFile(new ByteArrayInputStream(new byte[]{1, 2, 3}), "streamFile.txt", "text/plain").
        withDataFile(createTestFile("ExampleFile.txt"), "text/plain").
        withDataFile(new DataFile(testFile2.getPath(), "text/plain")).
        build();
    assertEquals(4, container.getDataFiles().size());
    assertEquals("testFile.txt", container.getDataFiles().get(0).getName());
    assertEquals("streamFile.txt", container.getDataFiles().get(1).getName());
    assertEquals("ExampleFile.txt", container.getDataFiles().get(2).getName());
    assertEquals("testFile2.txt", container.getDataFiles().get(3).getName());
  }

  @Test
  public void buildDDocContainerWithDataFiles() throws Exception {
    File testFile1 = createTestFile("testFile.txt");
    File testFile2 = createTestFile("testFile2.txt");
    Container container = ContainerBuilder.
        aContainer(DDOC_CONTAINER_TYPE).
        withDataFile(testFile1.getPath(), "text/plain").
        withDataFile(new ByteArrayInputStream(new byte[]{1, 2, 3}), "streamFile.txt", "text/plain").
        withDataFile(createTestFile("ExampleFile.txt"), "text/plain").
        withDataFile(new DataFile(testFile2.getPath(), "text/plain")).
        build();
    assertEquals(4, container.getDataFiles().size());
    assertEquals("testFile.txt", container.getDataFiles().get(0).getName());
    assertEquals("streamFile.txt", container.getDataFiles().get(1).getName());
    assertEquals("ExampleFile.txt", container.getDataFiles().get(2).getName());
    assertEquals("testFile2.txt", container.getDataFiles().get(3).getName());
  }

  @Test(expected = InvalidDataFileException.class)
  public void buildContainer_withNullFilePath_shouldThrowException() throws Exception {
    String path = null;
    ContainerBuilder.
        aContainer().
        withDataFile(path, "text/plain").
        build();
  }

  @Test(expected = InvalidDataFileException.class)
  public void buildContainer_withStreamDocAndNullFileName_shouldThrowException() throws Exception {
    String name = null;
    ContainerBuilder.
        aContainer().
        withDataFile(new ByteArrayInputStream(new byte[]{1, 2, 3}), name, "text/plain").
        build();
  }

  @Test(expected = InvalidDataFileException.class)
  public void buildContainer_withInvalidMimeType_shouldThrowException() throws Exception {
    String mimeType = null;
    ContainerBuilder.
        aContainer().
        withDataFile("testFile.txt", mimeType).
        build();
  }

  @Test
  public void signAndValidateContainer() throws Exception {
    Container container = TestDataBuilder.createContainerWithFile(testFolder);
    TestDataBuilder.signContainer(container);
    ValidationResult result = container.validate();
    assertFalse(result.hasErrors());
    assertFalse(result.hasWarnings());
    assertTrue(result.getContainerErrors().isEmpty());
    assertTrue(result.isValid());
    assertTrue(containsIgnoreCase(result.getReport(), "<Indication>VALID</Indication>"));
  }

  @Test
  public void signAndSaveContainerToFile() throws Exception {
    Container container = TestDataBuilder.createContainerWithFile(testFolder);
    TestDataBuilder.signContainer(container);
    String filePath = testFolder.newFile("test-container.bdoc").getPath();
    File file = container.saveAsFile(filePath);
    assertTrue(FileUtils.sizeOf(file) > 0);
  }

  @Test
  public void signAndSaveContainerToStream() throws Exception {
    Container container = TestDataBuilder.createContainerWithFile(testFolder);
    TestDataBuilder.signContainer(container);
    InputStream stream = container.saveAsStream();
    byte[] bytes = IOUtils.toByteArray(stream);
    assertTrue(bytes.length > 10);
  }

  @Test
  public void signAndSaveDDocContainerToStream() throws Exception {
    File testFile = createTestFile("testFile.txt");
    Container container = ContainerBuilder.
        aContainer(DDOC_CONTAINER_TYPE).
        withConfiguration(TEST_CONFIGURATION).
        withDataFile(testFile, "text/plain").
        build();
    TestDataBuilder.signContainer(container, DigestAlgorithm.SHA1);
    InputStream stream = container.saveAsStream();
    byte[] bytes = IOUtils.toByteArray(stream);
    assertTrue(bytes.length > 10);
  }

  @Test
  public void addAndRemoveSignatureFromDDocContainer() throws Exception {
    File testFile = createTestFile("testFile.txt");
    Container container = ContainerBuilder.
        aContainer(DDOC_CONTAINER_TYPE).
        withConfiguration(TEST_CONFIGURATION).
        withDataFile(testFile, "text/plain").
        build();
    DDocSignature signature1 = (DDocSignature) TestDataBuilder.signContainer(container, DigestAlgorithm.SHA1);
    DDocSignature signature2 = (DDocSignature) TestDataBuilder.signContainer(container, DigestAlgorithm.SHA1);
    assertEquals(0, ((DDocSignature) container.getSignatures().get(0)).getIndexInArray());
    assertEquals(1, ((DDocSignature) container.getSignatures().get(1)).getIndexInArray());
    assertEquals(0, signature1.getIndexInArray());
    assertEquals(1, signature2.getIndexInArray());

  }

  @Test
  public void removeSignatureFromSignedContainer() throws Exception {
    Container container = TestDataBuilder.createContainerWithFile(testFolder);
    Signature signature = TestDataBuilder.signContainer(container);
    container.saveAsFile(testFolder.newFile("test-container.bdoc").getPath());
    container.removeSignature(signature);
    assertTrue(container.getSignatures().isEmpty());
  }

  @Test
  public void buildCustomContainerWithCustomImplementation() throws Exception {
    ContainerBuilder.setContainerImplementation("TEST-FORMAT", TestContainer.class);
    Container container = ContainerBuilder.
        aContainer("TEST-FORMAT").
        build();
    assertEquals("TEST-FORMAT", container.getType());
  }

  @Test
  public void overrideExistingBDocContainerImplementation() throws Exception {
    ContainerBuilder.setContainerImplementation("BDOC", TestContainer.class);
    Container container = ContainerBuilder.
        aContainer("BDOC").
        build();
    assertEquals("TEST-FORMAT", container.getType());
  }

  @Test
  public void useExtendedBDocContainerImplementation() throws Exception {
    ContainerBuilder.setContainerImplementation("BDOC", ExtendedBDocContainer.class);
    Container container = ContainerBuilder.
        aContainer("BDOC").
        build();
    assertEquals("BDOC-EXTENDED", container.getType());
  }

  @Test
  public void clearCustomContainerImplementations_shouldUseDefaultContainerImplementation() throws Exception {
    ContainerBuilder.setContainerImplementation("BDOC", ExtendedBDocContainer.class);
    ContainerBuilder.removeCustomContainerImplementations();
    Container container = ContainerBuilder.
        aContainer("BDOC").
        build();
    assertEquals("BDOC", container.getType());

  }

  @Test
  public void createCustomContainerWithConfiguration() throws Exception {
    ContainerBuilder.setContainerImplementation("TEST-FORMAT", TestContainer.class);
    Container container = ContainerBuilder.
        aContainer("TEST-FORMAT").
        withConfiguration(TEST_CONFIGURATION).
        build();
    assertEquals("TEST-FORMAT", container.getType());
    assertSame(TEST_CONFIGURATION, ((TestContainer) container).getConfiguration());
  }

  @Test
  public void openDefaultContainerFromFile() throws Exception {
    Container container = ContainerBuilder.
        aContainer().
        fromExistingFile(BDOC_TEST_FILE).
        build();
    assertContainerOpened(container, "BDOC");
  }

  @Test
  public void openDefaultContainerFromFileWithConfiguration() throws Exception {
    Configuration configuration = new Configuration(Configuration.Mode.TEST);
    configuration.setTspSource("test-value");
    Container container = ContainerBuilder.
        aContainer().
        fromExistingFile(BDOC_TEST_FILE).
        withConfiguration(configuration).
        build();
    assertContainerOpened(container, "BDOC");
    assertEquals("test-value", ((BDocContainer) container).getAsicFacade().getConfiguration().getTspSource());
  }

  @Test
  public void openDDocContainerFromFile_whenUsingDefaultContainer() throws Exception {
    Container container = ContainerBuilder.
        aContainer().
        fromExistingFile(DDOC_TEST_FILE).
        build();
    assertContainerOpened(container, "DDOC");
  }

  @Test
  public void openDDocContainerFromFile() throws Exception {
    Container container = ContainerBuilder.
        aContainer("DDOC").
        fromExistingFile(DDOC_TEST_FILE).
        build();
    assertContainerOpened(container, "DDOC");
  }

  @Test
  public void openCustomContainerFromFile() throws Exception {
    File testFile = createTestFile("testFile.txt");
    ContainerBuilder.setContainerImplementation("TEST-FORMAT", TestContainer.class);
    Container container = ContainerBuilder.
        aContainer("TEST-FORMAT").
        fromExistingFile(testFile.getPath()).
        build();
    assertEquals("TEST-FORMAT", container.getType());
    assertEquals(testFile.getPath(), ((TestContainer) container).getOpenedFromFile());
  }

  @Test
  public void openCustomContainerFromFile_withConfiguration() throws Exception {
    File testFile = createTestFile("testFile.txt");
    ContainerBuilder.setContainerImplementation("TEST-FORMAT", TestContainer.class);
    Container container = ContainerBuilder.
        aContainer("TEST-FORMAT").
        withConfiguration(TEST_CONFIGURATION).
        fromExistingFile(testFile.getPath()).
        build();
    assertEquals("TEST-FORMAT", container.getType());
    assertEquals(testFile.getPath(), ((TestContainer) container).getOpenedFromFile());
    assertSame(TEST_CONFIGURATION, ((TestContainer) container).getConfiguration());
  }

  @Test
  public void openBDocContainerFromStream() throws Exception {
    InputStream stream = FileUtils.openInputStream(new File(BDOC_TEST_FILE));
    Container container = ContainerBuilder.
        aContainer("BDOC").
        fromStream(stream).
        build();
    assertContainerOpened(container, "BDOC");
  }

  @Test
  public void openBDocContainerFromStream_withConfiguration() throws Exception {
    Configuration configuration = new Configuration(Configuration.Mode.TEST);
    configuration.setTspSource("test-value");
    InputStream stream = FileUtils.openInputStream(new File(BDOC_TEST_FILE));
    Container container = ContainerBuilder.
        aContainer("BDOC").
        withConfiguration(configuration).
        fromStream(stream).
        build();
    assertContainerOpened(container, "BDOC");
    assertEquals("test-value", ((BDocContainer) container).getAsicFacade().getConfiguration().getTspSource());
  }

  @Test
  public void openDDocContainerFromStream() throws Exception {
    InputStream stream = FileUtils.openInputStream(new File(DDOC_TEST_FILE));
    Container container = ContainerBuilder.
        aContainer("DDOC").
        fromStream(stream).
        build();
    assertContainerOpened(container, "DDOC");
  }

  @Test
  public void openDDocContainerFromStream_withConfiguration() throws Exception {
    InputStream stream = FileUtils.openInputStream(new File(DDOC_TEST_FILE));
    Container container = ContainerBuilder.
        aContainer("DDOC").
        withConfiguration(TEST_CONFIGURATION).
        fromStream(stream).
        build();
    assertContainerOpened(container, "DDOC");
    assertSame(TEST_CONFIGURATION, ((DDocContainer) container).getJDigiDocFacade().getConfiguration());
  }

  @Test
  public void openDefaultContainerFromStream_withBDOC() throws Exception {
    InputStream stream = FileUtils.openInputStream(new File(BDOC_TEST_FILE));
    Container container = ContainerBuilder.
        aContainer().
        withConfiguration(TEST_CONFIGURATION).
        fromStream(stream).
        build();
    assertContainerOpened(container, "BDOC");
  }

  @Test
  public void openDefaultContainerFromStream_withDDOC() throws Exception {
    InputStream stream = FileUtils.openInputStream(new File(DDOC_TEST_FILE));
    Container container = ContainerBuilder.
        aContainer().
        withConfiguration(TEST_CONFIGURATION).
        fromStream(stream).
        build();
    assertContainerOpened(container, "DDOC");
  }

  @Test
  public void openCustomContainerFromStream() throws Exception {
    InputStream stream = FileUtils.openInputStream(createTestFile("testFile.txt"));
    ContainerBuilder.setContainerImplementation("TEST-FORMAT", TestContainer.class);
    Container container = ContainerBuilder.
        aContainer("TEST-FORMAT").
        fromStream(stream).
        build();
    assertEquals("TEST-FORMAT", container.getType());
    assertSame(stream, ((TestContainer) container).getOpenedFromStream());
  }

  @Test
  public void openCustomContainerFromStream_withConfiguration() throws Exception {
    InputStream stream = FileUtils.openInputStream(createTestFile("testFile.txt"));
    ContainerBuilder.setContainerImplementation("TEST-FORMAT", TestContainer.class);
    Container container = ContainerBuilder.
        aContainer("TEST-FORMAT").
        withConfiguration(TEST_CONFIGURATION).
        fromStream(stream).
        build();
    assertEquals("TEST-FORMAT", container.getType());
    assertSame(stream, ((TestContainer) container).getOpenedFromStream());
    assertSame(TEST_CONFIGURATION, ((TestContainer) container).getConfiguration());
  }

  private File createTestFile(String fileName) throws IOException {
    File testFile1 = testFolder.newFile(fileName);
    FileUtils.writeStringToFile(testFile1, "Banana Pancakes");
    return testFile1;
  }

  private void assertContainerOpened(Container container, String containerType) {
    assertEquals(containerType, container.getType());
    assertFalse(container.getDataFiles().isEmpty());
    assertFalse(container.getSignatures().isEmpty());
  }

  public static class ExtendedBDocContainer extends BDocContainer {

    @Override
    public String getType() {
      return "BDOC-EXTENDED";
    }
  }
}