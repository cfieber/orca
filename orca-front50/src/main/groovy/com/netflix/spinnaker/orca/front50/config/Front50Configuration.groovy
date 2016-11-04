/*
 * Copyright 2014 Netflix, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.netflix.spinnaker.orca.front50.config

import com.fasterxml.jackson.databind.ObjectMapper
import com.netflix.spinnaker.orca.front50.DependentPipelineStarter
import com.netflix.spinnaker.orca.front50.Front50Service
import com.netflix.spinnaker.orca.front50.spring.DependentPipelineExecutionListener
import com.netflix.spinnaker.orca.retrofit.RetrofitConfiguration
import com.netflix.spinnaker.retrofit.Slf4jRetrofitLogger
import groovy.transform.CompileStatic
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.ComponentScan
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Import
import retrofit.Endpoint
import retrofit.RequestInterceptor
import retrofit.RestAdapter
import retrofit.client.Client
import retrofit.converter.JacksonConverter
import static retrofit.Endpoints.newFixedEndpoint

@Configuration
@Import(RetrofitConfiguration)
@ComponentScan([
  "com.netflix.spinnaker.orca.front50.pipeline",
  "com.netflix.spinnaker.orca.front50.tasks",
  "com.netflix.spinnaker.orca.front50"
])
@CompileStatic
@EnableConfigurationProperties(Front50ConfigurationProperties)
class Front50Configuration {

  @Bean
  Endpoint front50Endpoint(Front50ConfigurationProperties front50ConfigurationProperties) {
    newFixedEndpoint(front50ConfigurationProperties.baseUrl)
  }

  @Bean
  Front50Service front50Service(Endpoint front50Endpoint,
                                ObjectMapper mapper,
                                Client retrofitClient,
                                RestAdapter.LogLevel retrofitLogLevel,
                                RequestInterceptor spinnakerRequestInterceptor) {
    new RestAdapter.Builder()
      .setRequestInterceptor(spinnakerRequestInterceptor)
      .setEndpoint(front50Endpoint)
      .setClient(retrofitClient)
      .setLogLevel(retrofitLogLevel)
      .setLog(new Slf4jRetrofitLogger(Front50Service))
      .setConverter(new JacksonConverter(mapper))
      .build()
      .create(Front50Service)
  }

  @Bean
  DependentPipelineExecutionListener dependentPipelineExecutionListener(
    Front50Service front50Service,
    DependentPipelineStarter dependentPipelineStarter
  ) {
    new DependentPipelineExecutionListener(front50Service, dependentPipelineStarter)
  }
}
