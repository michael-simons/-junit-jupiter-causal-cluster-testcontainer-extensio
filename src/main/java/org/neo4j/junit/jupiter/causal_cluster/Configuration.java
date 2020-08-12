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

import java.io.Serializable;
import java.time.Duration;
import java.util.Optional;

/**
 * The clusters configuration.
 *
 * @author Michael J. Simons
 */
final class Configuration implements Serializable {

	private final String neo4jVersion;

	private final int numberOfCoreServers;

	private final int numberOfReadReplicas;

	private final Duration startupTimeout;

	private final String password;

	private final int initialHeapSize;

	private final int pagecacheSize;

	/**
	 * Optional custom image name. Has precedence of the version number if set.
	 */
	private final String customImageName;

	Configuration(String neo4jVersion, int numberOfCoreServers, int numberOfReadReplicas,
		Duration startupTimeout, String password, int initialHeapSize, int pagecacheSize, String customImageName) {
		this.neo4jVersion = neo4jVersion;
		this.numberOfCoreServers = numberOfCoreServers;
		this.numberOfReadReplicas = numberOfReadReplicas;
		this.startupTimeout = startupTimeout;
		this.password = password;
		this.initialHeapSize = initialHeapSize;
		this.pagecacheSize = pagecacheSize;
		this.customImageName = customImageName;
	}

	String getImageName() {
		return Optional.ofNullable(customImageName).filter(s -> !s.isEmpty())
			.orElseGet(() -> String.format("neo4j:%s-enterprise", neo4jVersion));
	}

	public String getNeo4jVersion() {
		return this.neo4jVersion;
	}

	public int getNumberOfCoreServers() {
		return this.numberOfCoreServers;
	}

	public int getNumberOfReadReplicas() {
		return this.numberOfReadReplicas;
	}

	public Duration getStartupTimeout() {
		return this.startupTimeout;
	}

	public String getPassword() {
		return this.password;
	}

	public int getInitialHeapSize() {
		return this.initialHeapSize;
	}

	public int getPagecacheSize() {
		return this.pagecacheSize;
	}

	public String getCustomImageName() {
		return this.customImageName;
	}

	/**
	 * @return true if this configuration starts a Neo4j 3.5 cluster.
	 */
	boolean is35() {
		return getNeo4jVersion().startsWith("3.5");
	}

	boolean is41() {
		return getNeo4jVersion().startsWith("4.1");
	}

	public Configuration withNeo4jVersion(String newNeo4jVersion) {
		return this.neo4jVersion == newNeo4jVersion ?
			this :
			new Configuration(newNeo4jVersion, this.numberOfCoreServers, this.numberOfReadReplicas, this.startupTimeout,
				this.password, this.initialHeapSize, this.pagecacheSize, this.customImageName);
	}

	public Configuration withNumberOfCoreServers(int newNumberOfCoreServers) {
		return this.numberOfCoreServers == newNumberOfCoreServers ?
			this :
			new Configuration(this.neo4jVersion, newNumberOfCoreServers, this.numberOfReadReplicas, this.startupTimeout,
				this.password, this.initialHeapSize, this.pagecacheSize, this.customImageName);
	}

	public Configuration withNumberOfReadReplicas(int newNumberOfReadReplicas) {
		return this.numberOfReadReplicas == newNumberOfReadReplicas ?
			this :
			new Configuration(this.neo4jVersion, this.numberOfCoreServers, newNumberOfReadReplicas, this.startupTimeout,
				this.password, this.initialHeapSize, this.pagecacheSize, this.customImageName);
	}

	public Configuration withStartupTimeout(Duration newStartupTimeout) {
		return this.startupTimeout == newStartupTimeout ?
			this :
			new Configuration(this.neo4jVersion, this.numberOfCoreServers, this.numberOfReadReplicas, newStartupTimeout,
				this.password, this.initialHeapSize, this.pagecacheSize, this.customImageName);
	}

	public Configuration withPassword(String newPassword) {
		return this.password == newPassword ?
			this :
			new Configuration(this.neo4jVersion, this.numberOfCoreServers, this.numberOfReadReplicas,
				this.startupTimeout, newPassword, this.initialHeapSize, this.pagecacheSize, this.customImageName);
	}

	public Configuration withInitialHeapSize(int newInitialHeapSize) {
		return this.initialHeapSize == newInitialHeapSize ?
			this :
			new Configuration(this.neo4jVersion, this.numberOfCoreServers, this.numberOfReadReplicas,
				this.startupTimeout, this.password, newInitialHeapSize, this.pagecacheSize, this.customImageName);
	}

	public Configuration withPagecacheSize(int newPagecacheSize) {
		return this.pagecacheSize == newPagecacheSize ?
			this :
			new Configuration(this.neo4jVersion, this.numberOfCoreServers, this.numberOfReadReplicas,
				this.startupTimeout, this.password, this.initialHeapSize, newPagecacheSize, this.customImageName);
	}

	public Configuration withCustomImageName(String newCustomImageName) {
		return this.customImageName == newCustomImageName ?
			this :
			new Configuration(this.neo4jVersion, this.numberOfCoreServers, this.numberOfReadReplicas,
				this.startupTimeout, this.password, this.initialHeapSize, this.pagecacheSize, newCustomImageName);
	}

	public String toString() {
		return "Configuration(neo4jVersion=" + this.getNeo4jVersion() + ", numberOfCoreMembers=" + this
			.getNumberOfCoreServers() + ", numberOfReadReplicas=" + this.getNumberOfReadReplicas() + ", startupTimeout="
			+ this.getStartupTimeout() + ", password=" + this.getPassword() + ", initialHeapSize=" + this
			.getInitialHeapSize() + ", pagecacheSize=" + this.getPagecacheSize() + ", customImageName=" + this
			.getCustomImageName() + ")";
	}
}
