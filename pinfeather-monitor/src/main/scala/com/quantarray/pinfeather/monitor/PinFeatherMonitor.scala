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

import java.io.FileInputStream
import java.util.Properties

import akka.actor.SupervisorStrategy.{Restart, Stop}
import akka.actor.{Actor, ActorInitializationException, ActorLogging, ActorRef, ActorSystem, DeathPactException, OneForOneStrategy, Props, SupervisorStrategy}
import com.typesafe.config.ConfigFactory
import org.jetbrains.teamcity.rest.{BuildConfigurationId, BuildStatus}

import scala.collection.JavaConversions._
import scala.concurrent.duration.DurationInt

/**
  * PinFeather monitor.
  *
  * Uses TeamCityMonitor to obtain build status for configured projects and RaspberryPiController to set LED color state.
  *
  * @author Araik Grigoryan
  */
case class PinFeatherMonitor(monitorProps: Props) extends Actor with ActorLogging
{

  import context.dispatcher

  val tcMonitor = context.watch(context.actorOf(monitorProps, "TeamCityMonitor"))

  val osName = System.getProperty("os.name")

  val osArch = System.getProperty("os.arch")

  val isrPi = osName == "Linux" && osArch == "arm" // TODO: Is there a better Raspberry Pi identity?

  val rpiControllerProps =
    if (isrPi)
      RaspberryPiController.props(10.seconds)
    else
      RaspberryPiMockController.props

  val rpiController = context.watch(context.actorOf(rpiControllerProps))

  val subscribeBuildUpdatesTask = context.system.scheduler.schedule(1.second, 10.seconds, tcMonitor, TeamCityMonitor.Protocol.SubscribeBuildUpdates(self))

  override def supervisorStrategy: SupervisorStrategy = OneForOneStrategy()
  {
    case _: ActorInitializationException => Stop
    case _: DeathPactException => Stop
    case _: Exception => Restart
  }

  override def receive: Receive =
  {
    case TeamCityMonitor.Protocol.Connecting =>

      log.info("Connecting...")

      rpiController ! RaspberryPiController.Protocol.Connecting

    case TeamCityMonitor.Protocol.LatestBuildsUpdate(latestBuilds) =>

      log.info(s"$latestBuilds")

      import BuildStatus._

      val latestBuildsStatus = latestBuilds.map(_._2.getStatus).reduce
      {
        (x, y) =>

          (x, y) match
          {
            case (SUCCESS, _) => y
            case (FAILURE, ERROR) => ERROR
            case (FAILURE, _) => FAILURE
            case (ERROR, _) => ERROR
          }
      }

      latestBuildsStatus match
      {
        case SUCCESS => rpiController ! RaspberryPiController.Protocol.BuildSuccess
        case FAILURE => rpiController ! RaspberryPiController.Protocol.BuildFailure
        case ERROR => rpiController ! RaspberryPiController.Protocol.BuildError
      }

    case _ =>
  }

  override def postStop(): Unit =
  {
    subscribeBuildUpdatesTask.cancel()
  }
}

object PinFeatherMonitor
{
  def main(args: Array[String]): Unit =
  {
    start("PinFeatherMonitorSystem", "pinFeather")
  }

  def start(systemName: String, role: String): ActorRef =
  {
    val baseConfig = ConfigFactory.load

    val config = baseConfig.getConfig(role).withFallback(baseConfig)

    val teamCityConfig = config.getConfig("teamCity")

    val hostUrl = teamCityConfig.getString("hostUrl")
    val authFilePath = teamCityConfig.getString("authFilePath")
    val buildConfigIds = teamCityConfig.getStringList("buildConfigIds").map(new BuildConfigurationId(_))

    val httpAuthProperties = new Properties()
    httpAuthProperties.load(new FileInputStream(authFilePath))

    val method = httpAuthProperties.getProperty("method")
    val userName = Option(httpAuthProperties.getProperty("userName"))
    val password = Option(httpAuthProperties.getProperty("password"))

    if (method == "httpAuth")
    {
      if (userName.isDefined && password.isDefined)
      {
        val monitorProps = TeamCityMonitor.props(hostUrl, userName.get, password.get, buildConfigIds)

        val pinFeatherMonitorProps = Props(PinFeatherMonitor(monitorProps))

        val system = ActorSystem(systemName, config)

        system.actorOf(pinFeatherMonitorProps)
      }
      else if (userName.isEmpty)
      {
        throw new Exception(s"userName property is required in $authFilePath.")
      }
      else
      {
        throw new Exception(s"password property is required in $authFilePath.")
      }
    }
    else
    {
      throw new Exception(s"method property with value $method is not supported in $authFilePath.")
    }
  }
}

