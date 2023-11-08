package com.github.kugelsoft.paramscanner;
import com.github.kugelsoft.paramscanner.util.BytesUtil;
import com.github.kugelsoft.paramscanner.vo.JavaClass;
import com.github.kugelsoft.paramscanner.vo.JavaMethod;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;

import java.io.BufferedWriter;
import java.io.File;
import java.io.InputStream;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

@Mojo( name = "scanner", threadSafe = true )
public class ParamScannerMojo extends AbstractMojo {

	private static final String PREFIXO_PARAMETRO_TABELA_CAMPOS = "ParametroTabelaCampos";

	private Collection<JavaClass> todasClasses;
	private HashMap<String,List<JavaClass>> classesExtendsMap;

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

	@Parameter( property = "scanner.jsonFileCopyOrigin", defaultValue = "" )
	String jsonFileCopyOrigin;

	public void execute() throws MojoExecutionException {
		File jarFile = new File(directory, finalName + ".jar");
		if (!jarFile.exists()) {
			jarFile = new File(directory, finalName + ".ear");
			if (!jarFile.exists()) {
				jarFile = new File(directory, finalName + ".war");
			}
		}
		File paramFile;
		if (jsonFileCopyOrigin == null || jsonFileCopyOrigin.isEmpty()) {
			getLog().info("Escaneando parâmetros em: " + jarFile.getAbsolutePath());

			Map<String, Set<String>> progByParamMap = createMapProgByParam(jarFile);

			File dir = new File(outputDirectory);
			if (!dir.exists()) {
				dir.mkdirs();
			}
			paramFile = new File(dir, jsonFileDestination);

			getLog().info("Escrevendo arquivo: " + paramFile.getAbsolutePath());

			try (BufferedWriter writer = Files.newBufferedWriter(paramFile.toPath(), StandardCharsets.UTF_8)) {
				writer.append("{");

				int index = 0;
				for (Map.Entry<String, Set<String>> entry : progByParamMap.entrySet()) {
					if (index > 0) {
						writer.append(",");
					}

					writer.newLine();
					String arrayValues = "";
					Set<String> progs = entry.getValue();
					if (!progs.isEmpty()) {
						arrayValues = "\"" + String.join("\",\"", progs) + "\"";
					}
					writer.append(String.format("  \"%s\": [%s]", entry.getKey(), arrayValues));
					index++;
				}

				writer.newLine();
				writer.append("}");

				writer.close();
			} catch (Exception e) {
				throw new MojoExecutionException("Error creating file " + paramFile, e);
			}
		} else {
			paramFile = new File(jsonFileCopyOrigin);
			getLog().info("Ignorando escaneamento de parâmetros pois deve copiar apenas de " + paramFile.getAbsolutePath());
		}
		copyParamFileToJar(paramFile, jarFile);
	}

	private void copyParamFileToJar(File paramFile, File jarFile) throws MojoExecutionException {
		File tempJarFile = null;
		String projectFolderName = "";
		try (ZipFile zipFile = new ZipFile(jarFile)) {
			Enumeration<? extends ZipEntry> entries = zipFile.entries();
			while(entries.hasMoreElements()) {
				ZipEntry zipEntry = entries.nextElement();
				if (zipEntry.getName().startsWith(jsonFileProject)) {
					projectFolderName = zipEntry.getName().split("/")[0];

					if (!zipEntry.isDirectory()) {
						InputStream is = zipFile.getInputStream(zipEntry);
						tempJarFile = new File(System.getProperty("java.io.tmpdir"), zipEntry.getName());
						tempJarFile.delete();
						Files.write(tempJarFile.toPath(), BytesUtil.readAllBytes(is));
					}

					break;
				}
			}
		} catch (Exception e) {
			throw new MojoExecutionException( "Error opening file " + jarFile, e );
		}

		if (tempJarFile == null) {
			copyFileToJar(paramFile, jarFile, projectFolderName + "/" + jsonFileDestination);
		} else {
			copyFileToJar(paramFile, tempJarFile, jsonFileDestination);
			copyFileToJar(tempJarFile, jarFile, projectFolderName);
		}
	}

	protected Map<String, Set<String>> createMapProgByParam(File file) {
		TreeMap<String, Set<String>> progByParamMap = new TreeMap<>();
		try {
			JavaScanner javaScanner = new JavaScanner(file.getAbsolutePath());
			todasClasses = javaScanner.scanAllClasses().values();
			classesExtendsMap = new HashMap<>();
			for (JavaClass javaClass : todasClasses) {
				for (JavaClass superClass : javaClass.getInterfacesAndSuperClass()) {
					List<JavaClass> classes = classesExtendsMap
							.computeIfAbsent(superClass.getName(), k -> new ArrayList<>());
					classes.add(javaClass);
				}
			}

			Set<JavaClass> javaClasses = findAllClassesThatExtendsOrImplements("com/kugel/domain/param/Parametro");
			for( JavaClass javaClass : javaClasses ){
				if (!javaClass.getName().equals("com/kugel/domain/param/Parametro")) {
					getLog().debug("Name: " + javaClass.getName());

					String className = javaClass.getName().replace("/", ".");
					if (javaClass.getMethods().isEmpty()) {
						getLog().debug("Não encontrou método na classe " + className);
					} else if (className.equals("com.kugel.domain.param.ParametroCorMenuBanco")) {
						Set<String> progs = progByParamMap.get(className);
						if (progs == null || progs.isEmpty()) {
							progs = new HashSet<>();
							progs.add("MENU");
							progByParamMap.put(className, progs);
						}
					} else {
						for (JavaMethod method : javaClass.getMethods()) {
							scanProgByParamRecursive(progByParamMap, className, method, "-", new HashSet<>());
						}

						Set<String> progs = progByParamMap.get(className);
						if (progs == null || progs.isEmpty()) {
							int idx = className.indexOf(PREFIXO_PARAMETRO_TABELA_CAMPOS);
							if (idx >= 0) {
								idx += PREFIXO_PARAMETRO_TABELA_CAMPOS.length();
								String programa = className.substring(idx);
								progs = putProgByParam(progByParamMap, className, programa);
							}
						}
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

	private Set<JavaClass> findAllClassesThatExtendsOrImplements(String className) {
		List<JavaClass> list = classesExtendsMap.getOrDefault(className, Collections.emptyList());
		Set<JavaClass> javaClassSet = new HashSet<>(list);
		for (JavaClass javaClass : list) {
			javaClassSet.addAll(findAllClassesThatExtendsOrImplements(javaClass.getName()));
		}
		return javaClassSet;
	}

	private void scanProgByParamRecursive(Map<String, Set<String>> progByParamMap, String paramClassName, JavaMethod method, String prefix, HashSet<JavaMethod> methodSet) {
		if (methodSet.contains(method)) {
			getLog().debug("Ignorando recursividade");
			return;
		}

		HashSet<JavaMethod> newMethodSet = new HashSet<>(methodSet);
		newMethodSet.add(method);

		JavaClass javaClass = method.getJavaClass();
		if (javaClass.getSimpleClassName().equals("GenericRest")) {
			getLog().debug("Ignorando GenericRest pois utilizado por todos os programas");
			return;
		}

		getLog().debug(prefix + " " + javaClass.getSimpleClassName() + "." + method.getMethodName() + " - " + method.getMethodDesc());
		prefix += "-";

		String simpleClassName = javaClass.getSimpleClassName();
		if (isClassePrograma(javaClass)) {
			String prog = simpleClassName.substring(0, 7);
			putProgByParam(progByParamMap, paramClassName, prog);
		} else if (isClasseMenu(javaClass)) {
			putProgByParam(progByParamMap, paramClassName, "MENU");
		} else if (isClasseTarefaAgendadaAntiga(javaClass)) {
			putProgByParam(progByParamMap, paramClassName, "TAREFA_AGENDADA");
		} else if (isClasseZoomDaoOuZoomService(javaClass)) {
			for (JavaClass javaClassUsesAsField : javaClass.getClassesUsesAsField()) {
				scanProgByParamRecursive(progByParamMap, paramClassName, JavaMethod.emptyMethod(javaClassUsesAsField), prefix, newMethodSet);
			}
		}

		for (JavaMethod caller : method.getCallers()) {
			scanProgByParamRecursive(progByParamMap, paramClassName, caller, prefix, newMethodSet);
		}

		Set<JavaClass> classes = findAllClassesThatExtendsOrImplements(javaClass.getName());
		for (JavaClass extJavaClass : classes) {
			if (!extJavaClass.getName().equals(javaClass.getName())) {
				for (JavaMethod extMethod : extJavaClass.getMethods()) {
					if (extMethod.getMethodName().equals(method.getMethodName()) &&
							extMethod.getMethodDesc().equals(method.getMethodDesc())) {
						scanProgByParamRecursive(progByParamMap, paramClassName, extMethod, prefix, newMethodSet);
					}
				}
			}
		}
	}

	private boolean isClassePrograma(JavaClass javaClass) {
		if (javaClass.getSimpleClassName().startsWith("PW")) {
			return true;
		}
		if (javaClass.getSimpleClassName().startsWith("PR") &&
				javaClass.getName().replace("/", ".").startsWith("com.kugel.service.prjava") ) {
			return true;
		}
		return false;
	}

	private boolean isClasseMenu(JavaClass javaClass) {
		return javaClass.getSimpleClassName().equals("AuthenticationService") ||
				javaClass.getSimpleClassName().equals("MenuRest");
	}

	private boolean isClasseTarefaAgendadaAntiga(JavaClass javaClass) {
		return javaClass.getSuperClass().getSimpleClassName().equals("TarefaAgendadaAbstract");
	}

	private boolean isClasseZoomDaoOuZoomService(JavaClass javaClass) {
		List<JavaClass> superClasses = obterTodasSuperClasses(javaClass);
		for (JavaClass superJavaClass : superClasses) {
			if (superJavaClass.getSimpleClassName().equals("ZoomDAO") ||
					superJavaClass.getSimpleClassName().equals("GenericZoomService")) {
				return true;
			}
		}
		return false;
	}

	private Set<String> putProgByParam(Map<String, Set<String>> progByParamMap, String paramClassName, String prog) {
		Set<String> progSet = progByParamMap.computeIfAbsent(paramClassName, k -> new TreeSet<>());
		progSet.add(prog);
		return progSet;
	}

	private List<JavaClass> obterTodasSuperClasses(JavaClass javaClass) {
		List<JavaClass> superClasses = new ArrayList<>();
		superClasses.add(javaClass.getSuperClass());
		for (int i = 0; i < superClasses.size(); i++) {
			JavaClass classeAtual = superClasses.get(i);
			if (classeAtual.getSuperClass() != null) {
				superClasses.add(classeAtual.getSuperClass());
			}
		}
		return superClasses;
	}

	private void copyFileToJar(File fileToCopy, File destJarFile, String destPath) throws MojoExecutionException {
		URI uri;
		uri = URI.create("jar:" + destJarFile.toURI());
		try (FileSystem fileSystem = FileSystems.newFileSystem(uri, new HashMap<>())) {
			Path paramJarPath = fileSystem.getPath(destPath);
			getLog().info("Copiando " + fileToCopy.getAbsolutePath() + " para " + uri + "/" + paramJarPath);
			Files.deleteIfExists(paramJarPath);
			Files.copy(fileToCopy.toPath(), paramJarPath);
			fileSystem.close();
			getLog().info("Cópia realizada com sucesso");
		} catch (Exception e) {
			throw new MojoExecutionException( "Error opening file " + destJarFile, e );
		}
	}

}
