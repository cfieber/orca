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

package com.netflix.spinnaker.orca.clouddriver.pipeline.cluster;

import com.netflix.spinnaker.orca.clouddriver.tasks.DetermineHealthProvidersTask;
import com.netflix.spinnaker.orca.clouddriver.tasks.MonitorKatoTask;
import com.netflix.spinnaker.orca.clouddriver.tasks.cluster.AbstractClusterWideClouddriverTask;
import com.netflix.spinnaker.orca.clouddriver.tasks.cluster.AbstractWaitForClusterWideClouddriverTask;
import com.netflix.spinnaker.orca.clouddriver.tasks.servergroup.ServerGroupCacheForceRefreshTask;
import com.netflix.spinnaker.orca.pipeline.StageDefinitionBuilder;
import com.netflix.spinnaker.orca.pipeline.TaskNode;
import com.netflix.spinnaker.orca.pipeline.model.Stage;

import java.beans.Introspector;
import com.netflix.spinnaker.orca.clouddriver.pipeline.servergroup.support.Location;
import com.netflix.spinnaker.orca.clouddriver.tasks.DetermineHealthProvidersTask;
import com.netflix.spinnaker.orca.clouddriver.tasks.MonitorKatoTask;
import com.netflix.spinnaker.orca.clouddriver.tasks.cluster.AbstractClusterWideClouddriverTask;
import com.netflix.spinnaker.orca.clouddriver.tasks.cluster.AbstractClusterWideClouddriverTask.ClusterSelection;
import com.netflix.spinnaker.orca.clouddriver.tasks.cluster.AbstractWaitForClusterWideClouddriverTask;
import com.netflix.spinnaker.orca.clouddriver.tasks.servergroup.ServerGroupCacheForceRefreshTask;
import com.netflix.spinnaker.orca.clouddriver.utils.LockNameHelper;
import com.netflix.spinnaker.orca.locks.LockableStageSupport;
import com.netflix.spinnaker.orca.locks.LockingConfigurationProperties;
import com.netflix.spinnaker.orca.pipeline.TaskNode;
import com.netflix.spinnaker.orca.pipeline.model.Stage;
import org.springframework.beans.factory.annotation.Autowired;

import java.beans.Introspector;
import java.util.*;
import java.util.stream.Collectors;

import static java.util.Collections.emptyList;

abstract class AbstractClusterWideClouddriverOperationStage implements LockableStageSupport {
  @Autowired LockingConfigurationProperties lockingConfiguration;

  protected abstract Class<? extends AbstractClusterWideClouddriverTask> getClusterOperationTask();

  protected abstract Class<? extends AbstractWaitForClusterWideClouddriverTask> getWaitForTask();

  protected static String getStepName(String taskClassSimpleName) {
    if (taskClassSimpleName.endsWith("Task")) {
      return taskClassSimpleName.substring(0, taskClassSimpleName.length() - "Task".length());
    }
    return taskClassSimpleName;
  }

  public List<String> getLockNames(Stage stage) {
    final ClusterSelection cluster = stage.mapTo(AbstractClusterWideClouddriverTask.ClusterSelection.class);
    return getLocations(stage)
      .stream()
      .map(loc -> LockNameHelper.buildClusterLockName(cluster.getCloudProvider(), cluster.getCredentials(), cluster.getCluster(), loc.getValue()))
      .collect(Collectors.toList());
  }

  static List<Location> getLocations(Stage stage) {
    final Map<String, ?> context = stage.getContext();
    return
      getLocations(context, "namespaces", Location.Type.NAMESPACE)
        .orElseGet(() -> getLocations(context, "regions", Location.Type.REGION)
          .orElseGet(() -> getLocations(context, "zones", Location.Type.ZONE)
            .orElseGet(() -> getLocations(context, "namespace", Location.Type.NAMESPACE)
              .orElseGet(() -> getLocations(context, "region", Location.Type.REGION)
                .orElseGet(() -> getLocations(context, "zone", Location.Type.ZONE)
                  .orElse(emptyList()))))));
  }



  static Optional<List<Location>> getLocations(Map<String, ?> context, String key, Location.Type type) {
    if (!context.containsKey(key)) {
      return Optional.empty();
    }
    Object value = context.get("key");
    if (value instanceof String) {
      return Optional.of(Collections.singletonList(new Location(type, (String) value)));
    }
    if (value instanceof Collection) {
      return Optional.of(((Collection<String>) value).stream().map(l -> new Location(type, l)).collect(Collectors.toList()));
    }

    throw new IllegalStateException("unexpected value type " + value.getClass().getSimpleName() + " for location type " + type + ": " + value);
  }

  @Override
  public void taskGraph(Stage stage, TaskNode.Builder builder) {
    stage.resolveStrategyParams();
    Class<? extends AbstractClusterWideClouddriverTask> operationTask = getClusterOperationTask();
    String name = getStepName(operationTask.getSimpleName());
    String opName = Introspector.decapitalize(name);
    Class<? extends AbstractWaitForClusterWideClouddriverTask> waitTask = getWaitForTask();
    String waitName = Introspector.decapitalize(getStepName(waitTask.getSimpleName()));

    builder
      .withTask("determineHealthProviders", DetermineHealthProvidersTask.class)
      .withTask(opName, operationTask)
      .withTask("monitor" + name, MonitorKatoTask.class)
      .withTask("forceCacheRefresh", ServerGroupCacheForceRefreshTask.class)
      .withTask(waitName, waitTask)
      .withTask("forceCacheRefresh", ServerGroupCacheForceRefreshTask.class);
  }
}
