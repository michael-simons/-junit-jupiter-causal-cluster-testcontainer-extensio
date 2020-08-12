/*
 * Copyright (c) 2019-2020 "Neo4j,"
 * Neo4j Sweden AB [https://neo4j.com]
 *
 * This file is part of Neo4j.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.neo4j.junit.jupiter.causal_cluster;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.net.URI;
import java.time.Duration;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.ExtensionConfigurationException;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.TestInstances;
import org.junit.platform.commons.support.AnnotationSupport;
import org.junit.platform.commons.support.HierarchyTraversalMode;

/**
 * Provides exactly one instance of {@link Neo4jCluster}.
 *
 * @author Michael J. Simons
 */
class CausalClusterExtension implements BeforeAllCallback {

	private static final ExtensionContext.Namespace NAMESPACE = ExtensionContext.Namespace
		.create(CausalClusterExtension.class);

	static final String DEFAULT_NEO4J_VERSION = "4.0";
	static final int DEFAULT_NUMBER_OF_CORE_SERVERS = 3;
	static final int DEFAULT_NUMBER_OF_READ_REPLICAS = 0;
	static final long DEFAULT_STARTUP_TIMEOUT_IN_MILLIS = 5 * 60 * 1_000;
	static final String DEFAULT_PASSWORD = "password";

	// Concisely very low, not yet configurable
	static final int DEFAULT_HEAP_SIZE_IN_MB = 10;
	static final int DEFAULT_PAGE_CACHE_IN_MB = 10;

	private static final String KEY_CONFIG = "config";
	private static final String KEY = "neo4j.causalCluster";

	private static final Class[] URI_FIELD_SUPPORTED_CLASSES = { URI.class, String.class, Collection.class,
		List.class, Neo4jCluster.class };

	private static final Configuration DEFAULT_CONFIGURATION = new Configuration(DEFAULT_NEO4J_VERSION,
		DEFAULT_NUMBER_OF_CORE_SERVERS,
		DEFAULT_NUMBER_OF_READ_REPLICAS, Duration.ofMillis(DEFAULT_STARTUP_TIMEOUT_IN_MILLIS), DEFAULT_PASSWORD,
		DEFAULT_HEAP_SIZE_IN_MB, DEFAULT_PAGE_CACHE_IN_MB, null);

	public void beforeAll(ExtensionContext context) {

		AnnotationSupport.findAnnotation(context.getRequiredTestClass(), NeedsCausalCluster.class)
			.ifPresent(annotation -> {
				final ExtensionContext.Store store = context.getStore(NAMESPACE);

				final Configuration configuration = DEFAULT_CONFIGURATION
					.withNeo4jVersion(annotation.neo4jVersion())
					.withNumberOfCoreServers(annotation.numberOfCoreServers())
					.withNumberOfReadReplicas(annotation.numberOfReadReplicas())
					.withStartupTimeout(Duration.ofMillis(annotation.startupTimeOutInMillis()))
					.withPassword(annotation.password())
					.withCustomImageName(annotation.customImageName());
				store.put(KEY_CONFIG, configuration);

				injectFields(context, context.getTestInstances().map(TestInstances::getInnermostInstance).orElse(null));
			});
	}

	private void injectFields(ExtensionContext context, Object testInstance) {

		Predicate<Field> selectedFields = field -> true;
		if (testInstance == null) {
			selectedFields = field -> Modifier.isStatic(field.getModifiers());
		}

		AnnotationSupport.findAnnotatedFields(context.getRequiredTestClass(), CausalCluster.class, selectedFields,
			HierarchyTraversalMode.TOP_DOWN).forEach(field -> {
			assertSupportedType(CausalCluster.class, "field", field.getType(), URI_FIELD_SUPPORTED_CLASSES);
			field.setAccessible(true);
			try {
				assertFieldIsNull(CausalCluster.class, testInstance, field);
				if (Collection.class.isAssignableFrom(field.getType())) {
					Type collectionType = ((ParameterizedType) field.getGenericType()).getActualTypeArguments()[0];
					assertSupportedType(CausalCluster.class, "Collection<> field", collectionType,
						String.class, URI.class);
					field.set(testInstance, getURIs(collectionType, context));
				} else {
					field.set(testInstance, getInjectableValue(field.getType(), context));
				}
			} catch (IllegalAccessException e) {
				throw new RuntimeException(e);
			}
		});
	}

	private static void assertFieldIsNull(Class<?> annotation, Object testInstance, Field field)
		throws IllegalAccessException {
		if (field.get(testInstance) != null) {
			throw new IllegalStateException(String.format(
				"Fields annotated with @%s must be null", annotation.getName()
			));
		}
	}

	private static void assertSupportedType(Class<?> annotation, String target, Type type,
		Class<?>... supportedTypes) {

		if (Arrays.stream(supportedTypes).noneMatch(t -> type == t)) {
			String typeName = type.getTypeName();
			throw getExtensionConfigurationException(annotation, target, typeName, supportedTypes);
		}
	}

	private static void assertSupportedType(Class<?> annotation, String target, Class<?> type,
		Class<?>... supportedTypes) {

		if (Arrays.stream(supportedTypes).noneMatch(t -> type == t)) {
			String typeName = type.getName();
			throw getExtensionConfigurationException(annotation, target, typeName, supportedTypes);
		}
	}

	private static ExtensionConfigurationException getExtensionConfigurationException(
		Class<?> annotation, String target, String typeName, Class<?>[] supportedTypes
	) {

		String supportedTypesString = Arrays.stream(supportedTypes)
			.map(Class::toString)
			.collect(Collectors.joining(" or "));

		String message = String.format(
			"Can only resolve @%s %s of type %s but was: %s",
			annotation.getSimpleName(),
			target,
			supportedTypesString,
			typeName);

		return new ExtensionConfigurationException(message);
	}

	private static Neo4jCluster getOrCreateCluster(ExtensionContext extensionContext) {

		ExtensionContext.Store store = extensionContext.getStore(NAMESPACE);
		Configuration configuration = store
			.getOrComputeIfAbsent(KEY_CONFIG, key -> DEFAULT_CONFIGURATION, Configuration.class);

		return extensionContext.getStore(NAMESPACE)
			.getOrComputeIfAbsent(KEY, key -> new ClusterFactory(configuration).createCluster(), Neo4jCluster.class);
	}

	private static Object getInjectableValue(Class<?> type, ExtensionContext extensionContext) {

		Neo4jCluster cluster = getOrCreateCluster(extensionContext);
		if (type == Neo4jCluster.class) {
			return cluster;
		} else {
			URI uri = cluster.getURI();
			return type == URI.class ? uri : uri.toString();
		}
	}

	private static Object getURIs(Type collectionType, ExtensionContext extensionContext) {

		Collection<URI> uris = getOrCreateCluster(extensionContext).getURIs();
		return collectionType == URI.class ? uris : uris.stream().map(URI::toString).collect(Collectors.toList());
	}
}
