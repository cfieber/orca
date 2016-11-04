/*
 * Copyright 2016 Netflix, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License")
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.netflix.spinnaker.orca.pipeline.persistence.jedis

import groovy.transform.Canonical
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.boot.context.properties.NestedConfigurationProperty
import org.springframework.stereotype.Component

@Canonical
@Component
@ConfigurationProperties
class ExecutionRepositoryConfigurationProperties {
  @Canonical
  static class ThreadPoolProperties {
    int executionRepository = 150
  }

  @Canonical
  static class ChunkSizeProperties {
    int executionRepository = 75
  }

  @NestedConfigurationProperty
  ThreadPoolProperties threadPool = new ThreadPoolProperties()

  @NestedConfigurationProperty
  ChunkSizeProperties chunkSize = new ChunkSizeProperties()
}
