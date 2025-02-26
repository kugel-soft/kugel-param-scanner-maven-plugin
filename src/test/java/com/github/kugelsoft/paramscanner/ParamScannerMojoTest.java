package com.github.kugelsoft.paramscanner;

import com.github.kugelsoft.paramscanner.util.BytesUtil;
import com.github.kugelsoft.paramscanner.util.MapUtil;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipInputStream;

import static java.nio.file.StandardCopyOption.REPLACE_EXISTING;
import static java.util.Arrays.asList;
import static org.junit.Assert.*;

public class ParamScannerMojoTest {

	private static File dirEarteste;
	@BeforeClass
	public static void before() {
		dirEarteste = new File("M:\\ERP Web REST\\ear-teste\\");
		if (!dirEarteste.exists()) {
			dirEarteste = new File("/mnt/driveM/ERP Web REST/ear-teste/");
			if (!dirEarteste.exists()) {
				fail("Teste deve ser executado apenas dentro do ambiente da Kugel.");
			}
		}
	}

	@Test
	public void createMapProgByParam() throws Exception {
		ParamScannerMojo mojo = new ParamScannerMojo();

		File file = new File(dirEarteste, "kugelapp_vteste3.ear");

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
		deveConter(mapProgByParam, "com.kugel.domain.param.ParametroCodigosGrItemMatDefensivos", "PW00099");
		deveConter(mapProgByParam, "com.kugel.domain.param.ParametroCodigosGrItemMatSementes", "PW00099");
		deveConter(mapProgByParam, "com.kugel.domain.param.ParametroCodigosGrItemMatFertilizantes", "PW00099");
		naoDeveConter(mapProgByParam, "com.kugel.domain.param.ParametroCorMenuBanco", "PW00099");
		naoDeveConter(mapProgByParam, "com.kugel.domain.param.ParametroCorMenuBanco", "PW0977A");
		naoDeveConter(mapProgByParam, "com.kugel.domain.param.ParametroCorMenuBanco", "PW90007");
		naoDeveConter(mapProgByParam, "com.kugel.domain.param.ParametroCorMenuBanco", "PW1533A");
	}

	@Test
	public void execute() throws Exception {
		File fileOrigem = new File(dirEarteste, "kugelapp_vteste4.ear");
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

		assertEquals("Deveria ter gerado arquivo " + file.getAbsolutePath(), true, file.exists());

		ZipFile zipFile = new ZipFile(fileDestino);
		ZipEntry zipEntryDomain = zipFile.stream()
				.filter(e -> e.getName().startsWith("kugel-domain"))
				.findAny()
				.orElse(null);

		ZipInputStream zis = new ZipInputStream(zipFile.getInputStream(zipEntryDomain));
		ZipEntry zipEntryParamJson;
		do {
			zipEntryParamJson = zis.getNextEntry();
			if (zipEntryParamJson != null && zipEntryParamJson.getName().equals("parametros.json")) {
				break;
			}
		} while (zipEntryParamJson != null);

		// e.getName().endsWith("parametros.json")
		assertNotNull("Deveria ter criado o arquivo parametros.json dentro do kugel-domain do ear", zipEntryParamJson);

		fileDestino.delete();
	}

	@Test
	public void executeApenasCopiar() throws Exception {
		File fileOrigem = new File(dirEarteste, "kugelapp_v552.ear");
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
		assertNotNull("Deveria ter criado o arquivo no ear", zipEntry);

		byte[] bytes = BytesUtil.readAllBytes(zipFile.getInputStream(zipEntry));
		String conteudo = new String(bytes).trim();
		assertEquals("Deveria ter copiado o arquivo de origem", conteudoArquivo, conteudo);

		fileDestino.delete();
	}

	@Test
	public void createMapProgByParamComRecursividade() throws Exception {
		ParamScannerMojo mojo = new ParamScannerMojo();

		mojo.parametrosProgramasMap = MapUtil.of(
				"com.kugel.domain.param.ParametroCorMenuBanco", asList("MENU")
		);

		File file = new File(dirEarteste, "kugelapp_vteste5.ear");

		Map<String, Set<String>> mapProgByParam = mojo.createMapProgByParam(file);

		for (Map.Entry<String, Set<String>> entry : mapProgByParam.entrySet()) {
			System.out.println(entry.getKey() + "=" + entry.getValue());
		}

		deveConter(mapProgByParam, "com.kugel.domain.param.ParametroPortaSocketCameraPlaca", "PW90999");
		deveConter(mapProgByParam, "com.kugel.domain.param.ParametroBancosAnaliseMinhaGranja", "PW90007");
		deveConter(mapProgByParam, "com.kugel.domain.param.ParametroBancosCopiarDC0045", "PW0045A");
		deveConter(mapProgByParam, "com.kugel.domain.param.ParametroBancosCopiarDC0045", "PW0072B");
		deveConter(mapProgByParam, "com.kugel.domain.param.ParametroChaveGoogleMaps", "MENU");
		deveConter(mapProgByParam, "com.kugel.domain.param.ParametroImagemCarimboMAPAAlimentacaoAnimal", "PW1533A");
		deveConter(mapProgByParam, "com.kugel.domain.param.ParametroTabelaCodTransacaoComercialTransferencia", "PR51946");
		deveConter(mapProgByParam, "com.kugel.domain.param.ParametroTabelaPortadorPix", "PW00323");
		deveConter(mapProgByParam, "com.kugel.domain.param.ParametroNumDiasNeZoom", "PW00173");
		deveConter(mapProgByParam, "com.kugel.domain.param.ParametroNumDiasNfZoom", "PW00120");
		deveConter(mapProgByParam, "com.kugel.domain.param.ParametroNumDiasNfZoom", "PW0977A");
		deveConter(mapProgByParam, "com.kugel.domain.param.ParametroTabelaCamposPW00182", "PW00182");
		deveConter(mapProgByParam, "com.kugel.domain.param.ParametroCodigosGrItemMatDefensivos", "PW00099");
		deveConter(mapProgByParam, "com.kugel.domain.param.ParametroCodigosGrItemMatSementes", "PW00099");
		deveConter(mapProgByParam, "com.kugel.domain.param.ParametroCodigosGrItemMatFertilizantes", "PW00099");
		naoDeveConter(mapProgByParam, "com.kugel.domain.param.ParametroCorMenuBanco", "PW00099");
		naoDeveConter(mapProgByParam, "com.kugel.domain.param.ParametroCorMenuBanco", "PW0977A");
		naoDeveConter(mapProgByParam, "com.kugel.domain.param.ParametroCorMenuBanco", "PW90007");
		naoDeveConter(mapProgByParam, "com.kugel.domain.param.ParametroCorMenuBanco", "PW1533A");
	}

	@Test
	public void createMapProgByParamComParamIgnorar() throws Exception {
		ParamScannerMojo mojo = new ParamScannerMojo();

		File file = new File(dirEarteste, "kugelapp_v7682.ear");

		mojo.parametrosProgramasMap = MapUtil.of(
				"com.kugel.domain.param.ParametroEndereracoIntegracaoErpWeb", asList("MENU"),
				"com.kugel.domain.param.ParametroBananas", asList("PW00125","PW00265"),
				"com.kugel.domain.param.ParametroCorMenuBanco", asList("MENU")
		);
		mojo.jarsIgnorar = new HashSet<>(Arrays.asList("kugel-common","kugel-integracao-erp-web","kugel-framework"));

		Map<String, Set<String>> mapProgDefaultMap = new HashMap<>();
		mapProgDefaultMap.put("", new HashSet<>(asList("MENU")));

		Map<String, Set<String>> mapProgByParam = mojo.createMapProgByParam(file);

		for (Map.Entry<String, Set<String>> entry : mapProgByParam.entrySet()) {
			System.out.println(entry.getKey() + "=" + entry.getValue());
		}

		assertEquals("MENU", String.join(",", mapProgByParam.getOrDefault("com.kugel.domain.param.ParametroEndereracoIntegracaoErpWeb", Collections.emptySet())));
		deveConter(mapProgByParam, "com.kugel.domain.param.ParametroBananas", "PW00125");
		deveConter(mapProgByParam, "com.kugel.domain.param.ParametroBananas", "PW00265");
		naoDeveConter(mapProgByParam, "com.kugel.domain.param.ParametroBananas", "PW00124");
		deveConter(mapProgByParam, "com.kugel.domain.param.ParametroCorMenuBanco", "MENU");
		naoDeveConter(mapProgByParam, "com.kugel.domain.param.ParametroCorMenuBanco", "PW0977A");
		naoDeveConter(mapProgByParam, "com.kugel.domain.param.ParametroCorMenuBanco", "PW90007");
		naoDeveConter(mapProgByParam, "com.kugel.domain.param.ParametroCorMenuBanco", "PW1533A");
		deveConter(mapProgByParam, "com.kugel.domain.param.ParametroBancosAnaliseMinhaGranja", "PW90007");
		deveConter(mapProgByParam, "com.kugel.domain.param.ParametroBancosCopiarDC0045", "PW0045A");
		deveConter(mapProgByParam, "com.kugel.domain.param.ParametroBancosCopiarDC0045", "PW0072B");
		deveConter(mapProgByParam, "com.kugel.domain.param.ParametroChaveGoogleMaps", "MENU");
		deveConter(mapProgByParam, "com.kugel.domain.param.ParametroImagemCarimboMAPAAlimentacaoAnimal", "PW1533A");
		deveConter(mapProgByParam, "com.kugel.domain.param.ParametroTabelaCodTransacaoComercialTransferencia", "PR51946");
		deveConter(mapProgByParam, "com.kugel.domain.param.ParametroTabelaPortadorPix", "PW00323");
		deveConter(mapProgByParam, "com.kugel.domain.param.ParametroNumDiasNeZoom", "PW00173");
		deveConter(mapProgByParam, "com.kugel.domain.param.ParametroNumDiasNfZoom", "PW00120");
		deveConter(mapProgByParam, "com.kugel.domain.param.ParametroNumDiasNfZoom", "PW0079A");
		deveConter(mapProgByParam, "com.kugel.domain.param.ParametroCodigosGrItemMatDefensivos", "PW00099");
		deveConter(mapProgByParam, "com.kugel.domain.param.ParametroCodigosGrItemMatSementes", "PW00099");
		deveConter(mapProgByParam, "com.kugel.domain.param.ParametroCodigosGrItemMatFertilizantes", "PW00099");
		naoDeveConter(mapProgByParam, "com.kugel.domain.param.ParametroEndereracoIntegracaoErpWeb", "PW00099");
		deveConter(mapProgByParam, "com.kugel.domain.param.ParametroTabelaEmpresasImplanta", "INTEGRACAO");
		deveConter(mapProgByParam, "com.kugel.domain.param.ParametrosAuroraConsumoWebService", "PRCOBOL");
		deveConter(mapProgByParam, "com.kugel.domain.param.ParametrosAuroraConsumoWebService", "PRJAVA");
		deveConter(mapProgByParam, "com.kugel.domain.param.ParametrosAuroraConsumoWebService", "PW00474");
	}

	private void deveConter(Map<String, Set<String>> mapProgByParam, String param, String prog) {
		Set<String> programas = mapProgByParam.getOrDefault(param, Collections.emptySet());
		assertEquals("Parâmetro " + param + " deveria conter " + prog + " mas contém apenas " + programas, true, programas.contains(prog));
	}

	private void naoDeveConter(Map<String, Set<String>> mapProgByParam, String param, String prog) {
		Set<String> programas = mapProgByParam.getOrDefault(param, Collections.emptySet());
		assertEquals("Parâmetro " + param + "  não deveria conter " + prog, false, programas.contains(prog));
	}
}