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

import akka.actor.{Actor, ActorLogging, Props}
import com.pi4j.io.gpio.{GpioFactory, PinState, RaspiPin}

import scala.concurrent.duration.FiniteDuration

/**
  * Raspberry Pi controller.
  *
  * @author Araik Grigoryan
  */
case class RaspberryPiController(errorPulseDuration: FiniteDuration) extends Actor with ActorLogging
{
  // Create GPIO controller
  val gpio = GpioFactory.getInstance()

  // Provision GPIO pin #01 as an output pin and turn off
  val successPin = gpio.provisionDigitalOutputPin(RaspiPin.GPIO_01, "Success", PinState.LOW)

  // Set shutdown state for this pin in case application terminates
  successPin.setShutdownOptions(true, PinState.LOW)

  // Provision GPIO pin #01 as an output pin and turn off
  val failureErrorPin = gpio.provisionDigitalOutputPin(RaspiPin.GPIO_02, "Failure/Error", PinState.LOW)

  // Set shutdown state for this pin in case application terminates
  failureErrorPin.setShutdownOptions(true, PinState.LOW)

  // Provision GPIO pin #01 as an output pin and turn off
  val connectionStatusPin = gpio.provisionDigitalOutputPin(RaspiPin.GPIO_03, "Connection Status", PinState.LOW)

  // Set shutdown state for this pin in case application terminates
  connectionStatusPin.setShutdownOptions(true, PinState.LOW)

  import RaspberryPiController.Protocol._

  override def receive: Receive =
  {
    case Connecting =>

      successPin.low()
      failureErrorPin.low()
      connectionStatusPin.blink(10, errorPulseDuration.toMillis)

    case BuildSuccess =>

      successPin.high()
      failureErrorPin.low()
      connectionStatusPin.low()

    case BuildFailure =>

      successPin.low()
      failureErrorPin.high()

    case BuildError =>

      successPin.low()
      failureErrorPin.blink(10, errorPulseDuration.toMillis)
  }

  override def postStop(): Unit =
  {
    gpio.shutdown()
  }
}

object RaspberryPiController
{
  def props(errorPulseDuration: FiniteDuration): Props = Props(RaspberryPiController(errorPulseDuration))

  object Protocol
  {
    case object Connecting

    case object BuildSuccess

    case object BuildFailure

    case object BuildError
  }
}
