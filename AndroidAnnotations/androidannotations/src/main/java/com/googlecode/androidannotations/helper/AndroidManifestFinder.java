/**
 * Copyright (C) 2010-2011 eBusiness Information, Excilys Group
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed To in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package com.googlecode.androidannotations.helper;

import java.io.File;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;

import javax.annotation.processing.Filer;
import javax.annotation.processing.Messager;
import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.util.Elements;
import javax.tools.Diagnostic.Kind;
import javax.tools.JavaFileObject;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

public class AndroidManifestFinder {

	private static final int MAX_PARENTS_FROM_SOURCE_FOLDER = 10;

	private ProcessingEnvironment processingEnv;

	public AndroidManifestFinder(ProcessingEnvironment processingEnv) {
		this.processingEnv = processingEnv;
	}

	public AndroidManifest extractAndroidManifest() {
		try {
			return extractAndroidManifestThrowing();
		} catch (Exception e) {
			if (e instanceof RuntimeException) {
				throw (RuntimeException) e;
			}
			throw new RuntimeException(e);
		}
	}

	private AndroidManifest extractAndroidManifestThrowing() throws Exception {
		File androidManifestFile = findManifestFileThrowing();
		return parseThrowing(androidManifestFile);
	}

	/**
	 * We use a dirty trick to find the AndroidManifest.xml file, since it's not
	 * available in the classpath. The idea is quite simple : create a fake
	 * class file, retrieve its URI, and start going up in parent folders to
	 * find the AndroidManifest.xml file. Any better solution will be
	 * appreciated.
	 */
	private File findManifestFileThrowing() throws Exception {
		Filer filer = processingEnv.getFiler();

		JavaFileObject dummySourceFile = filer.createSourceFile("dummy" + System.currentTimeMillis());
		String dummySourceFilePath = dummySourceFile.toUri().toString();

		if (dummySourceFilePath.startsWith("file:")) {
			if (!dummySourceFilePath.startsWith("file://")) {
				dummySourceFilePath = "file://" + dummySourceFilePath.substring("file:".length());
			}
		} else {
			dummySourceFilePath = "file://" + dummySourceFilePath;
		}

		Messager messager = processingEnv.getMessager();
		messager.printMessage(Kind.NOTE, "Dummy source file: " + dummySourceFilePath);

		URI cleanURI = new URI(dummySourceFilePath);

		File dummyFile = new File(cleanURI);

		File sourcesGenerationFolder = dummyFile.getParentFile();

		File projectRoot = sourcesGenerationFolder.getParentFile();

		File androidManifestFile = new File(projectRoot, "AndroidManifest.xml");
		for (int i = 0; i < MAX_PARENTS_FROM_SOURCE_FOLDER; i++) {
			if (androidManifestFile.exists()) {
				break;
			} else {
				if (projectRoot.getParentFile() != null) {
					projectRoot = projectRoot.getParentFile();
					androidManifestFile = new File(projectRoot, "AndroidManifest.xml");
				} else {
					break;
				}
			}
		}

		if (!androidManifestFile.exists()) {
			throw new IllegalStateException("Could not find the AndroidManifest.xml file, going up from path [" + sourcesGenerationFolder.getAbsolutePath() + "] found using dummy file [" + dummySourceFilePath + "] (max atempts: " + MAX_PARENTS_FROM_SOURCE_FOLDER + ")");
		} else {
			messager.printMessage(Kind.NOTE, "AndroidManifest.xml file found: " + androidManifestFile.toString());
		}

		return androidManifestFile;
	}

	private AndroidManifest parseThrowing(File androidManifestFile) throws Exception {

		DocumentBuilderFactory docBuilderFactory = DocumentBuilderFactory.newInstance();
		DocumentBuilder docBuilder = docBuilderFactory.newDocumentBuilder();
		Document doc = docBuilder.parse(androidManifestFile);

		Element documentElement = doc.getDocumentElement();
		documentElement.normalize();

		String applicationPackage = documentElement.getAttribute("package");

		NodeList applicationNodes = documentElement.getElementsByTagName("application");

		String applicationQualifiedName = null;

		if (applicationNodes.getLength() > 0) {
			Node applicationNode = applicationNodes.item(0);
			Node nameAttribute = applicationNode.getAttributes().getNamedItem("android:name");

			String qualifiedName = manifestNameToValidQualifiedName(applicationPackage, nameAttribute);

			if (qualifiedName != null) {
				applicationQualifiedName = qualifiedName;
			} else {
				Messager messager = processingEnv.getMessager();
				if (nameAttribute != null) {
					messager.printMessage(Kind.NOTE, String.format("The class application declared in the AndroidManifest.xml cannot be found in the compile path: [%s]", nameAttribute.getNodeValue()));
				}
			}
		}

		NodeList activityNodes = documentElement.getElementsByTagName("activity");

		List<String> activityQualifiedNames = new ArrayList<String>();

		for (int i = 0; i < activityNodes.getLength(); i++) {
			Node activityNode = activityNodes.item(i);
			Node nameAttribute = activityNode.getAttributes().getNamedItem("android:name");

			String qualifiedName = manifestNameToValidQualifiedName(applicationPackage, nameAttribute);

			if (qualifiedName != null) {
				activityQualifiedNames.add(qualifiedName);
			} else {
				Messager messager = processingEnv.getMessager();
				if (nameAttribute != null) {
					messager.printMessage(Kind.NOTE, String.format("A class activity declared in the AndroidManifest.xml cannot be found in the compile path: [%s]", nameAttribute.getNodeValue()));
				} else {
					messager.printMessage(Kind.NOTE, String.format("The %d activity node in the AndroidManifest.xml has no android:name attribute", i));
				}
			}
		}

		return new AndroidManifest(applicationPackage, applicationQualifiedName, activityQualifiedNames);
	}

	private String manifestNameToValidQualifiedName(String applicationPackage, Node nameAttribute) {
		if (nameAttribute != null) {
			String activityName = nameAttribute.getNodeValue();
			if (activityName.startsWith(applicationPackage)) {
				return returnClassIfExistsOrNull(activityName);
			} else {
				if (activityName.startsWith(".")) {
					return returnClassIfExistsOrNull(applicationPackage + activityName);
				} else {
					if (classOrModelClassExists(activityName)) {
						return activityName;
					} else {
						return returnClassIfExistsOrNull(applicationPackage + "." + activityName);
					}
				}
			}
		} else {
			return null;
		}
	}

	private boolean classOrModelClassExists(String className) {
		Elements elementUtils = processingEnv.getElementUtils();

		if (className.endsWith("_")) {
			className = className.substring(0, className.length() - 1);
		}
		return elementUtils.getTypeElement(className) != null;
	}

	private String returnClassIfExistsOrNull(String className) {
		if (classOrModelClassExists(className)) {
			return className;
		} else {
			return null;
		}
	}

}
