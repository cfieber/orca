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


package com.netflix.spinnaker.orca.retrofit

import com.netflix.spinnaker.config.OkHttpClientConfiguration
import com.netflix.spinnaker.orca.retrofit.exceptions.RetrofitExceptionHandler
import com.squareup.okhttp.Interceptor
import com.squareup.okhttp.Response
import groovy.transform.CompileStatic
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.beans.factory.config.ConfigurableBeanFactory
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Import
import org.springframework.context.annotation.Scope
import org.springframework.core.Ordered
import org.springframework.core.annotation.Order
import org.springframework.core.env.Environment
import retrofit.client.OkClient

@Configuration
@CompileStatic
@Import(OkHttpClientConfiguration)
@EnableConfigurationProperties
class RetrofitConfiguration {

   @Bean(name = ["retrofitClient", "okClient"])
   @Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
   OkClient retrofitClient(@Qualifier("okHttpClientConfiguration") OkHttpClientConfiguration okHttpClientConfig, Environment environment) {
     final String userAgent = "Spinnaker-${environment.getProperty('spring.application.name', 'unknown')}/${getClass().getPackage().implementationVersion ?: '1.0'}"
     def cfg = okHttpClientConfig.create()
     cfg.networkInterceptors().add(new Interceptor() {
       @Override
       Response intercept(Interceptor.Chain chain) throws IOException {
         def req = chain.request().newBuilder().header('User-Agent', userAgent).build()
         chain.proceed(req)
       }
     })

     new OkClient(cfg)
  }

  @Bean @Order(Ordered.HIGHEST_PRECEDENCE) RetrofitExceptionHandler retrofitExceptionHandler() {
    new RetrofitExceptionHandler()
  }

}
