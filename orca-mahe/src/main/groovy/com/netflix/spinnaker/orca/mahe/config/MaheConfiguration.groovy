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

package com.netflix.spinnaker.orca.mahe.config

import com.fasterxml.jackson.databind.ObjectMapper
import com.netflix.spinnaker.orca.mahe.MaheService
import com.netflix.spinnaker.orca.retrofit.RetrofitConfiguration
import com.netflix.spinnaker.retrofit.Slf4jRetrofitLogger
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.ComponentScan
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Import
import retrofit.Endpoint
import retrofit.RestAdapter
import retrofit.client.Client
import retrofit.converter.JacksonConverter
import static retrofit.Endpoints.newFixedEndpoint

@Configuration
@Import([RetrofitConfiguration])
@ComponentScan([
  "com.netflix.spinnaker.orca.mahe.pipeline",
  "com.netflix.spinnaker.orca.mahe.tasks",
  "com.netflix.spinnaker.orca.mahe.cleanup"
])
@ConditionalOnProperty(value = 'mahe.baseUrl')
@EnableConfigurationProperties(MaheConfigurationProperties)
class MaheConfiguration {

  @Bean
  Endpoint maheEndpoint(MaheConfigurationProperties maheConfigurationProperties) {
    newFixedEndpoint(maheConfigurationProperties.baseUrl)
  }

  @Bean
  MaheService maheService(Endpoint maheEndpoint, ObjectMapper objectMapper, Client retrofitClient, RestAdapter.LogLevel retrofitLogLevel) {
    new RestAdapter.Builder()
      .setEndpoint(maheEndpoint)
      .setClient(retrofitClient)
      .setLogLevel(retrofitLogLevel)
      .setLog(new Slf4jRetrofitLogger(MaheService))
      .setConverter(new JacksonConverter(objectMapper))
      .build()
      .create(MaheService)
  }
}
