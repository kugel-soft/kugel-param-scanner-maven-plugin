package com.github.kugelsoft.paramscanner;
import com.github.kugelsoft.paramscanner.vo.JavaClass;
import com.github.kugelsoft.paramscanner.vo.JavaMethod;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;

import java.io.BufferedWriter;
import java.io.File;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

@Mojo( name = "scanner", threadSafe = true )
public class ParamScannerMojo extends AbstractMojo {

	@Parameter( property = "scanner.directory", defaultValue = "${project.build.directory}" )
	String directory;

	@Parameter( property = "scanner.finalName", defaultValue = "${project.build.finalName}" )
	String finalName;

	@Parameter( property = "scanner.outputDirectory", defaultValue = "${project.build.outputDirectory}" )
	String outputDirectory;

	@Parameter( property = "scanner.jsonFileDestination", defaultValue = "parametros.json" )
	String jsonFileDestination;

	@Parameter( property = "scanner.jsonFileProject", defaultValue = "kugel-domain" )
	String jsonFileProject;

	public void execute() throws MojoExecutionException {
		File jarFile = new File(directory, finalName + ".jar");
		if (!jarFile.exists()) {
			jarFile = new File(directory, finalName + ".ear");
			if (!jarFile.exists()) {
				jarFile = new File(directory, finalName + ".war");
			}
		}

		getLog().info( "Escaneando parâmetros em: " + jarFile.getAbsolutePath() );

		Map<String,Set<String>> progByParamMap = createMapProgByParam(jarFile);

		File dir = new File(outputDirectory);
		if ( !dir.exists() ) {
			dir.mkdirs();
		}
		File paramFile = new File(dir, jsonFileDestination);

		getLog().info("Escrevendo arquivo: " + paramFile.getAbsolutePath());

		try (BufferedWriter writer = Files.newBufferedWriter(paramFile.toPath(), StandardCharsets.UTF_8)) {
			writer.append("{");

			int index = 0;
			for (Map.Entry<String,Set<String>> entry : progByParamMap.entrySet()) {
				if (index > 0) {
					writer.append(",");
				}

				writer.newLine();
				String arrayValues = "";
				Set<String> progs = entry.getValue();
				if (!progs.isEmpty()) {
					arrayValues = "\"" + String.join("\",\"", progs) + "\"";
				}
				writer.append( String.format("  \"%s\": [%s]", entry.getKey(), arrayValues ) );
				index++;
			}

			writer.newLine();
			writer.append("}");

			writer.close();
		} catch (Exception e ) {
			throw new MojoExecutionException( "Error creating file " + paramFile, e );
		}

		copyParamFileToJar(paramFile, jarFile);
	}

	private void copyParamFileToJar(File paramFile, File jarFile) throws MojoExecutionException {
		String projectFolderName = "";
		try (ZipFile zipFile = new ZipFile(jarFile)) {
			Enumeration<? extends ZipEntry> entries = zipFile.entries();
			while(entries.hasMoreElements()) {
				ZipEntry zipEntry = entries.nextElement();
				if (zipEntry.getName().startsWith(jsonFileProject)) {
					projectFolderName = zipEntry.getName().split("/")[0];
					break;
				}
			}
		} catch (Exception e) {
			throw new MojoExecutionException( "Error opening file " + jarFile, e );
		}

		URI uri = URI.create("jar:" + jarFile.toURI());
		try (FileSystem fileSystem = FileSystems.newFileSystem(uri, new HashMap<>())) {
			Path paramJarPath = fileSystem.getPath(projectFolderName + "/" + jsonFileDestination);
			getLog().info("Copiando " + paramFile.getAbsolutePath() + " para " + uri + "/" + paramJarPath);
			Files.deleteIfExists(paramJarPath);
			Files.copy(paramFile.toPath(), paramJarPath);
			fileSystem.close();
			getLog().info("Cópia realizada com sucesso");
		} catch (Exception e) {
			throw new MojoExecutionException( "Error opening file " + jarFile, e );
		}
	}

	protected Map<String, Set<String>> createMapProgByParam(File file) {
		TreeMap<String, Set<String>> progByParamMap = new TreeMap<>();
		try {
			JavaScanner javaScanner = new JavaScanner(file.getAbsolutePath());
			HashMap<String, JavaClass> javaClassesMap = javaScanner.scanAllClasses();

			List<JavaClass> javaClasses = findAllClassesThatExtendsOrImplements(javaClassesMap.values(), "com/kugel/domain/param/Parametro");
			for( JavaClass javaClass : javaClasses ){
				if (!javaClass.getName().equals("com/kugel/domain/param/Parametro")) {
					getLog().debug("Name: " + javaClass.getName());

					String className = javaClass.getName().replace("/", ".");
					if (javaClass.getMethods().isEmpty()) {
						getLog().debug("Não encontrou método na classe " + className);
					} else {
						for (JavaMethod method : javaClass.getMethods()) {
							scanProgByParam(progByParamMap, className, method, "-");
						}

						Set<String> progs = progByParamMap.get(className);
						if (progs == null || progs.isEmpty()) {
							getLog().warn("Não encontrou chamadas para a classe " + className);
						}
					}
				}
			}
		} catch (Exception ex) {
			getLog().error(ex);
		}
		return progByParamMap;
	}

	private List<JavaClass> findAllClassesThatExtendsOrImplements(Collection<JavaClass> allClasses, String className) {
		List<JavaClass> javaClasses = new ArrayList<>();
		for (JavaClass javaClass : allClasses) {
			if (classeExtendsOrImplements(javaClass, className)) {
				javaClasses.add(javaClass);
			}
		}
		return javaClasses;
	}

	private boolean classeExtendsOrImplements(JavaClass javaClass, String className) {
		if (javaClass.getName().equals(className)) {
			return true;
		}
		for (JavaClass c : javaClass.getInterfacesAndSuperClass()) {
			if (classeExtendsOrImplements(c, className)) {
				return true;
			}
		}
		return false;
	}

	private void scanProgByParam(Map<String, Set<String>> progByParamMap, String paramClassName, JavaMethod method, String prefix) {
		getLog().debug(prefix + " " + method.getJavaClass().getSimpleClassName() + "." + method.getMethodName() + " - " + method.getMethodDesc());

		String simpleClassName = method.getJavaClass().getSimpleClassName();
		if (simpleClassName.startsWith("PW")) {
			String prog = simpleClassName.substring(0, 7);
			putProgByParam(progByParamMap, paramClassName, prog);
		} else if(simpleClassName.equals("AuthenticationService") || simpleClassName.equals("MenuRest")) {
			putProgByParam(progByParamMap, paramClassName, "MENU");
		} else if(method.getJavaClass().getSuperClass().getSimpleClassName().equals("TarefaAgendadaAbstract")) {
			putProgByParam(progByParamMap, paramClassName, "TAREFA_AGENDADA");
		}

		for(JavaMethod caller : method.getCallers()) {
			scanProgByParam(progByParamMap, paramClassName, caller, prefix + "-");
		}
	}

	private void putProgByParam(Map<String, Set<String>> progByParamMap, String paramClassName, String prog) {
		Set<String> progSet = progByParamMap.computeIfAbsent(paramClassName, k -> new TreeSet<>());
		progSet.add(prog);
	}

}
