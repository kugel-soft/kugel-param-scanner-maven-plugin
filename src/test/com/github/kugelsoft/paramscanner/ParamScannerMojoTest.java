package com.github.kugelsoft.paramscanner;

import org.junit.Assert;
import org.junit.Test;

import java.io.File;
import java.util.Map;
import java.util.Set;

public class ParamScannerMojoTest {

	@Test
	public void createMapProgByParam() throws Exception {
		ParamScannerMojo mojo = new ParamScannerMojo();

		File file = new File("M:\\ERP Web REST\\ear-teste\\kugelapp_vteste2.ear");
		if (!file.exists()) {
			Assert.fail("Teste deve ser executado apenas dentro do ambiente da Kugel.");
		}

		Map<String, Set<String>> mapProgByParam = mojo.createMapProgByParam(file);

		deveConter(mapProgByParam, "com.kugel.domain.param.ParametroBancosAnaliseMinhaGranja", "PW90007");
		deveConter(mapProgByParam, "com.kugel.domain.param.ParametroBancosCopiarDC0045", "PW0045A");
		deveConter(mapProgByParam, "com.kugel.domain.param.ParametroBancosCopiarDC0045", "PW0072B");
		deveConter(mapProgByParam, "com.kugel.domain.param.ParametroChaveGoogleMaps", "MENU");
		deveConter(mapProgByParam, "com.kugel.domain.param.ParametroImagemCarimboMAPAAlimentacaoAnimal", "PW1533A");
		deveConter(mapProgByParam, "com.kugel.domain.param.ParametroTabelaCodTransacaoComercialTransferencia", "PR51946");
		deveConter(mapProgByParam, "com.kugel.domain.param.ParametroTabelaPortadorPix", "PW00323");
	}

	@Test
	public void execute() throws Exception {
		ParamScannerMojo mojo = new ParamScannerMojo();
		mojo.directory = "M:\\ERP Web REST\\ear-teste\\";
		mojo.finalName = "kugelapp_v552";
		mojo.outputDirectory = System.getProperty("java.io.tmpdir");
		mojo.jsonFileDestination = "parametros.json";
		mojo.jsonFileProject = "kugel-domain";

		File file = new File(mojo.outputDirectory, "parametros.json");
		file.delete();

		mojo.execute();

		Assert.assertEquals("Deveria ter gerado arquivo " + file.getAbsolutePath(), true, file.exists());
	}

	private void deveConter(Map<String, Set<String>> mapProgByParam, String param, String prog) {
		Assert.assertEquals("Parâmetro " + param + " deveria conter " + prog + " mas contém apenas " + mapProgByParam.get(param), true, mapProgByParam.get(param).contains(prog));
	}

}