package com.github.kugelsoft.paramscanner;

import org.junit.Assert;
import org.junit.Test;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import static java.nio.file.StandardCopyOption.REPLACE_EXISTING;

public class ParamScannerMojoTest {

	@Test
	public void createMapProgByParam() throws Exception {
		ParamScannerMojo mojo = new ParamScannerMojo();

		File file = new File("M:\\ERP Web REST\\ear-teste\\kugelapp_vteste2.ear");
		if (!file.exists()) {
			Assert.fail("Teste deve ser executado apenas dentro do ambiente da Kugel.");
		}

		Map<String, Set<String>> mapProgByParam = mojo.createMapProgByParam(file);

		for (Map.Entry<String, Set<String>> entry : mapProgByParam.entrySet()) {
			System.out.println(entry.getKey() + "=" + entry.getValue());
		}

		deveConter(mapProgByParam, "com.kugel.domain.param.ParametroBancosAnaliseMinhaGranja", "PW90007");
		deveConter(mapProgByParam, "com.kugel.domain.param.ParametroBancosCopiarDC0045", "PW0045A");
		deveConter(mapProgByParam, "com.kugel.domain.param.ParametroBancosCopiarDC0045", "PW0072B");
		deveConter(mapProgByParam, "com.kugel.domain.param.ParametroChaveGoogleMaps", "MENU");
		deveConter(mapProgByParam, "com.kugel.domain.param.ParametroImagemCarimboMAPAAlimentacaoAnimal", "PW1533A");
		deveConter(mapProgByParam, "com.kugel.domain.param.ParametroTabelaCodTransacaoComercialTransferencia", "PR51946");
		deveConter(mapProgByParam, "com.kugel.domain.param.ParametroTabelaPortadorPix", "PW00323");
		deveConter(mapProgByParam, "com.kugel.domain.param.ParametroOrigemArticolo", "PW00119");
		deveConter(mapProgByParam, "com.kugel.domain.param.ParametroNumDiasNeZoom", "PW00173");
		deveConter(mapProgByParam, "com.kugel.domain.param.ParametroNumDiasNfZoom", "PW00120");
		deveConter(mapProgByParam, "com.kugel.domain.param.ParametroNumDiasNfZoom", "PW0977A");
		deveConter(mapProgByParam, "com.kugel.domain.param.ParametroTabelaCamposPW00182", "PW00182");
	}

	@Test
	public void execute() throws Exception {
		File fileOrigem = new File("M:\\ERP Web REST\\ear-teste\\kugelapp_v552.ear");
		File fileDestino = File.createTempFile("kugelapp", ".ear");

		Files.copy(fileOrigem.toPath(), fileDestino.toPath(), REPLACE_EXISTING);

		String finalName = fileDestino.getName().substring(0, fileDestino.getName().lastIndexOf("."));

		ParamScannerMojo mojo = new ParamScannerMojo();
		mojo.directory = fileDestino.getParent();
		mojo.finalName = finalName;
		mojo.outputDirectory = System.getProperty("java.io.tmpdir");
		mojo.jsonFileDestination = "parametros.json";
		mojo.jsonFileProject = "kugel-domain";

		File file = new File(mojo.outputDirectory, "parametros.json");
		file.delete();

		mojo.execute();

		Assert.assertEquals("Deveria ter gerado arquivo " + file.getAbsolutePath(), true, file.exists());

		fileDestino.delete();
	}

	@Test
	public void executeApenasCopiar() throws Exception {
		File fileOrigem = new File("M:\\ERP Web REST\\ear-teste\\kugelapp_v552.ear");
		File fileDestino = File.createTempFile("kugelapp", ".ear");

		Files.copy(fileOrigem.toPath(), fileDestino.toPath(), REPLACE_EXISTING);

		String finalName = fileDestino.getName().substring(0, fileDestino.getName().lastIndexOf("."));

		String conteudoArquivo = "{}";

		File jsonFileCopyOrigin = File.createTempFile("parametros", ".json");
		Files.write(jsonFileCopyOrigin.toPath(), conteudoArquivo.getBytes(StandardCharsets.UTF_8));

		ParamScannerMojo mojo = new ParamScannerMojo();
		mojo.directory = fileDestino.getParent();
		mojo.finalName = finalName;
		mojo.outputDirectory = System.getProperty("java.io.tmpdir");
		mojo.jsonFileDestination = "parametros.json";
		mojo.jsonFileProject = "kugel-domain";
		mojo.jsonFileCopyOrigin = jsonFileCopyOrigin.getAbsolutePath();

		File file = new File(mojo.outputDirectory, "parametros.json");
		file.delete();

		mojo.execute();

		ZipFile zipFile = new ZipFile(fileDestino);
		ZipEntry zipEntry = zipFile.stream()
				.filter(e -> e.getName().startsWith("kugel-domain") &&
						e.getName().endsWith("parametros.json"))
				.findAny()
				.orElse(null);
		Assert.assertNotNull("Deveria ter criado o arquivo no ear", zipEntry);

		byte[] bytes = new byte[128];
		zipFile.getInputStream(zipEntry).read(bytes);
		String conteudo = new String(bytes).trim();
		Assert.assertEquals("Deveria ter copiado o arquivo de origem", conteudoArquivo, conteudo);

		fileDestino.delete();
	}

	private void deveConter(Map<String, Set<String>> mapProgByParam, String param, String prog) {
		Set<String> programas = mapProgByParam.getOrDefault(param, Collections.emptySet());
		Assert.assertEquals("Parâmetro " + param + " deveria conter " + prog + " mas contém apenas " + programas, true, programas.contains(prog));
	}

}