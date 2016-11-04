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

package com.netflix.spinnaker.orca.config

import groovy.transform.CompileStatic
import org.apache.commons.pool2.impl.GenericObjectPool
import org.apache.commons.pool2.impl.GenericObjectPoolConfig
import org.springframework.boot.actuate.health.Health
import org.springframework.boot.actuate.health.HealthIndicator
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import redis.clients.jedis.Jedis
import redis.clients.jedis.JedisPool
import redis.clients.jedis.Protocol
import redis.clients.util.Pool

import java.lang.reflect.Field

@Configuration
@CompileStatic
@EnableConfigurationProperties(RedisConfigurationProperties)
class RedisConfiguration {

  @Bean
  @ConfigurationProperties('redis')
  GenericObjectPoolConfig redisPoolConfig() {
    new GenericObjectPoolConfig(maxTotal: 100, maxIdle: 100, minIdle: 25)
  }

  @Bean
  Pool<Jedis> jedisPool(RedisConfigurationProperties redisConfigurationProperties,
                        GenericObjectPoolConfig redisPoolConfig) {
    return createPool(redisPoolConfig, redisConfigurationProperties.connection, redisConfigurationProperties.timeout)
  }

  @Bean
  JedisPool jedisPoolPrevious(RedisConfigurationProperties redisConfigurationProperties) {
    if (redisConfigurationProperties.connection == redisConfigurationProperties.connectionPrevious || redisConfigurationProperties.connectionPrevious == null) {
      return null
    }

    return createPool(null, redisConfigurationProperties.connectionPrevious, redisConfigurationProperties.timeout)
  }

  @Bean
  HealthIndicator redisHealth(Pool<Jedis> jedisPool) {
    final Pool<Jedis> src = jedisPool
    final Field poolAccess = Pool.getDeclaredField('internalPool')
    poolAccess.setAccessible(true)
    new HealthIndicator() {
      @Override
      Health health() {
        Jedis jedis = null
        Health.Builder health = null
        try {
          jedis = src.getResource()
          if ('PONG'.equals(jedis.ping())) {
            health = Health.up()
          } else {
            health = Health.down()
          }
        } catch (Exception ex) {
          health = Health.down(ex)
        } finally {
          jedis?.close()
        }
        GenericObjectPool<Jedis> internal = (GenericObjectPool<Jedis>) poolAccess.get(jedisPool)
        health.withDetail('maxIdle', internal.maxIdle)
        health.withDetail('minIdle', internal.minIdle)
        health.withDetail('numActive', internal.numActive)
        health.withDetail('numIdle', internal.numIdle)
        health.withDetail('numWaiters', internal.numWaiters)

        return health.build()
      }
    }
  }

  private static JedisPool createPool(GenericObjectPoolConfig redisPoolConfig, String connection, int timeout) {
    URI redisConnection = URI.create(connection)

    String host = redisConnection.host
    int port = redisConnection.port == -1 ? Protocol.DEFAULT_PORT : redisConnection.port

    String redisConnectionPath = redisConnection.path ?: "/${Protocol.DEFAULT_DATABASE}"
    int database = Integer.parseInt(redisConnectionPath.split('/', 2)[1])

    String password = redisConnection.userInfo ? redisConnection.userInfo.split(':', 2)[1] : null

    return new JedisPool(redisPoolConfig ?: new GenericObjectPoolConfig(), host, port, timeout, password, database, null)
  }
}
