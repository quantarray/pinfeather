/*
 * PinFeather
 * https://github.com/quantarray/pinfeather
 *
 * Copyright 2012-2016 Quantarray, LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.quantarray.pinfeather.monitor

import akka.actor.{Actor, ActorLogging, ActorRef, Props}
import org.apache.commons.codec.binary.Base64
import org.jetbrains.teamcity.rest.{Build, BuildConfigurationId, TeamCityInstance, TeamCityInstanceImpl}

import scala.concurrent.duration.{DurationDouble, FiniteDuration}

/**
  * TeamCity monitor.
  *
  * @author Araik Grigoryan
  */
case class TeamCityMonitor(hostUrl: String, userName: String, password: String,
                           buildConfigIds: Seq[BuildConfigurationId], buildStatusFrequency: FiniteDuration) extends Actor with ActorLogging
{

  import context.dispatcher
  import TeamCityMonitor.Protocol._

  val authorization = Base64.encodeBase64String(s"$userName:$password".toCharArray.map(_.toByte))

  private case object TaskTick

  val task = context.system.scheduler.schedule(buildStatusFrequency, buildStatusFrequency, self, TaskTick)

  private var buildUpdateTargets = Map[ActorRef, ActorRef]()

  private var teamCity: Option[TeamCityInstance] = None

  override def receive: Receive =
  {
    case TaskTick =>

      if (teamCity.isDefined)
      {
        try
        {
          val builds = teamCity.get.builds()

          val latestBuilds = buildConfigIds.map
          {
            bci => (bci, builds.fromConfiguration(bci).withAnyStatus().latest())
          }

          sendToTargets(LatestBuildsUpdate(latestBuilds))
        }
        catch
        {
          case t: Throwable =>

            teamCity = None

            sendToTargets(Connecting)
        }
      }
      else
      {

        try
        {
          teamCity = Some(new TeamCityInstanceImpl(hostUrl, "httpAuth", authorization, false))
        }
        catch
        {
          case t: Throwable =>

            sendToTargets(Connecting)
        }
      }

    case SubscribeBuildUpdates(target) => buildUpdateTargets += (target -> target)
  }

  override def postStop(): Unit =
  {
    task.cancel()
  }

  private def sendToTargets(message: Any): Unit =
  {
    buildUpdateTargets.keys.foreach
    {
      target =>

        target ! message
    }
  }
}

object TeamCityMonitor
{
  def props(hostUrl: String, userName: String, password: String, buildConfigIds: Seq[BuildConfigurationId], buildStatusFrequency: FiniteDuration = 3.seconds): Props =
    Props(new TeamCityMonitor(hostUrl, userName, password, buildConfigIds, buildStatusFrequency))

  object Protocol
  {
    case class SubscribeBuildUpdates(target: ActorRef)

    case object Connecting

    case class LatestBuildsUpdate(latestBuilds: Seq[(BuildConfigurationId, Build)])

    case class BuildUpdate(buildConfigId: BuildConfigurationId, build: Build)
  }
}


