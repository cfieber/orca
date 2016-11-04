/*
 * Copyright 2015 Netflix, Inc.
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

package com.netflix.spinnaker.orca.igor.config

import com.fasterxml.jackson.databind.ObjectMapper
import com.netflix.spinnaker.orca.igor.IgorService
import com.netflix.spinnaker.orca.retrofit.RetrofitConfiguration
import com.netflix.spinnaker.retrofit.Slf4jRetrofitLogger
import groovy.transform.CompileStatic
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
@Import(RetrofitConfiguration)
@ConditionalOnProperty(value = 'igor.baseUrl')
@CompileStatic
@ComponentScan("com.netflix.spinnaker.orca.igor")
@EnableConfigurationProperties(IgorConfigurationProperties)
class IgorConfiguration {

  @Bean
  Endpoint igorEndpoint(IgorConfigurationProperties igorConfigurationProperties) {
    newFixedEndpoint(igorConfigurationProperties.baseUrl)
  }

  @Bean
  IgorService igorService(Endpoint igorEndpoint, ObjectMapper mapper, Client retrofitClient, RestAdapter.LogLevel retrofitLogLevel) {
    new RestAdapter.Builder()
      .setEndpoint(igorEndpoint)
      .setClient(retrofitClient)
      .setLogLevel(retrofitLogLevel)
      .setLog(new Slf4jRetrofitLogger(IgorService))
      .setConverter(new JacksonConverter(mapper))
      .build()
      .create(IgorService)
  }

}
