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
import com.netflix.spinnaker.orca.clouddriver.pipeline.servergroup.support.Location
import com.netflix.spinnaker.orca.clouddriver.tasks.DetermineHealthProvidersTask
import com.netflix.spinnaker.orca.clouddriver.tasks.MonitorKatoTask
import com.netflix.spinnaker.orca.clouddriver.tasks.cluster.AbstractClusterWideClouddriverTask
import com.netflix.spinnaker.orca.clouddriver.tasks.cluster.AbstractClusterWideClouddriverTask.ClusterSelection
import com.netflix.spinnaker.orca.clouddriver.tasks.cluster.AbstractWaitForClusterWideClouddriverTask
import com.netflix.spinnaker.orca.clouddriver.tasks.servergroup.ServerGroupCacheForceRefreshTask
import com.netflix.spinnaker.orca.clouddriver.utils.LockNameHelper
import com.netflix.spinnaker.orca.locks.LockableStageSupport
import com.netflix.spinnaker.orca.locks.LockingConfigurationProperties
import com.netflix.spinnaker.orca.pipeline.TaskNode
import com.netflix.spinnaker.orca.pipeline.model.Stage
import org.springframework.beans.factory.annotation.Autowired

import java.beans.Introspector

abstract class AbstractClusterWideClouddriverOperationStage implements LockableStageSupport {
  @Autowired LockingConfigurationProperties lockingConfiguration

  protected abstract Class<? extends AbstractClusterWideClouddriverTask> getClusterOperationTask()

  protected abstract Class<? extends AbstractWaitForClusterWideClouddriverTask> getWaitForTask();

  protected static String getStepName(String taskClassSimpleName) {
    if (taskClassSimpleName.endsWith("Task")) {
      return taskClassSimpleName.substring(0, taskClassSimpleName.length() - "Task".length());
    }
    return taskClassSimpleName;
  }

  public List<String> getLockNames(Stage stage) {
    final ClusterSelection cluster = stage.mapTo(AbstractClusterWideClouddriverTask.ClusterSelection.class)
    return getLocations(stage).collect { LockNameHelper.buildClusterLockName(cluster.cloudProvider, cluster.credentials, cluster.cluster, it.value) }
  }

  static List<Location> getLocations(Stage stage) {
   List<Location> locations =
    stage.context.namespaces ? stage.context.namespaces.collect { new Location(type: Location.Type.NAMESPACE, value: it) } :
     stage.context.regions ? stage.context.regions.collect { new Location(type: Location.Type.REGION, value: it) } :
       stage.context.zones ? stage.context.zones.collect { new Location(type: Location.Type.ZONE, value: it) } :
         stage.context.namespace ? [new Location(type: Location.Type.NAMESPACE, value: stage.context.namespace)] :
           stage.context.region ? [new Location(type: Location.Type.REGION, value: stage.context.region)] :
             []

    return locations
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
